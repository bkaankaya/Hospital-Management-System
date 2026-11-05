package db;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Desktop-only HTTPS transport. Does not contain JDBC credentials or an
 * application-wide administrator token. Trust is provided by the JVM truststore;
 * it never installs an insecure TrustManager.
 */
public final class GatewayClient {
    private static final GatewayClient INSTANCE = new GatewayClient();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NEVER).build();
    private volatile String token;

    private GatewayClient() { }
    public static GatewayClient get() { return INSTANCE; }

    public static boolean useGateway() {
        if ("true".equals(System.getProperty("hospital.ci.legacyFixtures"))) return false;
        if (System.getenv("HOSPITAL_GATEWAY_URL") == null)
            throw new IllegalStateException("HOSPITAL_GATEWAY_URL is required for desktop operation");
        return true;
    }

    private URI endpoint() throws IOException {
        String configured = System.getenv("HOSPITAL_GATEWAY_URL");
        if (configured == null) throw new IOException("Gateway is not configured");
        final URI uri;
        try { uri = URI.create(configured); }
        catch (IllegalArgumentException e) { throw new IOException("Invalid gateway endpoint"); }
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                || uri.getUserInfo() != null || uri.getFragment() != null
                || uri.getQuery() != null || (uri.getPath() != null && !uri.getPath().isEmpty()
                && !"/api".equals(uri.getPath()))) {
            throw new IOException("Gateway must be an HTTPS /api endpoint");
        }
        return URI.create(uri.toString().replaceAll("/$", "") +
                (uri.getPath() == null || uri.getPath().isEmpty() ? "/api" : ""));
    }

    private List<String[]> send(String bearer, String operation, String... args) throws IOException {
        if (args.length > 12) throw new IOException("Request has too many fields");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeUTF(operation);
            output.writeInt(args.length);
            for (String arg : args) {
                if (arg == null || arg.length() > 8192) throw new IOException("Invalid request field");
                output.writeUTF(arg);
            }
        }
        HttpRequest.Builder request = HttpRequest.newBuilder(endpoint())
                .timeout(Duration.ofSeconds(35))
                .header("Content-Type", "application/octet-stream")
                .header("Cache-Control", "no-store")
                .POST(HttpRequest.BodyPublishers.ofByteArray(bytes.toByteArray()));
        if (bearer != null) request.header("Authorization", "Bearer " + bearer);
        final HttpResponse<byte[]> response;
        try { response = http.send(request.build(), HttpResponse.BodyHandlers.ofByteArray()); }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("Gateway request interrupted", ex);
        }
        if (response.statusCode() != 200 || response.body().length > 65536)
            throw new IOException("Gateway request rejected or unavailable: HTTP " + response.statusCode());
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(response.body()))) {
            if (!input.readBoolean()) throw new IOException("Gateway request denied");
            int rows = input.readInt();
            if (rows < 0 || rows > 100) throw new IOException("Invalid response");
            List<String[]> values = new ArrayList<>();
            for (int i=0;i<rows;i++) {
                int columns = input.readInt();
                if (columns < 0 || columns > 12) throw new IOException("Invalid response columns");
                String[] data = new String[columns];
                for (int j=0;j<columns;j++) data[j]=input.readUTF();
                values.add(data);
            }
            if (input.available() != 0) throw new IOException("Unexpected response data");
            return values;
        }
    }

    public synchronized int login(GatewayPolicy.Role role, String loginId, char[] password)
            throws IOException {
        if (role == null || password == null || password.length == 0) return 0;
        logout();
        List<String[]> result = send(null, "LOGIN_" + role.name(), loginId, new String(password));
        if (result.size() != 1 || result.get(0).length != 2) throw new IOException("Invalid login response");
        int internalId;
        try { internalId = Integer.parseInt(result.get(0)[0]); }
        catch (NumberFormatException ex) { throw new IOException("Invalid identity response"); }
        if (internalId <= 0 && role != GatewayPolicy.Role.ADMIN) throw new IOException("Invalid account response");
        token = result.get(0)[1];
        return internalId;
    }

    public synchronized void logout() {
        String previous = token;
        token = null;
        if (previous != null) {
            try { send(previous, "LOGOUT"); } catch (IOException ignored) { }
        }
    }

    public List<String[]> call(String operation, String... args) throws IOException {
        String active = token;
        if (active == null || active.isEmpty()) throw new IOException("Not authenticated");
        return send(active, operation, args);
    }

    public static boolean successOrFalse(List<String[]> result) {
        return result != null && success(result);
    }

    public static boolean success(List<String[]> result) {
        return result.size() == 1 && result.get(0).length > 0
                && "OK".equals(result.get(0)[0]);
    }
}
