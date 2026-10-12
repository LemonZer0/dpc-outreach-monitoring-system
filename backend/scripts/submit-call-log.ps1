param(
    [string] $ApiUrl = "http://localhost:8080/api/call-records"
)

$ErrorActionPreference = "Stop"

function Read-RequiredValue([string] $prompt) {
    do {
        $value = (Read-Host $prompt).Trim()
        if ([string]::IsNullOrWhiteSpace($value)) {
            Write-Host "This field is required." -ForegroundColor Yellow
        }
    } while ([string]::IsNullOrWhiteSpace($value))

    return $value
}

Write-Host "Submit call log metadata to $ApiUrl`n"

do {
    $dpcIdText = Read-RequiredValue "DPC ID"
    $dpcId = 0L
    $validDpcId = [long]::TryParse($dpcIdText, [ref] $dpcId) -and $dpcId -gt 0
    if (-not $validDpcId) {
        Write-Host "Enter a positive numeric DPC ID." -ForegroundColor Yellow
    }
} while (-not $validDpcId)

$phoneNumber = Read-RequiredValue "Phone number"

$timestampText = Read-RequiredValue "Call timestamp (ISO 8601 with timezone, e.g. 2026-10-13T10:00:00Z)"
do {
    $callTimestamp = [DateTimeOffset]::MinValue
    $hasTimezone = $timestampText -match '(?i)(Z|[+-]\d{2}:\d{2})$'
    $validTimestamp = $hasTimezone -and [DateTimeOffset]::TryParse(
        $timestampText,
        [Globalization.CultureInfo]::InvariantCulture,
        [Globalization.DateTimeStyles]::None,
        [ref] $callTimestamp)
    if (-not $validTimestamp) {
        Write-Host "Enter a valid ISO 8601 timestamp with Z or a UTC offset." -ForegroundColor Yellow
        $timestampText = Read-RequiredValue "Call timestamp"
    }
} while (-not $validTimestamp)

do {
    $durationText = Read-RequiredValue "Call duration in seconds"
    $durationSeconds = 0
    $validDuration = [int]::TryParse($durationText, [ref] $durationSeconds) -and $durationSeconds -ge 0
    if (-not $validDuration) {
        Write-Host "Enter a non-negative whole number of seconds." -ForegroundColor Yellow
    }
} while (-not $validDuration)

$requestBody = @{
    dpcId = $dpcId
    phoneNumber = $phoneNumber
    callTimestamp = $callTimestamp.ToUniversalTime().ToString("o")
    durationSeconds = $durationSeconds
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod `
        -Uri $ApiUrl `
        -Method Post `
        -ContentType "application/json" `
        -Body $requestBody

    Write-Host "`nCall record submitted successfully:" -ForegroundColor Green
    $response | Format-List id, dpcId, contactId, phoneNumber, callTimestamp, durationSeconds, verificationStatus
} catch {
    Write-Host "`nCall record submission failed: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.ErrorDetails.Message) {
        Write-Host $_.ErrorDetails.Message
    }
    throw "Call record submission failed."
}
