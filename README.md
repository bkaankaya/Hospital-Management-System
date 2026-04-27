# Hospital Management System

A desktop application for managing hospital operations — patient records, appointments, staff, and inventory — built with Java and MySQL.

© 2026 Kaan Kaya. All rights reserved.

---

## Features

- Role-based access for doctors, patients, and administrators
- Appointment scheduling with conflict detection and cancellation
- Patient profiles with medical history and prescription records
- Medicine inventory tracking with usage statistics
- AES encryption for sensitive patient data, SHA-256 + salt for passwords
- SQL injection protection through input sanitization at the entity level
- Dark and light theme support with adjustable font sizes

---

## Tech Stack

- **Language:** Java 17+
- **UI:** Java Swing with custom components (ModernSwingUtils)
- **Database:** MySQL 8.0+ via JDBC
- **Libraries:** JCalendar, MySQL Connector/J

---

## Setup

### Prerequisites

| Tool | Version |
|------|---------|
| Java JDK | 17+ |
| MySQL | 8.0+ |

### 1. Clone the repo

```bash
git clone https://github.com/bkaankaya/Hospital-Management-System.git
cd Hospital-Management-System
```

### 2. Configure the database

Open `src/db/login.properties` and fill in your credentials:

```properties
url=jdbc:mysql://localhost:3306/hospital_db
user=your_username
password=your_password
```

### 3. Run

```cmd
baslat.bat
```

---

## Project Structure

Hospital-Management-System/
├── src/db/       Core logic, DAO classes, and GUI components
├── bin/          Compiled bytecode
├── lib/          External dependencies (.jar files)
└── scripts/      Startup scripts

---

## License

This is proprietary software. The source code is not licensed for public use, copying, or distribution. All rights reserved.