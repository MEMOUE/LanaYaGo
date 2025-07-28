package com.lanayago.service;

import com.lanayago.dto.UserDTO;
import com.lanayago.entity.Client;
import com.lanayago.entity.Chauffeur;
import com.lanayago.entity.ProprietaireVehicule;
import com.lanayago.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserMapperService {

	private final FileStorageService fileStorageService;

	public UserDTO toDTO(User user) {
		UserDTO dto = new UserDTO();
		dto.setId(user.getId());
		dto.setNom(user.getNom());
		dto.setPrenom(user.getPrenom());
		dto.setEmail(user.getEmail());
		dto.setTelephone(user.getTelephone());
		dto.setTypeUtilisateur(user.getTypeUtilisateur());
		dto.setActif(user.getActif());
		dto.setDateCreation(user.getDateCreation());
		dto.setPhotoUrl(user.getPhotoUrl());

		// Champs spécifiques selon le type
		if (user instanceof Client client) {
			dto.setAdresse(client.getAdresse());
			dto.setVille(client.getVille());
			dto.setCodePostal(client.getCodePostal());
			dto.setNoteMoyenne(client.getNoteMoyenne());
			dto.setNombreCommandes(client.getNombreCommandes());
		} else if (user instanceof Chauffeur chauffeur) {
			dto.setNumeroPermis(chauffeur.getNumeroPermis());
			dto.setNoteMoyenne(chauffeur.getNoteMoyenne());
			dto.setDisponible(chauffeur.getDisponible());
		} else if (user instanceof ProprietaireVehicule proprietaire) {
			dto.setNomEntreprise(proprietaire.getNomEntreprise());
			dto.setNumeroSiret(proprietaire.getNumeroSiret());

			// ✅ NOUVEAUX CHAMPS MAPPÉS
			// Convertir les chemins relatifs en URLs publiques
			if (proprietaire.getPhotoUrl() != null && !proprietaire.getPhotoUrl().isEmpty()) {
				dto.setProprietairePhotoUrl(fileStorageService.getFileUrl(proprietaire.getPhotoUrl()));
			}

			if (proprietaire.getCarteIdentiteUrl() != null && !proprietaire.getCarteIdentiteUrl().isEmpty()) {
				dto.setCarteIdentiteUrl(fileStorageService.getFileUrl(proprietaire.getCarteIdentiteUrl()));
			}
		}

		return dto;
	}
}