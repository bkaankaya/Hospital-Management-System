package db;

public class MedicalRecords {
	String Prescription;
	String Diagnosis;
	int Patient_SSN;
	
	public MedicalRecords() {		
	}

	public MedicalRecords(String prescription, String diagnosis, int patient_SSN) {
		Prescription = prescription;
		Diagnosis = diagnosis;
		Patient_SSN = patient_SSN;
	}
	

}
