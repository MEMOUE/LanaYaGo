package com.lanayago.service;

import com.lanayago.dto.CommandeDTO;
import com.lanayago.dto.RechercheTransportDTO;
import com.lanayago.dto.VehiculeDTO;
import com.lanayago.entity.*;
import com.lanayago.enums.StatutCommande;
import com.lanayago.exception.BusinessException;
import com.lanayago.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommandeService {

	private final CommandeRepository commandeRepository;
	private final ClientRepository clientRepository;
	private final ChauffeurRepository chauffeurRepository;
	private final VehiculeRepository vehiculeRepository;
	private final RechercheTransportRepository rechercheTransportRepository;
	private final NotificationService notificationService;
	private final UserMapperService userMapperService;

	/**
	 * Nouvelle méthode pour créer une commande à partir d'une recherche de transport
	 */
	@Transactional
	public CommandeDTO.Response creerCommandeDepuisRecherche(
			RechercheTransportDTO.DemandeTransportRequest request) {

		log.info("Création d'une commande depuis la recherche: {}", request.getRechercheId());

		// Récupérer la recherche
		RechercheTransport recherche = rechercheTransportRepository.findById(request.getRechercheId())
				.orElseThrow(() -> new BusinessException("Recherche de transport non trouvée"));

		if (!recherche.getActive()) {
			throw new BusinessException("Cette recherche n'est plus active");
		}

		// Récupérer le chauffeur et vérifier sa disponibilité
		Chauffeur chauffeur = chauffeurRepository.findById(request.getChauffeurId())
				.orElseThrow(() -> new BusinessException("Chauffeur non trouvé"));

		if (!chauffeur.getDisponible()) {
			throw new BusinessException("Le chauffeur n'est plus disponible");
		}

		// Récupérer le véhicule et vérifier sa disponibilité
		Vehicule vehicule = vehiculeRepository.findById(request.getVehiculeId())
				.orElseThrow(() -> new BusinessException("Véhicule non trouvé"));

		if (!vehicule.getDisponible()) {
			throw new BusinessException("Le véhicule n'est plus disponible");
		}

		// Vérifier que le chauffeur correspond au véhicule
		if (!chauffeur.getVehiculeActuel().getId().equals(vehicule.getId())) {
			throw new BusinessException("Le chauffeur n'est pas assigné à ce véhicule");
		}

		// Créer la commande
		Commande commande = new Commande();
		commande.setClient(recherche.getClient());
		commande.setChauffeur(chauffeur);
		commande.setVehicule(vehicule);

		// Copier les données de la recherche
		commande.setLatitudeDepart(recherche.getLatitudeDepart());
		commande.setLongitudeDepart(recherche.getLongitudeDepart());
		commande.setAdresseDepart(recherche.getAdresseDepart());
		commande.setLatitudeArrivee(recherche.getLatitudeArrivee());
		commande.setLongitudeArrivee(recherche.getLongitudeArrivee());
		commande.setAdresseArrivee(recherche.getAdresseArrivee());
		commande.setPoidsMarchandise(recherche.getPoidsMarchandise());
		commande.setVolumeMarchandise(recherche.getVolumeMarchandise());
		commande.setDescriptionMarchandise(recherche.getDescriptionMarchandise());
		commande.setUrgent(recherche.getUrgent());
		commande.setDateRamassageSouhaitee(recherche.getDateRamassageSouhaitee());
		commande.setDateLivraisonSouhaitee(recherche.getDateLivraisonSouhaitee());
		commande.setDistance(recherche.getDistance());
		commande.setTarifCalcule(recherche.getTarifEstime());
		commande.setTarifFinal(recherche.getTarifEstime());
		commande.setStatut(StatutCommande.EN_ATTENTE);

		// Instructions spéciales
		if (request.getInstructionsSpeciales() != null) {
			commande.setCommentaireClient(request.getInstructionsSpeciales());
		}

		commande = commandeRepository.save(commande);

		// Désactiver la recherche
		recherche.setActive(false);
		rechercheTransportRepository.save(recherche);

		// Notifier le chauffeur de la nouvelle commande
		notificationService.notifierNouvelleCommande(List.of(chauffeur), commande);

		log.info("Commande créée avec succès depuis la recherche: {} - Commande ID: {}",
				request.getRechercheId(), commande.getId());

		return mapToResponse(commande);
	}

	/**
	 * Le chauffeur accepte la commande (après avoir vu la demande)
	 */
	@Transactional
	public CommandeDTO.Response accepterCommande(Long commandeId, Long chauffeurId) {
		log.info("Acceptation de la commande {} par le chauffeur {}", commandeId, chauffeurId);

		Commande commande = commandeRepository.findById(commandeId)
				.orElseThrow(() -> new BusinessException("Commande non trouvée"));

		if (commande.getStatut() != StatutCommande.EN_ATTENTE) {
			throw new BusinessException("Cette commande n'est plus disponible - Statut: " + commande.getStatut().getLibelle());
		}

		Chauffeur chauffeur = chauffeurRepository.findById(chauffeurId)
				.orElseThrow(() -> new BusinessException("Chauffeur non trouvé"));

		// Vérifier que c'est bien le chauffeur assigné à cette commande
		if (!commande.getChauffeur().getId().equals(chauffeurId)) {
			throw new BusinessException("Vous n'êtes pas le chauffeur assigné à cette commande");
		}

		if (!chauffeur.getDisponible()) {
			throw new BusinessException("Vous n'êtes plus disponible");
		}

		// Accepter la commande
		commande.setStatut(StatutCommande.ACCEPTEE);

		// Réserver les ressources
		chauffeur.setDisponible(false);
		chauffeurRepository.save(chauffeur);

		commande.getVehicule().setDisponible(false);
		vehiculeRepository.save(commande.getVehicule());

		commande = commandeRepository.save(commande);

		// Notification au client
		notificationService.notifierChangementStatut(commande);

		log.info("Commande acceptée avec succès par le chauffeur {} {}",
				chauffeur.getNom(), chauffeur.getPrenom());

		return mapToResponse(commande);
	}

	/**
	 * Le chauffeur refuse la commande
	 */
	@Transactional
	public void refuserCommande(Long commandeId, Long chauffeurId, String motifRefus) {
		log.info("Refus de la commande {} par le chauffeur {}", commandeId, chauffeurId);

		Commande commande = commandeRepository.findById(commandeId)
				.orElseThrow(() -> new BusinessException("Commande non trouvée"));

		if (commande.getStatut() != StatutCommande.EN_ATTENTE) {
			throw new BusinessException("Cette commande ne peut plus être refusée");
		}

		// Vérifier que c'est bien le chauffeur assigné
		if (!commande.getChauffeur().getId().equals(chauffeurId)) {
			throw new BusinessException("Vous n'êtes pas le chauffeur assigné à cette commande");
		}

		// Marquer la commande comme refusée
		commande.setStatut(StatutCommande.REFUSEE);
		commande.setCommentaireChauffeur("Refusée: " + motifRefus);

		// Libérer les ressources
		libererRessources(commande);

		commandeRepository.save(commande);

		// Notifier le client du refus
		notificationService.notifierChangementStatut(commande);

		log.info("Commande {} refusée par chauffeur {} - Motif: {}",
				commandeId, chauffeurId, motifRefus);
	}

	@Transactional
	public CommandeDTO.Response changerStatut(Long commandeId, StatutCommande nouveauStatut) {
		Commande commande = commandeRepository.findById(commandeId)
				.orElseThrow(() -> new BusinessException("Commande non trouvée"));

		StatutCommande ancienStatut = commande.getStatut();

		// Validation des transitions de statut
		if (!isTransitionValide(ancienStatut, nouveauStatut)) {
			throw new BusinessException(String.format(
					"Transition de statut invalide: %s vers %s",
					ancienStatut.getLibelle(), nouveauStatut.getLibelle()
			));
		}

		commande.setStatut(nouveauStatut);

		// Actions spécifiques selon le statut
		switch (nouveauStatut) {
			case EN_COURS:
				commande.setDateRamassageEffective(LocalDateTime.now());
				break;
			case RAMASSAGE:
				// Chauffeur arrivé au point de ramassage
				break;
			case EN_LIVRAISON:
				// Marchandise chargée, en route vers destination
				break;
			case LIVREE:
				gererLivraisonTerminee(commande);
				break;
			case ANNULEE:
				gererAnnulationCommande(commande);
				break;
			default:
				break;
		}

		commande = commandeRepository.save(commande);
		notificationService.notifierChangementStatut(commande);

		log.info("Statut de la commande {} changé de {} vers {}",
				commandeId, ancienStatut.getLibelle(), nouveauStatut.getLibelle());

		return mapToResponse(commande);
	}

	@Transactional
	public CommandeDTO.Response evaluerCommande(Long commandeId, BigDecimal note, String commentaire, String typeEvaluateur) {
		Commande commande = commandeRepository.findById(commandeId)
				.orElseThrow(() -> new BusinessException("Commande non trouvée"));

		if (commande.getStatut() != StatutCommande.LIVREE) {
			throw new BusinessException("Impossible d'évaluer une commande non livrée");
		}

		// Validation de la note
		if (note.compareTo(BigDecimal.valueOf(1)) < 0 || note.compareTo(BigDecimal.valueOf(5)) > 0) {
			throw new BusinessException("La note doit être comprise entre 1 et 5");
		}

		if ("CLIENT".equals(typeEvaluateur)) {
			if (commande.getNoteClient() != null) {
				throw new BusinessException("Cette commande a déjà été évaluée par le client");
			}
			commande.setNoteClient(note);
			commande.setCommentaireClient(commentaire);

			// Mettre à jour la note moyenne du chauffeur
			if (commande.getChauffeur() != null) {
				mettreAJourNoteMoyenneChauffeur(commande.getChauffeur().getId());
			}
		} else if ("CHAUFFEUR".equals(typeEvaluateur)) {
			if (commande.getNoteChauffeur() != null) {
				throw new BusinessException("Cette commande a déjà été évaluée par le chauffeur");
			}
			commande.setNoteChauffeur(note);
			commande.setCommentaireChauffeur(commentaire);

			// Mettre à jour la note moyenne du client
			mettreAJourNoteMoyenneClient(commande.getClient().getId());
		} else {
			throw new BusinessException("Type d'évaluateur invalide");
		}

		commande = commandeRepository.save(commande);
		log.info("Évaluation ajoutée pour la commande {} par {}: {} étoiles",
				commandeId, typeEvaluateur, note);

		return mapToResponse(commande);
	}

	// Méthodes de lecture existantes
	@Transactional(readOnly = true)
	public List<CommandeDTO.Response> getCommandesClient(Long clientId) {
		return commandeRepository.findByClientIdOrderByDateCreationDesc(clientId)
				.stream()
				.map(this::mapToResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<CommandeDTO.Response> getCommandesChauffeur(Long chauffeurId) {
		return commandeRepository.findByChauffeurIdOrderByDateCreationDesc(chauffeurId)
				.stream()
				.map(this::mapToResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public CommandeDTO.Response getCommandeById(Long commandeId) {
		Commande commande = commandeRepository.findById(commandeId)
				.orElseThrow(() -> new BusinessException("Commande non trouvée"));
		return mapToResponse(commande);
	}

	@Transactional(readOnly = true)
	public List<CommandeDTO.Response> getCommandesParStatut(StatutCommande statut) {
		return commandeRepository.findByStatutOrderByDateCreationAsc(statut)
				.stream()
				.map(this::mapToResponse)
				.toList();
	}

	// =================== MÉTHODES PRIVÉES ===================

	private boolean isTransitionValide(StatutCommande ancien, StatutCommande nouveau) {
		return switch (ancien) {
			case EN_ATTENTE -> nouveau == StatutCommande.ACCEPTEE || nouveau == StatutCommande.REFUSEE || nouveau == StatutCommande.ANNULEE;
			case ACCEPTEE -> nouveau == StatutCommande.EN_COURS || nouveau == StatutCommande.ANNULEE;
			case EN_COURS -> nouveau == StatutCommande.RAMASSAGE || nouveau == StatutCommande.ANNULEE;
			case RAMASSAGE -> nouveau == StatutCommande.EN_LIVRAISON;
			case EN_LIVRAISON -> nouveau == StatutCommande.LIVREE;
			case LIVREE, ANNULEE, REFUSEE -> false; // États finaux
		};
	}

	private void gererLivraisonTerminee(Commande commande) {
		commande.setDateLivraisonEffective(LocalDateTime.now());

		// Libérer les ressources
		libererRessources(commande);

		// Mettre à jour les statistiques
		if (commande.getChauffeur() != null) {
			commande.getChauffeur().setNombreCourses(
					commande.getChauffeur().getNombreCourses() + 1
			);
			chauffeurRepository.save(commande.getChauffeur());
		}

		commande.getClient().setNombreCommandes(
				commande.getClient().getNombreCommandes() + 1
		);
		clientRepository.save(commande.getClient());
	}

	private void gererAnnulationCommande(Commande commande) {
		libererRessources(commande);
	}

	private void libererRessources(Commande commande) {
		if (commande.getChauffeur() != null) {
			commande.getChauffeur().setDisponible(true);
			chauffeurRepository.save(commande.getChauffeur());
		}

		if (commande.getVehicule() != null) {
			commande.getVehicule().setDisponible(true);
			vehiculeRepository.save(commande.getVehicule());
		}
	}

	private void mettreAJourNoteMoyenneChauffeur(Long chauffeurId) {
		List<Commande> commandes = commandeRepository.findByChauffeurIdOrderByDateCreationDesc(chauffeurId);
		BigDecimal sommeTotale = BigDecimal.ZERO;
		int nombreNotes = 0;

		for (Commande cmd : commandes) {
			if (cmd.getNoteClient() != null) {
				sommeTotale = sommeTotale.add(cmd.getNoteClient());
				nombreNotes++;
			}
		}

		if (nombreNotes > 0) {
			BigDecimal noteMoyenne = sommeTotale.divide(BigDecimal.valueOf(nombreNotes), 2, java.math.RoundingMode.HALF_UP);
			chauffeurRepository.findById(chauffeurId).ifPresent(chauffeur -> {
				chauffeur.setNoteMoyenne(noteMoyenne);
				chauffeurRepository.save(chauffeur);
			});
		}
	}

	private void mettreAJourNoteMoyenneClient(Long clientId) {
		List<Commande> commandes = commandeRepository.findByClientIdOrderByDateCreationDesc(clientId);
		BigDecimal sommeTotale = BigDecimal.ZERO;
		int nombreNotes = 0;

		for (Commande cmd : commandes) {
			if (cmd.getNoteChauffeur() != null) {
				sommeTotale = sommeTotale.add(cmd.getNoteChauffeur());
				nombreNotes++;
			}
		}

		if (nombreNotes > 0) {
			BigDecimal noteMoyenne = sommeTotale.divide(BigDecimal.valueOf(nombreNotes), 2, java.math.RoundingMode.HALF_UP);
			clientRepository.findById(clientId).ifPresent(client -> {
				client.setNoteMoyenne(noteMoyenne);
				clientRepository.save(client);
			});
		}
	}

	private CommandeDTO.Response mapToResponse(Commande commande) {
		CommandeDTO.Response response = new CommandeDTO.Response();
		response.setId(commande.getId());
		response.setNumeroCommande(commande.getNumeroCommande());

		// Mapping des utilisateurs avec UserMapperService
		if (commande.getClient() != null) {
			response.setClient(userMapperService.toDTO(commande.getClient()));
		}
		if (commande.getChauffeur() != null) {
			response.setChauffeur(userMapperService.toDTO(commande.getChauffeur()));
		}

		// Mapping du véhicule
		if (commande.getVehicule() != null) {
			response.setVehicule(mapVehiculeToDTO(commande.getVehicule()));
		}

		response.setLatitudeDepart(commande.getLatitudeDepart());
		response.setLongitudeDepart(commande.getLongitudeDepart());
		response.setAdresseDepart(commande.getAdresseDepart());
		response.setLatitudeArrivee(commande.getLatitudeArrivee());
		response.setLongitudeArrivee(commande.getLongitudeArrivee());
		response.setAdresseArrivee(commande.getAdresseArrivee());
		response.setPoidsMarchandise(commande.getPoidsMarchandise());
		response.setVolumeMarchandise(commande.getVolumeMarchandise());
		response.setDescriptionMarchandise(commande.getDescriptionMarchandise());
		response.setUrgent(commande.getUrgent());
		response.setDateCreation(commande.getDateCreation());
		response.setDateRamassageSouhaitee(commande.getDateRamassageSouhaitee());
		response.setDateRamassageEffective(commande.getDateRamassageEffective());
		response.setDateLivraisonSouhaitee(commande.getDateLivraisonSouhaitee());
		response.setDateLivraisonEffective(commande.getDateLivraisonEffective());
		response.setDistance(commande.getDistance());
		response.setTarifCalcule(commande.getTarifCalcule());
		response.setTarifFinal(commande.getTarifFinal());
		response.setStatut(commande.getStatut());
		response.setNoteClient(commande.getNoteClient());
		response.setNoteChauffeur(commande.getNoteChauffeur());
		response.setCommentaireClient(commande.getCommentaireClient());
		response.setCommentaireChauffeur(commande.getCommentaireChauffeur());

		return response;
	}

	private VehiculeDTO.Response mapVehiculeToDTO(Vehicule vehicule) {
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

	public List<CommandeDTO.Response> getCommandesProprietaire(Long proprietaireId) {
		return List.of();
	}
}