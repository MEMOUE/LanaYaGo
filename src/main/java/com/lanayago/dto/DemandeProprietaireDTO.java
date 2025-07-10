package com.lanayago.dto;

import com.lanayago.enums.StatutDemandeProprietaire;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public class DemandeProprietaireDTO {

	@Data
	public static class CreateRequest {
		@NotBlank(message = "Le nom de l'entreprise est obligatoire")
		@Size(max = 150, message = "Le nom de l'entreprise ne peut pas dépasser 150 caractères")
		private String nomEntreprise;

		@Size(max = 14, message = "Le numéro SIRET ne peut pas dépasser 14 caractères")
		private String numeroSiret;

		@NotBlank(message = "L'adresse de l'entreprise est obligatoire")
		@Size(max = 255, message = "L'adresse ne peut pas dépasser 255 caractères")
		private String adresseEntreprise;

		@NotBlank(message = "La ville est obligatoire")
		@Size(max = 100, message = "La ville ne peut pas dépasser 100 caractères")
		private String villeEntreprise;

		@NotBlank(message = "Le code postal est obligatoire")
		@Size(max = 10, message = "Le code postal ne peut pas dépasser 10 caractères")
		private String codePostalEntreprise;
	}

	@Data
	public static class Response {
		private Long id;
		private UserDTO user;
		private String nomEntreprise;
		private String numeroSiret;
		private String adresseEntreprise;
		private String villeEntreprise;
		private String codePostalEntreprise;

		// Documents
		private String pieceIdentiteUrl;
		private String extraitUrl;
		private String justificatifAdresseUrl;

		private StatutDemandeProprietaire statut;
		private String commentaireAdmin;

		private LocalDateTime dateCreation;
		private LocalDateTime dateModification;
		private LocalDateTime dateTraitement;

		private Long adminId;
	}

	@Data
	public static class TraitementRequest {
		@NotBlank(message = "Le statut est obligatoire")
		private String statut; // APPROUVEE ou REJETEE

		private String commentaireAdmin;
	}

	@Data
	public static class DocumentUploadResponse {
		private String documentType; // piece_identite, extrait, justificatif_adresse
		private String documentUrl;
		private String message;
	}
}