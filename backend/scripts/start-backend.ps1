$ErrorActionPreference = "Stop"

$backendDirectory = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$hadPreviousPassword = Test-Path Env:DB_PASSWORD
$previousPassword = $env:DB_PASSWORD
$hadPreviousJavaHome = Test-Path Env:JAVA_HOME
$previousJavaHome = $env:JAVA_HOME
$hadPreviousProfiles = Test-Path Env:SPRING_PROFILES_ACTIVE
$previousProfiles = $env:SPRING_PROFILES_ACTIVE
$hadPreviousDemoPhone = Test-Path Env:DEMO_CONTACT_PHONE
$previousDemoPhone = $env:DEMO_CONTACT_PHONE
$passwordBuffer = [IntPtr]::Zero
$adminPasswordBuffer = [IntPtr]::Zero
$hadPreviousAdminUsername = Test-Path Env:ADMIN_USERNAME
$previousAdminUsername = $env:ADMIN_USERNAME
$hadPreviousAdminPassword = Test-Path Env:ADMIN_PASSWORD
$previousAdminPassword = $env:ADMIN_PASSWORD
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

do {
    $demoChoice = (Read-Host "Enable demo mode for this run? (Y/N, default N)").Trim()
    if ([string]::IsNullOrWhiteSpace($demoChoice) -or $demoChoice -match '(?i)^n(o)?$') {
        $enableDemoMode = $false
        $validDemoChoice = $true
    } elseif ($demoChoice -match '(?i)^y(es)?$') {
        $enableDemoMode = $true
        $validDemoChoice = $true
    } else {
        Write-Host "Enter Y to enable demo mode or N to disable it." -ForegroundColor Yellow
        $validDemoChoice = $false
    }
} while (-not $validDemoChoice)

if ($enableDemoMode) {
    do {
        $demoContactPhone = (Read-Host "Phone number for the demo contact").Trim()
        if ([string]::IsNullOrWhiteSpace($demoContactPhone)) {
            Write-Host "A phone number is required when demo mode is enabled." -ForegroundColor Yellow
        }
    } while ([string]::IsNullOrWhiteSpace($demoContactPhone))

    $activeProfiles = @($previousProfiles -split ',' | ForEach-Object { $_.Trim() } | Where-Object { $_ })
    if ($activeProfiles -notcontains "demo") {
        $activeProfiles += "demo"
    }
    $env:SPRING_PROFILES_ACTIVE = $activeProfiles -join ","
    $env:DEMO_CONTACT_PHONE = $demoContactPhone
} else {
    $activeProfiles = @($previousProfiles -split ',' | ForEach-Object { $_.Trim() } | Where-Object { $_ -and $_ -ne "demo" })
    if ($activeProfiles.Count -gt 0) {
        $env:SPRING_PROFILES_ACTIVE = $activeProfiles -join ","
    } else {
        Remove-Item Env:SPRING_PROFILES_ACTIVE -ErrorAction SilentlyContinue
    }
}

try {
    $javaHomes = @()
    if (-not [string]::IsNullOrWhiteSpace($env:JAVA_HOME)) {
        $javaHomes += $env:JAVA_HOME
    }

    $javaCommand = Get-Command java -ErrorAction SilentlyContinue
    if ($javaCommand -and $javaCommand.Source) {
        $javaHomes += Split-Path (Split-Path $javaCommand.Source -Parent) -Parent
    }

    foreach ($javaRoot in @(
            (Join-Path $env:ProgramFiles "Java"),
            (Join-Path $env:ProgramFiles "Eclipse Adoptium"))) {
        if (Test-Path $javaRoot) {
            $javaHomes += Get-ChildItem $javaRoot -Directory -Filter "jdk*" |
                Select-Object -ExpandProperty FullName
        }
    }

    $availableJdks = foreach ($javaHome in ($javaHomes | Where-Object { $_ } | Select-Object -Unique)) {
        $javaExecutable = Join-Path $javaHome "bin\java.exe"
        if (-not (Test-Path $javaExecutable)) {
            continue
        }

        $versionOutput = (& $env:ComSpec /d /c "`"$javaExecutable`" -version 2>&1" | Out-String)
        if ($versionOutput -match '(?:version\s+"|openjdk\s+)(?<major>\d+)') {
            $majorVersion = [int]$Matches.major
            if ($majorVersion -ge 21) {
                [PSCustomObject]@{
                    Home = (Resolve-Path $javaHome).Path
                    MajorVersion = $majorVersion
                }
            }
        }
    }

    $compatibleJdk = $availableJdks | Sort-Object MajorVersion -Descending | Select-Object -First 1
    if (-not $compatibleJdk) {
        throw "Java 21 or newer is required, but no compatible JDK was found. Install JDK 21+ and set JAVA_HOME to its installation directory."
    }
    $env:JAVA_HOME = $compatibleJdk.Home
    Write-Host "Using Java $($compatibleJdk.MajorVersion) from $env:JAVA_HOME"

    if ([string]::IsNullOrWhiteSpace($env:ADMIN_USERNAME)) {
        do {
            $env:ADMIN_USERNAME = (Read-Host "Admin username for contact/company write access").Trim()
            if ([string]::IsNullOrWhiteSpace($env:ADMIN_USERNAME)) {
                Write-Host "An admin username is required to enable protected write operations." -ForegroundColor Yellow
            }
        } while ([string]::IsNullOrWhiteSpace($env:ADMIN_USERNAME))
    }

    if ([string]::IsNullOrWhiteSpace($env:ADMIN_PASSWORD)) {
        $secureAdminPassword = Read-Host "Admin password for '$env:ADMIN_USERNAME'" -AsSecureString
        if ($secureAdminPassword.Length -eq 0) {
            throw "The admin password cannot be empty."
        }

        $adminPasswordBuffer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureAdminPassword)
        $env:ADMIN_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($adminPasswordBuffer)
    }

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
    Write-Host "Demo mode: $(if ($enableDemoMode) { 'enabled' } else { 'disabled' })"
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
                Write-Host "  Dashboard: http://localhost:$serverPort/" -ForegroundColor Green
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
    if ($adminPasswordBuffer -ne [IntPtr]::Zero) {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($adminPasswordBuffer)
    }

    if ($hadPreviousPassword) {
        $env:DB_PASSWORD = $previousPassword
    } else {
        Remove-Item Env:DB_PASSWORD -ErrorAction SilentlyContinue
    }

    if ($hadPreviousJavaHome) {
        $env:JAVA_HOME = $previousJavaHome
    } else {
        Remove-Item Env:JAVA_HOME -ErrorAction SilentlyContinue
    }

    if ($hadPreviousProfiles) {
        $env:SPRING_PROFILES_ACTIVE = $previousProfiles
    } else {
        Remove-Item Env:SPRING_PROFILES_ACTIVE -ErrorAction SilentlyContinue
    }

    if ($hadPreviousDemoPhone) {
        $env:DEMO_CONTACT_PHONE = $previousDemoPhone
    } else {
        Remove-Item Env:DEMO_CONTACT_PHONE -ErrorAction SilentlyContinue
    }

    if ($hadPreviousAdminUsername) {
        $env:ADMIN_USERNAME = $previousAdminUsername
    } else {
        Remove-Item Env:ADMIN_USERNAME -ErrorAction SilentlyContinue
    }
    if ($hadPreviousAdminPassword) {
        $env:ADMIN_PASSWORD = $previousAdminPassword
    } else {
        Remove-Item Env:ADMIN_PASSWORD -ErrorAction SilentlyContinue
    }
}
