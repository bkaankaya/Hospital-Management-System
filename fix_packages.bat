@echo off
echo Package tanımları düzeltiliyor...

cd src\main\java\com\hospital

echo ModernLoginPanel.java düzeltiliyor...
powershell -Command "(Get-Content 'ModernLoginPanel.java') -replace 'package db;', 'package com.hospital;' | Set-Content 'ModernLoginPanel.java'"

echo ModernSwingUtils.java düzeltiliyor...
powershell -Command "(Get-Content 'ModernSwingUtils.java') -replace 'package db;', 'package com.hospital;' | Set-Content 'ModernSwingUtils.java'"

echo SecurityManager.java düzeltiliyor...
powershell -Command "(Get-Content 'SecurityManager.java') -replace 'package db;', 'package com.hospital;' | Set-Content 'SecurityManager.java'"

echo DatabaseManager.java düzeltiliyor...
powershell -Command "(Get-Content 'DatabaseManager.java') -replace 'package db;', 'package com.hospital;' | Set-Content 'DatabaseManager.java'"

echo Doctor.java düzeltiliyor...
powershell -Command "(Get-Content 'Doctor.java') -replace 'package db;', 'package com.hospital;' | Set-Content 'Doctor.java'"

echo Patient.java düzeltiliyor...
powershell -Command "(Get-Content 'Patient.java') -replace 'package db;', 'package com.hospital;' | Set-Content 'Patient.java'"

echo MainFrame.java düzeltiliyor...
powershell -Command "(Get-Content 'MainFrame.java') -replace 'package db;', 'package com.hospital;' | Set-Content 'MainFrame.java'"

echo Package düzeltme tamamlandı!
pause
