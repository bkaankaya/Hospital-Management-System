package db;

import com.sun.net.httpserver.*;
import javax.net.ssl.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;

/**
 * Trusted, explicitly deployed JDK 11 HTTPS boundary. Only this process
 * receives MySQL connection credentials and medical encryption keys.
 *
 * Every action uses the role and ID stored by the server, never client-supplied
 * identity fields. No arbitrary SQL, file access or reflection endpoints.
 */
public final class GatewayServer {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int MAX_REQUEST = 16384;
    private static final int MAX_RESPONSE = 65536;
    private static final long SESSION_IDLE_MS = 15 * 60_000L;
    private static final long SESSION_ABSOLUTE_MS = 8 * 60 * 60_000L;
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final java.util.concurrent.atomic.AtomicInteger adminFailures =
            new java.util.concurrent.atomic.AtomicInteger();
    private volatile long adminLockedUntil;

    private static final class Session {
        final GatewayPolicy.Role role;
        final int id;
        final long created;
        volatile long lastUsed;
        Session(GatewayPolicy.Role role, int id) {
            this.role = role; this.id = id;
            this.created = System.currentTimeMillis();
            this.lastUsed = created;
        }
        boolean expired() {
            long now = System.currentTimeMillis();
            return now - lastUsed > SESSION_IDLE_MS || now - created > SESSION_ABSOLUTE_MS;
        }
    }

    private GatewayServer() { }

    private static String envRequired(String name) {
        String value = System.getenv(name);
        if (value == null || value.isEmpty()) throw new IllegalStateException(name + " is required");
        return value;
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("hospital.gateway.server", "true");
        if (args.length != 0) throw new IllegalArgumentException("Configure using protected environment variables");
        String host = System.getenv().getOrDefault("HOSPITAL_SERVER_BIND", "127.0.0.1");
        if (!"127.0.0.1".equals(host) && !"::1".equals(host)
                && !"true".equals(System.getenv("HOSPITAL_SERVER_ALLOW_REMOTE"))) {
            throw new IllegalStateException("Remote bind requires explicit approval");
        }
        if ("root".equalsIgnoreCase(envRequired("HOSPITAL_DB_USER")))
            throw new IllegalStateException("Gateway refuses MySQL root credentials");
        envRequired("HOSPITAL_DB_PASSWORD");
        envRequired("HOSPITAL_DB_URL");
        // New medical writes use versioned keys stored on the trusted host.
        envRequired("HOSPITAL_DATA_KEYRING_DIR");
        envRequired("HOSPITAL_DATA_ACTIVE_KEY_ID");
        // Fail immediately if the database is not accessible or wrongly configured.
        try (Connection connection = DatabaseManager.getConnection()) {
            if (!connection.isValid(5)) throw new SQLException("Database connection unavailable");
        }
        SSLContext sslContext = sslContext();
        int port = Integer.parseInt(System.getenv().getOrDefault("HOSPITAL_SERVER_PORT", "8443"));
        HttpsServer server = HttpsServer.create(new InetSocketAddress(host, port), 64);
        server.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
            @Override public void configure(HttpsParameters parameters) {
                SSLParameters config = sslContext.getDefaultSSLParameters();
                config.setProtocols(new String[]{"TLSv1.3", "TLSv1.2"});
                parameters.setSSLParameters(config);
            }
        });
        GatewayServer gateway = new GatewayServer();
        server.createContext("/api", gateway::handle);
        server.setExecutor(Executors.newFixedThreadPool(8, runnable -> {
            Thread thread = new Thread(runnable, "hospital-gateway-request");
            thread.setDaemon(true);
            return thread;
        }));
        server.start();
        System.out.println("Hospital HTTPS gateway listening on " + host + ":" + port
                + " (no patient or credential details are logged)");
    }

    private static SSLContext sslContext() throws Exception {
        char[] secret = envRequired("HOSPITAL_SERVER_KEYSTORE_PASSWORD").toCharArray();
        try {
            KeyStore store = KeyStore.getInstance("PKCS12");
            try (InputStream input = Files.newInputStream(Path.of(envRequired("HOSPITAL_SERVER_KEYSTORE")))) {
                store.load(input, secret);
            }
            KeyManagerFactory factory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            factory.init(store, secret);
            SSLContext ssl = SSLContext.getInstance("TLS");
            ssl.init(factory.getKeyManagers(), null, new SecureRandom());
            return ssl;
        } finally { Arrays.fill(secret, '\0'); }
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
            if (!"POST".equals(exchange.getRequestMethod())
                    || !"/api".equals(exchange.getRequestURI().getPath())) {
                respond(exchange, 404, false, Collections.emptyList());
                return;
            }
            if (!"application/octet-stream".equalsIgnoreCase(
                    exchange.getRequestHeaders().getFirst("Content-Type"))
                    || exchange.getRequestHeaders().getFirst("Origin") != null) {
                respond(exchange, 400, false, Collections.emptyList());
                return;
            }
            byte[] input = exchange.getRequestBody().readNBytes(MAX_REQUEST + 1);
            if (input.length > MAX_REQUEST) {
                respond(exchange, 413, false, Collections.emptyList());
                return;
            }
            String operation;
            String[] values;
            try (DataInputStream data = new DataInputStream(new ByteArrayInputStream(input))) {
                operation = data.readUTF();
                int n = data.readInt();
                if (operation.length() > 40 || n < 0 || n > 12)
                    throw new IOException("Invalid protocol header");
                values = new String[n];
                for (int i = 0; i < n; i++) {
                    values[i] = data.readUTF();
                    if (values[i].length() > 8192) throw new IOException("Input field too long");
                }
                if (data.available() != 0) throw new IOException("Trailing request data");
            } catch (IOException ex) {
                respond(exchange, 400, false, Collections.emptyList());
                return;
            }

            if (operation.startsWith("LOGIN_")) {
                if (exchange.getRequestHeaders().getFirst("Authorization") != null) {
                    respond(exchange, 403, false, Collections.emptyList()); return;
                }
                String[] auth = login(operation, values);
                if (auth == null) respond(exchange, 401, false, Collections.emptyList());
                else respond(exchange, 200, true, Collections.singletonList(auth));
                return;
            }
            String authorization = exchange.getRequestHeaders().getFirst("Authorization");
            if (authorization == null || !authorization.startsWith("Bearer ")
                    || authorization.length() > 110) {
                respond(exchange, 401, false, Collections.emptyList()); return;
            }
            String id = digest(authorization.substring(7));
            Session session = sessions.get(id);
            if (session == null || session.expired() || !activeAccount(session)) {
                sessions.remove(id);
                respond(exchange, 401, false, Collections.emptyList()); return;
            }
            if ("LOGOUT".equals(operation)) {
                sessions.remove(id);
                respond(exchange, 200, true, Collections.emptyList());
                return;
            }
            if (!GatewayPolicy.allowed(session.role, operation)) {
                respond(exchange, 403, false, Collections.emptyList()); return;
            }
            session.lastUsed = System.currentTimeMillis();
            List<String[]> answer = execute(session, operation, values);
            respond(exchange, 200, true, answer);
        } catch (RuntimeException | SQLException ex) {
            // No SQL, passwords, identifiers, clinical data or exception details in responses.
            respond(exchange, 500, false, Collections.emptyList());
        } finally {
            exchange.close();
        }
    }

    private static void respond(HttpExchange exchange, int code, boolean success,
                                List<String[]> values) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream data = new DataOutputStream(bytes)) {
            data.writeBoolean(success);
            data.writeInt(values.size());
            for (String[] row : values) {
                if (row.length > 12) throw new IOException("Too many response fields");
                data.writeInt(row.length);
                for (String value : row) data.writeUTF(value == null ? "" : value);
            }
        }
        byte[] payload = bytes.toByteArray();
        if (payload.length > MAX_RESPONSE) {
            payload = new byte[]{0, 0, 0, 0, 0};
            code = 500;
        }
        exchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
        exchange.sendResponseHeaders(code, payload.length);
        try (OutputStream output = exchange.getResponseBody()) { output.write(payload); }
    }

    private static String digest(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    private static String token() {
        byte[] raw = new byte[32];
        RANDOM.nextBytes(raw);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
    }

    private String[] login(String operation, String[] args) {
        if (args.length != 2 || args[0].length() > 100 || args[1].length() > 256) return null;
        GatewayPolicy.Role role;
        if ("LOGIN_PATIENT".equals(operation)) role = GatewayPolicy.Role.PATIENT;
        else if ("LOGIN_DOCTOR".equals(operation)) role = GatewayPolicy.Role.DOCTOR;
        else if ("LOGIN_ADMIN".equals(operation)) role = GatewayPolicy.Role.ADMIN;
        else return null;
        int internalId;
        char[] password = args[1].toCharArray();
        try {
            if (role == GatewayPolicy.Role.ADMIN) {
                if (System.currentTimeMillis() < adminLockedUntil) return null;
                String secret = System.getenv("HOSPITAL_ADMIN_PASSWORD");
                if (secret == null || secret.length() < 12) return null;
                byte[] expected = secret.getBytes(StandardCharsets.UTF_8);
                byte[] supplied = args[1].getBytes(StandardCharsets.UTF_8);
                boolean accepted = MessageDigest.isEqual(expected, supplied);
                Arrays.fill(expected, (byte) 0);
                Arrays.fill(supplied, (byte) 0);
                if (!accepted) {
                    if (adminFailures.incrementAndGet() >= 5) {
                        adminLockedUntil = System.currentTimeMillis() + 15 * 60_000L;
                        adminFailures.set(0);
                    }
                    return null;
                }
                adminFailures.set(0);
                internalId = 0;
            } else {
                internalId = SecureAccounts.authenticate(role == GatewayPolicy.Role.PATIENT
                        ? SecureAccounts.Kind.PATIENT : SecureAccounts.Kind.DOCTOR,
                        args[0], password);
                if (internalId <= 0) return null;
            }
        } finally { Arrays.fill(password, '\0'); }
        if (sessions.size() >= 1000) sessions.entrySet().removeIf(e -> e.getValue().expired());
        if (sessions.size() >= 1000) return null;
        String bearer = token();
        sessions.put(digest(bearer), new Session(role, internalId));
        return new String[]{String.valueOf(internalId), bearer};
    }

    private static boolean activeAccount(Session session) {
        if (session.role == GatewayPolicy.Role.ADMIN) return true;
        String table = session.role == GatewayPolicy.Role.PATIENT ? "Patient" : "Doctor";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement check = con.prepareStatement(
                     "SELECT is_active FROM " + table + " WHERE SSN=? AND password_hash IS NOT NULL")) {
            check.setInt(1, session.id);
            try (ResultSet rs = check.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }
        } catch (SQLException ex) { return false; }
    }

    private static void fields(String[] fields, int expected) {
        if (fields.length != expected) throw new IllegalArgumentException("Invalid request shape");
    }
    private static int positive(String value) {
        int result = Integer.parseInt(value);
        if (result <= 0) throw new IllegalArgumentException("Invalid internal ID");
        return result;
    }
    private static String clean(String value, int max) {
        String normalized = value.trim();
        if (normalized.isEmpty() || normalized.length() > max)
            throw new IllegalArgumentException("Invalid field length");
        return normalized;
    }
    private static List<String[]> single(boolean ok) {
        return Collections.singletonList(new String[]{ok ? "OK" : "NO"});
    }
    private static List<String[]> rows(List<String[]> values) {
        if (values == null) throw new IllegalStateException("Database result unavailable");
        return values.size() > 100 ? new ArrayList<>(values.subList(0, 100)) : values;
    }
    private static List<String[]> column(List<String> items) {
        if (items == null) throw new IllegalStateException("Database result unavailable");
        List<String[]> result = new ArrayList<>();
        for (String item : items) {
            if (result.size() == 100) break;
            result.add(new String[]{item});
        }
        return result;
    }

    private static List<String[]> execute(Session session, String operation, String[] args)
            throws SQLException {
        switch (operation) {
            case "DEPARTMENTS":
                fields(args, 0);
                return column(new DepartmentDAO().getDepartmentList());
            case "DOCTORS":
                fields(args, 1);
                return column(new DoctorDAO().getDoctorList(clean(args[0], 80)));
            case "DOCTOR_ID":
                fields(args, 1);
                return Collections.singletonList(new String[]{
                        String.valueOf(new DoctorDAO().getDoctorSsn(clean(args[0], 220)))});
            case "SLOTS":
                fields(args, 2);
                return column(new AppointmentDAO().getTimeList(positive(args[0]),
                        clean(args[1], 10)));
            case "BOOK":
                fields(args, 3);
                return single(new AppointmentDAO().bookAppointment(
                        new Appointment(clean(args[1], 10), clean(args[2], 8),
                                session.id, positive(args[0]))));
            case "PATIENT_SCHEDULE":
                fields(args, 0);
                return rows(new AppointmentDAO().getPatientUpcomingAppointments(session.id));
            case "DOCTOR_SCHEDULE":
                fields(args, 0);
                return rows(new AppointmentDAO().getDoctorUpcomingAppointments(session.id));
            case "DOCTOR_CANCEL":
                fields(args, 1);
                return single(new AppointmentDAO().cancelDoctorAppointment(
                        positive(args[0]), session.id));
            case "ADDRESS":
                fields(args, 1);
                String address = clean(args[0], 500);
                return single(session.role == GatewayPolicy.Role.PATIENT
                        ? new PatientDAO().tryUpdateAddress(session.id, address)
                        : new DoctorDAO().tryUpdateAddress(session.id, address));
            case "REGISTER_PATIENT":
                fields(args, 8);
                char[] patientPassword = args[7].toCharArray();
                try {
                    SecurityManager.requireStrongPassword(patientPassword);
                    return single(SecureAccounts.registerPatient(
                            new Patient(positive(args[0]), clean(args[1], 100),
                                    clean(args[2], 100), clean(args[3], 20),
                                    clean(args[4], 500), clean(args[5], 10)),
                            clean(args[6], 11), patientPassword));
                } finally { Arrays.fill(patientPassword, '\0'); }
            case "REGISTER_DOCTOR":
                fields(args, 9);
                char[] doctorPassword = args[8].toCharArray();
                try {
                    SecurityManager.requireStrongPassword(doctorPassword);
                    return single(SecureAccounts.registerDoctor(
                            new Doctor(positive(args[0]), clean(args[1], 100),
                                    clean(args[2], 100), clean(args[3], 20),
                                    clean(args[4], 500), clean(args[5], 10),
                                    clean(args[6], 80)),
                            clean(args[7], 11), doctorPassword));
                } finally { Arrays.fill(doctorPassword, '\0'); }
            case "PATIENT_SEARCH":
                fields(args, 1);
                List<ArrayList<String>> matches =
                        new PatientDAO().patientNameLike(clean(args[0], 100));
                if (matches == null) throw new IllegalStateException("Search failed");
                List<String[]> found = new ArrayList<>();
                for (List<String> row : matches) {
                    if (found.size() >= 100) break;
                    found.add(row.toArray(new String[0]));
                }
                return found;
            case "MED_STATS":
                fields(args, 1);
                return column(new MedicineDAO().getStatistics(clean(args[0], 80)));
            default:
                throw new IllegalArgumentException("Unsupported gateway operation");
        }
    }
}
