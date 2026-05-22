package com.tournament.tournament_manager.domain.port.out.registration;

import com.tournament.tournament_manager.domain.model.entities.Registration;

/**
 * Port sortant : sauvegarde d'une inscription en persistance.
 */
public interface SaveRegistrationPort {

    /**
     * Persiste une inscription et retourne l'entité sauvegardée.
     *
     * @param registration l'inscription à sauvegarder
     * @return l'inscription sauvegardée
     */
    Registration saveRegistration(Registration registration);
}
