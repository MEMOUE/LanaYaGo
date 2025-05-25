package com.lanayago.enums;

public enum StatutDocument {
	EN_ATTENTE("En attente", "Document soumis, en attente de validation"),
	VALIDE("Validé", "Document validé par l'administration"),
	REFUSE("Refusé", "Document refusé"),
	EXPIRE("Expiré", "Document expiré");

	private final String libelle;
	private final String description;

	StatutDocument(String libelle, String description) {
		this.libelle = libelle;
		this.description = description;
	}

	public String getLibelle() { return libelle; }
	public String getDescription() { return description; }
}