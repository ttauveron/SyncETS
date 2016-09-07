package com.gnut3ll4.syncets.model;

import com.gnut3ll4.signetswebserivces.model.Seances;

public class SeancesWrapper extends Seances {
    public String id;

    public SeancesWrapper(Seances seances) {
        this.coursGroupe = seances.coursGroupe;
        this.dateDebut = seances.dateDebut;
        this.dateFin = seances.dateFin;
        this.descriptionActivite = seances.descriptionActivite;
        this.libelleCours = seances.libelleCours;
        this.local = seances.local;
        this.nomActivite = seances.nomActivite;

        this.id = seances.coursGroupe +
                seances.dateDebut +
                seances.dateFin +
                seances.local;
    }
}
