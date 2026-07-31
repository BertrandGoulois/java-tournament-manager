package com.tournament.tournament_manager.domain.port.out.auth;

/**
 * Port sortant : vérifie qu'un utilisateur existe toujours.
 *
 * <p>Utilisé notamment par {@code RefreshTokenService.refresh} : un refresh token
 * reste utilisable jusqu'à 7 jours, il faut donc vérifier à chaque utilisation que
 * le compte associé existe toujours, plutôt que de faire confiance à l'existence
 * du refresh token seule.
 */
public interface UserExistsPort {

    /**
     * Vérifie l'existence d'un utilisateur par son username.
     *
     * @param username le username à vérifier
     * @return {@code true} si un utilisateur avec ce username existe
     */
    boolean existsByUsername(String username);
}
