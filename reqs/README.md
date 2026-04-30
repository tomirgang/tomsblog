# Requirements (Doorstop)

ASPICE-orientiertes Requirement Tracing mit Doorstop.

## Dokumenthierarchie

```
STK (Stakeholder Requirements)
 └── SWR (Software Requirements)
      ├── SWA (Architecture/Design)
      │    └── IMP (Implementation)
      └── TST (Test Specifications)
```

## Verzeichnisse

| Verzeichnis       | Prefix | Beschreibung                                |
| ----------------- | ------ | ------------------------------------------- |
| `stakeholder/`    | STK    | Stakeholder-Anforderungen                   |
| `software/`       | SWR    | Software-Anforderungen (abgeleitet aus STK) |
| `architecture/`   | SWA    | Architektur-/Design-Entscheidungen          |
| `implementation/` | IMP    | Implementierungs-Nachweise                  |
| `tests/`          | TST    | Testspezifikationen (verifizieren SWR)      |

## Befehle

```bash
source .venv/bin/activate                  # Python venv aktivieren
doorstop                                   # Validierung
doorstop publish all docs/requirements/    # HTML-Export
doorstop add STK                           # Neues Stakeholder-Requirement
doorstop link SWR001 STK001                # SWR an STK verlinken
```

## Aktueller Stand

| Dokument | Anzahl |
| -------- | ------ |
| STK      | 12     |
| SWR      | 15     |
| SWA      | 6      |
| IMP      | 0      |
| TST      | 0      |
