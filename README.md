# java-tournament-manager

![CI](https://github.com/BertrandGoulois/java-tournament-manager/actions/workflows/ci.yml/badge.svg)
[![Javadoc](https://img.shields.io/badge/javadoc-online-blue)](https://bertrandgoulois.github.io/java-tournament-manager/)
![Coverage](.github/badges/jacoco.svg)
![Branches](.github/badges/branches.svg)

API REST de gestion de tournois sportifs (élimination directe, round-robin, ou phase de groupes + bracket), développée en Java 21 / Spring Boot.

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
- **JSON-RPC 2.0** (API alternative coexistant avec REST, endpoint unique `POST /api/rpc`)
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
  model/          -> entités, enums, value objects
  port/
    in/           -> interfaces use cases (ex. RecordMatchResultUseCase)
    out/          -> interfaces infra (ex. SaveMatchPort, LoadMatchPort)
    out/strategy/ -> interfaces de stratégie métier (ex. TournamentStartStrategy)
    out/rpc/      -> interface de handler JSON-RPC (JsonRpcMethodHandler)

service/
  tournament/     -> use cases liés aux tournois
  bracket/        -> use cases liés au bracket
  match/          -> use cases liés aux matchs
  player/         -> use cases liés aux joueurs
  registration/   -> use cases liés aux inscriptions
  rpc/            -> dispatcher JSON-RPC (JsonRpcDispatchService)
  shared/         -> utilitaires partagés (BracketUtils, RoundRobinUtils)

infrastructure/
  persistence/    -> adapters JPA
  messaging/      -> adapter Kafka
  ai/             -> adapter OpenAI
  strategy/       -> stratégies de démarrage de tournoi
  rpc/            -> handlers JSON-RPC par domaine (tournament/, player/, registration/, match/)
```

### Multi-format de tournoi

Un tournoi peut être créé selon trois formats (`TournamentFormat`) :

- `SINGLE_ELIMINATION` (par défaut) - bracket en élimination directe
- `ROUND_ROBIN` - chaque joueur affronte tous les autres une fois, classement par points
- `GROUPS_THEN_KNOCKOUT` - phase de groupes round-robin puis bracket en élimination directe entre les qualifiés

Le démarrage du tournoi (`StartTournamentService`) délègue la génération des matchs initiaux à la stratégie correspondante, via le pattern **Strategy** : Spring injecte toutes les implémentations de `TournamentStartStrategy` et le service choisit la bonne selon `tournament.getFormat()`.

- `SingleEliminationStartStrategy` -> mélange aléatoire et génère le premier tour du bracket (avec byes si effectif impair)
- `RoundRobinStartStrategy` -> génère l'intégralité des confrontations en une seule fois via la **méthode du cercle** (`RoundRobinUtils`), garantissant que chaque paire de joueurs se rencontre exactement une fois
- `GroupsThenKnockoutStartStrategy` -> répartit les joueurs en `numberOfGroups` groupes égaux, puis génère un round-robin complet à l'intérieur de chaque groupe (même `RoundRobinUtils`, avec un `groupNumber` renseigné sur chaque match)

`BracketListener` route également la **progression** du tournoi selon le format et la nature du match :

- `SINGLE_ELIMINATION` -> avancement round par round via `AdvanceBracketService`
- `ROUND_ROBIN` -> vérification d'achèvement global dès que tous les matchs sont joués, via `CheckTournamentCompletionService`
- `GROUPS_THEN_KNOCKOUT` -> un match de phase de groupes (`groupNumber != null`) déclenche `GenerateKnockoutBracketFromGroupsService`, qui calcule les qualifiés de chaque groupe (3 points par victoire, top N par groupe) et génère le bracket final une fois tous les matchs de groupe terminés ; un match de bracket (`groupNumber == null`) réutilise directement la logique d'avancement de l'élimination directe

Le **classement round-robin** (`GetStandingsService`) est calculé à la demande à partir des matchs terminés - pas de table dédiée - avec 3 points par victoire, trié par points puis par nombre de victoires.

### API JSON-RPC 2.0

En parallèle de l'API REST, un endpoint unique `POST /api/rpc` expose les mêmes opérations métier via le protocole JSON-RPC 2.0. Le dispatcher (`JsonRpcDispatchService`) réutilise le même pattern Strategy que les stratégies de tournoi : Spring injecte automatiquement tous les handlers (`@Component` implémentant `JsonRpcMethodHandler`), indexés par nom de méthode. Aucune logique métier n'est dupliquée - chaque handler délègue directement au use case correspondant.

Méthodes disponibles : `tournament.create`, `tournament.start`, `tournament.getById`, `tournament.getAll`, `tournament.delete`, `tournament.getBracket`, `tournament.getStandings`, `player.create`, `player.getById`, `player.getAll`, `player.getStats`, `player.delete`, `registration.register`, `registration.getByTournament`, `match.getById`, `match.recordResult`, `match.getCommentary`.

L'authentification reste exclusivement sur REST (`/api/auth/**`) - le JWT est requis sur `/api/rpc` comme sur les autres endpoints protégés.

### Architecture événementielle (Kafka)

Les side effects métier (mise à jour ELO, avancement du bracket, notifications temps réel, génération de commentaires) sont découplés du service principal via Kafka. Lorsqu'un résultat de match est enregistré, un événement `MatchFinishedEvent` est publié sur le topic `match-finished`. Quatre consumers indépendants traitent cet événement de façon asynchrone :

- `elo-group` -> met à jour les ratings ELO des deux joueurs
- `bracket-group` -> fait progresser le tournoi selon son format
- `websocket-group` -> broadcast une notification temps réel à tous les clients connectés via WebSocket
- `commentary-group` -> génère un commentaire narratif via OpenAI GPT-4o-mini

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

> `KafkaIntegrationTest`, `PlayerIntegrationTest`, `RoundRobinIntegrationTest` et `GroupsThenKnockoutIntegrationTest` sont exclus de la CI standard (`maven-surefire-plugin`) pour rester rapide et stable. Les deux derniers valident leur flux complet respectif sans dépendre d'un container Kafka : les transitions normalement déclenchées par le listener Kafka asynchrone sont appelées directement pour isoler la logique métier.

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
  - `409` -> username ou email déjà utilisé

#### Get all players

- **GET** `/api/players?page=0&size=10&sort=username,asc`
- **Response JSON** : objet `Page` Spring avec `content`, `totalElements`, `totalPages`, `number`

#### Get player by ID

- **GET** `/api/players/{id}`
- **Errors** :
  - `404` -> joueur introuvable

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
- **Body JSON** (`SINGLE_ELIMINATION` ou `ROUND_ROBIN`) :

```json
{
  "name": "Spring Championship",
  "maxPlayers": 8,
  "format": "SINGLE_ELIMINATION"
}
```

- **Body JSON** (`GROUPS_THEN_KNOCKOUT`) :

```json
{
  "name": "Spring Groups Cup",
  "maxPlayers": 8,
  "format": "GROUPS_THEN_KNOCKOUT",
  "numberOfGroups": 2,
  "qualifiersPerGroup": 2
}
```

> `format` est optionnel - `SINGLE_ELIMINATION` par défaut si omis. Valeurs possibles : `SINGLE_ELIMINATION`, `ROUND_ROBIN`, `GROUPS_THEN_KNOCKOUT`. `numberOfGroups` et `qualifiersPerGroup` ne sont requis que pour `GROUPS_THEN_KNOCKOUT`.

- **Errors** :
  - `400` -> `maxPlayers` n'est pas une puissance de 2 (uniquement pour `SINGLE_ELIMINATION`)
  - `400` -> (`GROUPS_THEN_KNOCKOUT`) `numberOfGroups` absent ou < 2
  - `400` -> (`GROUPS_THEN_KNOCKOUT`) `maxPlayers` non divisible par `numberOfGroups`
  - `400` -> (`GROUPS_THEN_KNOCKOUT`) `qualifiersPerGroup` absent, < 1, ou >= à la taille d'un groupe
  - `400` -> (`GROUPS_THEN_KNOCKOUT`) le nombre total de qualifiés (`numberOfGroups x qualifiersPerGroup`) n'est pas une puissance de 2
  - `409` -> nom déjà utilisé

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

> Génère automatiquement les matchs initiaux selon le format du tournoi : bracket en élimination directe avec byes (`SINGLE_ELIMINATION`), intégralité des confrontations (`ROUND_ROBIN`), ou répartition en groupes suivie d'un round-robin par groupe (`GROUPS_THEN_KNOCKOUT`).

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

> Les rounds sont triés du premier (valeur la plus haute) à la finale (round 2). Pertinent pour `SINGLE_ELIMINATION`, et pour la phase finale d'un tournoi `GROUPS_THEN_KNOCKOUT` une fois le bracket généré.

#### Get tournament standings

- **GET** `/api/tournaments/{id}/standings`
- **Response JSON** :

```json
{
  "tournamentId": 1,
  "tournamentName": "Round Robin Cup",
  "standings": [
    {
      "playerId": 1,
      "username": "alice",
      "matchesPlayed": 3,
      "wins": 3,
      "losses": 0,
      "points": 9
    }
  ]
}
```

> Pertinent pour les tournois au format `ROUND_ROBIN`. Calculé à la demande à partir des matchs terminés (3 points par victoire), trié par points décroissants.

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
  - `400` -> tournoi non ouvert aux inscriptions
  - `400` -> joueur déjà inscrit
  - `400` -> tournoi complet

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

> Après enregistrement du résultat : publication d'un événement Kafka `match-finished`, mise à jour ELO des deux joueurs, notification WebSocket temps réel, génération de commentaire LLM, et progression du tournoi selon son format et la nature du match.

- **Errors** :
  - `400` -> match déjà terminé
  - `400` -> le gagnant n'est pas un des joueurs du match

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

### JSON-RPC

- **POST** `/api/rpc`
- **Body JSON** :

```json
{
  "jsonrpc": "2.0",
  "method": "tournament.create",
  "params": {
    "name": "Spring Championship",
    "maxPlayers": 8,
    "format": "SINGLE_ELIMINATION"
  },
  "id": "1"
}
```

- **Response JSON (succès)** :

```json
{
  "jsonrpc": "2.0",
  "result": { },
  "error": null,
  "id": "1"
}
```

- **Response JSON (erreur)** :

```json
{
  "jsonrpc": "2.0",
  "result": null,
  "error": {
    "code": -32601,
    "message": "Method not found: unknown.method",
    "data": null
  },
  "id": "1"
}
```

> Conformément à la spec JSON-RPC 2.0, le statut HTTP est toujours `200` même en cas d'erreur applicative. Codes standards : `-32601` méthode inconnue, `-32602` paramètres invalides, `-32603` erreur interne.

---

## Key Features

- Architecture hexagonale (ports & adapters) - domaine métier isolé de l'infra
- Use cases atomiques : une classe par use case (principe de responsabilité unique)
- Support multi-format de tournoi (`SINGLE_ELIMINATION`, `ROUND_ROBIN`, `GROUPS_THEN_KNOCKOUT`) via pattern Strategy, extensible pour de futurs formats
- Génération de bracket en élimination directe avec support des byes
- Génération round-robin via la méthode du cercle (chaque paire de joueurs se rencontre exactement une fois)
- Phase de groupes configurable (nombre de groupes et de qualifiés par groupe) avec transition automatique vers un bracket final entre qualifiés
- Classement round-robin calculé à la demande (`GET /tournaments/{id}/standings`)
- Consultation du bracket complet par round (`GET /tournaments/{id}/bracket`)
- API JSON-RPC 2.0 en parallèle de REST (`POST /api/rpc`) - même pattern Strategy que les stratégies de tournoi, aucune logique métier dupliquée
- Calcul ELO après chaque match (K=32, formule standard)
- Idempotence du calcul ELO - protection contre les doublons Kafka
- Progression de tournoi multi-format event-driven via Kafka (avancement de bracket, détection de fin de round-robin, ou transition phase de groupes -> bracket final)
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
- Couverture de tests élevée : tests unitaires (JUnit 5 / Mockito), tests controller (MockMvc) et tests d'intégration (Testcontainers) sur les trois formats de tournoi
- Circuit breaker Resilience4j sur l'appel OpenAI (testé : transitions CLOSED -> OPEN -> HALF_OPEN, court-circuit vérifié)

---

## Évolutions possibles

- Reverse proxy (nginx) devant l'application pour une validation complète de la confiance sur `X-Forwarded-For` en environnement de production
