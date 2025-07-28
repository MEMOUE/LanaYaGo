package com.lanayago.service.photo;

import org.springframework.web.multipart.MultipartFile;

/**
 * Interface commune pour la gestion des photos des utilisateurs
 */
public interface IPhotoService {

	/**
	 * Upload de la photo personnelle
	 */
	String uploadPhoto(Long userId, MultipartFile file);

	/**
	 * Upload de la carte d'identité
	 */
	String uploadCarteIdentite(Long userId, MultipartFile file);

	/**
	 * Récupérer l'URL de la photo personnelle
	 */
	String getPhotoUrl(Long userId);

	/**
	 * Récupérer l'URL de la carte d'identité
	 */
	String getCarteIdentiteUrl(Long userId);

	/**
	 * Supprimer la photo personnelle
	 */
	void supprimerPhoto(Long userId);

	/**
	 * Supprimer la carte d'identité
	 */
	void supprimerCarteIdentite(Long userId);

	/**
	 * Récupérer le type d'utilisateur géré par ce service
	 */
	String getUserType();
}