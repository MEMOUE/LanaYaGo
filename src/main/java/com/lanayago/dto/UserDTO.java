package com.lanayago.dto;

import com.lanayago.enums.TypeUtilisateur;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UserDTO {
	private Long id;
	private String nom;
	private String prenom;
	private String email;
	private String telephone;
	private TypeUtilisateur typeUtilisateur;
	private Boolean actif;
	private LocalDateTime dateCreation;
	private String photoUrl;

	// Champs spécifiques selon le type
	private String adresse;
	private String ville;
	private String codePostal;
	private BigDecimal noteMoyenne;
	private Integer nombreCommandes;
	private String numeroPermis;
	private Boolean disponible;
	private String nomEntreprise;
	private String numeroSiret;

	// ✅ NOUVEAUX CHAMPS POUR PROPRIETAIRE_VEHICULE
	private String proprietairePhotoUrl; // URL de la photo du propriétaire
	private String carteIdentiteUrl; // URL de la carte d'identité
}