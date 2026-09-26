@echo off
setlocal
set "DIR=%~dp0"
set "MAVEN_VERSION=3.9.9"
set "MAVEN_HOME=%USERPROFILE%\.m2\wrapper\dists\apache-maven-%MAVEN_VERSION%\apache-maven-%MAVEN_VERSION%"

if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
    echo Downloading and setting up Apache Maven %MAVEN_VERSION%...
    powershell -NoProfile -ExecutionPolicy Bypass -Command ^
        "$distDir = [System.IO.Path]::Combine($env:USERPROFILE, '.m2', 'wrapper', 'dists', 'apache-maven-%MAVEN_VERSION%');" ^
        "New-Item -ItemType Directory -Force -Path $distDir | Out-Null;" ^
        "$zipFile = [System.IO.Path]::Combine($distDir, 'apache-maven-%MAVEN_VERSION%-bin.zip');" ^
        "$url = 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.zip';" ^
        "if (-not (Test-Path $zipFile)) {" ^
        "    Write-Host 'Downloading Maven distribution from' $url '...';" ^
        "    [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.SecurityProtocolType]::Tls12 -bor [System.Net.SecurityProtocolType]::Tls13;" ^
        "    $webClient = New-Object System.Net.WebClient;" ^
        "    $webClient.DownloadFile($url, $zipFile);" ^
        "}" ^
        "Write-Host 'Extracting Maven...';" ^
        "Expand-Archive -Path $zipFile -DestinationPath $distDir -Force;" ^
        "Write-Host 'Maven setup complete.'"
)

if exist "%MAVEN_HOME%\bin\mvn.cmd" (
    "%MAVEN_HOME%\bin\mvn.cmd" %*
) else (
    echo Error: Failed to find or download Maven executable at "%MAVEN_HOME%\bin\mvn.cmd"
    exit /b 1
)
