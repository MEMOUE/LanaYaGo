package com.lanayago.controller;

import com.lanayago.dto.DemandeProprietaireDTO;
import com.lanayago.service.DemandeProprietaireService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/demandes-proprietaire")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "📋 Demandes Propriétaire", description = "Gestion des demandes pour devenir propriétaire de véhicules")
public class DemandeProprietaireController {

	private final DemandeProprietaireService demandeProprietaireService;

	@PostMapping("/user/{userId}")
	@Operation(
			summary = "Créer une demande pour devenir propriétaire",
			description = "Permet à un client de faire une demande pour devenir propriétaire de véhicules"
	)
	@PreAuthorize("hasRole('CLIENT') and #userId == authentication.principal.id")
	public ResponseEntity<DemandeProprietaireDTO.Response> creerDemande(
			@PathVariable Long userId,
			@Valid @RequestBody DemandeProprietaireDTO.CreateRequest request) {
		return ResponseEntity.ok(demandeProprietaireService.creerDemande(userId, request));
	}

	@PostMapping("/{demandeId}/documents/{documentType}")
	@Operation(
			summary = "Uploader un document pour la demande",
			description = "Types de documents acceptés: piece_identite, extrait, justificatif_adresse"
	)
	@PreAuthorize("hasRole('CLIENT')")
	public ResponseEntity<DemandeProprietaireDTO.DocumentUploadResponse> uploadDocument(
			@PathVariable Long demandeId,
			@PathVariable String documentType,
			@RequestParam("file") MultipartFile file) {
		return ResponseEntity.ok(demandeProprietaireService.uploadDocument(demandeId, documentType, file));
	}

	@GetMapping("/user/{userId}")
	@Operation(summary = "Récupérer les demandes d'un utilisateur")
	@PreAuthorize("hasRole('CLIENT') and #userId == authentication.principal.id")
	public ResponseEntity<List<DemandeProprietaireDTO.Response>> getDemandesUtilisateur(@PathVariable Long userId) {
		return ResponseEntity.ok(demandeProprietaireService.getDemandesUtilisateur(userId));
	}

	@GetMapping("/{demandeId}")
	@Operation(summary = "Récupérer une demande par son ID")
	@PreAuthorize("hasRole('CLIENT') or hasRole('ADMIN')")
	public ResponseEntity<DemandeProprietaireDTO.Response> getDemandeById(@PathVariable Long demandeId) {
		return ResponseEntity.ok(demandeProprietaireService.getDemandeById(demandeId));
	}

	// === ENDPOINTS ADMINISTRATEUR ===

	@GetMapping("/admin/en-attente")
	@Operation(summary = "Récupérer toutes les demandes en attente (Admin)")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<List<DemandeProprietaireDTO.Response>> getDemandesEnAttente() {
		return ResponseEntity.ok(demandeProprietaireService.getDemandesEnAttente());
	}

	@PutMapping("/admin/{demandeId}/traiter")
	@Operation(summary = "Traiter une demande (Admin)")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<DemandeProprietaireDTO.Response> traiterDemande(
			@PathVariable Long demandeId,
			@Valid @RequestBody DemandeProprietaireDTO.TraitementRequest request,
			@RequestParam Long adminId) {
		return ResponseEntity.ok(demandeProprietaireService.traiterDemande(demandeId, request, adminId));
	}
}