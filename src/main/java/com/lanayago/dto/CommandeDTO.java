package com.lanayago.dto;

import com.lanayago.enums.StatutCommande;
import lombok.Data;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CommandeDTO {

	@Data
	public static class CreateRequest {
		@NotNull(message = "La latitude de départ est obligatoire")
		@DecimalMin(value = "-90.0", message = "Latitude invalide")
		@DecimalMax(value = "90.0", message = "Latitude invalide")
		private Double latitudeDepart;

		@NotNull(message = "La longitude de départ est obligatoire")
		@DecimalMin(value = "-180.0", message = "Longitude invalide")
		@DecimalMax(value = "180.0", message = "Longitude invalide")
		private Double longitudeDepart;

		@NotBlank(message = "L'adresse de départ est obligatoire")
		private String adresseDepart;

		@NotNull(message = "La latitude d'arrivée est obligatoire")
		private Double latitudeArrivee;

		@NotNull(message = "La longitude d'arrivée est obligatoire")
		private Double longitudeArrivee;

		@NotBlank(message = "L'adresse d'arrivée est obligatoire")
		private String adresseArrivee;

		@NotNull(message = "Le poids de la marchandise est obligatoire")
		@DecimalMin(value = "0.1", message = "Le poids doit être supérieur à 0")
		private BigDecimal poidsMarchandise;

		private BigDecimal volumeMarchandise;
		private String descriptionMarchandise;
		private Boolean urgent = false;
		private LocalDateTime dateRamassageSouhaitee;
		private LocalDateTime dateLivraisonSouhaitee;
	}

	@Data
	public static class Response {
		private Long id;
		private String numeroCommande;
		private UserDTO client;
		private UserDTO chauffeur;

		// ✅ CORRECTION: Type corrigé
		private VehiculeDTO.Response vehicule;

		private Double latitudeDepart;
		private Double longitudeDepart;
		private String adresseDepart;
		private Double latitudeArrivee;
		private Double longitudeArrivee;
		private String adresseArrivee;
		private BigDecimal poidsMarchandise;
		private BigDecimal volumeMarchandise;
		private String descriptionMarchandise;
		private Boolean urgent;
		private LocalDateTime dateCreation;
		private LocalDateTime dateRamassageSouhaitee;
		private LocalDateTime dateRamassageEffective;
		private LocalDateTime dateLivraisonSouhaitee;
		private LocalDateTime dateLivraisonEffective;
		private Double distance;
		private BigDecimal tarifCalcule;
		private BigDecimal tarifFinal;
		private StatutCommande statut;
		private BigDecimal noteClient;
		private BigDecimal noteChauffeur;
		private String commentaireClient;
		private String commentaireChauffeur;
	}

	@Data
	public static class EvaluationRequest {
		@NotNull(message = "La note est obligatoire")
		@DecimalMin(value = "1.0", message = "La note minimum est 1")
		@DecimalMax(value = "5.0", message = "La note maximum est 5")
		private BigDecimal note;

		@Size(max = 500, message = "Le commentaire ne peut pas dépasser 500 caractères")
		private String commentaire;

		@NotBlank(message = "Le type d'évaluateur est obligatoire")
		@Pattern(regexp = "CLIENT|CHAUFFEUR", message = "Type d'évaluateur invalide")
		private String typeEvaluateur;
	}
}