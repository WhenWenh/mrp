# Media Rating Platform (MRP) – Entwicklungsprotokoll

## 1. App-Design und Architektur

### 1.1 Architekturentscheidungen

Die Media Rating Platform wurde als **eigenständiger REST-HTTP-Server in Java** umgesetzt.  
Es wurden **keine Frameworks wie Spring oder ASP.NET** verwendet, sondern ausschließlich Low-Level-HTTP (`com.sun.net.httpserver.HttpServer`), wie in der Aufgabenstellung gefordert.

Die Architektur orientiert sich an **Clean Architecture / Hexagonal Architecture**, um eine klare Trennung der Verantwortlichkeiten sicherzustellen.

Zentrale Entscheidungen:
- Klare Trennung von Domain-, Application- und Infrastructure-Layer
- Token-basierte Authentifizierung mit Bearer Tokens
- PostgreSQL als persistente Datenbank (Docker-basiert)
- Manuelle Dependency Injection über Konstruktoren
- Verwendung von UUIDs als Identifikatoren
- Keine OR-Mapping-Frameworks, sondern reines JDBC

---

### 1.2 Projektstruktur
```
application/
├── security/
│    └── PasswordHasher.java
├── UserService.java
├── MediaService.java
├── RatingService.java
├── FavoriteService.java
├── LeaderboardService.java
└── RecommendationService.java

domain/
├── model/
│    ├── User.java
│    ├── MediaEntry.java
│    ├── Rating.java
│    └── enums/
│         └── MediaType.java
└── ports/
     ├── UserRepository.java
     ├── MediaRepository.java
     ├── MediaSearch.java
     ├── RatingRepository.java
     ├── FavoriteRepository.java
     └── AuthTokenService.java

 dto/
├── ApiErrorResponse.java
├── LeaderboardEntry.java
├── LeaderboardEntryResponse.java
├── UserCredentials.java
├── UserResponse.java
├── UserRatingStas.java
├── UserProfileUpdate.java
├── MediaRequest.java
├── MediaResponse.java
├── RatingRequest.java
├── RatingResponse.java
└── TokenResponse.java

infrastructure/
├── http/
│    ├── AppFactory.java
│    ├── HttpResponses.java
│    ├── Routes.java
│    ├── Router.java
│    ├── RouteHandler.java
│    ├── UserHandler.java
│    ├── MediaHandler.java
│    ├── RatingHandler.java
│    ├── FavoriteHandler.
│    ├── LeaderboardHandler.java
│    └── RecommendationHandler.java
├── persistence/
│    ├── JdbcUserRepository.java
│    ├── JdbcMediaRepository.java
│    ├── JdbcRatingRepository.java
│    └── JdbcFavoriteRepository.java
├── security/
│    ├── AuthService.java
│    └── OpaqueTokenService.java
└── config/
│    └── ConnectionFactory.java

util/
└── UUIDv7.java

Main.java
```


---

### 1.3 Klassendesign (Überblick)

**Domain-Modelle**
- `User`: Benutzerinformationen und Statistiken
- `MediaEntry`: Filme, Serien und Spiele
- `Rating`: Bewertung eines Mediums inkl. Moderationsstatus

**DTOs (Data Transfer Objects)**

- `ApiErrorResponse`  
  Einheitliches DTO zur Rückgabe von Fehlermeldungen im HTTP-Layer.
  Enthält Statuscode und Fehlermeldung und wird ausschließlich für Fehlerantworten verwendet.

- `LeaderboardEntry`  
  DTO zur Darstellung eines einzelnen Eintrags in der öffentlichen Bestenliste.
  Enthält aggregierte, berechnete Benutzerdaten (z. B. Benutzername und Anzahl der Bewertungen).

- `LeaderboardEntryResponse`  
  Wrapper-DTO zur Rückgabe einer Liste von `LeaderboardEntry`-Objekten über die HTTP-API.

- `MediaRequest`  
  Enthält die Eingabedaten zum Erstellen oder Aktualisieren eines Media-Eintrags.

- `MediaResponse`  
  DTO zur Ausgabe eines Media-Eintrags inklusive aggregierter Informationen
  (z. B. durchschnittliche Bewertung).

- `RatingRequest`  
  Enthält die Eingabedaten zum Erstellen oder Aktualisieren einer Bewertung.

- `RatingResponse`  
  DTO zur Ausgabe einer Bewertung inklusive Metadaten (z. B. Likes, Sichtbarkeit des Kommentars).

- `TokenResponse`  
  DTO zur Rückgabe des Authentifizierungs-Tokens nach erfolgreichem Login.

- `UserCredentials`  
  DTO zur Übergabe von Benutzername und Passwort bei Registrierung und Login.

- `UserProfileResponse`  
  DTO zur Ausgabe des Benutzerprofils inklusive persönlicher Statistiken.

- `UserProfileUpdate`  
  DTO zur Übergabe von änderbaren Profildaten beim Aktualisieren des Benutzerprofils.

- `UserRatingStats`  
  DTO mit aggregierten Bewertungsstatistiken eines Benutzers
  (z. B. Anzahl der Bewertungen, Durchschnittsbewertung).

- `UserResponse`  
  Allgemeines DTO zur Ausgabe von Benutzerdaten ohne sensible Informationen.


**Application Services**
- `UserService`: Registrierung, Login, Profil
- `MediaService`: Erstellen, Bearbeiten und Löschen von Medien
- `RatingService`: Validierung und Verwaltung von Bewertungen
- `FavoriteService`: Favoritenlogik
- `RecommendationService`: Empfehlungslogik basierend auf Bewertungen
- `LeaderboardService`: Ermittlung der aktivsten Benutzer auf Basis der Anzahl ihrer Bewertungen

**Infrastructure**
- HTTP-Handler übernehmen die Request-/Response-Verarbeitung sowie das Routing
- Repositories implementieren die Domain-Ports mittels JDBC und kapseln den Datenbankzugriff
- Security-Komponenten prüfen Authentifizierung und Autorisierung über Bearer Tokens
- Config-Komponenten stellen technische Konfigurationen wie Datenbankverbindungen bereit
- Utility-Klassen enthalten technische Hilfsfunktionen ohne Geschäftslogik

---

## 2. Lessons Learned (Erkenntnisse)

- Eine saubere Schichtenarchitektur vereinfacht Wartung und Tests erheblich
- Der Verzicht auf Frameworks stärkt das Verständnis von HTTP, Routing und Security
- Interfaces für Repositories ermöglichen einfaches Mocking
- Empfehlungslogik sollte schrittweise entwickelt und gut getestet werden
- SQL-UPserts vereinfachen Session- und Token-Verwaltung deutlich

---

## 3. Unit-Testing-Strategie und Testabdeckung

Die Unit-Tests konzentrieren sich auf die **Business-Logik der Application Services**.

### Teststrategie:
- **JUnit 5** für alle Unit-Tests
- **Mockito** für das Mocken von Repositories
- Keine Tests auf HTTP- oder Datenbank-Ebene
- Fokus auf:
    - Validierung von Eingaben
    - Autorisierungslogik
    - Ausschluss ungültiger Zustände


Die Testabdeckung stellt sicher, dass alle zentralen Use-Cases der Business-Logik überprüft sind.

---

## 4. SOLID-Prinzipien (mit Beispielen)

### Single Responsibility Principle (SRP)

Jede Klasse hat genau eine Verantwortung.

**Beispiel:**
- `RatingService` enthält ausschließlich Logik rund um Bewertungen
- `AuthService` ist nur für Authentifizierung zuständig

---

### Dependency Inversion Principle (DIP)

Application Services hängen von **Interfaces**, nicht von konkreten Implementierungen ab.

**Beispiel:**
- `MediaService` arbeitet mit `MediaRepository`
- Die konkrete JDBC-Implementierung befindet sich im Infrastructure-Layer

Dadurch sind Unit-Tests ohne Datenbank möglich.

---

## 5. Zeitaufwand (Tracked Time)
- Arbeitszeit wurde nicht gemessen
---

## 6. Git Repository

- origin  https://github.com/WhenWenh/mrp.git (fetch)
- origin  https://github.com/WhenWenh/mrp.git (push)

