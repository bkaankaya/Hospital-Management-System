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
	public ArrayList<ArrayList<String>> patientNameLike(String like) {
        ArrayList<ArrayList<String>> rows = new ArrayList<>();
        String sql = "SELECT CONCAT(p.Name, ' ', p.Surname), a.Date, a.Time, "
                + "CONCAT(d.Name, ' ', d.Surname), d.Department_Name "
                + "FROM Patient p LEFT JOIN Appointment a ON a.Patient_SSN = p.SSN "
                + "LEFT JOIN Doctor d ON d.SSN = a.Doctor_SSN "
                + "WHERE p.Name LIKE ? OR p.Surname LIKE ? ORDER BY p.Name, p.Surname, a.Date, a.Time";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            String pattern = "%" + (like == null ? "" : like.trim()) + "%";
            stmt.setString(1, pattern);
            stmt.setString(2, pattern);
            try (ResultSet result = stmt.executeQuery()) {
                while (result.next()) {
                    ArrayList<String> row = new ArrayList<>();
                    for (int n = 1; n <= 5; n++) row.add(result.getString(n));
                    rows.add(row);
                }
            }
            return rows;
        } catch (SQLException ex) {
            System.err.println("Patient search failed: " + ex.getMessage());
            return null;
        }
    }
	

    public boolean tryUpdateAddress(int patientId, String newAddress) {
        if (patientId <= 0 || newAddress == null || newAddress.trim().isEmpty()) return false;
        String sql = "UPDATE Patient SET Address = ? WHERE SSN = ?";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, newAddress.trim());
            stmt.setInt(2, patientId);
            return stmt.executeUpdate() == 1;
        } catch (SQLException ex) {
            System.err.println("Patient address update failed: " + ex.getMessage());
            return false;
        }
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
