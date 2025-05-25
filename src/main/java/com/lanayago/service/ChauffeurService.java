package com.lanayago.service;

import com.lanayago.dto.CommandeDTO;
import com.lanayago.dto.UserDTO;
import com.lanayago.entity.Chauffeur;
import com.lanayago.exception.BusinessException;
import com.lanayago.repository.ChauffeurRepository;
import com.lanayago.repository.CommandeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChauffeurService {

	private final ChauffeurRepository chauffeurRepository;
	private final CommandeRepository commandeRepository;
	private final UserMapperService userMapperService;

	@Transactional(readOnly = true)
	public UserDTO getProfilChauffeur(Long chauffeurId) {
		Chauffeur chauffeur = chauffeurRepository.findById(chauffeurId)
				.orElseThrow(() -> new BusinessException("Chauffeur non trouvé"));
		return userMapperService.toDTO(chauffeur);
	}

	@Transactional
	public UserDTO mettreAJourProfil(Long chauffeurId, UserDTO request) {
		Chauffeur chauffeur = chauffeurRepository.findById(chauffeurId)
				.orElseThrow(() -> new BusinessException("Chauffeur non trouvé"));

		chauffeur.setNom(request.getNom());
		chauffeur.setPrenom(request.getPrenom());
		chauffeur.setTelephone(request.getTelephone());

		chauffeur = chauffeurRepository.save(chauffeur);
		log.info("Profil chauffeur {} mis à jour", chauffeurId);

		return userMapperService.toDTO(chauffeur);
	}

	@Transactional
	public void changerDisponibilite(Long chauffeurId, Boolean disponible) {
		Chauffeur chauffeur = chauffeurRepository.findById(chauffeurId)
				.orElseThrow(() -> new BusinessException("Chauffeur non trouvé"));

		chauffeur.setDisponible(disponible);
		chauffeurRepository.save(chauffeur);

		log.info("Disponibilité du chauffeur {} changée vers: {}", chauffeurId, disponible);
	}

	@Transactional(readOnly = true)
	public List<CommandeDTO.Response> getHistoriqueCourses(Long chauffeurId, int page, int size) {
		return commandeRepository.findByChauffeurIdOrderByDateCreationDesc(chauffeurId)
				.stream()
				.skip((long) page * size)
				.limit(size)
				.map(commande -> {
					CommandeDTO.Response response = new CommandeDTO.Response();
					response.setId(commande.getId());
					response.setNumeroCommande(commande.getNumeroCommande());
					response.setStatut(commande.getStatut());
					response.setDateCreation(commande.getDateCreation());
					response.setTarifFinal(commande.getTarifFinal());
					response.setDistance(commande.getDistance());
					return response;
				})
				.toList();
	}

	@Transactional(readOnly = true)
	public Map<String, Object> getRevenus(Long chauffeurId, String periode) {
		BigDecimal chiffreAffaire = BigDecimal.valueOf(commandeRepository.getChiffreAffaireChauffeur(chauffeurId));
		BigDecimal commission = chiffreAffaire.multiply(BigDecimal.valueOf(0.1)); // 10% de commission
		BigDecimal revenus = chiffreAffaire.subtract(commission);

		Map<String, Object> result = new HashMap<>();
		result.put("chiffreAffaire", chiffreAffaire);
		result.put("commission", commission);
		result.put("revenus", revenus);
		result.put("nombreCourses", commandeRepository.findByChauffeurIdOrderByDateCreationDesc(chauffeurId).size());

		return result;
	}

	@Transactional(readOnly = true)
	public Map<String, Object> getStatistiquesChauffeur(Long chauffeurId) {
		Chauffeur chauffeur = chauffeurRepository.findById(chauffeurId)
				.orElseThrow(() -> new BusinessException("Chauffeur non trouvé"));

		Map<String, Object> stats = new HashMap<>();
		stats.put("nombreCourses", chauffeur.getNombreCourses());
		stats.put("noteMoyenne", chauffeur.getNoteMoyenne());
		stats.put("disponible", chauffeur.getDisponible());
		stats.put("revenusTotal", getRevenus(chauffeurId, null).get("revenus"));

		return stats;
	}

	@Transactional
	public void signalerProbleme(Long chauffeurId, Map<String, String> signalement) {
		// TODO: Implémenter la création d'un ticket de support
		log.info("Problème signalé par le chauffeur {}: {}", chauffeurId, signalement.get("description"));
	}
}