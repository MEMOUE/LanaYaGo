package com.lanayago.service;

import com.lanayago.entity.Chauffeur;
import com.lanayago.repository.ChauffeurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeolocationService {

	private final ChauffeurRepository chauffeurRepository;
	private final RestTemplate restTemplate = new RestTemplate();

	@Value("${google.maps.api-key}")
	private String googleMapsApiKey;

	public Double calculerDistance(Double lat1, Double lon1, Double lat2, Double lon2) {
		// Formule de Haversine pour calculer la distance entre deux points
		final int R = 6371; // Rayon de la Terre en kilomètres

		double latDistance = Math.toRadians(lat2 - lat1);
		double lonDistance = Math.toRadians(lon2 - lon1);
		double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
				+ Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
				* Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		double distance = R * c;

		return Math.round(distance * 100.0) / 100.0;
	}

	public Double calculerDistanceGoogleMaps(Double lat1, Double lon1, Double lat2, Double lon2) {
		try {
			String url = String.format(
					"https://maps.googleapis.com/maps/api/distancematrix/json?" +
							"origins=%f,%f&destinations=%f,%f&key=%s&units=metric",
					lat1, lon1, lat2, lon2, googleMapsApiKey
			);

			Map response = restTemplate.getForObject(url, Map.class);
			if (response != null && "OK".equals(response.get("status"))) {
				List rows = (List) response.get("rows");
				if (!rows.isEmpty()) {
					Map row = (Map) rows.get(0);
					List elements = (List) row.get("elements");
					if (!elements.isEmpty()) {
						Map element = (Map) elements.get(0);
						if ("OK".equals(element.get("status"))) {
							Map distance = (Map) element.get("distance");
							return ((Number) distance.get("value")).doubleValue() / 1000.0; // Convertir en km
						}
					}
				}
			}
		} catch (Exception e) {
			log.error("Erreur lors du calcul de distance avec Google Maps", e);
		}

		// Fallback sur le calcul Haversine
		return calculerDistance(lat1, lon1, lat2, lon2);
	}

	public List<Chauffeur> trouverChauffeursProches(Double latitude, Double longitude, Double rayon) {
		return chauffeurRepository.findChauffeursProches(latitude, longitude, rayon);
	}

	public void mettreAJourPositionChauffeur(Long chauffeurId, Double latitude, Double longitude) {
		chauffeurRepository.findById(chauffeurId).ifPresent(chauffeur -> {
			chauffeur.setLatitudeActuelle(latitude);
			chauffeur.setLongitudeActuelle(longitude);
			chauffeurRepository.save(chauffeur);
			log.info("Position mise à jour pour le chauffeur {} : {}, {}",
					chauffeurId, latitude, longitude);
		});
	}
}