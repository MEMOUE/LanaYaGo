package com.lanayago.controller;

import com.lanayago.dto.DocumentDTO;
import com.lanayago.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Documents", description = "Gestion des documents")
public class DocumentController {

	private final DocumentService documentService;

	@PostMapping("/upload/{userId}")
	@Operation(summary = "Uploader un document")
	@PreAuthorize("hasRole('CLIENT') or hasRole('CHAUFFEUR') or hasRole('PROPRIETAIRE_VEHICULE')")
	public ResponseEntity<DocumentDTO.Response> uploadDocument(
			@PathVariable Long userId,
			@RequestParam("file") MultipartFile file,
			@Valid @ModelAttribute DocumentDTO.UploadRequest request) {
		return ResponseEntity.ok(documentService.uploadDocument(userId, file, request));
	}

	@GetMapping("/utilisateur/{userId}")
	@Operation(summary = "Récupérer les documents d'un utilisateur")
	@PreAuthorize("hasRole('CLIENT') or hasRole('CHAUFFEUR') or hasRole('PROPRIETAIRE_VEHICULE')")
	public ResponseEntity<List<DocumentDTO.Response>> getDocumentsUtilisateur(@PathVariable Long userId) {
		return ResponseEntity.ok(documentService.getDocumentsUtilisateur(userId));
	}

	@GetMapping("/{documentId}/download")
	@Operation(summary = "Télécharger un document")
	@PreAuthorize("hasRole('CLIENT') or hasRole('CHAUFFEUR') or hasRole('PROPRIETAIRE_VEHICULE')")
	public ResponseEntity<Resource> downloadDocument(@PathVariable Long documentId) {
		return documentService.downloadDocument(documentId);
	}

	@DeleteMapping("/{documentId}")
	@Operation(summary = "Supprimer un document")
	@PreAuthorize("hasRole('CLIENT') or hasRole('CHAUFFEUR') or hasRole('PROPRIETAIRE_VEHICULE')")
	public ResponseEntity<Void> supprimerDocument(@PathVariable Long documentId) {
		documentService.supprimerDocument(documentId);
		return ResponseEntity.ok().build();
	}

	@GetMapping("/types-requis/{typeUtilisateur}")
	@Operation(summary = "Récupérer les types de documents requis")
	public ResponseEntity<List<String>> getTypesDocumentsRequis(@PathVariable String typeUtilisateur) {
		return ResponseEntity.ok(documentService.getTypesDocumentsRequis(typeUtilisateur));
	}
}