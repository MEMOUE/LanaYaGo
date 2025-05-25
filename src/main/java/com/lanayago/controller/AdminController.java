package com.lanayago.controller;

import com.lanayago.dto.DocumentDTO;
import com.lanayago.enums.StatutDocument;
import com.lanayago.service.AdminService;
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
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Administration", description = "Fonctionnalités d'administration")
@PreAuthorize("hasRole('PROPRIETAIRE_VEHICULE')")
public class AdminController {

	private final AdminService adminService;

	@GetMapping("/statistiques")
	@Operation(summary = "Récupérer les statistiques globales")
	public ResponseEntity<Map<String, Object>> getStatistiques() {
		return ResponseEntity.ok(adminService.getStatistiquesGlobales());
	}

	@GetMapping("/utilisateurs")
	@Operation(summary = "Lister tous les utilisateurs")
	public ResponseEntity<List<Object>> getUtilisateurs(
			@RequestParam(required = false) String type,
			@RequestParam(required = false) Boolean actif,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ResponseEntity.ok(adminService.getUtilisateurs(type, actif, page, size));
	}

	@PutMapping("/utilisateurs/{userId}/statut")
	@Operation(summary = "Changer le statut d'un utilisateur")
	public ResponseEntity<Void> changerStatutUtilisateur(
			@PathVariable Long userId,
			@RequestParam Boolean actif) {
		adminService.changerStatutUtilisateur(userId, actif);
		return ResponseEntity.ok().build();
	}

	@GetMapping("/documents")
	@Operation(summary = "Lister tous les documents")
	public ResponseEntity<List<DocumentDTO.Response>> getDocuments(
			@RequestParam(required = false) StatutDocument statut,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ResponseEntity.ok(adminService.getDocuments(statut, page, size));
	}

	@PutMapping("/documents/{documentId}/valider")
	@Operation(summary = "Valider ou refuser un document")
	public ResponseEntity<DocumentDTO.Response> validerDocument(
			@PathVariable Long documentId,
			@Valid @RequestBody DocumentDTO.ValidationRequest request) {
		return ResponseEntity.ok(adminService.validerDocument(documentId, request));
	}

	@GetMapping("/revenus")
	@Operation(summary = "Récupérer les statistiques de revenus")
	public ResponseEntity<Map<String, Object>> getStatistiquesRevenus(
			@RequestParam(required = false) String periode) {
		return ResponseEntity.ok(adminService.getStatistiquesRevenus(periode));
	}
}