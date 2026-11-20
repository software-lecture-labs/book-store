@echo off
rem Starts the Book Store on Windows; double-click it or run it from cmd.
rem Runs start.ps1 without changing the computer's PowerShell execution policy.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start.ps1" %*
