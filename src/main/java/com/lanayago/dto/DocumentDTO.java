package com.lanayago.dto;

import com.lanayago.enums.StatutDocument;
import com.lanayago.enums.TypeDocument;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class DocumentDTO {

	@Data
	public static class UploadRequest {
		@NotNull(message = "Le type de document est obligatoire")
		private TypeDocument typeDocument;

		@NotBlank(message = "Le nom du fichier est obligatoire")
		private String nom;

		private LocalDateTime dateExpiration;
		private Boolean obligatoire = false;
	}

	@Data
	public static class Response {
		private Long id;
		private String nom;
		private TypeDocument typeDocument;
		private StatutDocument statut;
		private UserDTO utilisateur;
		private String commentaireValidation;
		private UserDTO validateur;
		private LocalDateTime dateCreation;
		private LocalDateTime dateValidation;
		private LocalDateTime dateExpiration;
		private Long tailleFichier;
		private String typeContenu;
		private Boolean obligatoire;
		private String urlTelecharger;
	}

	@Data
	public static class ValidationRequest {
		@NotNull(message = "Le statut est obligatoire")
		private StatutDocument statut;

		private String commentaire;
	}
}