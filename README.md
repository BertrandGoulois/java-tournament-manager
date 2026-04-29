# java-tournament-manager

API REST de gestion de tournois sportifs en élimination directe, développée en Java 21 / Spring Boot.

Le projet suit une architecture en couches (controller → service → repository), avec un découplage des side effects via Spring Events. Les tests couvrent les cas nominaux et les cas d'erreur, avec des tests d'intégration sur une vraie base PostgreSQL via Testcontainers.

---

## Technical Stack

- **Java 21**
- **Spring Boot 4**
- **Spring Security** + **JWT** (authentification stateless)
- **Spring Data JPA** / **Hibernate**
- **PostgreSQL** (production)
- **Liquibase** (migrations versionnées)
- **Spring Events** (découplage des side effects)
- **JUnit 5** + **Mockito** (tests unitaires)
- **Testcontainers** (tests d'intégration)
- **Springdoc / Swagger UI** (documentation API)
- **Lombok**

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

> Les tables sont créées automatiquement par Liquibase au démarrage. Aucun script SQL manuel requis.

---

## Running

**Démarrage :**

```bash
./mvnw spring-boot:run
```

**Tests unitaires :**

```bash
./mvnw test
```

> Les tests d'intégration nécessitent Docker (Testcontainers).

**Documentation API :**

Swagger UI disponible sur : `http://localhost:8080/swagger-ui/index.html`

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
  "token": "<JWT token>"
}
```

- **Usage** : inclure le JWT dans le header `Authorization` pour les endpoints protégés :

```
Authorization: Bearer <JWT token>
```

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

- **GET** `/api/players`

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

- **GET** `/api/tournaments`

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

> Après enregistrement du résultat : mise à jour ELO des deux joueurs, avancement automatique au tour suivant, fin du tournoi si c'était la finale.

- **Errors** :
  - `400` → match déjà terminé
  - `400` → le gagnant n'est pas un des joueurs du match

---

## Key Features

- Génération de bracket en élimination directe avec support des byes
- Calcul ELO après chaque match (K=32, formule standard)
- Avancement automatique au tour suivant via Spring Events
- Statistiques joueur (win rate, historique ELO)
- Authentification JWT avec rôles ADMIN / PLAYER
- Migrations versionnées avec Liquibase
- Tests unitaires (JUnit 5 / Mockito) et tests d'intégration (Testcontainers)

---

## Évolutions possibles

- Docker + Docker Compose
- Messaging distribué avec Kafka (remplacement des Spring Events)
- Format round-robin / phase de groupes
- Notifications temps réel (WebSocket)
