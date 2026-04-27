package db;

import javax.swing.JOptionPane;

public class Message {
	public static void WarningMessage(String string) {
		String msg;
		
		switch (string) {
		case "WARN":
			msg = "Fill in all the blanks to continue!";
			break;
			default:
				msg = string;
		}
		JOptionPane.showMessageDialog(null, msg, "Warning", JOptionPane.INFORMATION_MESSAGE);}
		
	public static void SuccessfulMessage(String string) {
		String msg;	
		switch (string) {
		case "SUCCESSFUL":
			msg = "SUCCESSFUL!";
			break;
			default:
				msg = string;
		}
		JOptionPane.showMessageDialog(null, msg, "SUCCESSFUL", JOptionPane.INFORMATION_MESSAGE);}
}