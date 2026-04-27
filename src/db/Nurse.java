package db;

public class Nurse {

	int SSN;
	String Name;
	String Surname;
	String Gender;
	String Address;
	String Date_Of_Birth;
	
	public Nurse() {		
	}

	public Nurse(int ssn, String name, String surname, String gender, String address, String date_Of_Birth,
			String department_Name) {
		SSN = ssn;
		Name = name;
		Surname = surname;
		Gender = gender;
		Address = address;
		Date_Of_Birth = date_Of_Birth;
	}

}
