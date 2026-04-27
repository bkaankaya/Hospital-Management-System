package db;

public class Patient {
	
	int SSN;
	String Name;
	String Surname;
	String Gender;
	String Address;
	String Date_Of_Birth;
	
	public Patient() {		
	}

	public Patient(int ssn, String name, String surname, String gender, String address, String date_Of_Birth) {
		SSN = ssn;
		Name = name;
		Surname = surname;
		Gender = gender;
		Address = address;
		Date_Of_Birth = date_Of_Birth;
	}
}
