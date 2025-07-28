package com.lanayago.service.photo;

import com.lanayago.enums.TypeUtilisateur;
import com.lanayago.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Factory pour récupérer le service de photos approprié selon le type d'utilisateur
 */
@Service
@RequiredArgsConstructor
public class PhotoServiceFactory {

	private final Map<String, IPhotoService> photoServices;

	/**
	 * Récupérer le service approprié selon le type d'utilisateur
	 */
	public IPhotoService getPhotoService(TypeUtilisateur typeUtilisateur) {
		return switch (typeUtilisateur) {
			case PROPRIETAIRE_VEHICULE -> getServiceByName("proprietairePhotoService");
			case CHAUFFEUR -> getServiceByName("chauffeurPhotoService");
			case CLIENT -> throw new BusinessException("La gestion des photos n'est pas encore disponible pour les clients");
		};
	}

	/**
	 * Récupérer le service par nom de bean Spring
	 */
	public IPhotoService getPhotoService(String serviceName) {
		return getServiceByName(serviceName);
	}

	private IPhotoService getServiceByName(String serviceName) {
		IPhotoService service = photoServices.get(serviceName);
		if (service == null) {
			throw new BusinessException("Service de photos non trouvé: " + serviceName);
		}
		return service;
	}

	/**
	 * Récupérer tous les services disponibles
	 */
	public Map<String, IPhotoService> getAllServices() {
		return photoServices;
	}
}