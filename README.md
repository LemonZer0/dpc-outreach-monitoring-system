# DPC Outreach Call Monitoring System

## CS5013 Course Project

A system for monitoring and verifying company outreach calls made by
Department Placement Coordinators (DPCs).

## Problem

The Placement & Internship CORE currently relies on DPCs manually
updating a Google Sheet with company outreach information. This makes
it difficult to verify whether outreach calls were actually made.

## Goal

The system automatically captures call metadata from an Android device,
matches the call to a verified recruiter/company contact, stores the
result in PostgreSQL, and provides the Placement & Internship CORE with
a dashboard and automatically synchronized Google Sheet.

## Architecture

```text
Android App
    |
    v
Normal Android Phone Dialer
    |
    v
Android Call Log
    |
    v
Spring Boot Backend
    |
    v
PostgreSQL Database
    |
    v
CORE Dashboard
    |
    v
Google Sheets Integration
```

*Note: This represents the planned system architecture.*

## Backend demo data

The backend can seed one local demo DPC, company, and contact when the
`demo` Spring profile is enabled. Set `DEMO_CONTACT_PHONE` to the phone
number whose calls should match the demo contact. The seeder normalizes
the number and reuses existing demo records on later starts.

From PowerShell, start the backend with the launcher:

```powershell
cd backend
.\scripts\start-backend.ps1
```

The backend requires JDK 21 or newer. The launcher checks installed JDKs and
selects a compatible version automatically, including when `JAVA_HOME` points
to an older Java runtime.

The launcher asks whether to show Maven logs, whether to enable demo mode,
and (if enabled) which phone number to use for the demo contact. If
`ADMIN_USERNAME` and `ADMIN_PASSWORD` are not already set, it prompts for
admin credentials; the password prompt is masked. If `DB_PASSWORD` is not
already set, it also asks for the PostgreSQL password using a masked prompt.
These launch settings are restored after the backend stops; passwords are
not saved to a file. A successful application startup confirms the
PostgreSQL connection.

To start manually and enable demo data instead, run:

```powershell
cd backend
$env:DB_PASSWORD = "your-local-database-password"
$env:ADMIN_USERNAME = "admin"
$env:ADMIN_PASSWORD = "your-local-admin-password"
$env:DEMO_CONTACT_PHONE = "+91 98765 43210"
$env:SPRING_PROFILES_ACTIVE = "demo"
.\mvnw.cmd spring-boot:run
```

The startup log prints the demo DPC, company, and contact IDs. Demo
records are not seeded unless the `demo` profile is enabled.

To use the interactive API client while the backend is running, open a
second PowerShell window in `backend/` and run:

```powershell
.\scripts\call-record-client.ps1
```

The menu lets you check backend health, submit call log data, retrieve a
record by ID, list records by DPC ID, and list known contacts. The backend
URL defaults to `http://localhost:8080`; set `DPC_API_BASE_URL` to use
another host.

The contact directory is available at `GET /api/contacts`. Each entry
includes the contact ID, name, phone number, and company ID and name.
This list is optional convenience data; call submissions are still
matched by phone number and are not restricted to listed contacts.

The read-only `GET /api/dpcs` endpoint lists DPC IDs and names for
dashboard selection; it does not expose account information.

## Dashboard

Start the backend with `backend/scripts/start-backend.ps1`. The backend serves
the dashboard and API from the same origin; open `http://localhost:8080/` after
the startup message appears. No separate dashboard server is needed. Press
Enter or select **Continue**. Choose
**Sign in** for the outreach dashboard, or
**Sign in as Admin** and enter the configured admin credentials for the
management dashboard. The normal dashboard lets a user select a DPC by
name and review its calls, latest call-verification results, and company
outreach decisions. An unmatched call can be matched from its row by entering
a contact name and selecting a company; the system creates that contact and
updates the call's verification status. If multiple contacts share a number,
new calls are kept unmatched rather than assigned to the wrong person, so they
can be manually matched from the dashboard.

On connection, the dashboard checks both backend health and the DPC directory
API. If health succeeds but an API request returns 404, the server is reachable
but is likely an older or different backend; stop it and start the backend from
this project before reconnecting.

The **Sign in for DPC Simulation** view requires admin credentials and lets an
admin create a call log for any registered DPC by entering a phone number,
call time, and duration. It also lists available contacts with their phone
numbers and companies; refresh the directory to load the latest DPCs and
contacts. **View simulated call logs** opens a DPC-filtered history that
contains only records created by the simulator. The normal dashboard shows
only Android app submissions and labels each as **Dialer** or **Manual
metadata**. The simulator history is available only after admin sign-in.
Simulated and app calls use the same verification rules.

The Android app marks calls found in the outgoing call log as `DIALER` and
manually entered timestamps and durations as `MANUAL_METADATA`. Records
created from the simulation form are stored as `SIMULATION`. Existing records
are preserved as app submissions; their type remains unrecorded because older
versions did not distinguish dialer calls from manual metadata.

The admin dashboard lets CORE create, rename, and delete companies; create,
edit, and delete contacts; create DPC entries; and set each company's
manual decision to `Confirmed`, `Pending`, `Declined`, or `Not set`.
Contact numbers are normalized by the backend. Deleting a contact or company
requires re-entering the current admin password. Call records are retained and
become unmatched when their contact is deleted; deleting a company also removes
its contacts and outreach statuses. Call verification remains automatic and
separate from the manual decision.
Any call record in the normal or simulated call log can be permanently deleted
after confirming with admin credentials. If deleting the latest app call for
a company, the dashboard recalculates that company's call-verification summary
from its latest remaining eligible app call.
The dashboard and command-line client both require password confirmation for
deletes.
Admin credentials are validated by the backend, held only in memory, and
cleared on logout; they are not saved in browser storage. The backend
permits dashboard requests only from the documented local origins; use
HTTPS for a remote backend.

## Admin contact and company management

Start the backend with `ADMIN_USERNAME` and `ADMIN_PASSWORD` configured.
Admin contact and company management is available in the dashboard or
through the optional command-line client. To use the client, open another
PowerShell window in `backend/` and run:

```powershell
.\scripts\admin-client.ps1
```

The admin client prompts for the configured username and password and
provides the same contact and company operations.

Admin write and company-management routes require HTTP Basic credentials.
These include `POST /api/admin/dpcs`; the `POST`, `PUT`, and `DELETE`
company and contact routes; and
`PUT /api/admin/companies/{companyId}/outreach-status`. Contact listing,
DPC listing, call-record, and outreach-status read APIs remain available
without admin credentials. Admin tools permit HTTP only for a loopback
backend; use HTTPS for a remote backend because Basic credentials must not
be sent over an unencrypted connection.

`GET /api/call-records?dpcId={dpcId}` lists app-submitted calls only.
Simulation call history uses the admin-protected
`GET /api/admin/simulation/call-records?dpcId={dpcId}` endpoint.

## Main Components

- Android Call Client
- Spring Boot Backend
- PostgreSQL Database
- CORE Dashboard
- Google Sheets Integration

## Core Workflow

1. DPC selects a company/contact.
2. Android application launches the normal phone dialer.
3. DPC makes the phone call.
4. Android records the call in the call log.
5. Android application obtains call metadata.
6. Metadata is sent to the backend.
7. Backend validates and matches the call.
8. Verification result is stored in PostgreSQL.
9. Dashboard displays updated outreach information.
10. Google Sheet is synchronized automatically.

## Scope

The system verifies recorded outreach activity using call metadata such
as phone number, timestamp, duration, and call direction.

Calls matched to a known contact with a duration greater than 60 seconds
are labeled `MATCHED_LONG_CALL`. This is a duration-based heuristic only;
it does not verify that a discussion took place.

The system does NOT:

- record calls
- transcribe calls
- inspect conversation content
- determine what was discussed
- determine whether the conversation was useful

## Technology

## Android emulator demo

Build the debug APK from PowerShell:

```powershell
cd android
.\gradlew.bat assembleDebug
```

The APK is written to
`android/app/build/outputs/apk/debug/app-debug.apk`. Run the backend with
demo mode enabled and use the DPC ID printed by its startup log. In the
Android app, set the backend URL to `http://10.0.2.2:8080` when the app
runs in an Android emulator or BlueStacks on the same computer. Load the
optional contact directory, enter a phone number, and submit call metadata.
For a real call, tap **Open dialer**, place the call, then return to the
app and check the call log. The app does not place calls without user
interaction.

BlueStacks may not include a dialer or expose call-log data. In that case,
use manual timestamp and duration entry to test submission, and use a
physical Android device to test the dialer and call-log flow. Manual
metadata submission has been tested with BlueStacks; physical-device
dialer and call-log behavior still needs testing.

### Backend
- Java
- Spring Boot
- Maven
- Spring Web
- Spring Data JPA
- PostgreSQL

### Android
- Android
- Kotlin

### Dashboard
- HTML, CSS, JavaScript

### Integration
- Google Sheets API
- Google OAuth

## Team

G. Sunisith Sagar  
CS23B021  
Team ID: 21
