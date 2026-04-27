package db;

/* for Add class, DAO class will not be created.
 * instead, MedicalRecordsDAO class will be used to
 * insert to Add and Medical_Records with transaction 
 */

public class Add {
	 int Doctor_SSN;
	 int Record_No;
	 String Date;
	 String Time;
	 
	 public Add() {
		 
	 }

	public Add(int doctor_SSN, int record_No, String date, String time) {
		Doctor_SSN = doctor_SSN;
		Record_No = record_No;
		Date = date;
		Time = time;
	}
	 
}

