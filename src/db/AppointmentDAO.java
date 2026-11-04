package db;
/*
 * AppointmentDAO.java
 */
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class AppointmentDAO {
	
	// Adds an appointment with Appointment object as parameter
	@Deprecated
    public void createAppointment(Appointment app) {
        throw new UnsupportedOperationException(
                "Unvalidated appointment insertion was removed; use bookAppointment()");
    }
	
	
    // Database unique constraints prevent concurrent double bookings.
    public boolean bookAppointment(Appointment appointment) {
        if (appointment == null || appointment.Patient_SSN <= 0 || appointment.Doctor_SSN <= 0) return false;
        final LocalDate date;
        try {
            date = LocalDate.parse(appointment.Date);
            if (date.isBefore(LocalDate.now()) || date.isAfter(LocalDate.now().plusDays(13))
                    || !timeConstraints().contains(appointment.Time)) return false;
        } catch (RuntimeException ex) {
            return false;
        }
        String sql = "INSERT INTO Appointment (Date, Time, Patient_SSN, Doctor_SSN) VALUES (?, ?, ?, ?)";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setDate(1, java.sql.Date.valueOf(date));
            stmt.setTime(2, java.sql.Time.valueOf(appointment.Time));
            stmt.setInt(3, appointment.Patient_SSN);
            stmt.setInt(4, appointment.Doctor_SSN);
            return stmt.executeUpdate() == 1;
        } catch (SQLException ex) {
            System.err.println("Appointment insert failed: " + ex.getMessage());
            return false;
        }
    }

    public boolean cancelDoctorAppointment(int appointmentId, int doctorId) {
        if (appointmentId <= 0 || doctorId <= 0) return false;
        String sql = "DELETE FROM Appointment WHERE app_id=? AND Doctor_SSN=? "
                + "AND (Date > CURRENT_DATE() OR (Date = CURRENT_DATE() AND Time > CURRENT_TIME())) "
                + "AND NOT EXISTS (SELECT 1 FROM \`Add\` a "
                + "WHERE a.Doctor_SSN=Appointment.Doctor_SSN "
                + "AND a.Date=Appointment.Date AND a.Time=Appointment.Time)";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, appointmentId);
            stmt.setInt(2, doctorId);
            return stmt.executeUpdate() == 1;
        } catch (SQLException ex) {
            System.err.println("Appointment cancellation failed: " + ex.getMessage());
            return false;
        }
    }

	// cancels an appointment using app_id
	@Deprecated
    public void cancelAppointment(int app_id) {
        throw new UnsupportedOperationException(
                "Unscoped cancellation was removed; use cancelDoctorAppointment()");
    }
	
	// returns the doctor's appointments
	public ArrayList<String> getDocAppointments(int doc_ssn)	
	{
		ArrayList<String> app_list = new ArrayList<>();	     
		//try with resources
		//automatically closes resources
		try (Connection con = DatabaseManager.getConnection();
			Statement st = con.createStatement() ){
			String query = "select app_id from Appointment join Doctor on SSN=Doctor_SSN where SSN=? and date>=current_date()";
		try	(PreparedStatement pst = con.prepareStatement(query) ){
			pst.setInt(1, doc_ssn);
			ResultSet set = pst.executeQuery();
			while(set.next() ) 
				{
					String app_id = set.getString(1);
					app_list.add(app_id);
				}
			
		}
			return app_list;
			} catch (SQLException e) {
				System.out.println("Sql Error.");
				e.printStackTrace(System.out);
			}
			return null;
	}

    /**
     * Patient-scoped appointment list, bounded for a usable Swing table.
     * Never accepts raw SQL fragments or returns national identifiers.
     */
    public java.util.List<String[]> getPatientUpcomingAppointments(int patientId) {
        if (patientId <= 0) return java.util.Collections.emptyList();
        String sql = "SELECT a.app_id, a.Date, a.Time, "
                + "CONCAT(d.Name, ' ', d.Surname), d.Department_Name "
                + "FROM Appointment a JOIN Doctor d ON d.SSN=a.Doctor_SSN "
                + "WHERE a.Patient_SSN=? AND a.Date >= CURRENT_DATE() "
                + "ORDER BY a.Date, a.Time LIMIT 100";
        java.util.List<String[]> result = new java.util.ArrayList<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, patientId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    result.add(new String[] {rs.getString(1), rs.getString(2), rs.getString(3),
                            rs.getString(4), rs.getString(5)});
                }
            }
            return result;
        } catch (SQLException ex) {
            System.err.println("Could not list patient appointments");
            return null;
        }
    }

    public java.util.List<String[]> getDoctorUpcomingAppointments(int doctorId) {
        if (doctorId <= 0) return java.util.Collections.emptyList();
        String sql = "SELECT a.app_id, a.Date, a.Time, "
                + "CONCAT(p.Name, ' ', p.Surname) FROM Appointment a "
                + "JOIN Patient p ON p.SSN=a.Patient_SSN "
                + "WHERE a.Doctor_SSN=? AND a.Date>=CURRENT_DATE() "
                + "ORDER BY a.Date, a.Time LIMIT 100";
        java.util.List<String[]> result = new java.util.ArrayList<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, doctorId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    result.add(new String[] {rs.getString(1), rs.getString(2), rs.getString(3),
                            rs.getString(4)});
                }
            }
            return result;
        } catch (SQLException ex) {
            System.err.println("Could not list doctor appointments");
            return null;
        }
    }

	// returns today and next 13 days
	public ArrayList<String> getDateList() 
	{
	    ArrayList<String> datelist = new ArrayList<>();	     
		
	    Date currentDate = Calendar.getInstance().getTime();
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        
		// Create a Calendar instance and set it to the current date
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDate);
               
        String today = dateFormat.format(currentDate);
        datelist.add(today);
        
        for(int i=1; i<14; i++)
        {
        // Add one day to the current date
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        // Get the next day as a Date object
        Date nextDay = calendar.getTime();
        String strNextDate = dateFormat.format(nextDay);
        datelist.add(strNextDate);
        }        
        return datelist;         		
	}
	
	// returns available appointment times by checking existing appointment records
	// on parametrized date for the parametrized doctor
	// !! the use of timeConstraints method might violate database application independce !!
	public ArrayList<String> getTimeList(int doc_ssn, String date) {
        ArrayList<String> timeList = timeConstraints();
        String sql = "SELECT Time FROM Appointment WHERE Date = ? AND Doctor_SSN = ?";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setDate(1, java.sql.Date.valueOf(date));
            pst.setInt(2, doc_ssn);
            try (ResultSet rows = pst.executeQuery()) {
                while (rows.next()) {
                    timeList.remove(rows.getString(1));
                }
            }
            return timeList;
        } catch (SQLException | IllegalArgumentException e) {
            System.err.println("Failed to load appointment slots: " + e.getMessage());
            return null;
        }
    }

	private ArrayList<String> timeConstraints(){
		// Valid appointment times
		String[] temp = new String[]{"09:00:00", "09:30:00", "10:00:00", "10:30:00", "11:00:00", 
									 "11:30:00", "13:00:00", "13:30:00", "14:00:00", "14:30:00", 
									 "15:00:00", "15:30:00", "16:00:00", "16:30:00"};
		ArrayList<String> timeList = new ArrayList<>();
		for(int i=0; i<temp.length; i++ )
			{
				timeList.add(temp[i]);
			}
		return timeList;
	}
	
	
}
