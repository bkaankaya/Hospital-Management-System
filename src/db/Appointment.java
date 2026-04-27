package db;

public class Appointment {
 
	String Date;
	String Time;
	int Patient_SSN;
	int Doctor_SSN;
	
	public Appointment() {	
	}

	public Appointment(String date, String time, int patient_SSN, int doctor_SSN) {
		Date = date;
		Time = time;
		Patient_SSN = patient_SSN;
		Doctor_SSN = doctor_SSN;
	}
}
