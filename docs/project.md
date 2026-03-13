# Clocked — Project Index

> Excel schedule shift parser for Android. Forked for Sporen — a residential youth care organisation.

> **Note:** The `docs/` folder is listed in `.gitignore` — these documents are private planning and strategy material, not part of the versioned codebase.

**Review date:** Wednesday 11 March 2026 — Sporen internal review.

---

## Documents

| File | Contents |
|---|---|
| [app.md](app.md) | Android app spec: UI, features, tech stack, icon, open items |
| [excel-format.md](excel-format.md) | Excel column map, shift types, parsing algorithm, name matching rules |
| [sporen.md](sporen.md) | Sporen fork: current demo scope, next steps if greenlit (parser refactor, server sync, hour registration, care operations modules) |
| [vision.md](vision.md) | Platform vision: problem map, full module scope, phased roadmap, differentiators, competitive landscape |
| [compliance.md](compliance.md) | GDPR, EU AI Act, data residency, DPIA requirements, Microsoft/CLOUD Act assessment, pre-production checklist |

---

## Current Status

| Item | Status |
|---|---|
| Android app — Phase 0 demo | ✅ Done |
| 3 departments (PIJL, ST3, KLIMOP), single Excel file | ✅ Verified against `202602.xlsx` |
| PIN + biometric lock | ✅ Done |
| Sporen internal review | 🕐 11 March 2026 |
| Multi-layout parser (12 departments) | ⏸ Pending greenlight |
| Server sync + push notifications | ⏸ Pending greenlight |
| Hour registration / Planpoint replacement | ⏸ Pending greenlight |
| Roostermaker web editor | ⏸ Pending greenlight |
| Care operations modules | ⏸ Pending greenlight |

---

## Repo Structure

```
app/                    Android source (Kotlin, Jetpack Compose)
  src/main/java/com/sporen/app/
    data/               Room DB, DataStore preferences, repository
    domain/             Models, use cases
    parser/             ExcelParser + (planned) SheetLayout, LayoutRegistry
    ui/                 Screens + ViewModels per feature
    di/                 Hilt module
    navigation/         NavGraph, Screen sealed class
docs/
  app.md                Android app spec
  excel-format.md       Excel format reference
  sporen.md             Sporen edition — scope + next steps
  vision.md             Platform vision
  202602.xlsx           Sample schedule used for parser verification
  clocked-icon.svg      App icon vector source
```