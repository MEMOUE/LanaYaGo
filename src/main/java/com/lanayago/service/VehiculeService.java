package com.lanayago.service;

import com.lanayago.dto.VehiculeDTO;
import com.lanayago.entity.ProprietaireVehicule;
import com.lanayago.entity.Vehicule;
import com.lanayago.exception.BusinessException;
import com.lanayago.repository.UserRepository;
import com.lanayago.repository.VehiculeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class VehiculeService {

	private final VehiculeRepository vehiculeRepository;
	private final UserRepository userRepository;
	private final UserMapperService userMapperService; // ✅ INJECTION AJOUTÉE

	@Transactional
	public VehiculeDTO.Response ajouterVehicule(Long proprietaireId, VehiculeDTO.CreateRequest request) {
		log.info("Ajout d'un nouveau véhicule pour le propriétaire: {}", proprietaireId);

		ProprietaireVehicule proprietaire = (ProprietaireVehicule) userRepository.findById(proprietaireId)
				.orElseThrow(() -> new BusinessException("Propriétaire non trouvé"));

		if (vehiculeRepository.existsByImmatriculation(request.getImmatriculation())) {
			throw new BusinessException("Un véhicule avec cette immatriculation existe déjà");
		}

		Vehicule vehicule = new Vehicule();
		vehicule.setImmatriculation(request.getImmatriculation());
		vehicule.setMarque(request.getMarque());
		vehicule.setModele(request.getModele());
		vehicule.setAnnee(request.getAnnee());
		vehicule.setCapacitePoids(request.getCapacitePoids());
		vehicule.setCapaciteVolume(request.getCapaciteVolume());
		vehicule.setTypeVehicule(request.getTypeVehicule());
		vehicule.setNumeroAssurance(request.getNumeroAssurance());
		vehicule.setNumeroCarteGrise(request.getNumeroCarteGrise());
		vehicule.setProprietaire(proprietaire);

		vehicule = vehiculeRepository.save(vehicule);
		log.info("Véhicule créé avec succès: {}", vehicule.getId());

		return mapToResponse(vehicule);
	}

	@Transactional(readOnly = true)
	public List<VehiculeDTO.Response> getVehiculesProprietaire(Long proprietaireId) {
		return vehiculeRepository.findByProprietaireId(proprietaireId)
				.stream()
				.map(this::mapToResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<VehiculeDTO.Response> getVehiculesDisponibles() {
		return vehiculeRepository.findVehiculesDisponibles()
				.stream()
				.map(this::mapToResponse)
				.toList();
	}

	@Transactional
	public void changerDisponibilite(Long vehiculeId, Boolean disponible) {
		Vehicule vehicule = vehiculeRepository.findById(vehiculeId)
				.orElseThrow(() -> new BusinessException("Véhicule non trouvé"));

		vehicule.setDisponible(disponible);
		vehiculeRepository.save(vehicule);

		log.info("Disponibilité du véhicule {} changée vers: {}", vehiculeId, disponible);
	}

	private VehiculeDTO.Response mapToResponse(Vehicule vehicule) {
		VehiculeDTO.Response response = new VehiculeDTO.Response();
		response.setId(vehicule.getId());
		response.setImmatriculation(vehicule.getImmatriculation());
		response.setMarque(vehicule.getMarque());
		response.setModele(vehicule.getModele());
		response.setAnnee(vehicule.getAnnee());
		response.setCapacitePoids(vehicule.getCapacitePoids());
		response.setCapaciteVolume(vehicule.getCapaciteVolume());
		response.setTypeVehicule(vehicule.getTypeVehicule());
		response.setDisponible(vehicule.getDisponible());
		response.setLatitudeActuelle(vehicule.getLatitudeActuelle());
		response.setLongitudeActuelle(vehicule.getLongitudeActuelle());
		response.setDateCreation(vehicule.getDateCreation());
		response.setPhotoUrl(vehicule.getPhotoUrl());

		if (vehicule.getProprietaire() != null) {
			// ✅ CORRECTION: Utilisation du service injecté
			response.setProprietaire(userMapperService.toDTO(vehicule.getProprietaire()));
		}

		return response;
	}
}