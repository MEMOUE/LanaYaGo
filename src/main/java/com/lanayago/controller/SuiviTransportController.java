package com.lanayago.controller;

import com.lanayago.dto.RechercheTransportDTO;
import com.lanayago.service.SuiviTransportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/suivi-transport")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "📱 Suivi Transport", description = "Suivi temps réel des transports")
public class SuiviTransportController {

	private final SuiviTransportService suiviTransportService;

	@GetMapping("/commande/{commandeId}")
	@Operation(
			summary = "Obtenir le suivi d'une commande",
			description = "Retourne les informations de suivi temps réel d'une commande (position, statut, etc.)"
	)
	@PreAuthorize("hasRole('CLIENT') or hasRole('CHAUFFEUR')")
	public ResponseEntity<RechercheTransportDTO.SuiviTransportResponse> getSuiviCommande(@PathVariable Long commandeId) {
		return ResponseEntity.ok(suiviTransportService.getSuiviCommande(commandeId));
	}

	@PutMapping("/chauffeur/{chauffeurId}/position")
	@Operation(
			summary = "Mettre à jour la position du chauffeur",
			description = "Met à jour la position GPS du chauffeur et notifie les clients en temps réel"
	)
	@PreAuthorize("hasRole('CHAUFFEUR') and #chauffeurId == authentication.principal.id")
	public ResponseEntity<Void> mettreAJourPosition(
			@PathVariable Long chauffeurId,
			@RequestParam Double latitude,
			@RequestParam Double longitude) {
		suiviTransportService.mettreAJourPositionChauffeur(chauffeurId, latitude, longitude);
		return ResponseEntity.ok().build();
	}

	@PutMapping("/chauffeur/{chauffeurId}/statut-connexion")
	@Operation(
			summary = "Mettre à jour le statut de connexion du chauffeur",
			description = "Indique si le chauffeur est en ligne ou hors ligne"
	)
	@PreAuthorize("hasRole('CHAUFFEUR') and #chauffeurId == authentication.principal.id")
	public ResponseEntity<Void> mettreAJourStatutConnexion(
			@PathVariable Long chauffeurId,
			@RequestParam Boolean enLigne) {
		suiviTransportService.mettreAJourStatutConnexion(chauffeurId, enLigne);
		return ResponseEntity.ok().build();
	}

	@PostMapping("/commande/{commandeId}/etape")
	@Operation(
			summary = "Ajouter une étape de suivi",
			description = "Ajoute une nouvelle étape dans le suivi de la commande (ramassage effectué, en route, etc.)"
	)
	@PreAuthorize("hasRole('CHAUFFEUR')")
	public ResponseEntity<Void> ajouterEtapeSuivi(
			@PathVariable Long commandeId,
			@RequestParam String description,
			@RequestParam(required = false) Double latitude,
			@RequestParam(required = false) Double longitude) {
		suiviTransportService.ajouterEtapeSuivi(commandeId, description, latitude, longitude);
		return ResponseEntity.ok().build();
	}

	@GetMapping("/chauffeur/{chauffeurId}/commande-active")
	@Operation(
			summary = "Obtenir la commande active d'un chauffeur",
			description = "Retourne la commande actuellement en cours pour un chauffeur"
	)
	@PreAuthorize("hasRole('CHAUFFEUR') and #chauffeurId == authentication.principal.id")
	public ResponseEntity<RechercheTransportDTO.SuiviTransportResponse> getCommandeActive(@PathVariable Long chauffeurId) {
		return ResponseEntity.ok(suiviTransportService.getCommandeActiveChauffeur(chauffeurId));
	}
}