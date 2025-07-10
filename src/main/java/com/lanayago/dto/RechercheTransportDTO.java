package com.lanayago.dto;

import com.lanayago.enums.TypeVehicule;
import lombok.Data;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class RechercheTransportDTO {

	@Data
	public static class RechercheRequest {
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

		// Rayon de recherche (en km)
		@DecimalMin(value = "1.0", message = "Le rayon minimum est de 1 km")
		@DecimalMax(value = "100.0", message = "Le rayon maximum est de 100 km")
		private Double rayonRecherche = 50.0;
	}

	@Data
	public static class RechercheResponse {
		private Long rechercheId;
		private TypeVehicule typeVehiculeRecommande;
		private Double distance;
		private BigDecimal tarifEstime;
		private List<VehiculeDisponible> vehiculesDisponibles;
		private String sessionId; // Pour le suivi temps réel
	}

	@Data
	public static class VehiculeDisponible {
		private Long vehiculeId;
		private Long chauffeurId;
		private VehiculeDTO.Response vehicule;
		private UserDTO chauffeur;

		// Position actuelle du véhicule/chauffeur
		private Double latitudeActuelle;
		private Double longitudeActuelle;

		// Distance depuis le point de départ
		private Double distanceDepuisDepart;

		// Temps estimé pour arriver au point de départ
		private Integer tempsEstimeArrivee; // en minutes

		// Note du chauffeur
		private BigDecimal noteChauffeur;
		private Integer nombreCourses;

		// Disponibilité immédiate
		private Boolean disponibleImmediatement;
	}

	@Data
	public static class DemandeTransportRequest {
		@NotNull(message = "L'ID de recherche est obligatoire")
		private Long rechercheId;

		@NotNull(message = "L'ID du véhicule choisi est obligatoire")
		private Long vehiculeId;

		@NotNull(message = "L'ID du chauffeur choisi est obligatoire")
		private Long chauffeurId;

		// Instructions spéciales
		private String instructionsSpeciales;
	}

	@Data
	public static class SuiviTransportResponse {
		private Long commandeId;
		private String numeroCommande;
		private VehiculeDTO.Response vehicule;
		private UserDTO chauffeur;

		// Position temps réel
		private Double latitudeActuelle;
		private Double longitudeActuelle;

		// Statut et progression
		private String statut;
		private Integer progressionPourcentage; // 0-100%

		// Temps estimés
		private LocalDateTime heureArriveePrevue;
		private LocalDateTime heureLivraisonPrevue;

		// Contact chauffeur
		private String telephoneChauffeur;
		private Boolean chauffeurEnLigne;

		// Informations de suivi
		private List<EtapeSuivi> etapesSuivi;
	}

	@Data
	public static class EtapeSuivi {
		private String description;
		private LocalDateTime heureEtape;
		private Double latitude;
		private Double longitude;
		private Boolean completed;
	}
}