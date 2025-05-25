package com.lanayago.enums;

public enum TypeDocument {
	PERMIS_CONDUIRE("Permis de conduire"),
	CARTE_IDENTITE("Carte d'identité"),
	CARTE_GRISE("Carte grise"),
	ASSURANCE_VEHICULE("Assurance véhicule"),
	CONTROLE_TECHNIQUE("Contrôle technique"),
	KBIS("Extrait K-bis"),
	JUSTIFICATIF_DOMICILE("Justificatif de domicile"),
	PHOTO_PROFIL("Photo de profil"),
	PHOTO_VEHICULE("Photo du véhicule"),
	AUTRES("Autres documents");

	private final String libelle;

	TypeDocument(String libelle) {
		this.libelle = libelle;
	}

	public String getLibelle() {
		return libelle;
	}
}