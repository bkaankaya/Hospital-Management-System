package db;
/*
 * AppointmentDAO.java
 * Author: Eyup Celen
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
	public void createAppointment(Appointment app) 
	{
		//try with resources
		//automatically closes resources
		try (Connection con = DatabaseManager.getConnection();
			Statement st = con.createStatement() ){
			String query = "insert into Appointment (Date, Time, Patient_SSN, Doctor_SSN) values (?, ?, ?, ?)";
		try	(PreparedStatement pst = con.prepareStatement(query) ){
			pst.setString(1, app.Date);
			pst.setString(2, app.Time);
			pst.setInt(3, app.Patient_SSN);
			pst.setInt(4, app.Doctor_SSN);
			pst.executeUpdate();
			} 
			} catch (SQLException e) {
			System.out.println("Sql Error.");
			e.printStackTrace(System.out);
			}
	}
	
	
	// cancels an appointment using app_id
	public void cancelAppointment(int app_id)	
	{
		//try with resources
		//automatically closes resources
		try (Connection con = DatabaseManager.getConnection();
			Statement st = con.createStatement() ){
			String query = "delete from Appointment where app_id=?";
		try	(PreparedStatement pst = con.prepareStatement(query) ){
			pst.setInt(1, app_id);
			pst.executeUpdate();
		}
			} catch (SQLException e) {
				System.out.println("Sql Error.");
				e.printStackTrace(System.out);
			}
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
	public ArrayList<String> getTimeList(int doc_ssn, String date)  
	{
		ArrayList<String> timeList = timeConstraints();
		try (Connection con = DatabaseManager.getConnection();
			Statement st = con.createStatement() ){
			String query = "select time from appointment where date='" + date +"' and Doctor_SSN=" + doc_ssn;
		try (ResultSet set = st.executeQuery(query) ){
			
			while(set.next() )
			{
				String current_time = set.getString(1);
				// timeList will contain unbooked times by removing existing times
				if(timeList.contains(current_time))
					{
						timeList.remove(current_time);
					}
			}
			return timeList;
		}
		}catch (SQLException e) {
			System.out.println("Sql Error.");
			e.printStackTrace(System.out);
		}
		return null;
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
