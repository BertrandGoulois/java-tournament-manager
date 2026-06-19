# java-tournament-manager

![CI](https://github.com/BertrandGoulois/java-tournament-manager/actions/workflows/ci.yml/badge.svg)
[![Javadoc](https://img.shields.io/badge/javadoc-online-blue)](https://bertrandgoulois.github.io/java-tournament-manager/)
![Coverage](.github/badges/jacoco.svg)
![Branches](.github/badges/branches.svg)

API REST de gestion de tournois sportifs en élimination directe, développée en Java 21 / Spring Boot.

---

## Technical Stack

- **Java 21** (Virtual Threads activés via `spring.threads.virtual.enabled=true`)
- **Spring Boot 4**
- **Spring Security** + **JWT** (authentification stateless)
- **Spring Data JPA** / **Hibernate**
- **PostgreSQL** (production)
- **Liquibase** (migrations versionnées)
- **Apache Kafka** (messaging distribué, remplacement des Spring Events)
- **Redis** (cache des statistiques joueur)
- **WebSocket** / **STOMP** (notifications temps réel)
- **OpenAI GPT-4o-mini** (génération de commentaires de matchs via LLM)
- **Prometheus** + **Grafana** (monitoring et visualisation des métriques JVM / HTTP)
- **JUnit 5** + **Mockito** (tests unitaires)
- **Testcontainers** (tests d'intégration)
- **Gatling** (tests de charge)
- **Springdoc / Swagger UI** (documentation API)
- **Lombok**
- **Resilience4j** (circuit breaker sur l'appel OpenAI)

---

## Architecture & Design

Le projet suit une **architecture hexagonale** (ports & adapters) : le domaine métier est isolé de toute dépendance technique (JPA, Kafka, Redis, OpenAI). Chaque use case est implémenté par une classe dédiée (principe de responsabilité unique).

```
domain/
  model/        → entités, enums, value objects
  port/
    in/         → interfaces use cases (ex. RecordMatchResultUseCase)
    out/        → interfaces infra (ex. SaveMatchPort, LoadMatchPort)

service/        → use cases atomiques (une classe par use case, ex. CreateTournamentService, RecordMatchResultService)

infrastructure/
  persistence/  → adapters JPA (implémentent les ports sortants)
  messaging/    → adapter Kafka
  ai/           → adapter OpenAI
```

Les side effects métier (mise à jour ELO, avancement du bracket, notifications temps réel, génération de commentaires) sont découplés du service principal via Kafka. Lorsqu'un résultat de match est enregistré, un événement `MatchFinishedEvent` est publié sur le topic `match-finished`. Quatre consumers indépendants traitent cet événement de façon asynchrone :

- `elo-group` → met à jour les ratings ELO des deux joueurs
- `bracket-group` → avance le bracket au tour suivant
- `websocket-group` → broadcast une notification temps réel à tous les clients connectés via WebSocket
- `commentary-group` → génère un commentaire narratif via OpenAI GPT-4o-mini

En cas d'échec répété d'un listener (3 tentatives espacées d'1 seconde), le message est redirigé vers le topic `match-finished.DLT` (Dead Letter Topic) pour inspection et rejeu manuel via Kafka UI.

> L'appel à OpenAI dans `OpenAiCommentaryAdapter` est protégé par un circuit breaker Resilience4j : après une série d'échecs, les appels suivants sont court-circuités sans solliciter l'API, le `CommentaryListener` reçoit une exception dédiée et journalise l'incident sans bloquer le traitement des autres événements.

> Cette approche remplace une première implémentation basée sur les Spring Events synchrones, afin de se rapprocher d'une architecture orientée événements distribuée.

**Test WebSocket** : une page de démonstration est disponible sur `http://localhost:8080/ws-test.html`. Elle permet de visualiser en temps réel les événements de fin de match sans authentification.

---

## Installation & Configuration

1. Cloner le projet :

```bash
git clone https://github.com/BertrandGoulois/java-tournament-manager.git
cd java-tournament-manager
```

2. Créer un fichier `.env` à la racine du projet :

```
POSTGRES_PASSWORD=tonmotdepasse
OPENAI_API_KEY=sk-...ta-clef
```

3. Créer un fichier `src/main/resources/application-local.properties` :

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/tournament_manager
spring.datasource.username=postgres
spring.datasource.password=tonmotdepasse
openai.api.key=sk-...ta-clef
```

> Les tables et le user admin sont créés automatiquement par Liquibase au démarrage. Aucun script SQL manuel requis.

4. Démarrer tous les services :

```bash
docker-compose up -d
```

> Lance PostgreSQL, Redis, Kafka, Zookeeper, Kafka UI, Prometheus, Grafana et l'application Spring Boot dans des containers Docker.

---

## Running

**Démarrage en local (hors Docker) :**

```bash
./mvnw spring-boot:run
```

**Tests unitaires :**

```bash
./mvnw test
```

**Tests d'intégration (nécessitent Docker) :**

```bash
./mvnw verify
```

**Tests de charge (Gatling) :**

Prérequis : l'appli doit tourner sur `localhost:8080`.

```bash
./mvnw gatling:test
```

Le rapport HTML est généré dans `target/gatling/<run-id>/index.html`.

> Le scénario simule un flux complet : login, création d'un tournoi, inscription de 8 joueurs, démarrage, consultation du bracket et des stats. La charge monte progressivement de 1 à 500 utilisateurs simultanés, chacun avec une IP simulée distincte via le header `X-Forwarded-For` pour valider le rate limiting par client sans saturer un bucket partagé.

**Documentation API :**

Swagger UI disponible sur : `http://localhost:8080/swagger-ui/index.html`

**Test WebSocket :**

Page de démonstration disponible sur : `http://localhost:8080/ws-test.html`

**Kafka UI :**

Interface de monitoring Kafka disponible sur : `http://localhost:8090`

Permet de visualiser les topics, les messages en échec dans `match-finished.DLT` et de les rejouer manuellement.

**Monitoring :**

Prometheus disponible sur : `http://localhost:9090`

Grafana disponible sur : `http://localhost:3000` (admin / admin)

> Dashboard Spring Boot 3.x Statistics (ID `19004`) disponible après import manuel dans Grafana. Visualise en temps réel : uptime, heap, CPU, GC, HikariCP, threads.

---

## Endpoints

### Authentication

#### Login

- **POST** `/api/auth/login`
- **Body JSON** :

```json
{
  "username": "admin",
  "password": "password123"
}
```

- **Response JSON** :

```json
{
  "token": "<JWT access token>",
  "refreshToken": "<refresh token>"
}
```

- **Usage** : inclure le JWT dans le header `Authorization` pour les endpoints protégés :

```
Authorization: Bearer <JWT token>
```

#### Refresh token

- **POST** `/api/auth/refresh`
- **Body JSON** :

```json
{
  "refreshToken": "<refresh token>"
}
```

- **Response JSON** :

```json
{
  "token": "<nouveau JWT access token>",
  "refreshToken": "<refresh token>"
}
```

#### Logout

- **POST** `/api/auth/logout`
- **Body JSON** :

```json
{
  "refreshToken": "<refresh token>"
}
```

> Révoque le refresh token. Les appels ultérieurs avec ce token retourneront une erreur `400`.

---

### Players

#### Create player

- **POST** `/api/players`
- **Body JSON** :

```json
{
  "username": "player1",
  "email": "player1@mail.com"
}
```

- **Response JSON** :

```json
{
  "id": 1,
  "username": "player1",
  "email": "player1@mail.com",
  "eloRating": 1000,
  "createdAt": "2026-01-01T00:00:00"
}
```

- **Errors** :
  - `409` → username ou email déjà utilisé

#### Get all players

- **GET** `/api/players?page=0&size=10&sort=username,asc`
- **Response JSON** : objet `Page` Spring avec `content`, `totalElements`, `totalPages`, `number`

#### Get player by ID

- **GET** `/api/players/{id}`
- **Errors** :
  - `404` → joueur introuvable

#### Delete player (soft delete)

- **DELETE** `/api/players/{id}`
- **Response** : `204 No Content`

#### Get player stats

- **GET** `/api/players/{id}/stats`
- **Response JSON** :

```json
{
  "id": 1,
  "username": "player1",
  "eloRating": 1024,
  "matchesPlayed": 3,
  "wins": 2,
  "losses": 1,
  "winRate": 66.67,
  "eloHistory": [
    {
      "eloChange": 24,
      "eloAfter": 1024,
      "createdAt": "2026-01-01T00:00:00",
      "matchId": 1
    }
  ]
}
```

---

### Tournaments

#### Create tournament

- **POST** `/api/tournaments`
- **Body JSON** :

```json
{
  "name": "Spring Championship",
  "maxPlayers": 8
}
```

- **Errors** :
  - `400` → `maxPlayers` n'est pas une puissance de 2
  - `409` → nom déjà utilisé

#### Get all tournaments

- **GET** `/api/tournaments?page=0&size=10&sort=name,asc`
- **Response JSON** : objet `Page` Spring avec `content`, `totalElements`, `totalPages`, `number`

#### Get tournament by ID

- **GET** `/api/tournaments/{id}`

#### Delete tournament (soft delete)

- **DELETE** `/api/tournaments/{id}`
- **Response** : `204 No Content`

#### Start tournament

- **POST** `/api/tournaments/{id}/start`

> Génère automatiquement le bracket en élimination directe. Les joueurs sans adversaire (byes) sont qualifiés automatiquement.

#### Get tournament bracket

- **GET** `/api/tournaments/{id}/bracket`
- **Response JSON** :

```json
{
  "tournamentId": 1,
  "tournamentName": "Spring Championship",
  "status": "IN_PROGRESS",
  "rounds": [
    {
      "round": 8,
      "matches": [
        {
          "id": 1,
          "player1Id": 1,
          "player2Id": 2,
          "winnerId": 1,
          "status": "FINISHED"
        }
      ]
    },
    {
      "round": 4,
      "matches": [
        {
          "id": 5,
          "player1Id": 1,
          "player2Id": null,
          "winnerId": null,
          "status": "PENDING"
        }
      ]
    }
  ]
}
```

> Les rounds sont triés du premier (valeur la plus haute) à la finale (round 2).

---

### Registrations

#### Register player to tournament

- **POST** `/api/registrations`
- **Body JSON** :

```json
{
  "playerId": 1,
  "tournamentId": 1
}
```

- **Errors** :
  - `400` → tournoi non ouvert aux inscriptions
  - `400` → joueur déjà inscrit
  - `400` → tournoi complet

#### Get tournament registrations

- **GET** `/api/registrations/{tournamentId}`

---

### Matches

#### Get match by ID

- **GET** `/api/matches/{id}`

#### Record match result

- **PUT** `/api/matches/{id}/result`
- **Body JSON** :

```json
{
  "winnerId": 1
}
```

> Après enregistrement du résultat : publication d'un événement Kafka `match-finished`, mise à jour ELO des deux joueurs, avancement automatique au tour suivant, notification WebSocket temps réel, génération de commentaire LLM, fin du tournoi si c'était la finale.

- **Errors** :
  - `400` → match déjà terminé
  - `400` → le gagnant n'est pas un des joueurs du match

#### Get match commentary

- **GET** `/api/matches/{id}/commentary`
- **Response JSON** :

```json
{
  "matchId": 1,
  "commentary": "Dans un duel spectaculaire, player1 (ELO 1200) a dominé player2 (ELO 1000) avec 76% de chances de victoire. Une performance solide qui lui rapporte 8 points ELO."
}
```

> Le commentaire est généré de façon asynchrone via OpenAI GPT-4o-mini après la fin du match. Retourne "Commentaire en cours de génération..." si le LLM n'a pas encore répondu.

---

## Key Features

- Architecture hexagonale (ports & adapters) - domaine métier isolé de l'infra
- Use cases atomiques : une classe par use case (principe de responsabilité unique)
- Génération de bracket en élimination directe avec support des byes
- Consultation du bracket complet par round (`GET /tournaments/{id}/bracket`)
- Calcul ELO après chaque match (K=32, formule standard)
- Idempotence du calcul ELO - protection contre les doublons Kafka
- Avancement automatique au tour suivant via événements Kafka
- Génération automatique de commentaires de matchs via OpenAI GPT-4o-mini (event-driven via Kafka)
- Dead Letter Queue Kafka pour les messages en échec (`match-finished.DLT`)
- Kafka UI pour l'inspection et le rejeu des messages en échec
- Notifications temps réel via WebSocket à chaque fin de match
- Cache Redis sur les statistiques joueur (`GET /players/{id}/stats`)
- Statistiques joueur (win rate, historique ELO)
- Authentification JWT avec refresh token et révocation (logout)
- Rate limiting sur les endpoints sensibles (Login : 5 req/min, Create Player : 10 req/min)
- Tests de charge multi-IP simulées via `X-Forwarded-For` pour valider le rate limiting par client
- Soft delete sur joueurs et tournois
- Pagination sur les listes de joueurs et tournois
- Migrations versionnées avec Liquibase
- Monitoring Prometheus / Grafana avec dashboard Spring Boot (métriques JVM, HTTP, HikariCP)
- Tests de charge Gatling - scénario complet end-to-end (login, tournoi, bracket, stats)
- Couverture de tests élevée : tests unitaires (JUnit 5 / Mockito), tests controller (MockMvc) et tests d'intégration (Testcontainers + Kafka embarqué)
- Circuit breaker Resilience4j sur l'appel OpenAI (testé : transitions CLOSED → OPEN → HALF_OPEN, court-circuit vérifié)

---

## Évolutions possibles

- Format round-robin / phase de groupes
- Reverse proxy (nginx) devant l'application pour une validation complète de la confiance sur `X-Forwarded-For` en environnement de production
