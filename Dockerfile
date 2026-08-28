FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline
COPY src ./src
RUN ./mvnw package -DskipTests

# Étape dédiée à l'extraction en couches (jar en couches, activé par défaut par
# spring-boot-maven-plugin depuis Spring Boot 2.3) : sépare dépendances / loader /
# snapshot-dependencies / code applicatif dans des layers Docker distincts. Les
# dépendances (qui changent rarement) restent en cache entre deux builds qui ne
# modifient que le code applicatif - un simple changement de code n'invalide plus
# que la couche "application" (quelques Ko), au lieu de re-télécharger/reconstruire
# le jar complet (dépendances comprises) à chaque build.
#
# NOTE : la syntaxe exacte du jarmode (-Djarmode=tools, syntaxe unifiée depuis Spring
# Boot 3.3) n'a pas pu être vérifiée contre la version réelle de ce projet (accès
# réseau indisponible ici) - si cette étape échoue avec "Unable to find layers.idx"
# ou une erreur de jarmode inconnu, remplacer par l'ancienne syntaxe dédiée :
# `java -Djarmode=layertools -jar app.jar extract`.
FROM eclipse-temurin:21-jdk-alpine AS extractor
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app
COPY --from=extractor /app/extracted/dependencies/ ./
COPY --from=extractor /app/extracted/spring-boot-loader/ ./
COPY --from=extractor /app/extracted/snapshot-dependencies/ ./
COPY --from=extractor /app/extracted/application/ ./

RUN chown -R spring:spring /app
USER spring

EXPOSE 8080 9001

# Port 9001, pas 8080 : l'actuator tourne sur management.server.port, séparé du
# port applicatif principal (voir docker-compose.yml et SecurityConfig) - ce
# healthcheck tapait encore sur l'ancien port et échouait silencieusement.
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost:9001/actuator/health || exit 1

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
