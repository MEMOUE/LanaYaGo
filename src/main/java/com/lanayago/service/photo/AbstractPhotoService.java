package com.lanayago.service.photo;

import com.lanayago.entity.User;
import com.lanayago.exception.BusinessException;
import com.lanayago.repository.UserRepository;
import com.lanayago.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service abstrait contenant la logique commune pour la gestion des photos
 */
@RequiredArgsConstructor
@Slf4j
public abstract class AbstractPhotoService implements IPhotoService {

	protected final UserRepository userRepository;
	protected final FileStorageService fileStorageService;

	@Override
	@Transactional
	public String uploadPhoto(Long userId, MultipartFile file) {
		log.info("Upload de photo pour {} ID: {}", getUserType(), userId);

		User user = getUser(userId);

		// Validation du fichier image
		if (!fileStorageService.isImageFile(file)) {
			throw new BusinessException("Le fichier doit être une image (JPEG, PNG, GIF)");
		}

		// Suppression de l'ancienne photo si elle existe
		String currentPhotoUrl = getCurrentPhotoUrl(user);
		if (currentPhotoUrl != null && !currentPhotoUrl.isEmpty()) {
			fileStorageService.deleteFile(currentPhotoUrl);
		}

		// Stockage de la nouvelle photo
		String photoPath = fileStorageService.storeFile(file, getUserStoragePath(userId) + "/photos");

		// Mise à jour de l'entité
		setPhotoUrl(user, photoPath);
		userRepository.save(user);

		log.info("Photo uploadée avec succès pour {} {}: {}", getUserType(), userId, photoPath);
		return fileStorageService.getFileUrl(photoPath);
	}

	@Override
	@Transactional
	public String uploadCarteIdentite(Long userId, MultipartFile file) {
		log.info("Upload de carte d'identité pour {} ID: {}", getUserType(), userId);

		User user = getUser(userId);

		// Validation du fichier (image ou PDF acceptés)
		if (!fileStorageService.isImageFile(file) && !fileStorageService.isPdfFile(file)) {
			throw new BusinessException("Le fichier doit être une image (JPEG, PNG) ou un PDF");
		}

		// Suppression de l'ancienne carte d'identité si elle existe
		String currentCarteUrl = getCurrentCarteIdentiteUrl(user);
		if (currentCarteUrl != null && !currentCarteUrl.isEmpty()) {
			fileStorageService.deleteFile(currentCarteUrl);
		}

		// Stockage de la nouvelle carte d'identité
		String cartePath = fileStorageService.storeFile(file, getUserStoragePath(userId) + "/documents");

		// Mise à jour de l'entité
		setCarteIdentiteUrl(user, cartePath);
		userRepository.save(user);

		log.info("Carte d'identité uploadée avec succès pour {} {}: {}", getUserType(), userId, cartePath);
		return fileStorageService.getFileUrl(cartePath);
	}

	@Override
	@Transactional(readOnly = true)
	public String getPhotoUrl(Long userId) {
		User user = getUser(userId);
		String photoUrl = getCurrentPhotoUrl(user);
		return photoUrl != null ? fileStorageService.getFileUrl(photoUrl) : null;
	}

	@Override
	@Transactional(readOnly = true)
	public String getCarteIdentiteUrl(Long userId) {
		User user = getUser(userId);
		String carteUrl = getCurrentCarteIdentiteUrl(user);
		return carteUrl != null ? fileStorageService.getFileUrl(carteUrl) : null;
	}

	@Override
	@Transactional
	public void supprimerPhoto(Long userId) {
		log.info("Suppression de photo pour {} ID: {}", getUserType(), userId);

		User user = getUser(userId);
		String currentPhotoUrl = getCurrentPhotoUrl(user);

		if (currentPhotoUrl != null && !currentPhotoUrl.isEmpty()) {
			fileStorageService.deleteFile(currentPhotoUrl);
			setPhotoUrl(user, null);
			userRepository.save(user);
			log.info("Photo supprimée avec succès pour {} {}", getUserType(), userId);
		}
	}

	@Override
	@Transactional
	public void supprimerCarteIdentite(Long userId) {
		log.info("Suppression de carte d'identité pour {} ID: {}", getUserType(), userId);

		User user = getUser(userId);
		String currentCarteUrl = getCurrentCarteIdentiteUrl(user);

		if (currentCarteUrl != null && !currentCarteUrl.isEmpty()) {
			fileStorageService.deleteFile(currentCarteUrl);
			setCarteIdentiteUrl(user, null);
			userRepository.save(user);
			log.info("Carte d'identité supprimée avec succès pour {} {}", getUserType(), userId);
		}
	}

	// ================ MÉTHODES ABSTRAITES À IMPLÉMENTER ================

	/**
	 * Récupérer l'utilisateur et valider son type
	 */
	protected abstract User getUser(Long userId);

	/**
	 * Récupérer l'URL actuelle de la photo
	 */
	protected abstract String getCurrentPhotoUrl(User user);

	/**
	 * Récupérer l'URL actuelle de la carte d'identité
	 */
	protected abstract String getCurrentCarteIdentiteUrl(User user);

	/**
	 * Définir l'URL de la photo
	 */
	protected abstract void setPhotoUrl(User user, String photoUrl);

	/**
	 * Définir l'URL de la carte d'identité
	 */
	protected abstract void setCarteIdentiteUrl(User user, String carteUrl);

	/**
	 * Récupérer le chemin de stockage pour ce type d'utilisateur
	 */
	protected String getUserStoragePath(Long userId) {
		return getUserType().toLowerCase() + "/" + userId;
	}
}