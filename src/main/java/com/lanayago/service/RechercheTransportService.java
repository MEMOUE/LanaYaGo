package com.lanayago.service;

import com.lanayago.dto.RechercheTransportDTO;
import com.lanayago.dto.UserDTO;
import com.lanayago.dto.VehiculeDTO;
import com.lanayago.entity.*;
import com.lanayago.enums.TypeVehicule;
import com.lanayago.exception.BusinessException;
import com.lanayago.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class RechercheTransportService {

	private final RechercheTransportRepository rechercheTransportRepository;
	private final ClientRepository clientRepository;
	private final ChauffeurRepository chauffeurRepository;
	private final VehiculeRepository vehiculeRepository;
	private final GeolocationService geolocationService;
	private final TarificationService tarificationService;
	private final UserMapperService userMapperService;
	private final SimpMessagingTemplate messagingTemplate;

	@Transactional
	public RechercheTransportDTO.RechercheResponse rechercherTransport(
			Long clientId,
			RechercheTransportDTO.RechercheRequest request) {

		log.info("Recherche de transport pour le client: {}", clientId);

		Client client = clientRepository.findById(clientId)
				.orElseThrow(() -> new BusinessException("Client non trouvé"));

		// Calcul de la distance
		Double distance = geolocationService.calculerDistanceGoogleMaps(
				request.getLatitudeDepart(), request.getLongitudeDepart(),
				request.getLatitudeArrivee(), request.getLongitudeArrivee()
		);

		if (distance == null || distance <= 0) {
			throw new BusinessException("Impossible de calculer la distance entre les deux points");
		}

		// Détermination du type de véhicule recommandé
		TypeVehicule typeRecommande = determinerTypeVehicule(request.getPoidsMarchandise());

		// Calcul du tarif estimé
		BigDecimal tarifEstime = tarificationService.calculerTarif(
				convertToCommandeRequest(request), distance, typeRecommande
		);

		// Génération d'un ID de session pour le suivi temps réel
		String sessionId = UUID.randomUUID().toString();

		// Sauvegarde de la recherche
		RechercheTransport recherche = new RechercheTransport();
		recherche.setClient(client);
		recherche.setLatitudeDepart(request.getLatitudeDepart());
		recherche.setLongitudeDepart(request.getLongitudeDepart());
		recherche.setAdresseDepart(request.getAdresseDepart());
		recherche.setLatitudeArrivee(request.getLatitudeArrivee());
		recherche.setLongitudeArrivee(request.getLongitudeArrivee());
		recherche.setAdresseArrivee(request.getAdresseArrivee());
		recherche.setPoidsMarchandise(request.getPoidsMarchandise());
		recherche.setVolumeMarchandise(request.getVolumeMarchandise());
		recherche.setDescriptionMarchandise(request.getDescriptionMarchandise());
		recherche.setTypeVehiculeRecommande(typeRecommande);
		recherche.setDistance(distance);
		recherche.setTarifEstime(tarifEstime);
		recherche.setUrgent(request.getUrgent());
		recherche.setDateRamassageSouhaitee(request.getDateRamassageSouhaitee());
		recherche.setDateLivraisonSouhaitee(request.getDateLivraisonSouhaitee());
		recherche.setSessionId(sessionId);

		recherche = rechercheTransportRepository.save(recherche);

		// Recherche des véhicules disponibles en temps réel
		List<RechercheTransportDTO.VehiculeDisponible> vehiculesDisponibles =
				rechercherVehiculesDisponibles(request, typeRecommande);

		// Notification aux chauffeurs compatibles
		RechercheTransport finalRecherche = recherche;
		CompletableFuture.runAsync(() -> notifierChauffeursCompatibles(finalRecherche, vehiculesDisponibles));

		RechercheTransportDTO.RechercheResponse response = new RechercheTransportDTO.RechercheResponse();
		response.setRechercheId(recherche.getId());
		response.setTypeVehiculeRecommande(typeRecommande);
		response.setDistance(distance);
		response.setTarifEstime(tarifEstime);
		response.setVehiculesDisponibles(vehiculesDisponibles);
		response.setSessionId(sessionId);

		log.info("Recherche de transport terminée: {} véhicules trouvés", vehiculesDisponibles.size());
		return response;
	}

	@Transactional(readOnly = true)
	public List<RechercheTransportDTO.VehiculeDisponible> rechercherVehiculesDisponibles(
			RechercheTransportDTO.RechercheRequest request,
			TypeVehicule typeRecommande) {

		List<RechercheTransportDTO.VehiculeDisponible> vehiculesDisponibles = new ArrayList<>();

		// Recherche des chauffeurs proches avec véhicules compatibles
		List<Chauffeur> chauffeursProches = geolocationService.trouverChauffeursProches(
				request.getLatitudeDepart(),
				request.getLongitudeDepart(),
				request.getRayonRecherche()
		);

		for (Chauffeur chauffeur : chauffeursProches) {
			if (chauffeur.getVehiculeActuel() != null &&
					chauffeur.getVehiculeActuel().getDisponible() &&
					isVehiculeCompatible(chauffeur.getVehiculeActuel(), request.getPoidsMarchandise(), request.getVolumeMarchandise())) {

				Vehicule vehicule = chauffeur.getVehiculeActuel();

				// Calcul de la distance depuis le point de départ
				Double distanceDepuisDepart = geolocationService.calculerDistance(
						request.getLatitudeDepart(), request.getLongitudeDepart(),
						chauffeur.getLatitudeActuelle(), chauffeur.getLongitudeActuelle()
				);

				// Estimation du temps d'arrivée (vitesse moyenne 50 km/h en ville)
				Integer tempsEstimeArrivee = (int) Math.ceil(distanceDepuisDepart / 50.0 * 60);

				RechercheTransportDTO.VehiculeDisponible vehiculeDisponible = new RechercheTransportDTO.VehiculeDisponible();
				vehiculeDisponible.setVehiculeId(vehicule.getId());
				vehiculeDisponible.setChauffeurId(chauffeur.getId());
				vehiculeDisponible.setVehicule(mapVehiculeToDTO(vehicule));
				vehiculeDisponible.setChauffeur(userMapperService.toDTO(chauffeur));
				vehiculeDisponible.setLatitudeActuelle(chauffeur.getLatitudeActuelle());
				vehiculeDisponible.setLongitudeActuelle(chauffeur.getLongitudeActuelle());
				vehiculeDisponible.setDistanceDepuisDepart(distanceDepuisDepart);
				vehiculeDisponible.setTempsEstimeArrivee(tempsEstimeArrivee);
				vehiculeDisponible.setNoteChauffeur(chauffeur.getNoteMoyenne());
				vehiculeDisponible.setNombreCourses(chauffeur.getNombreCourses());
				vehiculeDisponible.setDisponibleImmediatement(tempsEstimeArrivee <= 30); // Disponible si < 30 min

				vehiculesDisponibles.add(vehiculeDisponible);
			}
		}

		// Tri par distance croissante
		vehiculesDisponibles.sort((v1, v2) ->
				Double.compare(v1.getDistanceDepuisDepart(), v2.getDistanceDepuisDepart()));

		return vehiculesDisponibles;
	}

	@Transactional
	public void mettreAJourRechercheTempReel(String sessionId) {
		rechercheTransportRepository.findBySessionId(sessionId).ifPresent(recherche -> {
			if (recherche.getActive()) {
				// Recherche mise à jour des véhicules disponibles
				RechercheTransportDTO.RechercheRequest request = new RechercheTransportDTO.RechercheRequest();
				request.setLatitudeDepart(recherche.getLatitudeDepart());
				request.setLongitudeDepart(recherche.getLongitudeDepart());
				request.setLatitudeArrivee(recherche.getLatitudeArrivee());
				request.setLongitudeArrivee(recherche.getLongitudeArrivee());
				request.setPoidsMarchandise(recherche.getPoidsMarchandise());
				request.setVolumeMarchandise(recherche.getVolumeMarchandise());
				request.setRayonRecherche(50.0);

				List<RechercheTransportDTO.VehiculeDisponible> vehiculesDisponibles =
						rechercherVehiculesDisponibles(request, recherche.getTypeVehiculeRecommande());

				// Notification au client avec la mise à jour
				messagingTemplate.convertAndSend(
						"/topic/client/" + recherche.getClient().getId() + "/recherche",
						createRechercheUpdateNotification(recherche.getId(), vehiculesDisponibles)
				);
			}
		});
	}

	@Transactional
	public void desactiverRecherche(Long rechercheId) {
		rechercheTransportRepository.findById(rechercheId).ifPresent(recherche -> {
			recherche.setActive(false);
			rechercheTransportRepository.save(recherche);
		});
	}

	private void notifierChauffeursCompatibles(
			RechercheTransport recherche,
			List<RechercheTransportDTO.VehiculeDisponible> vehiculesDisponibles) {

		for (RechercheTransportDTO.VehiculeDisponible vehicule : vehiculesDisponibles) {
			try {
				messagingTemplate.convertAndSend(
						"/topic/chauffeur/" + vehicule.getChauffeurId() + "/nouvelles-recherches",
						createNouvelleRechercheNotification(recherche)
				);
			} catch (Exception e) {
				log.error("Erreur lors de la notification du chauffeur {}", vehicule.getChauffeurId(), e);
			}
		}
	}

	private boolean isVehiculeCompatible(Vehicule vehicule, BigDecimal poids, BigDecimal volume) {
		// Vérifier la capacité de poids (conversion kg -> tonnes)
		BigDecimal poidsEnTonnes = poids.divide(BigDecimal.valueOf(1000), 3, RoundingMode.HALF_UP);
		if (vehicule.getCapacitePoids().compareTo(poidsEnTonnes) < 0) {
			return false;
		}

		// Vérifier la capacité de volume si spécifiée
		if (volume != null && vehicule.getCapaciteVolume() != null) {
			if (vehicule.getCapaciteVolume().compareTo(volume) < 0) {
				return false;
			}
		}

		return true;
	}

	private TypeVehicule determinerTypeVehicule(BigDecimal poids) {
		double poidsEnTonnes = poids.doubleValue() / 1000.0;

		if (poidsEnTonnes <= 3.5) return TypeVehicule.CAMIONNETTE;
		if (poidsEnTonnes <= 7.5) return TypeVehicule.CAMION_LEGER;
		if (poidsEnTonnes <= 19.0) return TypeVehicule.CAMION_MOYEN;
		return TypeVehicule.CAMION_LOURD;
	}

	private com.lanayago.dto.CommandeDTO.CreateRequest convertToCommandeRequest(
			RechercheTransportDTO.RechercheRequest request) {

		com.lanayago.dto.CommandeDTO.CreateRequest commandeRequest = new com.lanayago.dto.CommandeDTO.CreateRequest();
		commandeRequest.setLatitudeDepart(request.getLatitudeDepart());
		commandeRequest.setLongitudeDepart(request.getLongitudeDepart());
		commandeRequest.setAdresseDepart(request.getAdresseDepart());
		commandeRequest.setLatitudeArrivee(request.getLatitudeArrivee());
		commandeRequest.setLongitudeArrivee(request.getLongitudeArrivee());
		commandeRequest.setAdresseArrivee(request.getAdresseArrivee());
		commandeRequest.setPoidsMarchandise(request.getPoidsMarchandise());
		commandeRequest.setVolumeMarchandise(request.getVolumeMarchandise());
		commandeRequest.setDescriptionMarchandise(request.getDescriptionMarchandise());
		commandeRequest.setUrgent(request.getUrgent());
		commandeRequest.setDateRamassageSouhaitee(request.getDateRamassageSouhaitee());
		commandeRequest.setDateLivraisonSouhaitee(request.getDateLivraisonSouhaitee());

		return commandeRequest;
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

	private Object createRechercheUpdateNotification(Long rechercheId, List<RechercheTransportDTO.VehiculeDisponible> vehicules) {
		return new Object() {
			public String getType() { return "RECHERCHE_UPDATE"; }
			public Long getRechercheId() { return rechercheId; }
			public List<RechercheTransportDTO.VehiculeDisponible> getVehiculesDisponibles() { return vehicules; }
			public String getMessage() { return "Mise à jour des véhicules disponibles"; }
		};
	}

	private Object createNouvelleRechercheNotification(RechercheTransport recherche) {
		return new Object() {
			public String getType() { return "NOUVELLE_RECHERCHE"; }
			public Long getRechercheId() { return recherche.getId(); }
			public String getAdresseDepart() { return recherche.getAdresseDepart(); }
			public String getAdresseArrivee() { return recherche.getAdresseArrivee(); }
			public BigDecimal getTarifEstime() { return recherche.getTarifEstime(); }
			public String getMessage() { return "Nouvelle demande de transport disponible"; }
		};
	}
}