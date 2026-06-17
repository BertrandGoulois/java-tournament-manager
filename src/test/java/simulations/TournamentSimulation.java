package simulations;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;

public class TournamentSimulation extends Simulation {

    HttpProtocolBuilder httpProtocol = http
            .baseUrl("http://localhost:8080")
            .acceptHeader("application/json")
            .contentTypeHeader("application/json");

    ScenarioBuilder scenario = scenario("Tournament Full Flow")
            .exec(session -> session.set("userId", session.userId()))
            .exec(session -> {
                long id = session.userId();
                int b = (int) (id % 250) + 1;
                int c = (int) ((id / 250) % 250) + 1;
                return session.set("fakeIp", "10.0." + b + "." + c);
            })
            // Login
            .exec(http("Login")
                    .post("/api/auth/login")
                    .header("X-Forwarded-For", session -> session.getString("fakeIp"))
                    .body(StringBody("""
                            {"username":"admin","password":"password123"}
                            """))
                    .check(jsonPath("$.token").saveAs("jwt")))
            .exitHereIfFailed()
            .pause(1)
            // Créer un tournoi
            .exec(http("Create Tournament")
                    .post("/api/tournaments")
                    .header("Authorization", session -> "Bearer " + session.getString("jwt"))
                    .header("X-Forwarded-For", session -> session.getString("fakeIp"))
                    .body(StringBody(session -> """
                            {"name":"T_%s","maxPlayers":8}
                            """.formatted(java.util.UUID.randomUUID().toString().substring(0, 8))))
                    .check(jsonPath("$.id").saveAs("tournamentId")))
            .pause(1)
            // Créer 8 joueurs
            .exec(createPlayer(1))
            .exec(createPlayer(2))
            .exec(createPlayer(3))
            .exec(createPlayer(4))
            .exec(createPlayer(5))
            .exec(createPlayer(6))
            .exec(createPlayer(7))
            .exec(createPlayer(8))
            .pause(1)
            // Inscrire 8 joueurs
            .exec(registerPlayer(1))
            .exec(registerPlayer(2))
            .exec(registerPlayer(3))
            .exec(registerPlayer(4))
            .exec(registerPlayer(5))
            .exec(registerPlayer(6))
            .exec(registerPlayer(7))
            .exec(registerPlayer(8))
            .pause(1)
            // Démarrer le tournoi
            .exec(http("Start Tournament")
                    .post("/api/tournaments/#{tournamentId}/start")
                    .header("Authorization", session -> "Bearer " + session.getString("jwt"))
                    .header("X-Forwarded-For", session -> session.getString("fakeIp")))
            .pause(1)
            // Consulter le bracket
            .exec(http("Get Bracket")
                    .get("/api/tournaments/#{tournamentId}/bracket")
                    .header("Authorization", session -> "Bearer " + session.getString("jwt"))
                    .header("X-Forwarded-For", session -> session.getString("fakeIp")))
            .pause(1)
            // Consulter les stats du joueur 1
            .exec(http("Get Player Stats")
                    .get("/api/players/#{playerId1}/stats")
                    .header("Authorization", session -> "Bearer " + session.getString("jwt"))
                    .header("X-Forwarded-For", session -> session.getString("fakeIp")));

    {
        setUp(
                scenario.injectOpen(
                        atOnceUsers(1),
                        nothingFor(Duration.ofSeconds(10)),
                        rampUsers(50).during(Duration.ofSeconds(20)),
                        nothingFor(Duration.ofSeconds(10)),
                        rampUsers(100).during(Duration.ofSeconds(30)),
                        nothingFor(Duration.ofSeconds(10)),
                        rampUsers(200).during(Duration.ofSeconds(30)),
                        nothingFor(Duration.ofSeconds(10)),
                        rampUsers(500).during(Duration.ofSeconds(30))
                )
        ).protocols(httpProtocol)
                .assertions(
                        global().responseTime().percentile(95).lt(2000),
                        global().successfulRequests().percent().gt(95.0)
                );
    }

    private ChainBuilder createPlayer(int index) {
        return exec(http("Create Player " + index)
                .post("/api/players")
                .header("Authorization", session -> "Bearer " + session.getString("jwt"))
                .header("X-Forwarded-For", session -> session.getString("fakeIp"))
                .body(StringBody(session -> {
                    String uid = java.util.UUID.randomUUID().toString().substring(0, 8);
                    return """
                            {"username":"p_%s_%d","email":"p_%s_%d@mail.com"}
                            """.formatted(uid, index, uid, index);
                }))
                .check(jsonPath("$.id").saveAs("playerId" + index)));
    }

    private ChainBuilder registerPlayer(int index) {
        return exec(http("Register Player " + index)
                .post("/api/registrations")
                .header("Authorization", session -> "Bearer " + session.getString("jwt"))
                .header("X-Forwarded-For", session -> session.getString("fakeIp"))
                .body(StringBody(session -> """
                        {"playerId":%s,"tournamentId":%s}
                        """.formatted(
                        session.getString("playerId" + index),
                        session.getString("tournamentId")))));
    }
}