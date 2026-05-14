# ADR-0014: Shadow-Banning im Kommentarsystem

## Status

Accepted

## Context

Klassisches Banning (harter Ban) informiert den Benutzer, dass er gesperrt ist. Dies führt häufig dazu, dass der Benutzer neue Accounts erstellt oder andere Umgehungsstrategien nutzt.

Shadow-Banning ist eine Anti-Troll-Maßnahme, bei der der gebannte Benutzer nicht erfährt, dass er gesperrt wurde. Seine Kommentare sind für ihn selbst sichtbar, für alle anderen Benutzer jedoch nicht.

## Decision

Das Kommentarsystem unterstützt **Shadow-Banning** als Moderationswerkzeug.

**Funktionsweise:**
- Administratoren und Autoren können einen Benutzer shadow-bannen
- Shadow-gebannte Benutzer sehen ihre eigenen Kommentare weiterhin normal angezeigt
- Andere Benutzer (inklusive anonyme) sehen Kommentare von shadow-gebannten Benutzern nicht
- Der Shadow-Ban ist pro Tenant (ein Ban auf Blog A wirkt nicht auf Blog B)
- Shadow-Bans können von Admins/Autoren aufgehoben werden

**Technische Umsetzung:**
- `ShadowBanStatus` am Benutzer-Profil (pro Tenant): `ACTIVE`, `SHADOW_BANNED`
- Query-Filter in der Kommentar-Abfrage: Kommentare von shadow-gebannten Benutzern werden nur für den Autor selbst zurückgegeben
- Kein sichtbarer Hinweis im UI für den gebannten Benutzer
- Audit-Log über Ban/Unban-Aktionen (wer hat wann gebannt)

## Consequences

**Positiv:**
- Reduziert Motivation für neue Account-Erstellung
- Troll-Beiträge verschwinden für die Community ohne Eskalation
- Einfach implementierbar (Filter-Logik in der Query)
- Kombinierbar mit LLM-Moderation (automatischer Shadow-Ban bei wiederholten Verstößen)

**Negativ:**
- Ethische Bedenken: Benutzer wird getäuscht (vertretbar bei Spam/Trolling)
- Benutzer könnte über Inkognito-Modus den Ban bemerken
- Kein Feedback an den Benutzer (keine Chance zur Verhaltensänderung)
- Admin muss bewusst entscheiden (nicht als Standardreaktion verwenden)

## References

- SWR-019: Shadow-Banning für Kommentare
- SWR-007: Rollenbasierte Zugriffskontrolle (Admin/Author können Bans vergeben)
