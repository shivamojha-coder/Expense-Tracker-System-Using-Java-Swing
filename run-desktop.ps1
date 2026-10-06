$scriptDir = if ($PSScriptRoot) { $PSScriptRoot } else { (Get-Location).Path }
Set-Location $scriptDir

Write-Host "==============================================" -ForegroundColor Cyan
Write-Host "   Starting Expense Tracker Desktop App" -ForegroundColor Cyan
Write-Host "==============================================" -ForegroundColor Cyan

$project = Join-Path $scriptDir "expense-tracker-desktop"

# Maven builds the exact classpath from pom.xml and recompiles anything that changed,
# so the app always runs the current code. Settings are read from the project's .env file.
& (Join-Path $project "mvnw.cmd") -q -f (Join-Path $project "pom.xml") compile exec:java
