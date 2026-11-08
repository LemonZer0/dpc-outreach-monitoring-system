# Project Report Context

Use this document as background when preparing the project report. The author
will provide a separate report template; follow that template's structure and
format. This file is context, not a replacement report. Do not invent test
results, stakeholder names, institutional details, or requirements that are
not provided.

## Project identity

- **Project:** DPC Outreach Call Monitoring System
- **Course:** CS5013-PAI course project
- **Purpose:** Help the Placement & Internship CORE monitor and verify company
  outreach calls made by Department Placement Coordinators (DPCs).
- **Project state:** Working stakeholder-demo version deployed to Render, with
  PostgreSQL hosted by Neon. It is a course/demo system, not a hardened
  production service.

## Problem and motivation

The existing outreach workflow relies on DPCs manually updating a Google
Sheet. That makes it difficult for the Placement & Internship CORE to review
call activity, associate a call with a verified recruiter/company contact, and
track subsequent outreach decisions. The system centralizes call metadata and
related records in a backend and provides both a dashboard and a synchronized
spreadsheet view.

## Main users

- **DPCs:** Use the Android app to submit call metadata and self-reported call
  outcomes.
- **Placement & Internship CORE:** Use the dashboard to review calls, verify
  matches, manage outreach records, and record decisions.
- **Administrators:** Manage DPCs, companies, and contacts; use the simulator;
  and check or retry Google Sheets synchronization.

## Implemented functionality

- **Android client:** Configurable backend URL (the hosted Render service is
  the default), contact directory loading, and call submission. Calls can be
  submitted using dialer call-log details or manually entered metadata.
- **Reported call outcomes:** DPCs can report whether they spoke with a person,
  reached voicemail, encountered forwarding or an automated system, received
  no answer, or could not determine the outcome. This is stored separately
  from technical verification and is self-reported.
- **Backend matching and verification:** Submitted calls are matched against
  known contacts using phone numbers. Ambiguous matches are left unmatched for
  manual review. Verification also accounts for duration and duplicate-call
  detection; call duration by itself does not prove that a person answered.
- **Dashboard:** DPC-focused outreach/call review, call-verification results,
  company outreach decisions, and manual matching of unmatched calls. An
  admin area supports managing DPCs, companies, contacts, and simulated calls.
- **Simulation:** Admins can create simulated call records for registered
  DPCs. Records distinguish simulation, dialer, and manually entered metadata
  sources where known.
- **DPC administration:** Admins can create and delete DPCs. New DPCs receive
  the lowest available positive ID. Deleting a DPC permanently deletes its
  call history; the freed ID can later be assigned to another DPC.
- **Google Sheets mirror:** The backend synchronizes six managed tabs:
  Call records, DPCs, Contacts, Companies, DPC outreach status, and CORE
  outreach decisions. Missing managed tabs are created and their contents are
  refreshed; unrelated tabs are left untouched. Sync runs at startup and after
  database changes. A Sheets failure does not roll back a successful database
  write; admins can inspect status and request a retry.
- **Deployment:** The dashboard and backend are served from the same Render
  service. PostgreSQL is hosted separately on Neon. The dashboard uses its
  own origin for its API connection when hosted together. A course handoff
  image packages the backend, dashboard, and local PostgreSQL in one container
  so staff can run it with one `docker run` command. A Docker volume can retain
  the database between container replacements.

## High-level architecture and data flow

1. A DPC records a call using the Android app, or an administrator enters a
   simulated call through the dashboard.
2. The Spring Boot REST backend validates and processes the submission,
   resolves it against contact/company data, and stores the record in
   PostgreSQL.
3. The dashboard retrieves records and related data from the backend for
   review and administrative workflows.
4. After committed database changes, an asynchronous backend task mirrors the
   managed data to Google Sheets. A startup sync and admin-triggered retry are
   also supported.

**Components and technologies:** Android app written in Kotlin; Spring Boot
backend written in Java; PostgreSQL with Flyway schema migrations; browser
dashboard using HTML, CSS, and JavaScript; Google Sheets API for the optional
spreadsheet mirror; Render hosting and Neon managed PostgreSQL for the demo.

## Deployment and current demo

- **Dashboard/backend:** `https://dpc-outreach-monitoring.onrender.com`
- **Health endpoint:** `/api/health`
- **Android:** The app defaults to the hosted service and keeps the backend URL
  editable for local testing.
- **Database:** Neon PostgreSQL; schema migrations are applied by Flyway.
- **Secrets:** Database credentials, dashboard admin credentials, and the
  Google service-account key are configured outside the source repository.
  Do not include passwords, API keys, service-account JSON, or private contact
  data in the report.
- **Hosting caveat:** The Render free service may sleep when idle, so the first
  request after inactivity can be delayed. The current setup is suitable for
  demonstration and evaluation, not a guarantee of production-grade
  availability or recovery.
- **Local Docker demo:** Staff need Docker and the SHA-tagged image archive.
  The container generates an admin password at startup and prints it in its
  logs. A named volume retains the local database between container runs. The
  Android APK is distributed separately and still defaults to the hosted
  backend.

## Validation evidence and known caveats

- The user confirmed that the deployed health check passes and rotated the
  previously exposed Neon database password.
- The user has manually checked the app-to-backend/dashboard data flow and
  Google Sheets behavior.
- The Android debug APK build completed successfully on 2026-11-08.
- Earlier development validation included backend tests and database
  integration testing; obtain the exact final test command and output from the
  project author before reporting precise counts or coverage.
- Google Sheets synchronization is asynchronous. A database update can appear
  before its spreadsheet mirror; administrators can inspect the sync status
  endpoint and retry a failed sync.
- The deployed service and free hosting tier have provider-specific
  availability, storage, and usage constraints. Do not describe this setup as
  hardened or production-ready without additional evidence.
- The system handles call and contact information. The report should describe
  appropriate access and privacy considerations, and should not claim legal or
  regulatory compliance unless independently established.

## Suggested report emphasis

Describe the project as an implemented end-to-end course/demo system:
requirements and motivation, architecture, key workflows, data persistence and
matching, dashboard and Android clients, spreadsheet integration, deployment,
validation, limitations, and possible future improvements. Clearly distinguish
implemented features from planned enhancements. Use the supplied template and
request any missing names, dates, diagrams, screenshots, test logs, or
institution-specific details from the project author rather than guessing.
