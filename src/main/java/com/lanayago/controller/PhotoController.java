package com.lanayago.controller;

import com.lanayago.dto.photo.PhotoUploadDTO;
import com.lanayago.entity.User;
import com.lanayago.enums.TypeUtilisateur;
import com.lanayago.exception.BusinessException;
import com.lanayago.repository.UserRepository;
import com.lanayago.service.photo.IPhotoService;
import com.lanayago.service.photo.PhotoServiceFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Contrôleur unifié pour la gestion des photos de tous les types d'utilisateurs
 */
@RestController
@RequestMapping("/api/photos")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "📸 Gestion Photos", description = "Gestion unifiée des photos et documents pour tous les utilisateurs")
@Slf4j
public class PhotoController {

	private final PhotoServiceFactory photoServiceFactory;
	private final UserRepository userRepository;

	@PostMapping("/{userType}/{userId}/photo")
	@Operation(
			summary = "Uploader une photo personnelle",
			description = "Upload la photo personnelle d'un utilisateur. Types supportés: proprietaire, chauffeur"
	)
	@PreAuthorize("hasRole('PROPRIETAIRE_VEHICULE') or hasRole('CHAUFFEUR') or hasRole('CLIENT')")
	public ResponseEntity<PhotoUploadDTO.Response> uploadPhoto(
			@PathVariable String userType,
			@PathVariable Long userId,
			@RequestParam("file") MultipartFile file) {

		// Validation des permissions
		validateUserAccess(userType, userId);

		// Récupération du service approprié
		IPhotoService photoService = getPhotoServiceByType(userType);

		// Upload de la photo
		String photoUrl = photoService.uploadPhoto(userId, file);

		PhotoUploadDTO.Response response = new PhotoUploadDTO.Response(
				"Photo uploadée avec succès",
				photoUrl,
				"photo",
				photoService.getUserType(),
				userId
		);

		return ResponseEntity.ok(response);
	}

	@PostMapping("/{userType}/{userId}/carte-identite")
	@Operation(
			summary = "Uploader une carte d'identité",
			description = "Upload la carte d'identité d'un utilisateur"
	)
	@PreAuthorize("hasRole('PROPRIETAIRE_VEHICULE') or hasRole('CHAUFFEUR')")
	public ResponseEntity<PhotoUploadDTO.Response> uploadCarteIdentite(
			@PathVariable String userType,
			@PathVariable Long userId,
			@RequestParam("file") MultipartFile file) {

		// Validation des permissions
		validateUserAccess(userType, userId);

		// Récupération du service approprié
		IPhotoService photoService = getPhotoServiceByType(userType);

		// Upload de la carte d'identité
		String carteUrl = photoService.uploadCarteIdentite(userId, file);

		PhotoUploadDTO.Response response = new PhotoUploadDTO.Response(
				"Carte d'identité uploadée avec succès",
				carteUrl,
				"carte_identite",
				photoService.getUserType(),
				userId
		);

		return ResponseEntity.ok(response);
	}

	@GetMapping("/{userType}/{userId}/photo")
	@Operation(
			summary = "Récupérer l'URL de la photo",
			description = "Retourne l'URL publique de la photo d'un utilisateur"
	)
	@PreAuthorize("hasRole('PROPRIETAIRE_VEHICULE') or hasRole('CHAUFFEUR') or hasRole('CLIENT')")
	public ResponseEntity<String> getPhotoUrl(
			@PathVariable String userType,
			@PathVariable Long userId) {

		IPhotoService photoService = getPhotoServiceByType(userType);
		String photoUrl = photoService.getPhotoUrl(userId);

		return photoUrl != null ? ResponseEntity.ok(photoUrl) : ResponseEntity.notFound().build();
	}

	@GetMapping("/{userType}/{userId}/carte-identite")
	@Operation(
			summary = "Récupérer l'URL de la carte d'identité",
			description = "Retourne l'URL publique de la carte d'identité (accès restreint)"
	)
	@PreAuthorize("hasRole('PROPRIETAIRE_VEHICULE') or hasRole('CHAUFFEUR')")
	public ResponseEntity<String> getCarteIdentiteUrl(
			@PathVariable String userType,
			@PathVariable Long userId) {

		// Validation des permissions strictes pour carte d'identité
		validateUserAccess(userType, userId);

		IPhotoService photoService = getPhotoServiceByType(userType);
		String carteUrl = photoService.getCarteIdentiteUrl(userId);

		return carteUrl != null ? ResponseEntity.ok(carteUrl) : ResponseEntity.notFound().build();
	}

	@DeleteMapping("/{userType}/{userId}/photo")
	@Operation(
			summary = "Supprimer la photo",
			description = "Supprime la photo personnelle d'un utilisateur"
	)
	@PreAuthorize("hasRole('PROPRIETAIRE_VEHICULE') or hasRole('CHAUFFEUR')")
	public ResponseEntity<Void> supprimerPhoto(
			@PathVariable String userType,
			@PathVariable Long userId) {

		validateUserAccess(userType, userId);

		IPhotoService photoService = getPhotoServiceByType(userType);
		photoService.supprimerPhoto(userId);

		return ResponseEntity.ok().build();
	}

	@DeleteMapping("/{userType}/{userId}/carte-identite")
	@Operation(
			summary = "Supprimer la carte d'identité",
			description = "Supprime la carte d'identité d'un utilisateur"
	)
	@PreAuthorize("hasRole('PROPRIETAIRE_VEHICULE') or hasRole('CHAUFFEUR')")
	public ResponseEntity<Void> supprimerCarteIdentite(
			@PathVariable String userType,
			@PathVariable Long userId) {

		validateUserAccess(userType, userId);

		IPhotoService photoService = getPhotoServiceByType(userType);
		photoService.supprimerCarteIdentite(userId);

		return ResponseEntity.ok().build();
	}

	@GetMapping("/{userType}/{userId}/info")
	@Operation(
			summary = "Récupérer toutes les informations photos",
			description = "Retourne les URLs de la photo et carte d'identité en une seule requête"
	)
	@PreAuthorize("hasRole('PROPRIETAIRE_VEHICULE') or hasRole('CHAUFFEUR')")
	public ResponseEntity<PhotoUploadDTO.DetailedResponse> getPhotoInfo(
			@PathVariable String userType,
			@PathVariable Long userId) {

		IPhotoService photoService = getPhotoServiceByType(userType);

		PhotoUploadDTO.DetailedResponse response = new PhotoUploadDTO.DetailedResponse();
		response.setUserId(userId);
		response.setUserType(photoService.getUserType());
		response.setPhotoUrl(photoService.getPhotoUrl(userId));
		response.setCarteIdentiteUrl(photoService.getCarteIdentiteUrl(userId));
		response.setMessage("Informations récupérées avec succès");

		return ResponseEntity.ok(response);
	}

	// ================== MÉTHODES PRIVÉES ==================

	private IPhotoService getPhotoServiceByType(String userType) {
		return switch (userType.toLowerCase()) {
			case "proprietaire", "proprietaires" -> photoServiceFactory.getPhotoService("proprietairePhotoService");
			case "chauffeur", "chauffeurs" -> photoServiceFactory.getPhotoService("chauffeurPhotoService");
			default -> throw new BusinessException("Type d'utilisateur non supporté: " + userType);
		};
	}

	private void validateUserAccess(String userType, Long userId) {
		// Récupérer l'utilisateur pour valider le type
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException("Utilisateur non trouvé"));

		// Valider la correspondance du type
		boolean typeMatches = switch (userType.toLowerCase()) {
			case "proprietaire", "proprietaires" -> user.getTypeUtilisateur() == TypeUtilisateur.PROPRIETAIRE_VEHICULE;
			case "chauffeur", "chauffeurs" -> user.getTypeUtilisateur() == TypeUtilisateur.CHAUFFEUR;
			default -> false;
		};

		if (!typeMatches) {
			throw new BusinessException("Type d'utilisateur incompatible");
		}
	}
}