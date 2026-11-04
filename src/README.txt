Legacy Java source tree:
- db/MainFrame.java starts the Swing desktop application.
- db/LoginPanel.java hosts patient, doctor and administrator sign-in.
- db/AppointmentPanel.java and db/CancelAppointmentPanel.java manage scheduling.
- db/*DAO.java performs the original JDBC/MySQL queries.
- db/DesktopStyles.java provides common responsive Swing components.
- db/DatabaseManager.java reads local MySQL credentials.
Tests live in test/db; the Java and MySQL/GUI GitHub Actions workflows run them.
This is a teaching/demo application. See README.md for setup and security limitations.
