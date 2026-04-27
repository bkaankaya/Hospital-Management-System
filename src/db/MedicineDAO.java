package db;
/*
 * MedicineDAO.java
 * Author: Eyup Celen
 */
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class MedicineDAO {

	public void insertMedicine(Medicine med)
	{
		//try with resources
		//automatically closes resources
		try (Connection con = DatabaseManager.getConnection();
			Statement st = con.createStatement() ){
			String query = "insert into medicine values (?, ?, ?)";
		try	(PreparedStatement pst = con.prepareStatement(query) ){
			pst.setInt(1, med.Barcode_No);
			pst.setString(2, med.Medicine_Name);
			pst.setString(3, med.Department_Name);
			pst.setInt(4, med.consumed);
			pst.executeUpdate();
			} 
			} catch (SQLException e) {
			System.out.println("Sql Error.");
			e.printStackTrace(System.out);
			}		
	}
	
	// returns medicine names from department inventory
	public ArrayList<String> getMedicineList(String department_name){
		   ArrayList<String> list = new ArrayList<String>() ;
		try (Connection con = DatabaseManager.getConnection();
			Statement st = con.createStatement() ){
			String query = "select distinct medicine_name from medicine where department_name='" + department_name +"'" + "and consumed=0 order by medicine_name";
		try (ResultSet set = st.executeQuery(query) ){
			
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

	/* returns average medicine number by medicine names
	 * and some of medicine_barcode numbers (doesn't have to make sense)
	 * cubric: avg, count, union, sum
	 */
	public ArrayList<String> getStatistics(String department_name){
		   ArrayList<String> list = new ArrayList<String>() ;
		   try (Connection con = DatabaseManager.getConnection();
				Statement st = con.createStatement() ){
				String query = "select 'average', avg(medicine_count) from\r\n"
						+ "	(select COUNT(*) as medicine_count from medicine\r\n"
						+ "	where department_name = ?\r\n"
						+ "	group by medicine_name having count(*)> 0) as subquery\r\n"
						+ "	union\r\n"
						+ "	select 'sum', sum(Barcode_No) from medicine\r\n"
						+ "	where department_name = ?";
				try	(PreparedStatement pst = con.prepareStatement(query) ){
				pst.setString(1, department_name);
				pst.setString(2, department_name);
					ResultSet set = pst.executeQuery();
					while(set.next() ) 
						{
							String value = set.getString(2);
							list.add(value);
						}
			return list;	
		}
		}catch (SQLException e) {
			System.out.println("Sql Error.");
			e.printStackTrace(System.out);
		}
		return null;	
	}
public static void main(String args[]) {
	MedicineDAO dao = new MedicineDAO();
	System.out.println(dao.getStatistics("Diagnostics").toString() );
	}
}
