package com.lanayago.service.photo.impl;

import com.lanayago.entity.ProprietaireVehicule;
import com.lanayago.entity.User;
import com.lanayago.exception.BusinessException;
import com.lanayago.repository.UserRepository;
import com.lanayago.service.FileStorageService;
import com.lanayago.service.photo.AbstractPhotoService;
import org.springframework.stereotype.Service;

/**
 * Implémentation spécifique pour la gestion des photos des propriétaires de véhicules
 */
@Service("proprietairePhotoService")
public class ProprietairePhotoServiceImpl extends AbstractPhotoService {

	public ProprietairePhotoServiceImpl(UserRepository userRepository, FileStorageService fileStorageService) {
		super(userRepository, fileStorageService);
	}

	@Override
	protected User getUser(Long userId) {
		return userRepository.findById(userId)
				.filter(user -> user instanceof ProprietaireVehicule)
				.orElseThrow(() -> new BusinessException("Propriétaire non trouvé"));
	}

	@Override
	protected String getCurrentPhotoUrl(User user) {
		return ((ProprietaireVehicule) user).getPhotoUrl();
	}

	@Override
	protected String getCurrentCarteIdentiteUrl(User user) {
		return ((ProprietaireVehicule) user).getCarteIdentiteUrl();
	}

	@Override
	protected void setPhotoUrl(User user, String photoUrl) {
		((ProprietaireVehicule) user).setPhotoUrl(photoUrl);
	}

	@Override
	protected void setCarteIdentiteUrl(User user, String carteUrl) {
		((ProprietaireVehicule) user).setCarteIdentiteUrl(carteUrl);
	}

	@Override
	public String getUserType() {
		return "PROPRIETAIRE";
	}
}