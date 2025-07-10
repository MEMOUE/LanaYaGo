package com.lanayago.service;

import com.lanayago.dto.DemandeProprietaireDTO;
import com.lanayago.entity.Client;
import com.lanayago.entity.DemandeProprietaire;
import com.lanayago.entity.ProprietaireVehicule;
import com.lanayago.entity.User;
import com.lanayago.enums.StatutDemandeProprietaire;
import com.lanayago.enums.TypeUtilisateur;
import com.lanayago.exception.BusinessException;
import com.lanayago.repository.DemandeProprietaireRepository;
import com.lanayago.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DemandeProprietaireService {

	private final DemandeProprietaireRepository demandeProprietaireRepository;
	private final UserRepository userRepository;
	private final UserMapperService userMapperService;
	private final FileStorageService fileStorageService; // Service pour gérer les fichiers

	@Transactional
	public DemandeProprietaireDTO.Response creerDemande(Long userId, DemandeProprietaireDTO.CreateRequest request) {
		log.info("Création d'une demande de propriétaire pour l'utilisateur: {}", userId);

		User user = userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException("Utilisateur non trouvé"));

		if (user.getTypeUtilisateur() != TypeUtilisateur.CLIENT) {
			throw new BusinessException("Seuls les clients peuvent faire une demande de propriétaire");
		}

		// Vérifier qu'il n'y a pas déjà une demande en cours
		boolean demandeExistante = demandeProprietaireRepository.existsByUserIdAndStatutIn(
				userId,
				Arrays.asList(
						StatutDemandeProprietaire.EN_ATTENTE,
						StatutDemandeProprietaire.EN_REVISION,
						StatutDemandeProprietaire.APPROUVEE
				)
		);

		if (demandeExistante) {
			throw new BusinessException("Vous avez déjà une demande en cours ou approuvée");
		}

		// Vérifier le SIRET si fourni
		if (request.getNumeroSiret() != null && !request.getNumeroSiret().isEmpty()) {
			if (demandeProprietaireRepository.findAll().stream()
					.anyMatch(d -> request.getNumeroSiret().equals(d.getNumeroSiret()) &&
							d.getStatut() == StatutDemandeProprietaire.APPROUVEE)) {
				throw new BusinessException("Ce numéro SIRET est déjà utilisé par un autre propriétaire");
			}
		}

		DemandeProprietaire demande = new DemandeProprietaire();
		demande.setUser(user);
		demande.setNomEntreprise(request.getNomEntreprise());
		demande.setNumeroSiret(request.getNumeroSiret());
		demande.setAdresseEntreprise(request.getAdresseEntreprise());
		demande.setVilleEntreprise(request.getVilleEntreprise());
		demande.setCodePostalEntreprise(request.getCodePostalEntreprise());

		demande = demandeProprietaireRepository.save(demande);
		log.info("Demande de propriétaire créée avec succès: {}", demande.getId());

		return mapToResponse(demande);
	}

	@Transactional
	public DemandeProprietaireDTO.DocumentUploadResponse uploadDocument(
			Long demandeId,
			String documentType,
			MultipartFile file) {

		log.info("Upload de document {} pour la demande {}", documentType, demandeId);

		DemandeProprietaire demande = demandeProprietaireRepository.findById(demandeId)
				.orElseThrow(() -> new BusinessException("Demande non trouvée"));

		if (demande.getStatut() != StatutDemandeProprietaire.EN_ATTENTE &&
				demande.getStatut() != StatutDemandeProprietaire.DOCUMENTS_MANQUANTS) {
			throw new BusinessException("Cette demande ne peut plus être modifiée");
		}

		// Validation du type de document
		if (!Arrays.asList("piece_identite", "extrait", "justificatif_adresse").contains(documentType)) {
			throw new BusinessException("Type de document invalide");
		}

		try {
			String documentUrl = fileStorageService.storeFile(file, "demandes/" + demandeId);

			// Mettre à jour la demande avec l'URL du document
			switch (documentType) {
				case "piece_identite":
					demande.setPieceIdentiteUrl(documentUrl);
					break;
				case "extrait":
					demande.setExtraitUrl(documentUrl);
					break;
				case "justificatif_adresse":
					demande.setJustificatifAdresseUrl(documentUrl);
					break;
			}

			// Si tous les documents sont fournis, changer le statut
			if (demande.getPieceIdentiteUrl() != null &&
					demande.getExtraitUrl() != null &&
					demande.getJustificatifAdresseUrl() != null &&
					demande.getStatut() == StatutDemandeProprietaire.DOCUMENTS_MANQUANTS) {
				demande.setStatut(StatutDemandeProprietaire.EN_ATTENTE);
			}

			demandeProprietaireRepository.save(demande);

			DemandeProprietaireDTO.DocumentUploadResponse response = new DemandeProprietaireDTO.DocumentUploadResponse();
			response.setDocumentType(documentType);
			response.setDocumentUrl(documentUrl);
			response.setMessage("Document uploadé avec succès");

			return response;

		} catch (Exception e) {
			log.error("Erreur lors de l'upload du document", e);
			throw new BusinessException("Erreur lors de l'upload du document: " + e.getMessage());
		}
	}

	@Transactional(readOnly = true)
	public List<DemandeProprietaireDTO.Response> getDemandesUtilisateur(Long userId) {
		return demandeProprietaireRepository.findByUserIdOrderByDateCreationDesc(userId)
				.stream()
				.map(this::mapToResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<DemandeProprietaireDTO.Response> getDemandesEnAttente() {
		return demandeProprietaireRepository.findDemandesEnAttente()
				.stream()
				.map(this::mapToResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public DemandeProprietaireDTO.Response getDemandeById(Long demandeId) {
		DemandeProprietaire demande = demandeProprietaireRepository.findById(demandeId)
				.orElseThrow(() -> new BusinessException("Demande non trouvée"));
		return mapToResponse(demande);
	}

	@Transactional
	public DemandeProprietaireDTO.Response traiterDemande(
			Long demandeId,
			DemandeProprietaireDTO.TraitementRequest request,
			Long adminId) {

		log.info("Traitement de la demande {} par l'admin {}", demandeId, adminId);

		DemandeProprietaire demande = demandeProprietaireRepository.findById(demandeId)
				.orElseThrow(() -> new BusinessException("Demande non trouvée"));

		if (demande.getStatut() != StatutDemandeProprietaire.EN_ATTENTE &&
				demande.getStatut() != StatutDemandeProprietaire.EN_REVISION) {
			throw new BusinessException("Cette demande a déjà été traitée");
		}

		StatutDemandeProprietaire nouveauStatut = StatutDemandeProprietaire.valueOf(request.getStatut());
		demande.setStatut(nouveauStatut);
		demande.setCommentaireAdmin(request.getCommentaireAdmin());
		demande.setDateTraitement(LocalDateTime.now());
		demande.setAdminId(adminId);

		// Si la demande est approuvée, convertir l'utilisateur en propriétaire
		if (nouveauStatut == StatutDemandeProprietaire.APPROUVEE) {
			convertirEnProprietaire(demande);
		}

		demande = demandeProprietaireRepository.save(demande);
		log.info("Demande {} traitée avec le statut: {}", demandeId, nouveauStatut);

		return mapToResponse(demande);
	}

	@Transactional
	private void convertirEnProprietaire(DemandeProprietaire demande) {
		log.info("Conversion de l'utilisateur {} en propriétaire", demande.getUser().getId());

		Client client = (Client) demande.getUser();

		// Créer le nouveau ProprietaireVehicule
		ProprietaireVehicule proprietaire = new ProprietaireVehicule();
		proprietaire.setNom(client.getNom());
		proprietaire.setPrenom(client.getPrenom());
		proprietaire.setEmail(client.getEmail());
		proprietaire.setTelephone(client.getTelephone());
		proprietaire.setMotDePasse(client.getMotDePasse());
		proprietaire.setTypeUtilisateur(TypeUtilisateur.PROPRIETAIRE_VEHICULE);
		proprietaire.setActif(client.getActif());
		proprietaire.setDateCreation(client.getDateCreation());
		proprietaire.setPhotoUrl(client.getPhotoUrl());

		// Données spécifiques au propriétaire
		proprietaire.setNomEntreprise(demande.getNomEntreprise());
		proprietaire.setNumeroSiret(demande.getNumeroSiret());
		proprietaire.setAdresseEntreprise(demande.getAdresseEntreprise());

		// Sauvegarder le nouveau propriétaire
		userRepository.save(proprietaire);

		// Supprimer l'ancien client
		userRepository.delete(client);

		log.info("Utilisateur {} converti en propriétaire avec succès", proprietaire.getId());
	}

	private DemandeProprietaireDTO.Response mapToResponse(DemandeProprietaire demande) {
		DemandeProprietaireDTO.Response response = new DemandeProprietaireDTO.Response();
		response.setId(demande.getId());
		response.setUser(userMapperService.toDTO(demande.getUser()));
		response.setNomEntreprise(demande.getNomEntreprise());
		response.setNumeroSiret(demande.getNumeroSiret());
		response.setAdresseEntreprise(demande.getAdresseEntreprise());
		response.setVilleEntreprise(demande.getVilleEntreprise());
		response.setCodePostalEntreprise(demande.getCodePostalEntreprise());
		response.setPieceIdentiteUrl(demande.getPieceIdentiteUrl());
		response.setExtraitUrl(demande.getExtraitUrl());
		response.setJustificatifAdresseUrl(demande.getJustificatifAdresseUrl());
		response.setStatut(demande.getStatut());
		response.setCommentaireAdmin(demande.getCommentaireAdmin());
		response.setDateCreation(demande.getDateCreation());
		response.setDateModification(demande.getDateModification());
		response.setDateTraitement(demande.getDateTraitement());
		response.setAdminId(demande.getAdminId());
		return response;
	}
}