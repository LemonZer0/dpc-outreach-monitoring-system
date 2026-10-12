$ErrorActionPreference = "Stop"

$apiBaseUrl = $env:DPC_API_BASE_URL
if ([string]::IsNullOrWhiteSpace($apiBaseUrl)) {
    $apiBaseUrl = "http://localhost:8080"
}
$apiBaseUrl = $apiBaseUrl.TrimEnd("/")

function Read-PositiveId([string] $prompt) {
    do {
        $value = (Read-Host $prompt).Trim()
        $id = 0L
        $valid = [long]::TryParse($value, [ref] $id) -and $id -gt 0
        if (-not $valid) {
            Write-Host "Enter a positive numeric ID." -ForegroundColor Yellow
        }
    } while (-not $valid)

    return $id
}

function Show-Response($response) {
    if ($null -eq $response) {
        Write-Host "The API returned no records."
        return
    }

    $response | ConvertTo-Json -Depth 6
}

function Show-RequestError($errorRecord) {
    Write-Host "Request failed: $($errorRecord.Exception.Message)" -ForegroundColor Red
    if ($errorRecord.ErrorDetails.Message) {
        Write-Host $errorRecord.ErrorDetails.Message
    }
}

do {
    Write-Host "`nDPC Call Record API Client"
    Write-Host "Backend: $apiBaseUrl"
    Write-Host "1. Check backend health"
    Write-Host "2. Submit call log data"
    Write-Host "3. Get a call record by ID"
    Write-Host "4. List call records by DPC ID"
    Write-Host "5. Exit"
    $choice = Read-Host "Choose an option"

    try {
        switch ($choice) {
            "1" {
                $response = Invoke-RestMethod -Uri "$apiBaseUrl/api/health" -Method Get
                Show-Response $response
            }
            "2" {
                & "$PSScriptRoot\submit-call-log.ps1" -ApiUrl "$apiBaseUrl/api/call-records"
            }
            "3" {
                $recordId = Read-PositiveId "Call record ID"
                $response = Invoke-RestMethod -Uri "$apiBaseUrl/api/call-records/$recordId" -Method Get
                Show-Response $response
            }
            "4" {
                $dpcId = Read-PositiveId "DPC ID"
                $response = Invoke-RestMethod -Uri "$apiBaseUrl/api/call-records?dpcId=$dpcId" -Method Get
                Show-Response $response
            }
            "5" {
                Write-Host "Goodbye."
            }
            default {
                Write-Host "Choose an option from 1 to 5." -ForegroundColor Yellow
            }
        }
    } catch {
        Show-RequestError $_
    }
} while ($choice -ne "5")
