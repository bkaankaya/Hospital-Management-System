@echo off
javac -cp "mysql-connector-j-8.2.0.jar;jcalendar-1.4.jar;src" src/db/*.java -d bin
java -cp "bin;mysql-connector-j-8.2.0.jar;jcalendar-1.4.jar" db.MainFrame
pause
