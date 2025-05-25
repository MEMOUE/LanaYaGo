package com.lanayago.service;

import com.lanayago.dto.DocumentDTO;
import com.lanayago.entity.Document;
import com.lanayago.entity.User;
import com.lanayago.enums.StatutDocument;
import com.lanayago.enums.TypeUtilisateur;
import com.lanayago.exception.BusinessException;
import com.lanayago.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
public class AdminService {

	private final UserRepository userRepository;
	private final CommandeRepository commandeRepository;
	private final VehiculeRepository vehiculeRepository;
	private final DocumentRepository documentRepository;
	private final UserMapperService userMapperService;

	@Transactional(readOnly = true)
	public Map<String, Object> getStatistiquesGlobales() {
		Map<String, Object> stats = new HashMap<>();

		// Statistiques utilisateurs
		stats.put("totalUtilisateurs", userRepository.count());
		stats.put("totalClients", userRepository.countByTypeUtilisateurAndActifTrue(TypeUtilisateur.CLIENT));
		stats.put("totalChauffeurs", userRepository.countByTypeUtilisateurAndActifTrue(TypeUtilisateur.CHAUFFEUR));
		stats.put("totalProprietaires", userRepository.countByTypeUtilisateurAndActifTrue(TypeUtilisateur.PROPRIETAIRE_VEHICULE));

		// Statistiques commandes
		stats.put("totalCommandes", commandeRepository.count());
		stats.put("commandesEnAttente", commandeRepository.countByStatut(com.lanayago.enums.StatutCommande.EN_ATTENTE));
		stats.put("commandesLivrees", commandeRepository.countByStatut(com.lanayago.enums.StatutCommande.LIVREE));

		// Statistiques véhicules
		stats.put("totalVehicules", vehiculeRepository.count());
		stats.put("vehiculesDisponibles", vehiculeRepository.findVehiculesDisponibles().size());

		// Statistiques documents
		stats.put("documentsEnAttente", documentRepository.countByStatut(StatutDocument.EN_ATTENTE));
		stats.put("documentsValides", documentRepository.countByStatut(StatutDocument.VALIDE));

		return stats;
	}

	@Transactional(readOnly = true)
	public List<Object> getUtilisateurs(String type, Boolean actif, int page, int size) {
		Pageable pageable = PageRequest.of(page, size);

		// Simplification : retourner tous les utilisateurs avec pagination basique
		return userRepository.findAll(pageable)
				.stream()
				.filter(user -> type == null || user.getTypeUtilisateur().name().equals(type))
				.filter(user -> actif == null || user.getActif().equals(actif))
				.map(userMapperService::toDTO)
				.map(dto -> (Object) dto)
				.toList();
	}

	@Transactional
	public void changerStatutUtilisateur(Long userId, Boolean actif) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException("Utilisateur non trouvé"));

		user.setActif(actif);
		userRepository.save(user);

		log.info("Statut de l'utilisateur {} changé vers : {}", userId, actif);
	}

	@Transactional(readOnly = true)
	public List<DocumentDTO.Response> getDocuments(StatutDocument statut, int page, int size) {
		Pageable pageable = PageRequest.of(page, size);

		return (statut != null ?
				documentRepository.findByStatutOrderByDateCreationDesc(statut, pageable) :
				documentRepository.findAllByOrderByDateCreationDesc(pageable))
				.stream()
				.map(this::mapDocumentToDTO)
				.toList();
	}

	@Transactional
	public DocumentDTO.Response validerDocument(Long documentId, DocumentDTO.ValidationRequest request) {
		Document document = documentRepository.findById(documentId)
				.orElseThrow(() -> new BusinessException("Document non trouvé"));

		if (document.getStatut() != StatutDocument.EN_ATTENTE) {
			throw new BusinessException("Ce document a déjà été traité");
		}

		document.setStatut(request.getStatut());
		document.setCommentaireValidation(request.getCommentaire());
		document.setDateValidation(LocalDateTime.now());

		document = documentRepository.save(document);

		log.info("Document {} {} par l'admin", documentId,
				request.getStatut() == StatutDocument.VALIDE ? "validé" : "refusé");

		return mapDocumentToDTO(document);
	}

	@Transactional(readOnly = true)
	public Map<String, Object> getStatistiquesRevenus(String periode) {
		Map<String, Object> revenus = new HashMap<>();

		// TODO: Implémenter le calcul des revenus selon la période
		revenus.put("chiffreAffaireTotal", calculateChiffreAffaireTotal());
		revenus.put("commissionPlateforme", calculateCommissionPlateforme());
		revenus.put("revenusParMois", getRevenusParMois());

		return revenus;
	}

	private BigDecimal calculateChiffreAffaireTotal() {
		// Simulation - à remplacer par une vraie requête
		return BigDecimal.valueOf(150000.00);
	}

	private BigDecimal calculateCommissionPlateforme() {
		// Simulation - à remplacer par une vraie requête
		return BigDecimal.valueOf(15000.00);
	}

	private List<Map<String, Object>> getRevenusParMois() {
		// Simulation - à remplacer par une vraie requête
		return List.of(
				Map.of("mois", "2024-01", "montant", 12000),
				Map.of("mois", "2024-02", "montant", 15000),
				Map.of("mois", "2024-03", "montant", 18000)
		);
	}

	private DocumentDTO.Response mapDocumentToDTO(Document document) {
		DocumentDTO.Response dto = new DocumentDTO.Response();
		dto.setId(document.getId());
		dto.setNom(document.getNom());
		dto.setTypeDocument(document.getTypeDocument());
		dto.setStatut(document.getStatut());
		dto.setUtilisateur(userMapperService.toDTO(document.getUtilisateur()));
		dto.setCommentaireValidation(document.getCommentaireValidation());
		if (document.getValidateur() != null) {
			dto.setValidateur(userMapperService.toDTO(document.getValidateur()));
		}
		dto.setDateCreation(document.getDateCreation());
		dto.setDateValidation(document.getDateValidation());
		dto.setDateExpiration(document.getDateExpiration());
		dto.setTailleFichier(document.getTailleFichier());
		dto.setTypeContenu(document.getTypeContenu());
		dto.setObligatoire(document.getObligatoire());
		dto.setUrlTelecharger("/api/documents/" + document.getId() + "/download");
		return dto;
	}
}