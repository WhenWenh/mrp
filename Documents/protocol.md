# Media Ratings Platform (MRP) – Detaillierter Entwicklungsbericht (Stand: 18.10.2025)

Dieses Dokument beschreibt die technischen Schritte, Architekturentscheidungen, Teststrategie, aufgetretene Probleme und den Zeitaufwand der Entwicklung des Projekts **MRP – Media Ratings Platform**.  
Das System ist ein **Standalone-Java-REST-Server** (ohne Framework) mit **PostgreSQL-Persistenz** und **tokenbasierter Authentifizierung**.

---

## 1. Überblick & Zielsetzung

- **Ziel:** Entwicklung einer RESTful HTTP-API für Medienverwaltung und Benutzerinteraktion (Registrierung, Login, Bewertungen, Favoriten).  
  Die API dient als Basis für mögliche Frontends (Web, Mobile, CLI).
- **Sprache:** Java
- **Frameworks:** Keine (nur Java SE + JDBC + JSON-Verarbeitung)
- **Architekturprinzip:** Clean Architecture (Domain / Application / Infrastructure)
- **Datenbank:** PostgreSQL (Docker-Container `mrp_db`)
- **Authentifizierung:** Token-basiert mit `Authorization: Bearer <token>`

---

## 2. Beschreibung technischer Schritte & Architekturentscheidungen

### 2.1 Architekturprinzip
Die Implementierung folgt dem **Clean Architecture** / **Ports & Adapters** Ansatz:

- **Domain Layer:** Enthält reine Geschäftslogik und Entitäten (`User`, `MediaEntry`, `Rating`, `MediaType`).  
  Keine externen Abhängigkeiten.
- **Application Layer:** Enthält die Use-Cases (z. B. `UserService` für Registrierung und Login).  
  Kommuniziert nur über **Ports (Interfaces)** mit der Außenwelt.
- **Infrastructure Layer:** Implementiert technische Details:
    - `JdbcUserRepository` (DB-Zugriff via PreparedStatements)
    - `OpaqueTokenService` (Token-Erzeugung/Verwaltung)
    - `Router` + `UserHandler` (HTTP-Verarbeitung)
    - `ConnectionFactory` (PostgreSQL-Connection)
    - `AuthService` (Token-Prüfung)
    - `UUIDv7` 

**Gründe für diese Architektur:**
- Minimale Abhängigkeiten (SOLID-Prinzipien eingehalten)
- Austauschbarkeit von Infrastrukturkomponenten (z. B. DB- oder Auth-System)
- Hohe Testbarkeit der Business-Logik
- Keine Framework-Kopplung → leichtgewichtig und portabel

---

### 2.2 HTTP-Server
- Eigene Implementierung über **Java HTTP-Handler (kein Spring, kein ASP)**.
- `Router` leitet Anfragen anhand von Pfad und Methode an passende Handler weiter.
- `UserHandler` verarbeitet z. B.:
    - `POST /api/register`
    - `POST /api/login`

- **Content-Type / Accept / JSON** werden explizit behandelt.

---

### 2.3 Persistenz & Sicherheit
- Datenbank: PostgreSQL über JDBC (Docker-basiert)
- Sicherung vor SQL-Injection durch **Prepared Statements**
- Passwortsicherheit durch **`PasswordHasher` (SHA-256 / Salted Hashes)** - wird wahrscheinlich im VErlauf des Proektes noch angepasst.
- Token-Authentifizierung über:
    - `OpaqueTokenService`: generiert und prüft Tokens
    - Ablaufzeit (TTL) via `Duration.ofHours(24)`
    - Speicherung des Tokens in der DB mit User-Bezug (`user_id`, `issued_at`, `expires_at`)

---

## 3. Unit-Test-Strategie und Begründung

> Für die Zwischenabgabe war kein vollständiger Unit-Testumfang gefordert.


---

## 4. Probleme & Lösungen während der Entwicklung

| Problem | Ursache | Lösung |
|----------|----------|--------|
| **UUIDv7**-Implementierung erzeugte anfangs fehlerhafte Zeitbits | ByteBuffer-Packing unpräzise | Anpassung: timestamp nur teilweise in High-Bits integriert |
| **Token-Ablaufzeit (TTL)** wurde nicht geprüft | Logikfehler in `AuthService` | Prüfung `if exp < now → 401` ergänzt |
| **DB-Verbindung in Docker** nicht erreichbar | falsche Connection-URL / Port | Fix in `ConnectionFactory`: `jdbc:postgresql://localhost:5432/mrp` |
| **PreparedStatement-Fehler** bei Insert | falsche Reihenfolge der Parameter | Statement angepasst & getestet |
| **Login-Response (Token)** war zunächst reiner String | nicht kompatibel mit Postman-Test | Änderung: Rückgabe als JSON `{ "token": "..." }` |

---

## 5. Geschätzter Zeitaufwand pro Hauptbereich

| Teilbereich | Zeit (ca.) | Beschreibung |
|--------------|------------|---------------|
| Architektur-Entwurf / Projektaufbau | 6 h | Strukturierung in Domain, Application, Infrastructure |
| UserService & Authentifizierung | 5 h | Registrierung, Login, Tokenlogik |
| HTTP-Server & Routing | 4 h | Router, Handler, Request/Response |
| PostgreSQL-Integration | 5 h | JDBC, ConnectionFactory, SQL-Schema |
| Security (PasswordHasher, AuthService) | 3 h | Hashing, Headerprüfung |
| Testing & Postman-Setup | 2 h | Postman-Collection, manuelle Tests |
| Dokumentation & Cleanup | 2 h | README, Protokoll, UML |
| **Gesamt** | **27 Stunden** | – |

---

## 6. Git-History als Entwicklungsnachweis

Die Git-History wurde nicht ausreichend dokumentiert - dies wird sich im Verlauf bis zur finalen Abgabe durch regelmäßige Commits ändern und soll dann die iterative Entwicklung des Projekts darstellung:


---

## 7. Postman-Collection (Integrationstest)


### 7.1 Registrierung
- `POST /api/register`
- **Request (JSON)**:
  ```json
  { "username": "alice", "password": "secret" }
  ```
- **Responses**:
    - `201 Created` mit einfacher User-Repräsentation (ohne Passworthash).
    - `400 Bad Request` bei ungültigen Eingaben (z.B. leerer Username oder Passwort).
    - `409 Conflict` bei bereits vorhandenem Username.

### 7.2 Login
- `POST /api/login`
- **Request (JSON)**:
  ```json
  { "username": "alice", "password": "secret" }
  ```
- **Responses**:
    - `200 OK` mit Token-String (Body).
    - `400 Bad Request` bei ungültigen Eingaben (z.B. leerer Username oder Passwort).
    - `401 Unauthorized` bei falschen Credentials.


> Hinweis: Weitere Endpunkte (Media CRUD, Ratings, Favorites, Leaderboard, Suche/Filter/Sort) folgen in der Final-Abgabe.

---
## 8. Projektstruktur (Kurzüberblick)

```
application/
 ├── security/
 │    └── PasswordHasher.java
 └── UserService.java

domain/
 ├── model/
 │    ├── enums/
 │    │    └── MediaType.java
 │    ├── MediaEntry.java
 │    ├── Rating.java
 │    └── User.java
 ├── ports/
 │    ├── AuthTokenService.java
 │    ├── MediaRepository.java
 │    ├── RatingRepository.java
 │    └── UserRepository.java
 └── dto/
      ├── TokenResponse.java
      ├── UserCredentials.java
      └── UserResponse.java

infrastructure/
 ├── config/
 │    └── ConnectionFactory.java
 ├── http/
 │    ├── RouteHandler.java
 │    ├── Router.java
 │    └── UserHandler.java
 ├── persistence/
 │    └── JdbcUserRepository.java
 ├── security/
 │    ├── AuthService.java
 │    └── OpaqueTokenService.java
 └── util/
      └── UUIDv7.java

Main.java
resources/
```


---

## 9. Repository

- **GitHub:** [https://github.com/WhenWenh/mrp](https://github.com/WhenWenh/mrp)
### 9.1 Versionierung
- Zwischenabgabe 1: Git Tag `v1.0-intermediate`.
- Aktive Entwicklung: Branch `develop`.
---

## 10. Ausblick auf Final-Abgabe

- Vollständige Implementierung von Media CRUD, Ratings, Likes, Favorites
- Leaderboard (aktivste Nutzer)
- Such- & Filterfunktionen (Genre, Jahr, Typ, Bewertung)
- Empfehlungssystem (Genre- & Inhaltsähnlichkeit)
- ≥ 20 Unit-Tests mit klarer Logikabdeckung
- Verbesserte Fehlerbehandlung (HTTP 4xx/5xx Differenzierung)

---
