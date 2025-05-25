package com.lanayago.controller;

import com.lanayago.dto.VehiculeDTO;
import com.lanayago.service.VehiculeService;
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
@RequestMapping("/api/vehicules")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Véhicules", description = "Gestion des véhicules et flottes")
public class VehiculeController {

	private final VehiculeService vehiculeService;

	@PostMapping("/proprietaire/{proprietaireId}")
	@Operation(summary = "Ajouter un nouveau véhicule")
	@PreAuthorize("hasRole('PROPRIETAIRE_VEHICULE') and #proprietaireId == authentication.principal.id")
	public ResponseEntity<VehiculeDTO.Response> ajouterVehicule(
			@PathVariable Long proprietaireId,
			@Valid @RequestBody VehiculeDTO.CreateRequest request) {
		return ResponseEntity.ok(vehiculeService.ajouterVehicule(proprietaireId, request));
	}

	@GetMapping("/proprietaire/{proprietaireId}")
	@Operation(summary = "Récupérer les véhicules d'un propriétaire")
	@PreAuthorize("hasRole('PROPRIETAIRE_VEHICULE') and #proprietaireId == authentication.principal.id")
	public ResponseEntity<List<VehiculeDTO.Response>> getVehiculesProprietaire(@PathVariable Long proprietaireId) {
		return ResponseEntity.ok(vehiculeService.getVehiculesProprietaire(proprietaireId));
	}

	@GetMapping("/disponibles")
	@Operation(summary = "Récupérer tous les véhicules disponibles")
	@PreAuthorize("hasRole('PROPRIETAIRE_VEHICULE') or hasRole('CHAUFFEUR')")
	public ResponseEntity<List<VehiculeDTO.Response>> getVehiculesDisponibles() {
		return ResponseEntity.ok(vehiculeService.getVehiculesDisponibles());
	}

	@PutMapping("/{vehiculeId}/disponibilite")
	@Operation(summary = "Changer la disponibilité d'un véhicule")
	@PreAuthorize("hasRole('PROPRIETAIRE_VEHICULE') or hasRole('CHAUFFEUR')")
	public ResponseEntity<Void> changerDisponibilite(
			@PathVariable Long vehiculeId,
			@RequestParam Boolean disponible) {
		vehiculeService.changerDisponibilite(vehiculeId, disponible);
		return ResponseEntity.ok().build();
	}
}