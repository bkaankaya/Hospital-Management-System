package db;
/*
 * RoomDAO.java
 * Author: Eyup Celen
 * This class is not used in GUI applications
 */
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class RoomDAO {

	public void insertRoom(Room room)
	{
		//try with resources
		//automatically closes resources
		try (Connection con = DatabaseManager.getConnection();
			Statement st = con.createStatement() ){
			String query = "insert into Room values (?, ?)";
		try	(PreparedStatement pst = con.prepareStatement(query) ){
			pst.setInt(1, room.Room_ID);
			pst.setString(2, room.Department_Name);
			pst.executeUpdate();
			} 
			} catch (SQLException e) {
			System.out.println("Sql Error.");
			e.printStackTrace(System.out);
			}		
	}

	
	/*
	 * Updates the department of a room identified by its room_id.
	 * 
	 * To change the department of a room, create a Room object with the specified room_id
	 * and set the Department_Name to the desired value. Pass the Room object to this method.
	 */
	  	public void updateRoomDepartment(Room room) 
	{
		//try with resources
		//automatically closes resources
		try (Connection con = DatabaseManager.getConnection();
			Statement st = con.createStatement() ){
			String query1 = "select * from Room where Room_ID=" + room.Room_ID;
			int returnedRow = st.executeUpdate(query1);
			if(returnedRow==1)
			{
				String query2 = "update Room set Department_Name='" + room.Department_Name + "' where Room_ID=" + room.Room_ID;	
				st.executeUpdate(query2);
			}
			} catch (SQLException e) {
				System.out.println("Sql Error.");
				e.printStackTrace(System.out);
			}
		
	}
	
	  	public ArrayList<String> getRoomList(String dep)  
		{
			ArrayList<String> list = new ArrayList<String>() ;
			try (Connection con = DatabaseManager.getConnection();
				Statement st = con.createStatement() ){
				String query = "select * from Room where department_name=" + dep;
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
