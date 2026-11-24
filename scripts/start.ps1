# Builds the Book Store and starts it on Payara Micro 7 (Windows PowerShell).
# The shop opens at http://localhost:8080/ ; stop it with Ctrl+C.
#
#   .\scripts\start.ps1              build, then start
#   .\scripts\start.ps1 -SkipBuild   start what was built last time
#
# Needs JDK 21: set JAVA_HOME, or have java 21 on the PATH.

param([switch]$SkipBuild)

Set-Location (Split-Path $PSScriptRoot -Parent)

# java -version writes to stderr, which Windows PowerShell 5.1 turns into errors; read it leniently.
$java = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin\java.exe' } else { 'java' }
$ErrorActionPreference = 'Continue'
$version = "$(& $java -version 2>&1 | Select-Object -First 1)"
$ErrorActionPreference = 'Stop'
if ($version -notmatch '"(2[1-9]|[3-9]\d)') {
    Write-Error "JDK 21 or newer is needed; found: $version. Set JAVA_HOME to a JDK 21 folder."
}

if (-not $SkipBuild) {
    Write-Host 'Building (tests skipped)...'
    & .\mvnw.cmd -B -ntp -q install -DskipTests
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

Write-Host 'Starting Payara Micro; the shop opens at http://localhost:8080/ once it says "ready".'
& .\mvnw.cmd -B -ntp -pl app -Ppayara package exec:exec -DskipTests
