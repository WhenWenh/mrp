# Media Ratings Platform (MRP) – Protokoll (Stand: 18.10.2025)

> **Hinweis:** Der Git-Link wird vom Team ergänzt.

Dieses Dokument beschreibt Architekturentscheidungen, Implementierungsdetails und die Nachweise für die Muss-Kriterien der Zwischenabgabe. Der Server ist ein **Standalone-Java-Programm** ohne Webframework, mit **eigenem HTTP-Routing**, Persistenz in **PostgreSQL** und **Token-basierter Authentifizierung**. Das Protokoll kombiniert Fließtext und Bullet Points.

---

## 1. Überblick & Zielsetzung

- **Ziel**: Bereitstellung einer REST-API für mögliche Frontends (Mobil, Web, CLI). Kein Frontend Bestandteil dieses Projekts.
- **Sprache**: Java
- **Frameworks**: Keins (nur Standardbibliothek + JSON-Serialisierung).
- **Datenbank**: PostgreSQL (lokal/Docker).
- **Auth**: Token-basierte Autorisierung via `Authorization: Bearer <token>` (Registrierung/Login ausgenommen).

---

## 2. Architektur & zentrale Entscheidungen

### 2.1 Layering / Clean Architecture
- **Domain** (`domain/`)
  - Reine Modellklassen: `User`, `MediaEntry`, `Rating`, `MediaType` (Enum).
  - Ports/Interfaces: `UserRepository`, `MediaRepository`, `RatingRepository`, `AuthTokenService`.
- **Application** (`application/`)
  - Use-Cases: `UserService` (Registrierung, Login, Profilzugriff).
  - Keine Infrastruktur-Abhängigkeiten, nur gegen Ports.
- **Infrastructure** (`infrastructure/`)
  - **HTTP**: `Router`, `RouteHandler`, `UserHandler` (Routing nach HTTP-Pfad/Methoden).
  - **Persistence**: `ConnectionFactory` (JDBC), `JdbcUserRepository`.
  - **Security**: `AuthService` (Request-Prüfung), `OpaqueTokenService` (Token-Issuance/Verification).
  - **Util**: `UUIDv7` (vorbereitet; aktuell überwiegend `UUID.randomUUID()` noch in Benutzung).

**Warum so?**
- **SOLID**, v. a. DIP: Application hangelt sich ausschließlich an Ports, Implementierungen liegen in Infrastructure (Austauschbarkeit, Testbarkeit).
- **Kopplung minimiert**, Verantwortlichkeiten klar getrennt (Single Responsibility).
- **Erweiterbarkeit**: Neue Adapter (z. B. andere DB) ohne Änderung der Business-Logik.

### 2.2 HTTP-Server & Routing
- Eigener, schlanker HTTP-Server mit zentralem **`Router`**, der:
  - nach **HTTP-Methode** und **Pfad** dispatcht,
  - **Header**, **Query-Params** und **Body** korrekt einliest,
  - **Content-Type** / **Accept** beachtet (JSON).
- **Handler** je Verantwortlichkeit (z. B. `UserHandler` für Register/Login/Profile).

### 2.3 Persistenz & Sicherheit
- **PostgreSQL** via JDBC, abgesichert mit **Prepared Statements**.
- **Auth**:
  - `POST /api/login` gibt einen **Token-String** zurück.
  - Geschützte Endpunkte verlangen `Authorization: Bearer <token>`.
  - Token wird serverseitig **persistiert** (inkl. Erstellzeit/TTL) und bei jedem Request geprüft.

---

## 3. Erfüllung der Muss-Kriterien (Zwischenabgabe)

### 3.1 Features & Server
- ✅ **Java**-Standalone-Anwendung (kein ASP/Spring/JSP/JSF).
- ✅ **Server lauscht** auf eingehende Verbindungen und verarbeitet Requests.
- ✅ **Pure-HTTP**: eigener Router, kein Web-Framework.

### 3.2 REST-Spezifika
- ✅ **HTTP-Pfad/Methoden/Headers/Body** werden korrekt verarbeitet.
- ✅ **Routing** nach Pfad/HTTP-Methode (z. B. `/api/register`, `/api/login`).
- ✅ **Statuscodes**: 2xx bei Erfolg, 4xx bei Client-Fehler (z. B. 400/401/409), 5xx bei Serverfehlern.

### 3.3 Funktionale Anforderungen (Scope der Zwischenabgabe)
- ✅ **Modelle**: `User`, `MediaEntry`, `Rating` vorhanden.
- ✅ **User-Registrierung & Login** mit **tokenbasierter** Authentisierung.
- ⏳ **Weitere Use-Cases** (Ratings, Likes, Favorites, Leaderboard, Suche/Filter/Sortierung) sind für die Final-Abgabe vorgesehen.

### 3.4 Nicht-funktionale Anforderungen
- ✅ **SOLID**/Clean-Architecture nachweisbar (Ports/Adapter, Layer-Trennung).
- ✅ **Integrationstests** via **Postman-Collection** (bereitgestellt im Repo/Anhang).
- 🚫 **Unit-Tests**: Für die Zwischenabgabe **nicht vorgesehen** (werden zur Final-Abgabe ergänzt).

---

## 4. API-Entwürfe (aktueller Stand)

### 4.1 Registrierung
- `POST /api/register`
- **Request (JSON)**:
  ```json
  { "username": "alice", "password": "secret" }
  ```
- **Responses**:
  - `201 Created` mit einfacher User-Repräsentation (ohne Passworthash).
  - `400 Bad Request` bei ungültigen Eingaben.
  - `409 Conflict` bei bereits vorhandenem Username.

### 4.2 Login
- `POST /api/login`
- **Request (JSON)**:
  ```json
  { "username": "alice", "password": "secret" }
  ```
- **Responses**:
  - `200 OK` mit Token-String (Body).
  - `401 Unauthorized` bei falschen Credentials.

### 4.3 Geschütztes Beispiel („Profile lesen“)
- `GET /api/users/{username}/profile`
- **Header**: `Authorization: Bearer <token>`
- **Responses**:
  - `200 OK` mit Profil-JSON.
  - `401 Unauthorized` ohne/ungültigen Token.
  - `403 Forbidden` bei fehlenden Rechten (anderes Profil).

> Hinweis: Weitere Endpunkte (Media CRUD, Ratings, Favorites, Leaderboard, Suche/Filter/Sort) folgen in der Final-Abgabe.

---

## 5. Fehlerbehandlung & Security-Guidelines

- **Input-Validation** zentral in Application-Use-Cases, zusätzlich Guard-Clauses in Domain-Settern.
- **Password-Handling**: Hashing (z. B. über `PasswordHasher`), keine Klartextspeicherung.
- **SQL-Injection**: ausschließlich **Prepared Statements**.
- **Token-Expiry**: Token enthält Erstellzeit/TTL; Ablauf wird beim Request geprüft → `401`.

---

## 6. Projektstruktur (Kurzüberblick)

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

## 7. Postman-Collection (Integrationstests)

Eine lauffähige **`MRP.postman_collection.json`** ist beigefügt. Sie enthält:
- `Register` (201/409/400)
- `Login` (200/401) inkl. Testscript zum automatischen Speichern des Tokens in `{{token}}`
- `Get Profile` (200/401/403) mit `Authorization: Bearer {{token}}`

> **Import**: Postman → „Import“ → JSON-Datei auswählen → Collection ausführen.

---

## 8. Git

- **Repository-Link**: *wird vom Team ergänzt*

---

## 9. Ausblick (Final-Abgabe)

- Media CRUD, Ratings (1–5) + Kommentar (Moderation), Likes, Favorites
- Suche/Filter/Sortierung, Leaderboard, Empfehlungen
- ≥ 20 Unit-Tests (Business-Logik)
- Erweiterte Fehler- und Berechtigungsprüfungen