package com.lanayago.enums;

public enum StatutCommande {
	EN_ATTENTE("En attente", "Commande créée, en attente d'acceptation"),
	ACCEPTEE("Acceptée", "Acceptée par un chauffeur"),
	EN_COURS("En cours", "Chauffeur en route vers le point de ramassage"),
	RAMASSAGE("Ramassage", "Marchandise récupérée"),
	EN_LIVRAISON("En livraison", "En cours de transport"),
	LIVREE("Livrée", "Livraison terminée"),
	ANNULEE("Annulée", "Commande annulée"),
	REFUSEE("Refusée", "Refusée par le chauffeur");

	private final String libelle;
	private final String description;

	StatutCommande(String libelle, String description) {
		this.libelle = libelle;
		this.description = description;
	}

	public String getLibelle() { return libelle; }
	public String getDescription() { return description; }
}