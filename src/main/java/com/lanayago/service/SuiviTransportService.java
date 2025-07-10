package com.lanayago.service;

import com.lanayago.dto.RechercheTransportDTO;
import com.lanayago.dto.VehiculeDTO;
import com.lanayago.entity.Chauffeur;
import com.lanayago.entity.Commande;
import com.lanayago.enums.StatutCommande;
import com.lanayago.exception.BusinessException;
import com.lanayago.repository.ChauffeurRepository;
import com.lanayago.repository.CommandeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SuiviTransportService {

	private final CommandeRepository commandeRepository;
	private final ChauffeurRepository chauffeurRepository;
	private final UserMapperService userMapperService;
	private final SimpMessagingTemplate messagingTemplate;
	private final GeolocationService geolocationService;

	@Transactional(readOnly = true)
	public RechercheTransportDTO.SuiviTransportResponse getSuiviCommande(Long commandeId) {
		Commande commande = commandeRepository.findById(commandeId)
				.orElseThrow(() -> new BusinessException("Commande non trouvée"));

		if (commande.getChauffeur() == null) {
			throw new BusinessException("Aucun chauffeur assigné à cette commande");
		}

		return mapToSuiviResponse(commande);
	}

	@Transactional
	public void mettreAJourPositionChauffeur(Long chauffeurId, Double latitude, Double longitude) {
		Chauffeur chauffeur = chauffeurRepository.findById(chauffeurId)
				.orElseThrow(() -> new BusinessException("Chauffeur non trouvé"));

		// Mettre à jour la position du chauffeur
		chauffeur.setLatitudeActuelle(latitude);
		chauffeur.setLongitudeActuelle(longitude);
		chauffeurRepository.save(chauffeur);

		// Mettre à jour la position du véhicule si assigné
		if (chauffeur.getVehiculeActuel() != null) {
			chauffeur.getVehiculeActuel().setLatitudeActuelle(latitude);
			chauffeur.getVehiculeActuel().setLongitudeActuelle(longitude);
		}

		// Trouver les commandes actives du chauffeur
		List<Commande> commandesActives = commandeRepository.findByChauffeurIdOrderByDateCreationDesc(chauffeurId)
				.stream()
				.filter(c -> Arrays.asList(
						StatutCommande.ACCEPTEE,
						StatutCommande.EN_COURS,
						StatutCommande.RAMASSAGE,
						StatutCommande.EN_LIVRAISON
				).contains(c.getStatut()))
				.toList();

		// Notifier les clients des commandes actives
		for (Commande commande : commandesActives) {
			notifierMiseAJourPosition(commande, latitude, longitude);
		}

		log.info("Position mise à jour pour le chauffeur {} : {}, {}", chauffeurId, latitude, longitude);
	}

	@Transactional
	public void mettreAJourStatutConnexion(Long chauffeurId, Boolean enLigne) {
		Chauffeur chauffeur = chauffeurRepository.findById(chauffeurId)
				.orElseThrow(() -> new BusinessException("Chauffeur non trouvé"));

		// Le statut de connexion peut être géré via un champ séparé ou via disponible
		chauffeur.setDisponible(enLigne && chauffeur.getDisponible());
		chauffeurRepository.save(chauffeur);

		// Notifier les clients des commandes actives
		List<Commande> commandesActives = commandeRepository.findByChauffeurIdOrderByDateCreationDesc(chauffeurId)
				.stream()
				.filter(c -> Arrays.asList(
						StatutCommande.ACCEPTEE,
						StatutCommande.EN_COURS,
						StatutCommande.RAMASSAGE,
						StatutCommande.EN_LIVRAISON
				).contains(c.getStatut()))
				.toList();

		for (Commande commande : commandesActives) {
			notifierChangementConnexion(commande, enLigne);
		}

		log.info("Statut de connexion mis à jour pour le chauffeur {} : {}", chauffeurId, enLigne);
	}

	@Transactional
	public void ajouterEtapeSuivi(Long commandeId, String description, Double latitude, Double longitude) {
		Commande commande = commandeRepository.findById(commandeId)
				.orElseThrow(() -> new BusinessException("Commande non trouvée"));

		// Pour cet exemple, nous stockons les étapes de suivi dans les commentaires
		// Dans une vraie application, vous pourriez créer une entité EtapeSuivi séparée
		String etapeInfo = String.format("[%s] %s", LocalDateTime.now(), description);
		if (latitude != null && longitude != null) {
			etapeInfo += String.format(" (Position: %.6f, %.6f)", latitude, longitude);
		}

		String commentaireActuel = commande.getCommentaireChauffeur();
		if (commentaireActuel == null) {
			commentaireActuel = "";
		}
		commande.setCommentaireChauffeur(commentaireActuel + "\n" + etapeInfo);

		commandeRepository.save(commande);

		// Notifier le client
		notifierNouvelleEtape(commande, description, latitude, longitude);

		log.info("Étape de suivi ajoutée pour la commande {} : {}", commandeId, description);
	}

	@Transactional(readOnly = true)
	public RechercheTransportDTO.SuiviTransportResponse getCommandeActiveChauffeur(Long chauffeurId) {
		List<Commande> commandesActives = commandeRepository.findByChauffeurIdOrderByDateCreationDesc(chauffeurId)
				.stream()
				.filter(c -> Arrays.asList(
						StatutCommande.ACCEPTEE,
						StatutCommande.EN_COURS,
						StatutCommande.RAMASSAGE,
						StatutCommande.EN_LIVRAISON
				).contains(c.getStatut()))
				.toList();

		if (commandesActives.isEmpty()) {
			throw new BusinessException("Aucune commande active pour ce chauffeur");
		}

		return mapToSuiviResponse(commandesActives.get(0));
	}

	private RechercheTransportDTO.SuiviTransportResponse mapToSuiviResponse(Commande commande) {
		RechercheTransportDTO.SuiviTransportResponse response = new RechercheTransportDTO.SuiviTransportResponse();

		response.setCommandeId(commande.getId());
		response.setNumeroCommande(commande.getNumeroCommande());
		response.setChauffeur(userMapperService.toDTO(commande.getChauffeur()));

		if (commande.getVehicule() != null) {
			response.setVehicule(mapVehiculeToDTO(commande.getVehicule()));
		}

		// Position actuelle du chauffeur
		if (commande.getChauffeur() != null) {
			response.setLatitudeActuelle(commande.getChauffeur().getLatitudeActuelle());
			response.setLongitudeActuelle(commande.getChauffeur().getLongitudeActuelle());
			response.setTelephoneChauffeur(commande.getChauffeur().getTelephone());
			response.setChauffeurEnLigne(commande.getChauffeur().getDisponible()); // Approximation
		}

		response.setStatut(commande.getStatut().getLibelle());
		response.setProgressionPourcentage(calculerProgression(commande));

		// Calcul des heures estimées
		response.setHeureArriveePrevue(calculerHeureArriveePrevue(commande));
		response.setHeureLivraisonPrevue(calculerHeureLivraisonPrevue(commande));

		// Extraction des étapes de suivi depuis les commentaires
		response.setEtapesSuivi(extraireEtapesSuivi(commande));

		return response;
	}

	private VehiculeDTO.Response mapVehiculeToDTO(com.lanayago.entity.Vehicule vehicule) {
		VehiculeDTO.Response dto = new VehiculeDTO.Response();
		dto.setId(vehicule.getId());
		dto.setImmatriculation(vehicule.getImmatriculation());
		dto.setMarque(vehicule.getMarque());
		dto.setModele(vehicule.getModele());
		dto.setAnnee(vehicule.getAnnee());
		dto.setCapacitePoids(vehicule.getCapacitePoids());
		dto.setCapaciteVolume(vehicule.getCapaciteVolume());
		dto.setTypeVehicule(vehicule.getTypeVehicule());
		dto.setDisponible(vehicule.getDisponible());
		dto.setLatitudeActuelle(vehicule.getLatitudeActuelle());
		dto.setLongitudeActuelle(vehicule.getLongitudeActuelle());
		dto.setDateCreation(vehicule.getDateCreation());
		dto.setPhotoUrl(vehicule.getPhotoUrl());

		if (vehicule.getProprietaire() != null) {
			dto.setProprietaire(userMapperService.toDTO(vehicule.getProprietaire()));
		}

		return dto;
	}

	private Integer calculerProgression(Commande commande) {
		return switch (commande.getStatut()) {
			case ACCEPTEE -> 20;
			case EN_COURS -> 40;
			case RAMASSAGE -> 60;
			case EN_LIVRAISON -> 80;
			case LIVREE -> 100;
			default -> 0;
		};
	}

	private LocalDateTime calculerHeureArriveePrevue(Commande commande) {
		if (commande.getChauffeur() == null ||
				commande.getChauffeur().getLatitudeActuelle() == null) {
			return null;
		}

		// Calculer la distance depuis la position actuelle vers le point de ramassage ou livraison
		Double latitude = commande.getStatut() == StatutCommande.ACCEPTEE || commande.getStatut() == StatutCommande.EN_COURS ?
				commande.getLatitudeDepart() : commande.getLatitudeArrivee();
		Double longitude = commande.getStatut() == StatutCommande.ACCEPTEE || commande.getStatut() == StatutCommande.EN_COURS ?
				commande.getLongitudeDepart() : commande.getLongitudeArrivee();

		Double distance = geolocationService.calculerDistance(
				commande.getChauffeur().getLatitudeActuelle(),
				commande.getChauffeur().getLongitudeActuelle(),
				latitude,
				longitude
		);

		// Vitesse moyenne estimée : 40 km/h
		double tempsEstimeHeures = distance / 40.0;
		return LocalDateTime.now().plusMinutes((long) (tempsEstimeHeures * 60));
	}

	private LocalDateTime calculerHeureLivraisonPrevue(Commande commande) {
		LocalDateTime heureArrivee = calculerHeureArriveePrevue(commande);
		if (heureArrivee == null) {
			return null;
		}

		// Ajouter le temps de transport entre départ et arrivée
		double tempsTransportHeures = commande.getDistance() / 40.0; // Vitesse moyenne 40 km/h
		return heureArrivee.plusMinutes((long) (tempsTransportHeures * 60));
	}

	private List<RechercheTransportDTO.EtapeSuivi> extraireEtapesSuivi(Commande commande) {
		List<RechercheTransportDTO.EtapeSuivi> etapes = new ArrayList<>();

		// Étapes basées sur le statut actuel
		addEtapeStatut(etapes, "Commande acceptée", commande.getDateCreation(), StatutCommande.ACCEPTEE, commande.getStatut());

		if (commande.getDateRamassageEffective() != null) {
			addEtapeStatut(etapes, "Ramassage effectué", commande.getDateRamassageEffective(), StatutCommande.RAMASSAGE, commande.getStatut());
		}

		if (commande.getDateLivraisonEffective() != null) {
			addEtapeStatut(etapes, "Livraison effectuée", commande.getDateLivraisonEffective(), StatutCommande.LIVREE, commande.getStatut());
		}

		return etapes;
	}

	private void addEtapeStatut(List<RechercheTransportDTO.EtapeSuivi> etapes, String description,
	                            LocalDateTime heureEtape, StatutCommande statutEtape, StatutCommande statutActuel) {
		RechercheTransportDTO.EtapeSuivi etape = new RechercheTransportDTO.EtapeSuivi();
		etape.setDescription(description);
		etape.setHeureEtape(heureEtape);
		etape.setCompleted(statutActuel.ordinal() >= statutEtape.ordinal());
		etapes.add(etape);
	}

	private void notifierMiseAJourPosition(Commande commande, Double latitude, Double longitude) {
		try {
			messagingTemplate.convertAndSend(
					"/topic/client/" + commande.getClient().getId() + "/suivi",
					createPositionUpdateNotification(commande.getId(), latitude, longitude)
			);
		} catch (Exception e) {
			log.error("Erreur lors de la notification de mise à jour de position", e);
		}
	}

	private void notifierChangementConnexion(Commande commande, Boolean enLigne) {
		try {
			messagingTemplate.convertAndSend(
					"/topic/client/" + commande.getClient().getId() + "/suivi",
					createConnexionUpdateNotification(commande.getId(), enLigne)
			);
		} catch (Exception e) {
			log.error("Erreur lors de la notification de changement de connexion", e);
		}
	}

	private void notifierNouvelleEtape(Commande commande, String description, Double latitude, Double longitude) {
		try {
			messagingTemplate.convertAndSend(
					"/topic/client/" + commande.getClient().getId() + "/suivi",
					createEtapeUpdateNotification(commande.getId(), description, latitude, longitude)
			);
		} catch (Exception e) {
			log.error("Erreur lors de la notification de nouvelle étape", e);
		}
	}

	private Object createPositionUpdateNotification(Long commandeId, Double latitude, Double longitude) {
		return new Object() {
			public String getType() { return "POSITION_UPDATE"; }
			public Long getCommandeId() { return commandeId; }
			public Double getLatitude() { return latitude; }
			public Double getLongitude() { return longitude; }
			public LocalDateTime getTimestamp() { return LocalDateTime.now(); }
		};
	}

	private Object createConnexionUpdateNotification(Long commandeId, Boolean enLigne) {
		return new Object() {
			public String getType() { return "CONNEXION_UPDATE"; }
			public Long getCommandeId() { return commandeId; }
			public Boolean getEnLigne() { return enLigne; }
			public LocalDateTime getTimestamp() { return LocalDateTime.now(); }
		};
	}

	private Object createEtapeUpdateNotification(Long commandeId, String description, Double latitude, Double longitude) {
		return new Object() {
			public String getType() { return "ETAPE_UPDATE"; }
			public Long getCommandeId() { return commandeId; }
			public String getDescription() { return description; }
			public Double getLatitude() { return latitude; }
			public Double getLongitude() { return longitude; }
			public LocalDateTime getTimestamp() { return LocalDateTime.now(); }
		};
	}
}