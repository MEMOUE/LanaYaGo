package com.lanayago.dto;

import com.lanayago.enums.TypeVehicule;
import lombok.Data;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class VehiculeDTO {

	@Data
	public static class CreateRequest {
		@NotBlank(message = "L'immatriculation est obligatoire")
		private String immatriculation;

		@NotBlank(message = "La marque est obligatoire")
		private String marque;

		@NotBlank(message = "Le modèle est obligatoire")
		private String modele;

		@NotNull(message = "L'année est obligatoire")
		@Min(value = 1990, message = "L'année doit être supérieure à 1990")
		@Max(value = 2030, message = "L'année ne peut pas être dans le futur")
		private Integer annee;

		@NotNull(message = "La capacité de poids est obligatoire")
		@DecimalMin(value = "0.1", message = "La capacité doit être supérieure à 0")
		private BigDecimal capacitePoids;

		@NotNull(message = "La capacité de volume est obligatoire")
		@DecimalMin(value = "0.1", message = "La capacité doit être supérieure à 0")
		private BigDecimal capaciteVolume;

		@NotNull(message = "Le type de véhicule est obligatoire")
		private TypeVehicule typeVehicule;

		private String numeroAssurance;
		private String numeroCarteGrise;
	}

	@Data
	public static class Response {
		private Long id;
		private String immatriculation;
		private String marque;
		private String modele;
		private Integer annee;
		private BigDecimal capacitePoids;
		private BigDecimal capaciteVolume;
		private TypeVehicule typeVehicule;
		private Boolean disponible;
		private Double latitudeActuelle;
		private Double longitudeActuelle;
		private LocalDateTime dateCreation;
		private String photoUrl;
		private UserDTO proprietaire;
	}
}