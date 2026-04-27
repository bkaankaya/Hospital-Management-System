package db;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Doktor entity sınıfı - Modern Java standartlarına uygun
 * Encapsulation, validation ve güvenlik özellikleri ile
 */
public class Doctor {
    private static final Logger logger = Logger.getLogger(Doctor.class.getName());
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    private int ssn;
    private String name;
    private String surname;
    private String gender;
    private String address;
    private LocalDate dateOfBirth;
    private String departmentName;
    private String passwordHash;
    private String salt;
    private boolean isActive;
    
    // Default constructor
    public Doctor() {
        this.isActive = true;
    }
    
    // Full constructor with validation
    public Doctor(int ssn, String name, String surname, String gender, 
                  String address, String dateOfBirth, String departmentName) {
        this();
        setSSN(ssn);
        setName(name);
        setSurname(surname);
        setGender(gender);
        setAddress(address);
        setDateOfBirth(dateOfBirth);
        setDepartmentName(departmentName);
    }
    
    // Getters and Setters with validation
    public int getSSN() {
        return ssn;
    }
    
    public void setSSN(int ssn) {
        if (!SecurityManager.isValidSSN(String.valueOf(ssn))) {
            throw new IllegalArgumentException("Geçersiz SSN formatı");
        }
        this.ssn = ssn;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("İsim boş olamaz");
        }
        this.name = SecurityManager.sanitizeInput(name.trim());
    }
    
    public String getSurname() {
        return surname;
    }
    
    public void setSurname(String surname) {
        if (surname == null || surname.trim().isEmpty()) {
            throw new IllegalArgumentException("Soyisim boş olamaz");
        }
        this.surname = SecurityManager.sanitizeInput(surname.trim());
    }
    
    public String getGender() {
        return gender;
    }
    
    public void setGender(String gender) {
        if (gender == null || (!gender.equals("M") && !gender.equals("F"))) {
            throw new IllegalArgumentException("Cinsiyet M veya F olmalıdır");
        }
        this.gender = gender;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        if (address != null) {
            this.address = SecurityManager.sanitizeInput(address.trim());
        }
    }
    
    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }
    
    public void setDateOfBirth(String dateOfBirth) {
        try {
            if (dateOfBirth != null && !dateOfBirth.trim().isEmpty()) {
                this.dateOfBirth = LocalDate.parse(dateOfBirth, DATE_FORMATTER);
                
                // Yaş kontrolü
                if (this.dateOfBirth.isAfter(LocalDate.now().minusYears(18))) {
                    throw new IllegalArgumentException("Doktor en az 18 yaşında olmalıdır");
                }
                if (this.dateOfBirth.isBefore(LocalDate.now().minusYears(80))) {
                    throw new IllegalArgumentException("Geçersiz doğum tarihi");
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Geçersiz tarih formatı. YYYY-MM-DD kullanın");
        }
    }
    
    public String getDateOfBirthAsString() {
        return dateOfBirth != null ? dateOfBirth.format(DATE_FORMATTER) : null;
    }
    
    public String getDepartmentName() {
        return departmentName;
    }
    
    public void setDepartmentName(String departmentName) {
        if (departmentName == null || departmentName.trim().isEmpty()) {
            throw new IllegalArgumentException("Departman adı boş olamaz");
        }
        this.departmentName = SecurityManager.sanitizeInput(departmentName.trim());
    }
    
    public String getPasswordHash() {
        return passwordHash;
    }
    
    public void setPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.length() < 6) {
            throw new IllegalArgumentException("Şifre en az 6 karakter olmalıdır");
        }
        this.salt = SecurityManager.generateSalt();
        this.passwordHash = SecurityManager.hashPassword(plainPassword, this.salt);
    }
    
    public String getSalt() {
        return salt;
    }
    
    public void setSalt(String salt) {
        this.salt = salt;
    }
    
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        isActive = active;
    }
    
    /**
     * Şifre doğrulama
     */
    public boolean verifyPassword(String plainPassword) {
        if (this.salt == null || this.passwordHash == null) {
            return false;
        }
        String hashedInput = SecurityManager.hashPassword(plainPassword, this.salt);
        return this.passwordHash.equals(hashedInput);
    }
    
    /**
     * Tam isim döner
     */
    public String getFullName() {
        return (name != null ? name : "") + " " + (surname != null ? surname : "");
    }
    
    /**
     * Yaş hesaplama
     */
    public int getAge() {
        if (dateOfBirth == null) return 0;
        return LocalDate.now().getYear() - dateOfBirth.getYear();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Doctor doctor = (Doctor) obj;
        return ssn == doctor.ssn;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(ssn);
    }
    
    @Override
    public String toString() {
        return String.format("Doctor{SSN=%d, Name='%s', Surname='%s', Department='%s', Active=%s}", 
                           ssn, name, surname, departmentName, isActive);
    }
}
