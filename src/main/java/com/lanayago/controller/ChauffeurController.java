package com.lanayago.controller;

import com.lanayago.dto.UserDTO;
import com.lanayago.service.ChauffeurService;
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
@RequestMapping("/api/chauffeurs")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "🚗 Gestion Chauffeurs", description = "Gestion des chauffeurs par les propriétaires de véhicules")
public class ChauffeurController {

	private final ChauffeurService chauffeurService;

	@PostMapping("/proprietaire/{proprietaireId}")
	@Operation(
			summary = "Créer un nouveau chauffeur",
			description = "Le propriétaire crée un nouveau chauffeur pour son entreprise"
	)
	@PreAuthorize("hasRole('PROPRIETAIRE_VEHICULE') and #proprietaireId == authentication.principal.id")
	public ResponseEntity<UserDTO> creerChauffeur(
			@PathVariable Long proprietaireId,
			@Valid @RequestBody ChauffeurService.ChauffeurDTO.CreateRequest request) {
		return ResponseEntity.ok(chauffeurService.creerChauffeur(proprietaireId, request));
	}

	@PutMapping("/proprietaire/{proprietaireId}/chauffeur/{chauffeurId}")
	@Operation(
			summary = "Mettre à jour un chauffeur",
			description = "Le propriétaire met à jour les informations d'un de ses chauffeurs"
	)
	@PreAuthorize("hasRole('PROPRIETAIRE_VEHICULE') and #proprietaireId == authentication.principal.id")
	public ResponseEntity<UserDTO> mettreAJourChauffeur(
			@PathVariable Long proprietaireId,
			@PathVariable Long chauffeurId,
			@Valid @RequestBody ChauffeurService.ChauffeurDTO.CreateRequest request) {
		return ResponseEntity.ok(chauffeurService.mettreAJourChauffeur(proprietaireId, chauffeurId, request));
	}

	@PutMapping("/proprietaire/{proprietaireId}/chauffeur/{chauffeurId}/vehicule/{vehiculeId}")
	@Operation(
			summary = "Affecter un véhicule à un chauffeur",
			description = "Le propriétaire affecte un de ses véhicules à un de ses chauffeurs"
	)
	@PreAuthorize("hasRole('PROPRIETAIRE_VEHICULE') and #proprietaireId == authentication.principal.id")
	public ResponseEntity<Void> affecterVehicule(
			@PathVariable Long proprietaireId,
			@PathVariable Long chauffeurId,
			@PathVariable Long vehiculeId) {
		chauffeurService.affecterVehicule(proprietaireId, chauffeurId, vehiculeId);
		return ResponseEntity.ok().build();
	}

	@PutMapping("/proprietaire/{proprietaireId}/chauffeur/{chauffeurId}/liberer-vehicule")
	@Operation(
			summary = "Libérer le véhicule d'un chauffeur",
			description = "Le propriétaire libère le véhicule actuellement affecté à un chauffeur"
	)
	@PreAuthorize("hasRole('PROPRIETAIRE_VEHICULE') and #proprietaireId == authentication.principal.id")
	public ResponseEntity<Void> libererVehicule(
			@PathVariable Long proprietaireId,
			@PathVariable Long chauffeurId) {
		chauffeurService.libererVehicule(proprietaireId, chauffeurId);
		return ResponseEntity.ok().build();
	}

	@PutMapping("/{chauffeurId}/disponibilite")
	@Operation(
			summary = "Changer la disponibilité d'un chauffeur",
			description = "Le chauffeur ou le propriétaire change le statut de disponibilité"
	)
	@PreAuthorize("hasRole('CHAUFFEUR') and #chauffeurId == authentication.principal.id or hasRole('PROPRIETAIRE_VEHICULE')")
	public ResponseEntity<Void> changerDisponibilite(
			@PathVariable Long chauffeurId,
			@RequestParam Boolean disponible) {
		chauffeurService.changerDisponibilite(chauffeurId, disponible);
		return ResponseEntity.ok().build();
	}

	@DeleteMapping("/proprietaire/{proprietaireId}/chauffeur/{chauffeurId}")
	@Operation(
			summary = "Supprimer un chauffeur",
			description = "Le propriétaire désactive un de ses chauffeurs (préserve l'historique)"
	)
	@PreAuthorize("hasRole('PROPRIETAIRE_VEHICULE') and #proprietaireId == authentication.principal.id")
	public ResponseEntity<Void> supprimerChauffeur(
			@PathVariable Long proprietaireId,
			@PathVariable Long chauffeurId) {
		chauffeurService.supprimerChauffeur(proprietaireId, chauffeurId);
		return ResponseEntity.ok().build();
	}

	// === ENDPOINTS DE CONSULTATION ===

	@GetMapping("/proprietaire/{proprietaireId}")
	@Operation(
			summary = "Récupérer les chauffeurs d'un propriétaire",
			description = "Liste de tous les chauffeurs appartenant à un propriétaire"
	)
	@PreAuthorize("hasRole('PROPRIETAIRE_VEHICULE') and #proprietaireId == authentication.principal.id")
	public ResponseEntity<List<UserDTO>> getChauffeursProprietaire(@PathVariable Long proprietaireId) {
		return ResponseEntity.ok(chauffeurService.getChauffeursProprietaire(proprietaireId));
	}

	@GetMapping("/disponibles")
	@Operation(
			summary = "Récupérer tous les chauffeurs disponibles",
			description = "Liste des chauffeurs actuellement disponibles pour les commandes"
	)
	@PreAuthorize("hasRole('CLIENT') or hasRole('PROPRIETAIRE_VEHICULE')")
	public ResponseEntity<List<UserDTO>> getChauffeursDisponibles() {
		return ResponseEntity.ok(chauffeurService.getChauffeursDisponibles());
	}
}