package db;

public class Medicine {

	int Barcode_No;
	String Medicine_Name;
	String Department_Name;
	// 0 for false, 1 for true
	int consumed = 0;

	public Medicine() {
	}

	public Medicine(int barcode_No, String medicine_Name, String department_Name) {
		Barcode_No = barcode_No;
		Medicine_Name = medicine_Name;
		Department_Name = department_Name;
	}

	public Medicine(int barcode_No, String medicine_Name, String department_Name, int consumed) {
		if (barcode_No <= 0) {
			throw new IllegalArgumentException("Barkod numarası pozitif bir tam sayı olmalıdır.");
		}
		if (medicine_Name == null || medicine_Name.trim().isEmpty()) {
			throw new IllegalArgumentException("İlaç adı boş olamaz.");
		}
		if (department_Name == null || department_Name.trim().isEmpty()) {
			throw new IllegalArgumentException("Departman adı boş olamaz.");
		}
		if (consumed != 0 && consumed != 1) {
			throw new IllegalArgumentException("Tüketim durumu 0 veya 1 olmalıdır.");
		}

		this.Barcode_No = barcode_No;
		this.Medicine_Name = medicine_Name.trim();
		this.Department_Name = department_Name.trim();
		this.consumed = consumed;
	}
}
