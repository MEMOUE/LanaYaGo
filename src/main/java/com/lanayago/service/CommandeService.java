package com.lanayago.service;

import com.lanayago.dto.CommandeDTO;
import com.lanayago.dto.VehiculeDTO;
import com.lanayago.entity.Chauffeur;
import com.lanayago.entity.Client;
import com.lanayago.entity.Commande;
import com.lanayago.entity.Vehicule;
import com.lanayago.enums.StatutCommande;
import com.lanayago.enums.TypeVehicule;
import com.lanayago.exception.BusinessException;
import com.lanayago.repository.CommandeRepository;
import com.lanayago.repository.ClientRepository;
import com.lanayago.repository.ChauffeurRepository;
import com.lanayago.repository.VehiculeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
	private final TarificationService tarificationService;
	private final GeolocationService geolocationService;
	private final NotificationService notificationService;
	private final UserMapperService userMapperService;

	@Transactional
	public CommandeDTO.Response creerCommande(Long clientId, CommandeDTO.CreateRequest request) {
		log.info("Création d'une nouvelle commande pour le client: {}", clientId);

		Client client = clientRepository.findById(clientId)
				.orElseThrow(() -> new BusinessException("Client non trouvé"));

		// Validation des données
		validerDonneesCommande(request);

		// Calcul de la distance
		Double distance = geolocationService.calculerDistanceGoogleMaps(
				request.getLatitudeDepart(), request.getLongitudeDepart(),
				request.getLatitudeArrivee(), request.getLongitudeArrivee()
		);

		if (distance == null || distance <= 0) {
			throw new BusinessException("Impossible de calculer la distance entre les deux points");
		}

		// Détermination du type de véhicule nécessaire
		TypeVehicule typeVehicule = determinerTypeVehicule(request.getPoidsMarchandise());

		// Calcul du tarif
		BigDecimal tarif = tarificationService.calculerTarif(request, distance, typeVehicule);

		// Création de la commande
		Commande commande = new Commande();
		commande.setClient(client);
		commande.setLatitudeDepart(request.getLatitudeDepart());
		commande.setLongitudeDepart(request.getLongitudeDepart());
		commande.setAdresseDepart(request.getAdresseDepart());
		commande.setLatitudeArrivee(request.getLatitudeArrivee());
		commande.setLongitudeArrivee(request.getLongitudeArrivee());
		commande.setAdresseArrivee(request.getAdresseArrivee());
		commande.setPoidsMarchandise(request.getPoidsMarchandise());
		commande.setVolumeMarchandise(request.getVolumeMarchandise());
		commande.setDescriptionMarchandise(request.getDescriptionMarchandise());
		commande.setUrgent(request.getUrgent());
		commande.setDateRamassageSouhaitee(request.getDateRamassageSouhaitee());
		commande.setDateLivraisonSouhaitee(request.getDateLivraisonSouhaitee());
		commande.setDistance(distance);
		commande.setTarifCalcule(tarif);
		commande.setTarifFinal(tarif);

		commande = commandeRepository.save(commande);

		// Notification aux chauffeurs disponibles
		notifierChauffeursDisponibles(commande, request);

		log.info("Commande créée avec succès: {} - Montant: {} €", commande.getId(), tarif);
		return mapToResponse(commande);
	}

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

		if (!chauffeur.getDisponible()) {
			throw new BusinessException("Le chauffeur n'est pas disponible");
		}

		// Vérification de la compatibilité véhicule/commande
		if (chauffeur.getVehiculeActuel() == null) {
			throw new BusinessException("Le chauffeur n'a pas de véhicule assigné");
		}

		if (!verifierCompatibiliteVehicule(chauffeur.getVehiculeActuel(), commande)) {
			throw new BusinessException("Le véhicule du chauffeur n'est pas compatible avec cette commande");
		}

		// Affectation
		commande.setChauffeur(chauffeur);
		commande.setVehicule(chauffeur.getVehiculeActuel());
		commande.setStatut(StatutCommande.ACCEPTEE);

		// Mise à jour de la disponibilité
		chauffeur.setDisponible(false);
		chauffeurRepository.save(chauffeur);

		// Mise à jour de la disponibilité du véhicule
		chauffeur.getVehiculeActuel().setDisponible(false);
		vehiculeRepository.save(chauffeur.getVehiculeActuel());

		commande = commandeRepository.save(commande);

		// Notification au client
		notificationService.notifierChangementStatut(commande);

		log.info("Commande acceptée avec succès par le chauffeur {} {}",
				chauffeur.getNom(), chauffeur.getPrenom());
		return mapToResponse(commande);
	}

	@Transactional
	public CommandeDTO.Response refuserCommande(Long commandeId, Long chauffeurId, String motifRefus) {
		log.info("Refus de la commande {} par le chauffeur {}", commandeId, chauffeurId);

		Commande commande = commandeRepository.findById(commandeId)
				.orElseThrow(() -> new BusinessException("Commande non trouvée"));

		if (commande.getStatut() != StatutCommande.EN_ATTENTE) {
			throw new BusinessException("Cette commande ne peut plus être refusée");
		}

		Chauffeur chauffeur = chauffeurRepository.findById(chauffeurId)
				.orElseThrow(() -> new BusinessException("Chauffeur non trouvé"));

		// Logique de refus - peut être enregistrée pour analytics
		log.info("Commande {} refusée par chauffeur {} - Motif: {}",
				commandeId, chauffeurId, motifRefus);

		// La commande reste en attente pour d'autres chauffeurs
		// Notification au client qu'un chauffeur a refusé (optionnel)

		return mapToResponse(commande);
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
				// Commande en cours de ramassage
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
	public CommandeDTO.Response annulerCommande(Long commandeId, String motifAnnulation) {
		Commande commande = commandeRepository.findById(commandeId)
				.orElseThrow(() -> new BusinessException("Commande non trouvée"));

		if (commande.getStatut() == StatutCommande.LIVREE) {
			throw new BusinessException("Impossible d'annuler une commande déjà livrée");
		}

		if (commande.getStatut() == StatutCommande.EN_LIVRAISON) {
			throw new BusinessException("Impossible d'annuler une commande en cours de livraison");
		}

		commande.setStatut(StatutCommande.ANNULEE);
		commande.setCommentaireClient(motifAnnulation);

		// Libérer les ressources si la commande était acceptée
		if (commande.getChauffeur() != null) {
			libererRessources(commande);
		}

		commande = commandeRepository.save(commande);
		notificationService.notifierChangementStatut(commande);

		log.info("Commande {} annulée - Motif: {}", commandeId, motifAnnulation);
		return mapToResponse(commande);
	}

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

	@Transactional(readOnly = true)
	public List<CommandeDTO.Response> getCommandesEnAttente() {
		return getCommandesParStatut(StatutCommande.EN_ATTENTE);
	}

	@Transactional(readOnly = true)
	public List<CommandeDTO.Response> getCommandesProprietaire(Long proprietaireId) {
		return commandeRepository.findByProprietaireId(proprietaireId)
				.stream()
				.map(this::mapToResponse)
				.toList();
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

	@Transactional(readOnly = true)
	public BigDecimal getChiffreAffaireChauffeur(Long chauffeurId) {
		BigDecimal chiffre = BigDecimal.valueOf(commandeRepository.getChiffreAffaireChauffeur(chauffeurId));
		return chiffre != null ? chiffre : BigDecimal.ZERO;
	}

	@Transactional(readOnly = true)
	public long getNombreCommandesParStatut(StatutCommande statut) {
		return commandeRepository.countByStatut(statut);
	}

	// =================== MÉTHODES PRIVÉES ===================

	private void validerDonneesCommande(CommandeDTO.CreateRequest request) {
		if (request.getLatitudeDepart().equals(request.getLatitudeArrivee()) &&
				request.getLongitudeDepart().equals(request.getLongitudeArrivee())) {
			throw new BusinessException("L'adresse de départ et d'arrivée ne peuvent pas être identiques");
		}

		if (request.getDateRamassageSouhaitee() != null &&
				request.getDateRamassageSouhaitee().isBefore(LocalDateTime.now())) {
			throw new BusinessException("La date de ramassage ne peut pas être dans le passé");
		}

		if (request.getDateLivraisonSouhaitee() != null &&
				request.getDateRamassageSouhaitee() != null &&
				request.getDateLivraisonSouhaitee().isBefore(request.getDateRamassageSouhaitee())) {
			throw new BusinessException("La date de livraison ne peut pas être antérieure à la date de ramassage");
		}
	}

	private void notifierChauffeursDisponibles(Commande commande, CommandeDTO.CreateRequest request) {
		try {
			List<Chauffeur> chauffeursProches = geolocationService.trouverChauffeursProches(
					request.getLatitudeDepart(), request.getLongitudeDepart(), 50.0
			);

			// Filtrer les chauffeurs avec véhicules compatibles
			TypeVehicule typeRequis = determinerTypeVehicule(request.getPoidsMarchandise());
			List<Chauffeur> chauffeursCompatibles = chauffeursProches.stream()
					.filter(c -> c.getVehiculeActuel() != null &&
							c.getVehiculeActuel().getTypeVehicule() == typeRequis)
					.toList();

			if (!chauffeursCompatibles.isEmpty()) {
				notificationService.notifierNouvelleCommande(chauffeursCompatibles, commande);
				log.info("{} chauffeurs notifiés pour la commande {}",
						chauffeursCompatibles.size(), commande.getId());
			} else {
				log.warn("Aucun chauffeur compatible trouvé pour la commande {}", commande.getId());
			}
		} catch (Exception e) {
			log.error("Erreur lors de la notification des chauffeurs pour la commande {}",
					commande.getId(), e);
		}
	}

	private boolean verifierCompatibiliteVehicule(Vehicule vehicule, Commande commande) {
		// Vérifier la capacité de poids
		BigDecimal poidsCommandeEnTonnes = commande.getPoidsMarchandise().divide(BigDecimal.valueOf(1000), 3, RoundingMode.HALF_UP);
		if (vehicule.getCapacitePoids().compareTo(poidsCommandeEnTonnes) < 0) {
			return false;
		}

		// Vérifier la capacité de volume si spécifiée
		if (commande.getVolumeMarchandise() != null && vehicule.getCapaciteVolume() != null) {
			if (vehicule.getCapaciteVolume().compareTo(commande.getVolumeMarchandise()) < 0) {
				return false;
			}
		}

		return true;
	}

	private boolean isTransitionValide(StatutCommande ancien, StatutCommande nouveau) {
		return switch (ancien) {
			case EN_ATTENTE -> nouveau == StatutCommande.ACCEPTEE || nouveau == StatutCommande.ANNULEE;
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
			BigDecimal noteMoyenne = sommeTotale.divide(BigDecimal.valueOf(nombreNotes), 2, RoundingMode.HALF_UP);
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
			BigDecimal noteMoyenne = sommeTotale.divide(BigDecimal.valueOf(nombreNotes), 2, RoundingMode.HALF_UP);
			clientRepository.findById(clientId).ifPresent(client -> {
				client.setNoteMoyenne(noteMoyenne);
				clientRepository.save(client);
			});
		}
	}

	private TypeVehicule determinerTypeVehicule(BigDecimal poids) {
		// Conversion kg en tonnes
		double poidsEnTonnes = poids.doubleValue() / 1000.0;

		if (poidsEnTonnes <= 3.5) return TypeVehicule.CAMIONNETTE;
		if (poidsEnTonnes <= 7.5) return TypeVehicule.CAMION_LEGER;
		if (poidsEnTonnes <= 19.0) return TypeVehicule.CAMION_MOYEN;
		return TypeVehicule.CAMION_LOURD;
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
}