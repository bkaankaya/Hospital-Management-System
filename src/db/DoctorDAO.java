 package db;
 /*
  * DoctorDAO.java
  */
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class DoctorDAO {

	// takes Doctor object as parameter 
	// and inserts to the database using PreparedStatement
	public void insertDoctor(Doctor doc) 
	{
		//try with resources
		//automatically closes resources
		try (Connection con = DatabaseManager.getConnection();
			Statement st = con.createStatement() ){
			String query = "INSERT INTO Doctor (SSN, Name, Surname, Gender, Address, Date_Of_Birth, Department_Name) VALUES (?, ?, ?, ?, ?, ?, ?)";
		try	(PreparedStatement pst = con.prepareStatement(query) ){
			pst.setInt(1, doc.getSSN());
			pst.setString(2, doc.getName());
			pst.setString(3, doc.getSurname());
			pst.setString(4, doc.getGender());
			pst.setString(5, doc.getAddress());
			pst.setString(6, doc.getDateOfBirthAsString());
			pst.setString(7, doc.getDepartmentName());
			pst.executeUpdate();
			} 
			} catch (SQLException e) {
			System.out.println("Sql Error.");
			e.printStackTrace(System.out);
			}
	}
	
		
	// Explicit write outcome for UI forms; original insertDoctor API remains unchanged.
    public boolean tryInsertDoctor(Doctor doctor) {
        if (doctor == null) return false;
        String sql = "INSERT INTO Doctor (SSN, Name, Surname, Gender, Address, Date_Of_Birth, Department_Name) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, doctor.getSSN());
            stmt.setString(2, doctor.getName());
            stmt.setString(3, doctor.getSurname());
            stmt.setString(4, doctor.getGender());
            stmt.setString(5, doctor.getAddress());
            stmt.setString(6, doctor.getDateOfBirthAsString());
            stmt.setString(7, doctor.getDepartmentName());
            return stmt.executeUpdate() == 1;
        } catch (SQLException ex) {
            System.err.println("Doctor insert failed: " + ex.getMessage());
            return false;
        }
    }

    // returns Doctor object using primary key SSN
	public Doctor getDoctorInformation(int Ssn) 
	{
		Doctor doc = new Doctor();
		doc.setSSN(Ssn);
		//try with resources
		//automatically closes resources
		try (Connection con = DatabaseManager.getConnection();
			PreparedStatement pst = con.prepareStatement("select * from Doctor where SSN=?") ){
			pst.setInt(1, Ssn);
			
		try (ResultSet set = pst.executeQuery() ){
			if (set.next()) {
				doc.setSSN(set.getInt(1));
				doc.setName(set.getString(2));
				doc.setSurname(set.getString(3));
				doc.setGender(set.getString(4));
				doc.setAddress(set.getString(5));
				doc.setDateOfBirth(set.getString(6));
				doc.setDepartmentName(set.getString(7));
				return doc;
			}
			}
			} catch (SQLException e) {
				System.out.println("Sql Error.");
				e.printStackTrace(System.out);
			}
			return null;
	}
	
	
		// returns Doctor object using name surname
		public Doctor getDoctorInformation(String name_surname) 
		{
			Doctor doc = new Doctor();
			//try with resources
			//automatically closes resources
			try (Connection con = DatabaseManager.getConnection();
				PreparedStatement pst = con.prepareStatement("select * from Doctor where concat(name,' ', surname)=?") ){
				pst.setString(1, name_surname);
				
			try (ResultSet set = pst.executeQuery() ){
				if (set.next()) {
					doc.setSSN(set.getInt(1));
					doc.setName(set.getString(2));
					doc.setSurname(set.getString(3));
					doc.setGender(set.getString(4));
					doc.setAddress(set.getString(5));
					doc.setDateOfBirth(set.getString(6));
					doc.setDepartmentName(set.getString(7));
					return doc;
				}
				}
				} catch (SQLException e) {
					System.out.println("Sql Error.");
					e.printStackTrace(System.out);
				}
				return null;
		}
	
		// returns Doctor ssn using name surname
				public int getDoctorSsn(String name_surname) 
				{
					//try with resources
					//automatically closes resources
					try (Connection con = DatabaseManager.getConnection();
						PreparedStatement pst = con.prepareStatement("select * from Doctor where concat(name,' ', surname)=?") ){
						pst.setString(1, name_surname);
						
					try (ResultSet set = pst.executeQuery() ){
						if(set.next()) {
							int ssn = set.getInt(1);
							return ssn;
						}
						}
						} catch (SQLException e) {
							System.out.println("Sql Error.");
							e.printStackTrace(System.out);
						}
					return 0;
				}
	
	// returns doctors belonging to the parametrized department
	public ArrayList<String> getDoctorList(String dep)  
	{
		ArrayList<String> list = new ArrayList<String>() ;
		try (Connection con = DatabaseManager.getConnection();
			PreparedStatement pst = con.prepareStatement("select concat(name, ' ', surname) from Doctor where department_name=?") ){
			pst.setString(1, dep);
		try (ResultSet set = pst.executeQuery() ){
			
			while(set.next() )
			{
				list.add( set.getString(1) );
			}
			return list;	
		}
		}catch (SQLException e) {
			System.out.println("Sql Error.");
			e.printStackTrace(System.out);
		}
		return null;
	}
	 
	public void updateAddress(int doc_ssn, String new_address) {
		try (Connection con = DatabaseManager.getConnection();
			Statement st = con.createStatement() ){
			String query = "update Doctor set address = ? where SSN =" + doc_ssn;
		try	(PreparedStatement pst = con.prepareStatement(query) ){
			pst.setString(1, new_address);
			pst.executeUpdate();
		}
		}catch (SQLException e) {
			System.out.println("Sql Error.");
			e.printStackTrace(System.out);
		}

	}
	

    public boolean tryUpdateAddress(int doctorId, String newAddress) {
        if (doctorId <= 0 || newAddress == null || newAddress.trim().isEmpty()) return false;
        String sql = "UPDATE Doctor SET Address = ? WHERE SSN = ?";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, newAddress.trim());
            stmt.setInt(2, doctorId);
            return stmt.executeUpdate() == 1;
        } catch (SQLException ex) {
            System.err.println("Doctor address update failed: " + ex.getMessage());
            return false;
        }
    }

	public boolean checkDoctorLogin(int doc_SSN, String name, String surname) 
	{
		try (Connection con = DatabaseManager.getConnection();
			PreparedStatement pst = con.prepareStatement("SELECT * FROM Doctor where SSN=? and name=? and surname=?") ){
			pst.setInt(1, doc_SSN);
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

