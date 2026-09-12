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

Android App
    ↓
Normal Android Phone Application
    ↓
Android Call Log
    ↓
Spring Boot Backend
    ↓
PostgreSQL
    ↓
CORE Dashboard
    ↓
Google Sheets

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