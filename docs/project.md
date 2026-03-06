# Clocked

> Android app that scrapes your shifts from a company Excel schedule and puts them in your pocket.

---

## Problem

Many companies manage staff schedules in Excel. Employees have to manually hunt down their own shifts and copy them into their personal calendar  tedious, error-prone, and a recurring pain every month.

---

## Solution

Clocked lets an employee upload their company's Excel schedule, finds their shifts automatically, and presents them in a clean personal view. Shifts can be edited and pushed to Google Calendar with one tap.

---

## UI

- **Header:** Small digital clock (HH:MM) with current date  always visible at the top of the app.
- **Shift overview:** Monthly view, scrollable, one month per screen.
- **Month navigation:** Current month + up to 6 months ahead + 1 month history.

---

## Core Features

### Onboarding / Profile
- User enters their full name and the alias used on the schedule (e.g. "Ronnie Spoelstra"  "ron").
- One-time setup, editable later.

### Excel Import
- Upload `.xlsx` file from device storage (Android file picker).
- App parses the file using the known company format.
- Extracts all shifts matching the user's alias: date, start time, end time.
- Slots shifts into the correct month bucket.
- Duplicate detection: if a shift with the same date + start time already exists, skip or prompt user.

### Shift Management
- View all shifts per month.
- Add a shift manually.
- Edit an existing shift (date, start time, end time, note).
- Delete a shift.

### Google Calendar Sync
- Optional. User-triggered per shift or per month.
- Requires one-time Google Sign-In (OAuth).
- Pushes shifts as calendar events to the user's Google Calendar.

### Offline
- The app is fully functional offline.
- Internet is only required for Google Sign-In and Calendar push.

---

## Data / Storage

- Local storage via Room database.
- Shift retention window: 1 month past + current + up to 6 months ahead (8 months max).
- Months beyond the retention window are pruned automatically.

---

## Tech Stack

**Chosen: Native Android (Kotlin)**

Expo / React Native was considered but ruled out: Google Sign-In and Google Calendar both require native modules, which breaks the Expo Go sandbox immediately and forces EAS development builds. Native Kotlin has no such ceiling and is the better long-term fit.

| Layer | Technology | Notes |
|---|---|---|
| Language | Kotlin | |
| UI | Jetpack Compose | |
| Local DB | Room | |
| Excel parsing | Apache POI | reads `.xlsx` on-device |
| Calendar sync | Google Calendar API | |
| Auth | Google Sign-In (OAuth 2.0) | for calendar push only |
| Async | Kotlin Coroutines + Flow | |
| DI | Hilt | |

**Alternative (if React Native background):** Expo + EAS Build — swap Apache POI → SheetJS, Room → expo-sqlite. Viable but not chosen.

---

## Scope

### v1
- Single Excel layout (company-specific format, TBD based on sample file)
- Device file upload only
- Shift CRUD
- Google Calendar push
- Offline-first

### Future / Expansions
- Multi-layout Excel template system (support other companies)
- Import from Google Drive or email attachment
- Shift reminder notifications
- Widget for home screen

---

## Excel Format (Resolved)

File: `YYYYMM.xlsx` — year and month are derived from the filename.

**Sheets:** One file contains multiple sheets, each representing a ward/unit (e.g. PIJL, ST3, KLIMOP). An employee can appear on more than one sheet in the same file.

**Per sheet layout:**

| Row | Content |
|---|---|
| 1 | Col A = month name (e.g. "FEBRUARI") |
| 2 | Col A = ward name |
| 3 | Headers — employee names start at col Y (index 24+) |
| 4+ | One row per calendar day |

**Per day row (row 4+):**

| Col (0-indexed) | Contents |
|---|---|
| 0 (A) | Day of week abbreviation: Ma / Di / Wo / Do / Vr / Za / Zo |
| 1 (B) | Day number (integer, e.g. 3) |
| 2 (C) | Notes / remarks |
| 3 (D) | Leave (verlof) names |
| 4 (E) | DP person name — fixed shift 09:00–14:30 |
| 9 (J) | DAG 1 person name |
| 10 (K) | DAG 1 start time |
| 11 (L) | DAG 1 end time |
| 13 (N) | DAG 2 person name |
| 14 (O) | DAG 2 start time |
| 15 (P) | DAG 2 end time |
| 18 (S) | NACHT person 1 name |
| 19 (T) | NACHT person 2 name / WE PERM |
| 20 (U) | NACHT start time |
| 21 (V) | NACHT end time |
| 24+ (Y+) | Per-employee hour totals (timedelta, for reference only) |

**Parsing algorithm:**
1. Read filename → extract year + month number.
2. For each sheet, read rows 4+.
3. For each day row, check cols 4, 9, 13, 18, 19 for a case-insensitive match against the user's alias (strip whitespace).
4. On match, record: sheet (ward), date (year+month+day), shift type, start time, end time.
5. DP shift times are always fixed at 09:00–14:30 regardless of what the cell shows.
6. Night shifts (NACHT) cross midnight — end time the next calendar day.
7. Duplicate detection on import: skip if same date + start time already exists in local DB.

**Verified with:** `202602.xlsx` — parser correctly extracted all Ron shifts across PIJL and ST3.

---

## App Icon

**Concept:** Clock face circle. The word "Clocked" is the second hand — text runs from the center pivot outward to 3 o'clock (15:00), with the final "d" touching the circle rim. Short hour hand at ~10. Monospace font, dark face, blue accent.

**Source:** `docs/clocked-icon.svg` (vector — scales to any Android icon size)

**Export sizes needed for Android:**
| Density | Size |
|---|---|
| mdpi | 48×48 |
| hdpi | 72×72 |
| xhdpi | 96×96 |
| xxhdpi | 144×144 |
| xxxhdpi | 192×192 |
| Play Store | 512×512 |

---

## Open Items

- [x] Obtain sample Excel schedule to finalize parser logic
- [x] Define exact company Excel structure (columns, date format, name placement)
- [x] Decide minimum Android API level — **API 26 (Android 8.0)** for `java.time` support
- [x] App icon / branding — SVG source in `docs/clocked-icon.svg`
