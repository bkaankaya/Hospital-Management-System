package db;
/*
 * NurseDAO.java
 * Author: Eyup Celen
 */
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class NurseDAO {

		// takes Nurse object as parameter 
		// and inserts to the database using PreparedStatement
		public void insertNurse(Nurse n) 
		{
			//try with resources
			//automatically closes resources
			try (Connection con = DatabaseManager.getConnection();
				Statement st = con.createStatement() ){
				String query = "insert into Nurse values (?, ?, ?, ?, ?, ?)";
			try	(PreparedStatement pst = con.prepareStatement(query) ){
				pst.setInt(1, n.SSN);
				pst.setString(2, n.Name);
				pst.setString(3, n.Surname);
				pst.setString(4, n.Gender);
				pst.setString(5, n.Address);
				pst.setString(6, n.Date_Of_Birth);
				pst.executeUpdate();
				} 
				} catch (SQLException e) {
				System.out.println("Sql Error.");
				e.printStackTrace(System.out);
				}
		}
		
		
				
		// returns Nurse object using Ssn as parameter
		public Nurse getNurseInformation(int Ssn) 
		{
			Nurse n = new Nurse();
			n.SSN = Ssn;
			//try with resources
			//automatically closes resources
			try (Connection con = DatabaseManager.getConnection();
				Statement st = con.createStatement() ){
				String query = "select * from Nurse where SSN=" + Ssn ;
				
			try (ResultSet set = st.executeQuery(query) ){
				set.next();
				n.SSN = set.getInt(1);
				n.Name = set.getString(2);
				n.Surname = set.getString(3);
				n.Gender = set.getString(4);
				n.Address = set.getString(5);
				n.Date_Of_Birth = set.getString(6);
				return n;
				}
				} catch (SQLException e) {
					System.out.println("Sql Error.");
					e.printStackTrace(System.out);
				}
				return null;
		}
		
		public void updateAddress(int nurse_ssn, String new_address) {
			try (Connection con = DatabaseManager.getConnection();
				Statement st = con.createStatement() ){
				String query = "update nurse set address = ? where SSN =" + nurse_ssn;
			try	(PreparedStatement pst = con.prepareStatement(query) ){
				pst.setString(1, new_address);
				pst.executeUpdate();
			}
			}catch (SQLException e) {
				System.out.println("Sql Error.");
				e.printStackTrace(System.out);
			}

		}

}
