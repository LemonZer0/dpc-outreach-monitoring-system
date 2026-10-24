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

## Admin contact and company management

Start the backend with `ADMIN_USERNAME` and `ADMIN_PASSWORD` configured,
then open another PowerShell window in `backend/` and run:

```powershell
.\scripts\admin-client.ps1
```

The admin client prompts for the configured username and password, then
lets an administrator create, edit, and delete contacts; create and rename
companies; and list companies. Contact numbers are normalized when saved.
Contacts with call history cannot be deleted, and companies cannot be
deleted while they have contacts or outreach history. Delete actions
require typing `DELETE`.

Admin write and company-management routes require HTTP Basic credentials:
`POST`, `PUT`, and `DELETE /api/admin/contacts`, and the corresponding
`/api/admin/companies` routes. Contact listing and call-record APIs remain
available without admin credentials. The admin script permits HTTP only
for a loopback backend; use HTTPS for a remote backend because Basic
credentials must not be sent over an unencrypted connection.

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
- React

### Integration
- Google Sheets API
- Google OAuth

## Team

G. Sunisith Sagar  
CS23B021  
Team ID: 21
