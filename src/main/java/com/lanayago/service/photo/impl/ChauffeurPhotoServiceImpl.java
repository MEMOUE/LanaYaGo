package com.lanayago.service.photo.impl;

import com.lanayago.entity.Chauffeur;
import com.lanayago.entity.User;
import com.lanayago.exception.BusinessException;
import com.lanayago.repository.UserRepository;
import com.lanayago.service.FileStorageService;
import com.lanayago.service.photo.AbstractPhotoService;
import org.springframework.stereotype.Service;

/**
 * Implémentation spécifique pour la gestion des photos des chauffeurs
 */
@Service("chauffeurPhotoService")
public class ChauffeurPhotoServiceImpl extends AbstractPhotoService {

	public ChauffeurPhotoServiceImpl(UserRepository userRepository, FileStorageService fileStorageService) {
		super(userRepository, fileStorageService);
	}

	@Override
	protected User getUser(Long userId) {
		return userRepository.findById(userId)
				.filter(user -> user instanceof Chauffeur)
				.orElseThrow(() -> new BusinessException("Chauffeur non trouvé"));
	}

	@Override
	protected String getCurrentPhotoUrl(User user) {
		return ((Chauffeur) user).getPhotoUrl();
	}

	@Override
	protected String getCurrentCarteIdentiteUrl(User user) {
		return ((Chauffeur) user).getCarteIdentiteUrl();
	}

	@Override
	protected void setPhotoUrl(User user, String photoUrl) {
		((Chauffeur) user).setPhotoUrl(photoUrl);
	}

	@Override
	protected void setCarteIdentiteUrl(User user, String carteUrl) {
		((Chauffeur) user).setCarteIdentiteUrl(carteUrl);
	}

	@Override
	public String getUserType() {
		return "CHAUFFEUR";
	}
}