# DPC Outreach Call Monitoring System

**CS5013 Course Project**

A system for monitoring and verifying company outreach calls made by
Department Placement Coordinators (DPCs).

## Contents

- [Project overview](#project-overview): problem, goal, and architecture
- [Repository and local setup](#repository-and-local-setup): code layout, backend demo data, and Sheets setup
- [Deployment and handoff](#deployment-and-handoff): Render demo and course Docker image
- [Dashboard and administration](#dashboard-and-administration): user views, admin tools, and API clients
- [Technology stack](#technology-stack): backend, Android, dashboard, and integrations
- [Android client](#android-client): build and emulator or device demo
- [Team and support](#team-and-support): project team and contact route

## Project overview

### Problem

The Placement & Internship CORE currently relies on DPCs manually
updating a Google Sheet with company outreach information. This makes
it difficult to verify whether outreach calls were actually made.

### Goal

The system automatically captures call metadata from an Android device,
matches the call to a verified recruiter/company contact, stores the
result in PostgreSQL, and provides the Placement & Internship CORE with
a dashboard and automatically synchronized Google Sheet.

### Architecture

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

## System summary

### Main components

- Android Call Client
- Spring Boot Backend
- PostgreSQL Database
- CORE Dashboard
- Google Sheets Integration

### Core workflow

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

### Scope

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

### Technology stack

#### Backend

- Java
- Spring Boot
- Maven
- Spring Web
- Spring Data JPA
- PostgreSQL

#### Android

- Android
- Kotlin

#### Dashboard

- HTML, CSS, JavaScript

#### Integration

- Google Sheets API
- Google OAuth

## Repository and local setup

### Repository structure

The main source files and their roles are:

```text
.
├── android/                         Kotlin Android client and Gradle build
│   └── app/src/main/
│       ├── java/.../MainActivity.kt  App screens, API calls, and call metadata flow
│       ├── res/                      App strings, styles, and network configuration
│       └── AndroidManifest.xml       App permissions and component declarations
├── backend/                         Spring Boot API and PostgreSQL persistence
│   ├── src/main/java/.../
│   │   ├── controller/               HTTP endpoints
│   │   ├── service/                  Matching, verification, admin, and Sheets logic
│   │   ├── entity/                   Database entities and domain enums
│   │   ├── repository/               Spring Data database access
│   │   ├── dto/                      API request and response objects
│   │   ├── config/                   Security and async configuration
│   │   └── bootstrap/                Optional demo-data seeder
│   ├── src/main/resources/
│   │   ├── db/migration/             Flyway schema migrations
│   │   └── application.properties    Backend defaults and integration settings
│   ├── src/test/                     Unit, controller, and database integration tests
│   ├── scripts/                      PowerShell backend and API helper scripts
│   └── pom.xml                        Maven dependencies and build configuration
├── dashboard/                       Browser dashboard HTML, CSS, and JavaScript
├── docs/                            Project report background and supporting notes
│   └── PROJECT_REPORT_CONTEXT.md    Verified project facts for report writing
├── Dockerfile                       Render backend/dashboard image
├── Dockerfile.course-demo           Course handoff image with local PostgreSQL
├── docker-entrypoint.sh             Starts PostgreSQL and the app in that image
├── render.yaml                      Render deployment blueprint
├── .dockerignore                     Files excluded from Docker build contexts
├── .gitignore                        Local files excluded from Git
└── README.md                         Setup, deployment, API, and demo instructions
```

The course Docker image packages the backend and dashboard from `backend/` and
`dashboard/`; it does not contain the Android app. Build it with
`Dockerfile.course-demo`. The regular `Dockerfile` is used by the Render
deployment. Android build output is written under
`android/app/build/outputs/` and is ignored by Git.

### Backend demo data

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

### Google Sheets mirror

The backend can mirror its database into six tabs in a Google spreadsheet:
**Call records**, **DPCs**, **Contacts**, **Companies**, **DPC outreach
status**, and **CORE outreach decisions**. Simulated and app call records
share the Call records tab and are distinguished by the **Call source** value.
The backend creates any missing managed tabs and replaces their contents on
each sync; other tabs in the spreadsheet are left untouched. Database changes
are synchronized after commit, and the backend performs a sync when it starts.
If Google Sheets is unavailable, the database write still succeeds; the sync
status reports the failure and an admin can retry it.

To configure it:

1. In Google Cloud, enable the Google Sheets API and create a service account
   with a JSON key.
2. Create or choose a spreadsheet and share it with the service account's
   `client_email` as an editor. The spreadsheet ID is the value between `/d/`
   and `/edit` in its URL.
3. Keep the service-account JSON key outside the repository. In PowerShell,
   set the spreadsheet ID and key path before starting the backend:

   ```powershell
   $env:GOOGLE_SHEETS_SPREADSHEET_ID = "your-spreadsheet-id"
   $env:GOOGLE_SHEETS_CREDENTIALS_FILE = "C:\secure\service-account-key.json"
   Set-Location .\backend
   .\scripts\start-backend.ps1
   ```

4. Check sync state at
   `GET /api/admin/google-sheets/status` or trigger a full resync with
   `POST /api/admin/google-sheets/sync`; both endpoints require admin
   credentials.

The spreadsheet contains call phone numbers and contact details. Share it
only with people authorized to access this information. Do not commit the
service-account key.

## Deployment and handoff

### Free student-demo deployment

The repository includes a Render Blueprint and Dockerfile for deploying the
Spring Boot backend and dashboard to Render, with PostgreSQL hosted separately
on Neon's free tier. This configuration is intended for a class demo, not
production or sensitive real-world call data. Free services can sleep, restart,
or have provider-specific storage and usage limits. Check current provider
limits before relying on the deployment.

1. Create a Neon project in the Singapore region and create its database.
   Copy the connection details. Use Neon's pooled connection host if provided,
   and convert its JDBC URL to this form:
   `jdbc:postgresql://<host>/<database>?sslmode=require`
2. Push the repository to GitHub and create a new Blueprint in Render from
   that repository, using the root `render.yaml`. Set the environment values
   prompted by Render:
   - `SPRING_DATASOURCE_URL`: the JDBC URL from step 1
   - `SPRING_DATASOURCE_USERNAME` and `SPRING_DATASOURCE_PASSWORD`: Neon role
     credentials
   - `ADMIN_USERNAME` and `ADMIN_PASSWORD`: new, strong dashboard admin
     credentials
   - `GOOGLE_SHEETS_SPREADSHEET_ID` and
     `GOOGLE_SHEETS_CREDENTIALS_FILE`: optional; leave both unset to disable
     the Sheets mirror. To enable it, upload the service-account JSON as a
     Render secret file and set the file variable to its mounted path.
3. Wait for the first Docker build/deploy to finish. Render assigns an HTTPS
   URL such as `https://dpc-outreach-monitoring.onrender.com`. Open
   `<service-url>/api/health`; a healthy deployment returns `status: UP`.
   The first request after the free service sleeps can take about a minute.
   Flyway creates the initial schema and applies subsequent migrations on a
   new empty database.
4. Open the dashboard at the service root URL. It automatically connects to
   the backend on that same URL; no separate dashboard backend URL is needed.
   Use the configured admin credentials for protected dashboard actions. In
   the Android app, the deployed Render HTTPS URL is the default backend.
   Load contacts and test a call submission; change the app's URL only when
   intentionally testing against a local backend.

Render's free web service has an ephemeral filesystem, so the database must
remain in Neon and must not be stored in the container. Store credentials in
Render's environment/secret-file settings, never in the repository. Do not
reuse development admin passwords for a public deployment.
The Android client allows up to 90 seconds for a response so its first request
can wait for a sleeping free web service to start.

### Docker image for course staff

The course-demo image contains the backend, dashboard, and local PostgreSQL in
one container. It needs no hosted database, Compose file, or Google Sheets
credentials. PostgreSQL listens only inside the container. The image generates
a fresh admin password at startup and prints it in the container logs; no
personal admin credentials are built into the image. Database files are stored
in the container's `/var/lib/postgresql/data`; mount a Docker volume there to
keep data when replacing the container.

The submitted image is tagged `dpc-outreach-monitoring:232dd51d15c8c830d69168a17eb23af5396be88e` and is built from the `final-submission` Git tag. Staff can build the image from source using the steps below. If the archive is included with the course submission, it can be loaded directly instead.

To build an archive from the pinned source (requires Git and Docker Desktop):

```powershell
git clone https://github.com/LemonZer0/dpc-outreach-monitoring-system.git
Set-Location .\dpc-outreach-monitoring-system
git checkout final-submission
$sha = (git rev-parse HEAD).Trim()
$image = "dpc-outreach-monitoring:$sha"
docker build --pull -f Dockerfile.course-demo -t $image .
docker save -o ".\dpc-outreach-monitoring-$sha.tar" $image
```

The resulting archive is `dpc-outreach-monitoring-232dd51d15c8c830d69168a17eb23af5396be88e.tar` (about 231 MiB). If you receive this archive with the course submission, load it with:

```powershell
docker load -i .\dpc-outreach-monitoring-232dd51d15c8c830d69168a17eb23af5396be88e.tar
```

Then start the container, whether you built or loaded the image:

```powershell
docker run -d --name dpc-outreach-demo -p 127.0.0.1:8080:8080 -v dpc-outreach-data:/var/lib/postgresql/data dpc-outreach-monitoring:232dd51d15c8c830d69168a17eb23af5396be88e
```

Open `http://127.0.0.1:8080/`. Retrieve the generated admin credentials with
`docker logs dpc-outreach-demo`. Stop the demo with
`docker stop dpc-outreach-demo`; remove its container with
`docker rm dpc-outreach-demo`. The named database volume remains available for
the next run. The image also runs without a volume, in which case its database
is removed with the container. The Android APK is a separate artifact.

The image contains the backend, dashboard, and PostgreSQL; the Android APK is
a separate artifact and is not included. For help with the source or image,
contact the project team through the
[GitHub repository issues](https://github.com/LemonZer0/dpc-outreach-monitoring-system/issues).

## Dashboard and administration

### API and command-line clients

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

### Dashboard

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

For app calls, the DPC can also report whether they spoke with a person,
reached voicemail, encountered forwarding or an automated system, had no
answer, or could not determine the outcome. Dialer submissions load call
details into the form; the DPC selects an outcome and explicitly taps
**Submit dialer call**. Manual submissions select the outcome before tapping
**Submit call metadata**.
This self-reported outcome is stored separately and does not change call
verification status. Verification continues to depend on contact matching,
duration, and duplicate detection; call duration alone cannot establish that a
person answered.

Verification status is a technical classification and does not include the
self-reported outcome: an answered voicemail with a long duration can still be
`MATCHED_LONG_CALL`. Use the separate reported-outcome field to distinguish
that from a conversation with a person.

The admin dashboard lets CORE create, rename, and delete companies; create,
edit, and delete contacts; create and delete DPC entries; and set each company's
manual decision to `Confirmed`, `Pending`, `Declined`, or `Not set`.
Contact numbers are normalized by the backend. Deleting a contact or company
requires re-entering the current admin password. Call records are retained and
become unmatched when their contact is deleted; deleting a company also removes
its contacts and outreach statuses. Call verification remains automatic and
separate from the manual decision.
Deleting a DPC requires admin password confirmation and permanently removes
all call records belonging to that DPC. New DPCs are assigned the lowest
available positive ID. A deleted ID may therefore be assigned to a different
DPC; update any Android app still configured with the deleted DPC's ID.
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

### Admin contact and company management

Start the backend with `ADMIN_USERNAME` and `ADMIN_PASSWORD` configured.
Admin contact and company management is available in the dashboard or
through the optional command-line client. To use the client, open another
PowerShell window in `backend/` and run:

```powershell
.\scripts\admin-client.ps1
```

The admin client prompts for the configured username and password and
provides contact, company, and DPC deletion operations. DPC deletion removes
all call history for that DPC; the freed ID can be reused.

Admin write and company-management routes require HTTP Basic credentials.
These include `POST /api/admin/dpcs`,
`DELETE /api/admin/dpcs/{dpcId}`, the `POST`, `PUT`, and `DELETE` company and
contact routes, and `PUT /api/admin/companies/{companyId}/outreach-status`.
Contact listing, DPC listing, call-record, and outreach-status read APIs
remain available without admin credentials. Admin tools permit HTTP only for
a loopback backend; use HTTPS for a remote backend because Basic credentials
must not be sent over an unencrypted connection.

`GET /api/call-records?dpcId={dpcId}` lists app-submitted calls only.
Simulation call history uses the admin-protected
`GET /api/admin/simulation/call-records?dpcId={dpcId}` endpoint.

## Android client

### Android emulator demo

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

For a physical phone, connect it and the laptop to the same Wi-Fi network,
find the laptop's current IPv4 address, and set the app's backend URL to
`http://<laptop-ip>:8080` (for example, `http://192.168.1.25:8080`). The app
saves this URL when loading contacts or submitting a call. Debug builds allow
HTTP to the configured address so a changed laptop IP does not require
rebuilding the APK. Release builds do not allow HTTP to arbitrary LAN addresses;
use HTTPS for release deployments.

BlueStacks may not include a dialer or expose call-log data. In that case,
use manual timestamp and duration entry to test submission, and use a
physical Android device to test the dialer and call-log flow. Manual
metadata submission has been tested with BlueStacks; physical-device
dialer and call-log behavior still needs testing.

## Team and support

G. Sunisith Sagar  
CS23B021  
Team ID: 21

Project support: [GitHub repository issues](https://github.com/LemonZer0/dpc-outreach-monitoring-system/issues)
