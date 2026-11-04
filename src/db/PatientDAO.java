package db;
/*
 * PatientDAO.java
 * Author: Eyup Celen
 */
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class PatientDAO {

	// takes Patient object as parameter 
	// and inserts to the database using PreparedStatement
	public void insertPatient(Patient p) 
	{
		//try with resources
		//automatically closes resources
		try (Connection con = DatabaseManager.getConnection();
			Statement st = con.createStatement() ){
			String query = "INSERT INTO Patient (SSN, Name, Surname, Gender, Address, Date_Of_Birth) VALUES (?, ?, ?, ?, ?, ?)";
		try	(PreparedStatement pst = con.prepareStatement(query) ){
			pst.setInt(1, p.SSN);
			pst.setString(2, p.Name);
			pst.setString(3, p.Surname);
			pst.setString(4, p.Gender);
			pst.setString(5, p.Address);
			pst.setString(6, p.Date_Of_Birth);
			pst.executeUpdate();
			} 
			} catch (SQLException e) {
			System.out.println("Sql Error.");
			e.printStackTrace(System.out);
			}
	}
	
	// Explicit write outcome for UI forms; original insertPatient API remains unchanged.
    public boolean tryInsertPatient(Patient patient) {
        if (patient == null) return false;
        String sql = "INSERT INTO Patient (SSN, Name, Surname, Gender, Address, Date_Of_Birth) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, patient.SSN);
            stmt.setString(2, patient.Name);
            stmt.setString(3, patient.Surname);
            stmt.setString(4, patient.Gender);
            stmt.setString(5, patient.Address);
            stmt.setString(6, patient.Date_Of_Birth);
            return stmt.executeUpdate() == 1;
        } catch (SQLException ex) {
            System.err.println("Patient insert failed: " + ex.getMessage());
            return false;
        }
    }

    // returns Patient object using Ssn as parameter
	public Patient getPatient(int Ssn) 
	{
		Patient p = new Patient();
		p.SSN = Ssn;
		//try with resources
		//automatically closes resources
		try (Connection con = DatabaseManager.getConnection();
			PreparedStatement pst = con.prepareStatement("select * from Patient where SSN=?") ){
			pst.setInt(1, Ssn);
			
		try (ResultSet set = pst.executeQuery() ){
			if (set.next()) {
				p.SSN = set.getInt(1);
				p.Name = set.getString(2);
				p.Surname = set.getString(3);
				p.Gender = set.getString(4);
				p.Address = set.getString(5);
				p.Date_Of_Birth = set.getString(6);
				return p;
			}
			}
			} catch (SQLException e) {
				System.out.println("Sql Error.");
				e.printStackTrace(System.out);
			}
			return null;
	}
	
	public void updateAddress(int patient_ssn, String new_address) {
		try (Connection con = DatabaseManager.getConnection();
			Statement st = con.createStatement() ){
			String query = "update patient set address = ? where SSN =" + patient_ssn;
		try	(PreparedStatement pst = con.prepareStatement(query) ){
			pst.setString(1, new_address);
			pst.executeUpdate();
		}
		}catch (SQLException e) {
			System.out.println("Sql Error.");
			e.printStackTrace(System.out);
		}

	}
	
	/* patient name like '%like%' 
	 * cubric : 3 join, outer join, exists, is null, between and, string operation, subquery
	 */
	public ArrayList<ArrayList<String>> patientNameLike(String like)  
	{
		ArrayList<ArrayList<String>> twoDArrayList = new ArrayList<>();
		try (Connection con = DatabaseManager.getConnection();  
			Statement st = con.createStatement() ){
			String query = "select concat(patient.name,' ',patient.surname) as patient_name, \r\n"
					+ "appointment.date, appointment.Time, concat(doctor.name,' ',doctor.surname) as doctor_name, Doctor.department_name\r\n"
					+ "from Patient \r\n"
					+ "left outer join Appointment on patient.SSN=appointment.Patient_ssn\r\n"
					+ "right outer join Doctor on Doctor.SSN = Appointment.Doctor_ssn\r\n"
					+ "join department on Doctor.department_name = department.Department_Name\r\n"
					+ "where Patient.date_of_birth between '1960-01-01' and '2023-01-01'\r\n"
					+ "and patient.name like ? and not exists\r\n"
					+ "(select * from Doctor where name is null)";
		try (PreparedStatement pst = con.prepareStatement(query) ){
			pst.setString(1, "%" + like + "%");
		try	(ResultSet set = pst.executeQuery() ){
			while(set.next() )
			{ 
			ArrayList<String> columnList = new ArrayList<>();
            for (int j = 1; j <= 5; j++) 
            	{
                	columnList.add(set.getString(j));
            	}
            twoDArrayList.add(columnList);
			}
			return twoDArrayList;	
		}}
		}catch (SQLException e) {
			System.out.println("Sql Error.");
			e.printStackTrace(System.out);
		}
		return null;
	}
	

	public boolean checkPatientLogin(int patient_SSN, String name, String surname) 
	{
		try (Connection con = DatabaseManager.getConnection();
			PreparedStatement pst = con.prepareStatement("SELECT * FROM patient where SSN=? and name=? and surname=?") ){
			pst.setInt(1, patient_SSN);
			pst.setString(2, name);
			pst.setString(3, surname);
		try	(ResultSet set = pst.executeQuery() ){
			if(set.next() )
				{
					return true;
				}
		}
		}catch (SQLException e) {
			System.out.println("Sql Error.");
			e.printStackTrace(System.out);
		}
		return false;		
	}

}
