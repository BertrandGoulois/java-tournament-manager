package com.tournament.tournament_manager.application.token;

import com.tournament.tournament_manager.config.security.JwtService;
import com.tournament.tournament_manager.domain.model.entities.RefreshToken;
import com.tournament.tournament_manager.domain.port.in.auth.RefreshTokenUseCase;
import com.tournament.tournament_manager.domain.port.out.auth.DeleteRefreshTokenPort;
import com.tournament.tournament_manager.domain.port.out.auth.LoadRefreshTokenPort;
import com.tournament.tournament_manager.domain.port.out.auth.SaveRefreshTokenPort;
import com.tournament.tournament_manager.domain.port.out.auth.UserExistsPort;
import com.tournament.tournament_manager.dto.response.auth.AuthResponse;
import com.tournament.tournament_manager.exception.domain.InvalidException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Implémentation des cas d'utilisation liés au refresh token JWT.
 *
 * <p>Le refresh token brut est un UUID aléatoire (122 bits d'entropie), envoyé au client
 * et jamais stocké tel quel côté serveur : seul son hash SHA-256 est persisté en base
 * (voir {@link #hash(String)}). Une fuite de la table {@code refresh_tokens} ne donne donc
 * pas d'accès direct aux comptes — un hash ne peut pas être présenté tel quel comme un
 * refresh token valide.
 *
 * <p><b>Session unique par utilisateur</b> : {@link #generateRefreshToken} révoque tous les
 * refresh tokens existants de l'utilisateur avant d'en émettre un nouveau. Se connecter sur
 * un second appareil déconnecte donc silencieusement le premier. C'est un choix assumé (pas
 * de sessions concurrentes), pas un oubli.
 *
 * <p><b>Rotation</b> : {@link #refresh} invalide le token présenté et en émet un nouveau à
 * chaque appel (le client doit toujours remplacer son refresh token stocké par celui reçu en
 * retour). Un refresh token volé et déjà utilisé par son propriétaire légitime devient inerte
 * pour l'attaquant — et sa présentation après rotation constitue un signal de compromission
 * (voir le log en cas de réutilisation d'un token révoqué).
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class RefreshTokenService implements RefreshTokenUseCase {

    private final JwtService jwtService;
    private final SaveRefreshTokenPort saveRefreshTokenPort;
    private final LoadRefreshTokenPort loadRefreshTokenPort;
    private final DeleteRefreshTokenPort deleteRefreshTokenPort;
    private final UserExistsPort userExistsPort;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    public RefreshTokenService(JwtService jwtService,
                               SaveRefreshTokenPort saveRefreshTokenPort,
                               LoadRefreshTokenPort loadRefreshTokenPort,
                               DeleteRefreshTokenPort deleteRefreshTokenPort,
                               UserExistsPort userExistsPort) {
        this.jwtService = jwtService;
        this.saveRefreshTokenPort = saveRefreshTokenPort;
        this.loadRefreshTokenPort = loadRefreshTokenPort;
        this.deleteRefreshTokenPort = deleteRefreshTokenPort;
        this.userExistsPort = userExistsPort;
    }

    /**
     * Génère et persiste un nouveau refresh token pour un utilisateur.
     *
     * <p>Révoque d'abord tous les refresh tokens existants de cet utilisateur — une seule
     * session active à la fois (voir Javadoc de la classe).
     *
     * @param username le username de l'utilisateur
     * @return le refresh token brut (à transmettre au client, jamais stocké tel quel)
     */
    @Transactional
    public String generateRefreshToken(String username) {
        deleteRefreshTokenPort.deleteByUsername(username);
        return issueNewToken(username);
    }

    /**
     * Génère un nouveau access token à partir d'un refresh token valide, et fait tourner
     * (rotation) le refresh token lui-même : celui présenté est révoqué, un nouveau est émis.
     *
     * @param rawRefreshToken le refresh token brut présenté par le client
     * @return un nouvel access token JWT et un nouveau refresh token
     * @throws InvalidException si le token est invalide, expiré, révoqué, ou si le compte
     *                          associé n'existe plus
     */
    @Override
    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        RefreshToken token = loadRefreshTokenPort.loadByToken(hash(rawRefreshToken))
                .orElseThrow(() -> new InvalidException("Refresh token invalide"));

        if (token.isRevoked()) {
            // Un token révoqué ne peut légitimement plus être présenté : soit il a déjà servi
            // (rotation), soit il a été explicitement révoqué (logout). Dans les deux cas, sa
            // réutilisation est un signal à surveiller (token potentiellement volé, présenté
            // après que son propriétaire légitime a déjà tourné).
            log.warn("Tentative de réutilisation d'un refresh token révoqué [username={}]", token.getUsername());
            throw new InvalidException("Refresh token révoqué");
        }
        if (token.getExpiryDate().isBefore(Instant.now())) {
            throw new InvalidException("Refresh token expiré");
        }
        if (!userExistsPort.existsByUsername(token.getUsername())) {
            throw new InvalidException("Refresh token invalide");
        }

        token.setRevoked(true);
        saveRefreshTokenPort.saveRefreshToken(token);

        String newAccessToken = jwtService.generateToken(token.getUsername());
        String newRefreshToken = issueNewToken(token.getUsername());
        return new AuthResponse(newAccessToken, newRefreshToken);
    }

    /**
     * Révoque le refresh token (logout).
     *
     * @param rawRefreshToken le refresh token brut à révoquer
     * @throws InvalidException si le token n'existe pas
     */
    @Override
    @Transactional
    public void revoke(String rawRefreshToken) {
        RefreshToken token = loadRefreshTokenPort.loadByToken(hash(rawRefreshToken))
                .orElseThrow(() -> new InvalidException("Refresh token invalide"));
        token.setRevoked(true);
        saveRefreshTokenPort.saveRefreshToken(token);
    }

    private String issueNewToken(String username) {
        String rawToken = UUID.randomUUID().toString();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(hash(rawToken));
        refreshToken.setUsername(username);
        refreshToken.setExpiryDate(Instant.now().plusSeconds(refreshExpiration / 1000));

        saveRefreshTokenPort.saveRefreshToken(refreshToken);
        return rawToken;
    }

    /**
     * Hash SHA-256 (encodé en hexadécimal) d'un refresh token brut. SHA-256 simple (sans sel
     * ni facteur de coût type bcrypt/argon2) est suffisant ici : contrairement à un mot de
     * passe, un refresh token est déjà une valeur aléatoire à haute entropie (122 bits, UUID
     * v4) — aucune attaque par dictionnaire n'est possible contre son hash.
     */
    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 est garanti disponible dans toute JVM standard (java.security.Security) ;
            // ce cas ne devrait jamais se produire en pratique.
            throw new IllegalStateException("Algorithme de hash SHA-256 indisponible", e);
        }
    }
}
