package com.lanayago.controller;

import com.lanayago.dto.CommandeDTO;
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
import java.util.Map;

@RestController
@RequestMapping("/api/chauffeurs")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Chauffeurs", description = "Fonctionnalités spécifiques aux chauffeurs")
public class ChauffeurController {

	private final ChauffeurService chauffeurService;

	@GetMapping("/{chauffeurId}/profil")
	@Operation(summary = "Récupérer le profil du chauffeur")
	@PreAuthorize("hasRole('CHAUFFEUR') and #chauffeurId == authentication.principal.id")
	public ResponseEntity<UserDTO> getProfil(@PathVariable Long chauffeurId) {
		return ResponseEntity.ok(chauffeurService.getProfilChauffeur(chauffeurId));
	}

	@PutMapping("/{chauffeurId}/profil")
	@Operation(summary = "Mettre à jour le profil du chauffeur")
	@PreAuthorize("hasRole('CHAUFFEUR') and #chauffeurId == authentication.principal.id")
	public ResponseEntity<UserDTO> mettreAJourProfil(
			@PathVariable Long chauffeurId,
			@Valid @RequestBody UserDTO request) {
		return ResponseEntity.ok(chauffeurService.mettreAJourProfil(chauffeurId, request));
	}

	@PutMapping("/{chauffeurId}/disponibilite")
	@Operation(summary = "Changer la disponibilité du chauffeur")
	@PreAuthorize("hasRole('CHAUFFEUR') and #chauffeurId == authentication.principal.id")
	public ResponseEntity<Void> changerDisponibilite(
			@PathVariable Long chauffeurId,
			@RequestParam Boolean disponible) {
		chauffeurService.changerDisponibilite(chauffeurId, disponible);
		return ResponseEntity.ok().build();
	}

	@GetMapping("/{chauffeurId}/courses")
	@Operation(summary = "Récupérer l'historique des courses")
	@PreAuthorize("hasRole('CHAUFFEUR') and #chauffeurId == authentication.principal.id")
	public ResponseEntity<List<CommandeDTO.Response>> getHistoriqueCourses(
			@PathVariable Long chauffeurId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {
		return ResponseEntity.ok(chauffeurService.getHistoriqueCourses(chauffeurId, page, size));
	}

	@GetMapping("/{chauffeurId}/revenus")
	@Operation(summary = "Récupérer les revenus du chauffeur")
	@PreAuthorize("hasRole('CHAUFFEUR') and #chauffeurId == authentication.principal.id")
	public ResponseEntity<Map<String, Object>> getRevenus(
			@PathVariable Long chauffeurId,
			@RequestParam(required = false) String periode) {
		return ResponseEntity.ok(chauffeurService.getRevenus(chauffeurId, periode));
	}

	@GetMapping("/{chauffeurId}/statistiques")
	@Operation(summary = "Récupérer les statistiques du chauffeur")
	@PreAuthorize("hasRole('CHAUFFEUR') and #chauffeurId == authentication.principal.id")
	public ResponseEntity<Map<String, Object>> getStatistiques(@PathVariable Long chauffeurId) {
		return ResponseEntity.ok(chauffeurService.getStatistiquesChauffeur(chauffeurId));
	}

	@PostMapping("/{chauffeurId}/signalement")
	@Operation(summary = "Signaler un problème")
	@PreAuthorize("hasRole('CHAUFFEUR') and #chauffeurId == authentication.principal.id")
	public ResponseEntity<Void> signalerProbleme(
			@PathVariable Long chauffeurId,
			@RequestBody Map<String, String> signalement) {
		chauffeurService.signalerProbleme(chauffeurId, signalement);
		return ResponseEntity.ok().build();
	}
}