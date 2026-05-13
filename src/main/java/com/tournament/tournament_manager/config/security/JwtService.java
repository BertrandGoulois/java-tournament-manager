package com.tournament.tournament_manager.config.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Génère et valide les tokens JWT.
 *
 * <p>La clé secrète ({@code jwt.secret}) doit être une chaîne encodée en Base64.
 * La durée d'expiration ({@code jwt.expiration}) est exprimée en millisecondes.
 * L'algorithme de signature utilisé est HMAC-SHA (déduit de la longueur de la clé).
 */
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long expiration;

    /**
     * Génère un token JWT signé pour un utilisateur.
     *
     * @param username le subject du token
     * @return le token JWT compact et signé
     */
    public String generateToken(String username) {
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extrait le username (subject) d'un token JWT.
     *
     * @param token le token JWT
     * @return le username extrait
     */
    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    /**
     * Vérifie qu'un token est valide : le username correspond et le token n'est pas expiré.
     *
     * @param token    le token JWT à valider
     * @param username le username attendu
     * @return {@code true} si le token est valide
     */
    public boolean isTokenValid(String token, String username) {
        return extractUsername(token).equals(username) && !isTokenExpired(token);
    }

    /**
     * Vérifie si un token est expiré.
     *
     * @param token le token JWT
     * @return {@code true} si le token est expiré
     */
    private boolean isTokenExpired(String token) {
        return extractClaims(token).getExpiration().before(new Date());
    }

    /**
     * Extrait les claims d'un token JWT après vérification de la signature.
     *
     * @param token le token JWT
     * @return les claims du token
     */
    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Construit la clé HMAC à partir de la clé secrète encodée en Base64.
     *
     * @return la clé de signature
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}