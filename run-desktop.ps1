$scriptDir = if ($PSScriptRoot) { $PSScriptRoot } else { (Get-Location).Path }
Set-Location $scriptDir

Write-Host "==============================================" -ForegroundColor Cyan
Write-Host "   Starting Expense Tracker Desktop App" -ForegroundColor Cyan
Write-Host "==============================================" -ForegroundColor Cyan

$envFile = Join-Path $scriptDir ".env"
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith("#")) {
            $parts = $line.Split('=', 2)
            if ($parts.Count -eq 2) {
                $k = $parts[0].Trim()
                $v = $parts[1].Trim().Trim('"').Trim("'")
                [Environment]::SetEnvironmentVariable($k, $v, "Process")
            }
        }
    }
}


$classes = Join-Path $scriptDir "expense-tracker-desktop\target\classes"
$m2Repo = Join-Path $HOME ".m2\repository"

if (-not (Test-Path $classes)) {
    Write-Host "Compiling classes with Maven Wrapper..." -ForegroundColor Yellow
    & (Join-Path $scriptDir "expense-tracker-desktop\mvnw.cmd") -f (Join-Path $scriptDir "expense-tracker-desktop\pom.xml") compile
}

$jars = (Get-ChildItem -Path $m2Repo -Recurse -Filter "*.jar" | 
    Where-Object { 
        $_.FullName -notmatch "plugin" -and 
        $_.FullName -notmatch "junit" -and 
        $_.FullName -notmatch "surefire" -and 
        $_.FullName -notmatch "plexus" 
    } | Select-Object -ExpandProperty FullName) -join ";"

$cp = "$classes;$jars"

Write-Host "Launching Java Swing UI..." -ForegroundColor Green
java -cp "$cp" com.expensetracker.Main
