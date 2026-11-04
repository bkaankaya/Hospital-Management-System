package db;
/*
 * MedicineDAO.java
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
			String query = "insert into medicine (Barcode_No, Medicine_Name, Department_Name, consumed) values (?, ?, ?, ?)";
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
	
	// returns medicine names from Department inventory
	public ArrayList<String> getMedicineList(String department_name) {
        ArrayList<String> list = new ArrayList<>();
        String sql = "SELECT DISTINCT Medicine_Name FROM medicine WHERE Department_Name = ? AND consumed = 0 ORDER BY Medicine_Name";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, department_name);
            try (ResultSet rows = pst.executeQuery()) {
                while (rows.next()) {
                    list.add(rows.getString(1));
                }
            }
            return list;
        } catch (SQLException e) {
            System.err.println("Failed to load medicines: " + e.getMessage());
            return null;
        }
    }

	/* returns average medicine number by medicine names
	 * and some of medicine_barcode numbers (doesn't have to make sense)
	 * cubric: avg, count, union, sum
	 */
	public ArrayList<String> getStatistics(String department_name) {
        ArrayList<String> stats = new ArrayList<>();
        String sql = "SELECT (SELECT AVG(medicine_count) FROM "
                + "(SELECT COUNT(*) AS medicine_count FROM medicine "
                + "WHERE Department_Name = ? GROUP BY Medicine_Name) grouped) AS avg_count, "
                + "(SELECT SUM(Barcode_No) FROM medicine WHERE Department_Name = ?) AS barcode_sum";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, department_name);
            stmt.setString(2, department_name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    stats.add(rs.getString("avg_count"));
                    stats.add(rs.getString("barcode_sum"));
                }
            }
            return stats;
        } catch (SQLException ex) {
            System.err.println("Medicine statistics failed: " + ex.getMessage());
            return null;
        }
    }
public static void main(String args[]) {
	MedicineDAO dao = new MedicineDAO();
	System.out.println(dao.getStatistics("Diagnostics").toString() );
	}
}
