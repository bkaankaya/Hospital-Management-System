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
	  	public void updateRoomDepartment(Room room) {
        String sql = "UPDATE Room SET Department_Name = ? WHERE Room_ID = ?";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, room.Department_Name);
            pst.setInt(2, room.Room_ID);
            pst.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Failed to update room department: " + e.getMessage());
        }
    }
	
	  	public ArrayList<String> getRoomList(String dep) {
        ArrayList<String> list = new ArrayList<>();
        String sql = "SELECT Room_ID FROM Room WHERE Department_Name = ? ORDER BY Room_ID";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, dep);
            try (ResultSet rows = pst.executeQuery()) {
                while (rows.next()) {
                    list.add(rows.getString(1));
                }
            }
            return list;
        } catch (SQLException e) {
            System.err.println("Failed to load rooms: " + e.getMessage());
            return null;
        }
    }

}
