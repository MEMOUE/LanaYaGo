package com.lanayago.controller;

import com.lanayago.dto.RechercheTransportDTO;
import com.lanayago.service.RechercheTransportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recherche-transport")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "🔍 Recherche Transport", description = "Recherche de moyens de transport en temps réel")
public class RechercheTransportController {

	private final RechercheTransportService rechercheTransportService;

	@PostMapping("/client/{clientId}")
	@Operation(
			summary = "Rechercher un moyen de transport",
			description = "Recherche en temps réel des véhicules disponibles selon les critères de poids et volume"
	)
	@PreAuthorize("hasRole('CLIENT') and #clientId == authentication.principal.id")
	public ResponseEntity<RechercheTransportDTO.RechercheResponse> rechercherTransport(
			@PathVariable Long clientId,
			@Valid @RequestBody RechercheTransportDTO.RechercheRequest request) {
		return ResponseEntity.ok(rechercheTransportService.rechercherTransport(clientId, request));
	}

	@GetMapping("/vehicules-disponibles")
	@Operation(
			summary = "Obtenir la liste des véhicules disponibles",
			description = "Retourne la liste mise à jour des véhicules disponibles pour une recherche"
	)
	@PreAuthorize("hasRole('CLIENT')")
	public ResponseEntity<List<RechercheTransportDTO.VehiculeDisponible>> getVehiculesDisponibles(
			@RequestParam Double latitudeDepart,
			@RequestParam Double longitudeDepart,
			@RequestParam Double latitudeArrivee,
			@RequestParam Double longitudeArrivee,
			@RequestParam Double poidsMarchandise,
			@RequestParam(required = false) Double volumeMarchandise,
			@RequestParam(defaultValue = "50.0") Double rayonRecherche) {

		RechercheTransportDTO.RechercheRequest request = new RechercheTransportDTO.RechercheRequest();
		request.setLatitudeDepart(latitudeDepart);
		request.setLongitudeDepart(longitudeDepart);
		request.setLatitudeArrivee(latitudeArrivee);
		request.setLongitudeArrivee(longitudeArrivee);
		request.setPoidsMarchandise(java.math.BigDecimal.valueOf(poidsMarchandise));
		if (volumeMarchandise != null) {
			request.setVolumeMarchandise(java.math.BigDecimal.valueOf(volumeMarchandise));
		}
		request.setRayonRecherche(rayonRecherche);

		return ResponseEntity.ok(rechercheTransportService.rechercherVehiculesDisponibles(
				request,
				null // Le service déterminera automatiquement le type
		));
	}

	@PutMapping("/session/{sessionId}/update")
	@Operation(
			summary = "Mettre à jour une recherche en temps réel",
			description = "Met à jour la liste des véhicules disponibles pour une session de recherche active"
	)
	@PreAuthorize("hasRole('CLIENT')")
	public ResponseEntity<Void> mettreAJourRecherche(@PathVariable String sessionId) {
		rechercheTransportService.mettreAJourRechercheTempReel(sessionId);
		return ResponseEntity.ok().build();
	}

	@PutMapping("/{rechercheId}/desactiver")
	@Operation(
			summary = "Désactiver une recherche",
			description = "Désactive une recherche de transport (arrête les notifications temps réel)"
	)
	@PreAuthorize("hasRole('CLIENT')")
	public ResponseEntity<Void> desactiverRecherche(@PathVariable Long rechercheId) {
		rechercheTransportService.desactiverRecherche(rechercheId);
		return ResponseEntity.ok().build();
	}
}