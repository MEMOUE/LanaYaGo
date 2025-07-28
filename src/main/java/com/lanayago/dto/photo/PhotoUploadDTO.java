package com.lanayago.dto.photo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO commun pour les réponses d'upload de photos
 */
public class PhotoUploadDTO {

	/**
	 * Réponse d'upload de photo
	 */
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Response {
		private String message;
		private String photoUrl;
		private String type; // "photo" ou "carte_identite"
		private String userType; // "PROPRIETAIRE" ou "CHAUFFEUR"
		private Long userId;
	}

	/**
	 * Requête d'upload avec métadonnées optionnelles
	 */
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class UploadRequest {
		private String description;
		private Boolean remplacerExistant = true;
	}

	/**
	 * Réponse avec informations détaillées
	 */
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class DetailedResponse {
		private String message;
		private String photoUrl;
		private String carteIdentiteUrl;
		private String type;
		private String userType;
		private Long userId;
		private Long fileSize;
		private String fileName;
		private String contentType;
	}
}