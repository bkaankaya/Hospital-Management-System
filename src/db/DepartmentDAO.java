package db;
/*
 * DepartmentDAO.java
 * Author: Eyup Celen
 */
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class DepartmentDAO {

	public void insertDepartment(Department dep) {
		try (Connection con = DatabaseManager.getConnection();
			Statement st = con.createStatement() ){
			String query = "insert into Department values (?)";
		
		try	(PreparedStatement pst = con.prepareStatement(query) ){
				pst.setString(1, dep.department_name);
				pst.executeUpdate();
				} 
				} catch (SQLException e) {
				System.out.println("Sql Error.");
				e.printStackTrace(System.out);
				}
	}

	public ArrayList<String> getDepartmentList()  
	{
		ArrayList<String> list = new ArrayList<String>() ;
		try (Connection con = DatabaseManager.getConnection();
			Statement st = con.createStatement() ){
			String query = "select * from department order by department_name asc";
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

}
