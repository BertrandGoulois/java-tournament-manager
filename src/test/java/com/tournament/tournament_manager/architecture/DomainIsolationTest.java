package com.tournament.tournament_manager.architecture;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Vérifie en continu la règle d'isolation du domaine (points 21 et 22 de la revue) : un
 * README qui affirme une isolation non vérifiée par la CI n'est qu'une promesse non tenue —
 * cette classe rend la règle exécutable, elle échoue si quiconque réintroduit une
 * dépendance technique dans {@code domain}.
 *
 * <p>{@code domain} peut dépendre de lui-même et du JDK ({@code java.*}), ainsi que des
 * exceptions métier propres au projet ({@code exception.domain}, de simples
 * {@code RuntimeException} sans aucune dépendance technique elles-mêmes) — rien d'autre.
 * Ni JPA/Hibernate, ni Spring (y compris Spring Data {@code Page}/{@code Pageable}), ni
 * Lombok, ni Jackson, ni aucune autre librairie technique — et, depuis le point 22,
 * {@code dto.request}/{@code dto.response} non plus : ce sont des DTO orientés transport
 * HTTP (annotations Swagger, validation Jakarta), pas le langage du domaine. Un port
 * (interface entrante ou sortante) doit s'exprimer dans le vocabulaire du métier, pas dans
 * celui d'un format de sérialisation particulier — c'est ce qui permettait au JSON-RPC de
 * se contenter de réutiliser les DTO REST faute d'alternative (voir la Javadoc de
 * {@code PlayerRestMapper} pour la distinction avec la réutilisation légitime de ces DTO
 * *par un adaptateur*, à la frontière).
 */
class DomainIsolationTest {

    private static final String BASE_PACKAGE = "com.tournament.tournament_manager";

    @Test
    void domainShouldNotDependOnTechnicalFrameworks() {
        var classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE_PACKAGE);

        ArchRule rule = noClasses()
                .that().resideInAPackage(BASE_PACKAGE + ".domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "jakarta.persistence..",
                        "org.hibernate..",
                        "org.springframework..",
                        "lombok..",
                        "com.fasterxml.jackson..",
                        "tools.jackson..",
                        "io.github.bucket4j..",
                        "org.apache.kafka..",
                        BASE_PACKAGE + ".dto.."
                )
                .because("le domaine doit rester isolé de toute dépendance technique — "
                        + "voir infrastructure.output.persistence.entity pour la persistance, "
                        + "infrastructure.output.persistence.mapper pour la conversion JPA, "
                        + "et infrastructure.input.mapper pour la conversion des DTO REST/JSON-RPC");

        rule.check(classes);
    }

    /**
     * Point 3.2 de la revue : la couche applicative n'a pas le droit de nommer
     * l'infrastructure.
     *
     * <p>La regle ci-dessus ne surveillait que {@code domain}, ce qui laissait
     * {@code application} libre de dependre de n'importe quoi — et elle en profitait :
     * {@code AuthService} et {@code RefreshTokenService} importaient directement
     * {@code config.security.JwtService}. L'architecture hexagonale etait donc affirmee
     * partout dans le README et verifiee a moitie dans la CI. Un choix technologique (JWT)
     * remontait jusque dans le code metier, ou il n'a rien a faire : l'emission de jeton
     * passe desormais par {@code TokenProviderPort}.
     */
    @Test
    void applicationShouldNotDependOnInfrastructure() {
        var classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE_PACKAGE);

        ArchRule rule = noClasses()
                .that().resideInAPackage(BASE_PACKAGE + ".application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        BASE_PACKAGE + ".infrastructure..",
                        BASE_PACKAGE + ".config.."
                )
                .because("la couche applicative orchestre le metier et ne doit connaitre que "
                        + "des ports (domain.port.out) — voir TokenProviderPort, introduit "
                        + "pour que l'emission de jeton cesse de nommer JwtService");

        rule.check(classes);
    }

    /**
     * Les DTO de transport ne remontent pas dans la couche applicative — a une exception
     * pres, nommee ici plutot que tacite.
     *
     * <p>{@code application.rpc} manipule l'enveloppe JSON-RPC ({@code JsonRpcRequest},
     * {@code JsonRpcResponse}, {@code JsonRpcError}) parce que le dispatch <i>est</i> le
     * protocole : router une requete JSON-RPC suppose d'en lire le champ {@code method} et
     * d'en produire le format d'erreur normalise. C'est une dette assumee, pas un oubli.
     *
     * <p>L'interet de l'ecrire sous forme de regle est qu'elle est <b>bornee</b> : le jour
     * ou un autre paquet applicatif se mettra a importer des DTO, la CI le dira. Une
     * exception invisible se serait propagee sans que personne ne s'en apercoive.
     */
    @Test
    void applicationShouldNotDependOnTransportDtos_exceptJsonRpcEnvelope() {
        var classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE_PACKAGE);

        ArchRule rule = noClasses()
                .that().resideInAPackage(BASE_PACKAGE + ".application..")
                .and().resideOutsideOfPackage(BASE_PACKAGE + ".application.rpc..")
                .should().dependOnClassesThat().resideInAnyPackage(BASE_PACKAGE + ".dto..")
                .because("les DTO sont des objets de transport HTTP (annotations Swagger, "
                        + "validation Jakarta) ; seul application.rpc y touche, parce que "
                        + "l'enveloppe JSON-RPC est le protocole qu'il route");

        rule.check(classes);
    }
}
