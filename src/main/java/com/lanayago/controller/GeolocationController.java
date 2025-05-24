package com.lanayago.controller;

import com.lanayago.service.GeolocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/geolocation")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Géolocalisation", description = "Services de géolocalisation et cartographie")
public class GeolocationController {

	private final GeolocationService geolocationService;

	@PutMapping("/chauffeur/{chauffeurId}/position")
	@Operation(summary = "Mettre à jour la position d'un chauffeur")
	@PreAuthorize("hasRole('CHAUFFEUR') and #chauffeurId == authentication.principal.id")
	public ResponseEntity<Void> mettreAJourPosition(
			@PathVariable Long chauffeurId,
			@RequestParam Double latitude,
			@RequestParam Double longitude) {
		geolocationService.mettreAJourPositionChauffeur(chauffeurId, latitude, longitude);
		return ResponseEntity.ok().build();
	}

	@GetMapping("/distance")
	@Operation(summary = "Calculer la distance entre deux points")
	public ResponseEntity<Double> calculerDistance(
			@RequestParam Double lat1,
			@RequestParam Double lon1,
			@RequestParam Double lat2,
			@RequestParam Double lon2) {
		Double distance = geolocationService.calculerDistanceGoogleMaps(lat1, lon1, lat2, lon2);
		return ResponseEntity.ok(distance);
	}
}