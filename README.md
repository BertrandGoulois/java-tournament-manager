# java-tournament-manager

![CI](https://github.com/BertrandGoulois/java-tournament-manager/actions/workflows/ci.yml/badge.svg)
[![Javadoc](https://img.shields.io/badge/javadoc-online-blue)](https://bertrandgoulois.github.io/java-tournament-manager/)
![Coverage](.github/badges/jacoco.svg)
![Branches](.github/badges/branches.svg)

API REST de gestion de tournois sportifs en élimination directe, développée en Java 21 / Spring Boot.

---

## Technical Stack

- **Java 21**
- **Spring Boot 4**
- **Spring Security** + **JWT** (authentification stateless)
- **Spring Data JPA** / **Hibernate**
- **PostgreSQL** (production)
- **Liquibase** (migrations versionnées)
- **Apache Kafka** (messaging distribué, remplacement des Spring Events)
- **Redis** (cache des statistiques joueur)
- **WebSocket** / **STOMP** (notifications temps réel)
- **JUnit 5** + **Mockito** (tests unitaires)
- **Testcontainers** (tests d'intégration)
- **Springdoc / Swagger UI** (documentation API)
- **Lombok**

---

## Architecture & Design

Le projet suit une **architecture hexagonale** (ports & adapters) : le domaine métier est isolé de toute dépendance technique (JPA, Kafka, Redis). Les services implémentent des ports entrants (use cases) et dépendent de ports sortants (interfaces) implémentés par des adapters dans `infrastructure/`.

```
domain/
  model/        → entités, enums, value objects
  port/
    in/         → interfaces use cases (ex. RecordMatchResultUseCase)
    out/        → interfaces infra (ex. SaveMatchPort, LoadMatchPort)
  service/      → logique métier, implémente les ports entrants

infrastructure/
  persistence/  → adapters JPA (implémentent les ports sortants)
  messaging/    → adapter Kafka
```

Les side effects métier (mise à jour ELO, avancement du bracket, notifications temps réel) sont découplés du service principal via Kafka. Lorsqu'un résultat de match est enregistré, un événement `MatchFinishedEvent` est publié sur le topic `match-finished`. Trois consumers indépendants traitent cet événement de façon asynchrone :

- `elo-group` → met à jour les ratings ELO des deux joueurs
- `bracket-group` → avance le bracket au tour suivant
- `websocket-group` → broadcast une notification temps réel à tous les clients connectés via WebSocket

En cas d'échec répété d'un listener (3 tentatives espacées d'1 seconde), le message est redirigé vers le topic `match-finished.DLT` (Dead Letter Topic) pour inspection et rejeu manuel.

> Cette approche remplace une première implémentation basée sur les Spring Events synchrones, afin de se rapprocher d'une architecture orientée événements distribuée.

**Test WebSocket** : une page de démonstration est disponible sur `http://localhost:8080/ws-test.html`. Elle permet de visualiser en temps réel les événements de fin de match sans authentification.

---

## Installation & Configuration

1. Cloner le projet :

```bash
git clone https://github.com/BertrandGoulois/java-tournament-manager.git
cd java-tournament-manager
```

2. Créer un fichier `src/main/resources/application-local.properties` :

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/tournament_manager
spring.datasource.username=postgres
spring.datasource.password=tonmotdepasse
```

> Les tables et le user admin sont créés automatiquement par Liquibase au démarrage. Aucun script SQL manuel requis.

3. Démarrer les services (Kafka, Zookeeper, Redis) :

```bash
docker-compose up -d
```

---

## Running

**Démarrage :**

```bash
./mvnw spring-boot:run
```

**Tests unitaires :**

```bash
./mvnw verify
```

> Tests unitaires uniquement (sans Docker) : `./mvnw test`
> Les tests d'intégration (Testcontainers, Kafka embarqué) nécessitent Docker.

**Documentation API :**

Swagger UI disponible sur : `http://localhost:8080/swagger-ui/index.html`

**Test WebSocket :**

Page de démonstration disponible sur : `http://localhost:8080/ws-test.html`

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

#### Start tournament

- **POST** `/api/tournaments/{id}/start`

> Génère automatiquement le bracket en élimination directe. Les joueurs sans adversaire (byes) sont qualifiés automatiquement.

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

> Après enregistrement du résultat : publication d'un événement Kafka `match-finished`, mise à jour ELO des deux joueurs, avancement automatique au tour suivant, notification WebSocket temps réel, fin du tournoi si c'était la finale.

- **Errors** :
  - `400` → match déjà terminé
  - `400` → le gagnant n'est pas un des joueurs du match

---

## Key Features

- Architecture hexagonale (ports & adapters) - domaine métier isolé de l'infra
- Génération de bracket en élimination directe avec support des byes
- Calcul ELO après chaque match (K=32, formule standard)
- Idempotence du calcul ELO - protection contre les doublons Kafka
- Avancement automatique au tour suivant via événements Kafka
- Dead Letter Queue Kafka pour les messages en échec (`match-finished.DLT`)
- Notifications temps réel via WebSocket à chaque fin de match
- Cache Redis sur les statistiques joueur (`GET /players/{id}/stats`)
- Statistiques joueur (win rate, historique ELO)
- Authentification JWT avec refresh token et révocation (logout)
- Pagination sur les listes de joueurs et tournois
- Migrations versionnées avec Liquibase
- Couverture de tests élevée : tests unitaires (JUnit 5 / Mockito), tests controller (MockMvc) et tests d'intégration (Testcontainers + Kafka embarqué)

---

## Évolutions possibles

- Format round-robin / phase de groupes
- Tests de charge (Gatling)
