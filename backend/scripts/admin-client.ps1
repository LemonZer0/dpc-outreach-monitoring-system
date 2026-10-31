param(
    [string] $ApiBaseUrl = $env:DPC_API_BASE_URL
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($ApiBaseUrl)) {
    $ApiBaseUrl = "http://localhost:8080"
}
$ApiBaseUrl = $ApiBaseUrl.TrimEnd("/")

$baseUri = [Uri]$ApiBaseUrl
if ($baseUri.Scheme -ne "https" -and -not $baseUri.IsLoopback) {
    throw "Admin credentials require HTTPS unless the backend is on this computer."
}

$credential = Get-Credential -Message "Sign in with the backend admin account"
if ($null -eq $credential) {
    throw "Admin sign-in was cancelled."
}
$plainPassword = $credential.GetNetworkCredential().Password
$encodedCredentials = [Convert]::ToBase64String(
    [Text.Encoding]::UTF8.GetBytes("$($credential.UserName):$plainPassword"))
$headers = @{ Authorization = "Basic $encodedCredentials" }
$plainPassword = $null
$encodedCredentials = $null

function Invoke-ApiRequest(
    [string] $Method,
    [string] $Path,
    [object] $Body = $null,
    [string] $RequestAuthorization
) {
    if ([string]::IsNullOrWhiteSpace($RequestAuthorization)) {
        $RequestAuthorization = $headers.Authorization
    }
    $parameters = @{
        Uri = "$ApiBaseUrl$Path"
        Method = $Method
        Headers = @{ Authorization = $RequestAuthorization }
    }
    if ($null -ne $Body) {
        $parameters.ContentType = "application/json"
        $parameters.Body = $Body | ConvertTo-Json -Depth 6
    }

    return Invoke-RestMethod @parameters
}

function Invoke-ConfirmedDelete([string] $Path, [string] $ResourceLabel) {
    $confirmation = Read-Host "Type DELETE to delete $ResourceLabel"
    if ($confirmation -cne "DELETE") {
        Write-Host "Deletion cancelled."
        return
    }

    $securePassword = Read-Host -AsSecureString "Enter the current admin password"
    $passwordPointer = [IntPtr]::Zero
    try {
        $passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
        $password = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)
        $encoded = [Convert]::ToBase64String(
            [Text.Encoding]::UTF8.GetBytes("$($credential.UserName):$password"))
        $confirmationAuthorization = "Basic $encoded"
        $null = Invoke-ApiRequest -Method Delete -Path $Path -RequestAuthorization $confirmationAuthorization
        Write-Host "Deleted $ResourceLabel." -ForegroundColor Green
    } finally {
        if ($passwordPointer -ne [IntPtr]::Zero) {
            [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)
        }
        $password = $null
        $encoded = $null
        $confirmationAuthorization = $null
        $securePassword.Dispose()
    }
}

function Read-RequiredValue([string] $Prompt) {
    do {
        $value = (Read-Host $Prompt).Trim()
        if ([string]::IsNullOrWhiteSpace($value)) {
            Write-Host "This field is required." -ForegroundColor Yellow
        }
    } while ([string]::IsNullOrWhiteSpace($value))

    return $value
}

function Read-PositiveId([string] $Prompt) {
    do {
        $value = (Read-Host $Prompt).Trim()
        $id = 0L
        $valid = [long]::TryParse($value, [ref] $id) -and $id -gt 0
        if (-not $valid) {
            Write-Host "Enter a positive numeric ID." -ForegroundColor Yellow
        }
    } while (-not $valid)

    return $id
}

function Show-Companies {
    $companies = @(Invoke-ApiRequest -Method Get -Path "/api/admin/companies")
    if ($companies.Count -eq 0) {
        Write-Host "No companies are available."
        return
    }
    $companies | Format-Table id, name -AutoSize | Out-Host
}

function Read-CompanyId {
    $companies = @(Invoke-ApiRequest -Method Get -Path "/api/admin/companies")
    if ($companies.Count -eq 0) {
        throw "Create a company before adding or moving a contact."
    }
    $companies | Format-Table id, name -AutoSize | Out-Host

    do {
        $companyId = Read-PositiveId "Company ID"
        $selectedCompany = $companies | Where-Object { $_.id -eq $companyId } | Select-Object -First 1
        if ($null -eq $selectedCompany) {
            Write-Host "Choose an ID from the company list." -ForegroundColor Yellow
        }
    } while ($null -eq $selectedCompany)

    return $companyId
}

function Read-ContactValues {
    return @{
        name = Read-RequiredValue "Contact name"
        phoneNumber = Read-RequiredValue "Phone number"
        companyId = Read-CompanyId
    }
}

try {
    $null = Invoke-ApiRequest -Method Get -Path "/api/admin/companies"
} catch {
    throw "Admin sign-in failed or admin credentials are not configured on the backend. $($_.Exception.Message)"
}

Write-Host "Signed in to $ApiBaseUrl as $($credential.UserName)."

do {
    Write-Host "`nDPC Outreach Admin"
    Write-Host "1. List contacts"
    Write-Host "2. List companies"
    Write-Host "3. Create contact"
    Write-Host "4. Edit contact"
    Write-Host "5. Delete contact"
    Write-Host "6. Create company"
    Write-Host "7. Rename company"
    Write-Host "8. Delete company"
    Write-Host "9. Exit"
    $choice = Read-Host "Choose an option"

    try {
        switch ($choice) {
            "1" {
                $contacts = @(Invoke-ApiRequest -Method Get -Path "/api/contacts")
                if ($contacts.Count -eq 0) {
                    Write-Host "No contacts are available."
                } else {
                    $contacts | Format-Table id, name, phoneNumber, companyName -AutoSize
                }
            }
            "2" {
                Show-Companies
            }
            "3" {
                $contact = Read-ContactValues
                $result = Invoke-ApiRequest -Method Post -Path "/api/admin/contacts" -Body $contact
                Write-Host "Contact created:" -ForegroundColor Green
                $result | Format-List id, name, phoneNumber, companyId, companyName
            }
            "4" {
                $contactId = Read-PositiveId "Contact ID"
                $contact = Read-ContactValues
                $result = Invoke-ApiRequest -Method Put -Path "/api/admin/contacts/$contactId" -Body $contact
                Write-Host "Contact updated:" -ForegroundColor Green
                $result | Format-List id, name, phoneNumber, companyId, companyName
            }
            "5" {
                $contactId = Read-PositiveId "Contact ID"
                Invoke-ConfirmedDelete -Path "/api/admin/contacts/$contactId" -ResourceLabel "contact $contactId"
            }
            "6" {
                $companyName = Read-RequiredValue "Company name"
                $result = Invoke-ApiRequest -Method Post -Path "/api/admin/companies" -Body @{ name = $companyName }
                Write-Host "Company created:" -ForegroundColor Green
                $result | Format-List id, name
            }
            "7" {
                $companyId = Read-PositiveId "Company ID"
                $companyName = Read-RequiredValue "New company name"
                $result = Invoke-ApiRequest -Method Put -Path "/api/admin/companies/$companyId" -Body @{ name = $companyName }
                Write-Host "Company renamed:" -ForegroundColor Green
                $result | Format-List id, name
            }
            "8" {
                $companyId = Read-PositiveId "Company ID"
                Invoke-ConfirmedDelete -Path "/api/admin/companies/$companyId" -ResourceLabel "company $companyId"
            }
            "9" {
                Write-Host "Goodbye."
            }
            default {
                Write-Host "Choose an option from 1 to 9." -ForegroundColor Yellow
            }
        }
    } catch {
        Write-Host "Request failed: $($_.Exception.Message)" -ForegroundColor Red
        if ($_.ErrorDetails.Message) {
            Write-Host $_.ErrorDetails.Message
        }
    }
} while ($choice -ne "9")
