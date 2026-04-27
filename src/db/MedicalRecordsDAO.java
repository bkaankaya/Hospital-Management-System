package db;
/*
 * MedicalRecordsDAO.java
 * Author: Eyup Celen
 * This class is not used in GUI applications
 */
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class MedicalRecordsDAO {

	// With a transaction, inserts values to Add Table
	// after inserting values to Medical_Records Table
	
	public void add_medRecord(MedicalRecords record, Appointment app) throws SQLException
	{
		Connection con = DatabaseManager.getConnection();
		Statement st = con.createStatement();
	   
		try {con.setAutoCommit(false);
			insertRecord(record); // inserts to the Medical_Records Table 						
			// max(record_no) gives the last record_no
			String record_query = "select max(record_no) from medical_records";  	 
	   		// try with resources
			// automatically closes the ResultSet
			try (ResultSet set = st.executeQuery(record_query)){;						 
			set.next();
			int record_no = set.getInt(1); // gets current record_no
			// created an AddTable object to insert value to the Add Table
			Add adder = new Add();
			adder.Date = app.Date;
			adder.Time = app.Time;
			adder.Doctor_SSN = app.Doctor_SSN;
			adder.Record_No = record_no ;
			insertToAdd(adder);  // inserts to the Add Table
			con.commit();		
	   }
	   } catch (SQLException e) {
		 con.rollback();
		 e.printStackTrace();
	   } finally {
		 con.close();
		 st.close();		 
	   }
	}
		
	private void insertRecord(MedicalRecords record)
	{
	//try with resources
	//automatically closes resources
	try (Connection con = DatabaseManager.getConnection();
		Statement st = con.createStatement() ){
		String query = "insert into Medical_Records (Prescription, Diagnosis, Patient_SSN) values (?, ?, ?)";
	try	(PreparedStatement pst = con.prepareStatement(query) ){
		pst.setString(1, record.Prescription);
		pst.setString(2, record.Diagnosis);
		pst.setInt(3, record.Patient_SSN);
		pst.executeUpdate();
	}
	} catch (SQLException e) {
		System.out.println("Sql Error.");
		e.printStackTrace(System.out);
		}
		
	}
	
	private void insertToAdd (Add adder)
	{
	//try with resources
	//automatically closes resources
	try (Connection con = DatabaseManager.getConnection();
		Statement st = con.createStatement() ){
		String query = "insert into hospital.add values (?, ?, ?, ?)";
	try	(PreparedStatement pst = con.prepareStatement(query) ){
		pst.setInt(1, adder.Doctor_SSN);
		pst.setString(2, adder.Date);
		pst.setString(3, adder.Time);
		pst.setInt(4, adder.Record_No);
		pst.executeUpdate();
	}
	} catch (SQLException e) {
		System.out.println("Sql Error.");
		e.printStackTrace(System.out);
		}
		
	}

}
