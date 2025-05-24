package com.lanayago.enums;

public enum TypeUtilisateur {
	CLIENT("Client"),
	CHAUFFEUR("Chauffeur"),
	PROPRIETAIRE_VEHICULE("Propriétaire de véhicule");

	private final String libelle;

	TypeUtilisateur(String libelle) {
		this.libelle = libelle;
	}

	public String getLibelle() {
		return libelle;
	}
}