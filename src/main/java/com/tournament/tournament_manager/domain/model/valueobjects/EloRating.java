package com.tournament.tournament_manager.domain.model.valueobjects;

/**
 * Value object représentant le classement ELO d'un joueur.
 *
 * <p>Immuable : toute modification retourne une nouvelle instance.
 * La valeur ne peut pas être négative — tout calcul résultant en une valeur
 * inférieure à {@link #MIN} est automatiquement plafonné à {@code 0}.
 *
 * <p>La valeur par défaut à la création d'un joueur est {@link #DEFAULT} ({@code 1000}).
 *
 * <p>Type de domaine pur — aucune annotation JPA. Côté persistance,
 * {@code PlayerEntity} stocke directement la valeur ({@code int}) en colonne ;
 * {@code PlayerMapper} fait la conversion aux deux frontières.
 */
public record EloRating(int value) {

    public static final int DEFAULT = 1000;
    public static final int MIN = 0;

    /**
     * Valide que la valeur fournie respecte le plancher à {@link #MIN}.
     *
     * @param value la valeur du classement
     * @throws IllegalArgumentException si {@code value} est négative
     */
    public EloRating {
        if (value < MIN) {
            throw new IllegalArgumentException("EloRating cannot be negative: " + value);
        }
    }

    /**
     * Retourne un nouvel {@code EloRating} après application d'une variation.
     * Le résultat est plafonné à {@link #MIN} : un ELO ne peut pas descendre sous 0.
     *
     * @param delta la variation à appliquer (positive ou négative)
     * @return un nouvel {@code EloRating} avec la valeur mise à jour
     */
    public EloRating add(int delta) {
        return new EloRating(Math.max(MIN, value + delta));
    }

    /**
     * Indique si appliquer {@code delta} à cette valeur produirait un résultat plafonné
     * à {@link #MIN} (c'est-à-dire que le calcul brut aurait donné une valeur négative).
     *
     * <p>{@link #add} plafonne silencieusement — ce plafonnement est une perte
     * d'information (le classement "réel" aurait continué à baisser), invisible pour
     * quiconque ne vérifie pas explicitement. Cette méthode existe pour que l'appelant
     * (qui a accès au logging, contrairement à ce value object qui reste volontairement
     * pur - voir la Javadoc de la classe) puisse le détecter et le journaliser -
     * voir {@code EloService.updateElo}.
     *
     * @param delta la variation qui serait appliquée
     * @return {@code true} si le résultat brut (non plafonné) aurait été négatif
     */
    public boolean wouldClamp(int delta) {
        return value + delta < MIN;
    }

    /**
     * Retourne un {@code EloRating} initialisé à la valeur par défaut ({@link #DEFAULT}).
     *
     * @return un {@code EloRating} à {@code 1000}
     */
    public static EloRating defaultRating() {
        return new EloRating(DEFAULT);
    }
}
