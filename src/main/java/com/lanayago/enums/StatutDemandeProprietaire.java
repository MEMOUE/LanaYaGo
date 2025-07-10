package com.lanayago.enums;

public enum StatutDemandeProprietaire {
	EN_ATTENTE("En attente", "Demande soumise, en attente de validation"),
	APPROUVEE("Approuvée", "Demande approuvée, utilisateur promu propriétaire"),
	REJETEE("Rejetée", "Demande rejetée"),
	DOCUMENTS_MANQUANTS("Documents manquants", "Documents requis manquants ou invalides"),
	EN_REVISION("En révision", "Demande en cours d'examen par l'administration");

	private final String libelle;
	private final String description;

	StatutDemandeProprietaire(String libelle, String description) {
		this.libelle = libelle;
		this.description = description;
	}

	public String getLibelle() { return libelle; }
	public String getDescription() { return description; }
}