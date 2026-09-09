package com.tournament.tournament_manager.config.security;

import com.tournament.tournament_manager.infrastructure.output.persistence.entity.UserEntity;
import com.tournament.tournament_manager.infrastructure.output.persistence.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Fixe le mot de passe du compte {@code admin} a partir de l'environnement, au demarrage.
 *
 * <p><b>Pourquoi ce composant existe (point 1.5 de la revue).</b> La migration
 * {@code 003-insert-admin-user.sql} inserait le compte {@code admin} avec un hash bcrypt
 * ecrit en dur dans le depot. Deux consequences :
 * <ul>
 *   <li>le hash est public — quiconque clone le projet peut le casser hors ligne, sans
 *       limite de debit et sans qu'aucun rate limiting ni aucune alerte ne le voie ;</li>
 *   <li><b>toutes</b> les installations du projet partagent le meme mot de passe admin.</li>
 * </ul>
 *
 * <p>La migration n'est pas modifiee (elle a deja tourne sur les bases existantes, Liquibase
 * refuserait le changement de checksum) : le compte continue d'etre cree par elle, mais son
 * mot de passe est immediatement remplace ici par la valeur de {@code ADMIN_INITIAL_PASSWORD}.
 *
 * <p>Comportement :
 * <ul>
 *   <li>propriete renseignee → le mot de passe est (re)hashe et applique a chaque demarrage,
 *       ce qui en fait aussi le mecanisme de rotation ;</li>
 *   <li>propriete absente et compte encore porteur du hash commite →
 *       <b>echec du demarrage</b>. Demarrer avec un mot de passe admin public est un defaut
 *       qui doit etre bruyant, pas un avertissement noye dans les logs ;</li>
 *   <li>propriete absente et mot de passe deja change → rien a faire, demarrage normal.</li>
 * </ul>
 *
 * <p>Le hash est compare tel quel : bcrypt etant sale, un mot de passe change produit
 * necessairement un hash different de la constante ci-dessous.
 */
@Slf4j
@Component
public class AdminAccountInitializer {

    private static final String ADMIN_USERNAME = "admin";

    /**
     * Hash bcrypt figé dans la migration {@code 003}. Deja public dans l'historique git :
     * le reproduire ici n'aggrave rien et permet de detecter les installations qui ne l'ont
     * jamais change.
     */
    private static final String COMMITTED_HASH =
            "$2a$12$tKWA7GKVtM6ti5vQIwnm9.k5.h/XsHmmMu30cierOor2I16QDLXLi";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.initial-password:}")
    private String initialPassword;

    public AdminAccountInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void applyInitialAdminPassword() {
        Optional<UserEntity> maybeAdmin = userRepository.findByUsername(ADMIN_USERNAME);
        if (maybeAdmin.isEmpty()) {
            log.debug("Aucun compte '{}' en base, rien a initialiser", ADMIN_USERNAME);
            return;
        }
        UserEntity admin = maybeAdmin.get();
        boolean stillCommittedHash = COMMITTED_HASH.equals(admin.getPassword());

        if (initialPassword == null || initialPassword.isBlank()) {
            if (stillCommittedHash) {
                throw new IllegalStateException(
                        "Le compte 'admin' porte encore le hash bcrypt commite dans la migration 003, "
                                + "dont le mot de passe est public. Definissez ADMIN_INITIAL_PASSWORD "
                                + "(voir .env.example) avant de demarrer l'application.");
            }
            log.debug("ADMIN_INITIAL_PASSWORD non defini et mot de passe admin deja personnalise, "
                    + "aucune action");
            return;
        }

        admin.setPassword(passwordEncoder.encode(initialPassword));
        userRepository.save(admin);
        log.info("Mot de passe du compte '{}' applique depuis l'environnement{}",
                ADMIN_USERNAME,
                stillCommittedHash ? " (remplacement du hash public de la migration 003)" : " (rotation)");
    }
}
