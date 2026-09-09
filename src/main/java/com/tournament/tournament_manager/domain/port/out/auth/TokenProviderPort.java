package com.tournament.tournament_manager.domain.port.out.auth;

/**
 * Port sortant : émission d'un jeton d'accès pour un utilisateur authentifié.
 *
 * <p><b>Point 3.2 de la revue.</b> {@code AuthService} et {@code RefreshTokenService}
 * importaient directement {@code config.security.JwtService} — une classe d'infrastructure,
 * dans une couche qui n'a pas le droit d'en connaître. La règle ArchUnit existante ne le
 * voyait pas : elle ne surveillait que {@code domain}, laissant {@code application} libre de
 * dépendre de n'importe quoi. L'architecture hexagonale était donc affirmée partout et
 * vérifiée à moitié.
 *
 * <p>L'interface est délibérément réduite à ce que la couche applicative utilise réellement,
 * plutôt qu'un miroir de {@code JwtService}. Elle ne parle pas de JWT : ni signature, ni
 * expiration, ni algorithme n'apparaissent ici. Remplacer les JWT par des jetons opaques
 * adossés à un stockage serveur ne toucherait que l'adaptateur.
 */
public interface TokenProviderPort {

    /**
     * Émet un jeton d'accès pour {@code username}.
     *
     * <p>La durée de validité et le format relèvent de l'implémentation : la couche
     * applicative n'a besoin de savoir ni l'une ni l'autre.
     *
     * @param username identifiant de l'utilisateur authentifié
     * @return le jeton, à transmettre au client
     */
    String generateAccessToken(String username);
}
