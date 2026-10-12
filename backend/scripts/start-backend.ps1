$ErrorActionPreference = "Stop"

$backendDirectory = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$hadPreviousPassword = Test-Path Env:DB_PASSWORD
$previousPassword = $env:DB_PASSWORD
$passwordBuffer = [IntPtr]::Zero
$showMavenLogs = $true

do {
    $logChoice = (Read-Host "Show Maven and Spring Boot logs? (Y/N, default Y)").Trim()
    if ([string]::IsNullOrWhiteSpace($logChoice) -or $logChoice -match '(?i)^y(es)?$') {
        $showMavenLogs = $true
        $validLogChoice = $true
    } elseif ($logChoice -match '(?i)^n(o)?$') {
        $showMavenLogs = $false
        $validLogChoice = $true
    } else {
        Write-Host "Enter Y to show logs or N to hide them." -ForegroundColor Yellow
        $validLogChoice = $false
    }
} while (-not $validLogChoice)

try {
    if ([string]::IsNullOrWhiteSpace($env:DB_PASSWORD)) {
        $securePassword = Read-Host "PostgreSQL password for user 'postgres'" -AsSecureString
        if ($securePassword.Length -eq 0) {
            throw "The database password cannot be empty."
        }

        $passwordBuffer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
        $env:DB_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordBuffer)
    }

    if (-not (Test-Path (Join-Path $backendDirectory "mvnw.cmd"))) {
        throw "Maven wrapper not found in '$backendDirectory'."
    }

    Write-Host "Starting the backend from $backendDirectory"
    Write-Host "Spring Boot will connect to PostgreSQL at localhost:5432/dpc_outreach."
    Write-Host "Successful application startup confirms the database connection; startup errors will be shown below."
    Write-Host "Press Ctrl+C to stop the backend.`n"

    $databasePasswordRejected = $false
    $serverStarted = $false
    $serverPort = if ([string]::IsNullOrWhiteSpace($env:SERVER_PORT)) { "8080" } else { $env:SERVER_PORT }
    Push-Location $backendDirectory
    try {
        & .\mvnw.cmd spring-boot:run 2>&1 | ForEach-Object {
            $line = $_.ToString()
            if ($showMavenLogs) {
                Write-Host $line
            }

            if ($line -match '(?i)password authentication failed|SQLSTATE\s*:?\s*28P01') {
                $databasePasswordRejected = $true
            }

            if (-not $serverStarted -and $line -match '\bStarted\s+\S+\s+in\s+[\d.]+\s+seconds\b') {
                $serverStarted = $true
                Write-Host ""
                Write-Host "============================================================" -ForegroundColor Green
                Write-Host "  BACKEND STARTED SUCCESSFULLY" -ForegroundColor Green
                Write-Host "  API:    http://localhost:$serverPort" -ForegroundColor Green
                Write-Host "  Health: http://localhost:$serverPort/api/health" -ForegroundColor Green
                Write-Host "============================================================" -ForegroundColor Green
                Write-Host ""
            }
        }

        if ($databasePasswordRejected) {
            Write-Host "The PostgreSQL password is incorrect for user 'postgres'. Check the password and try again." -ForegroundColor Red
        }

        if ($LASTEXITCODE -ne 0) {
            Write-Host "Backend process exited with code $LASTEXITCODE." -ForegroundColor Red
            if (-not $showMavenLogs -and -not $databasePasswordRejected) {
                Write-Host "Logs were hidden. Run the launcher again and choose Y to see the startup error details." -ForegroundColor Yellow
            }
        }
    } finally {
        Pop-Location
    }
} finally {
    if ($passwordBuffer -ne [IntPtr]::Zero) {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordBuffer)
    }

    if ($hadPreviousPassword) {
        $env:DB_PASSWORD = $previousPassword
    } else {
        Remove-Item Env:DB_PASSWORD -ErrorAction SilentlyContinue
    }
}
