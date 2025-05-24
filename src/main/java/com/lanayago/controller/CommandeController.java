package com.lanayago.controller;

import com.lanayago.dto.CommandeDTO;
import com.lanayago.enums.StatutCommande;
import com.lanayago.service.CommandeService;
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
@RequestMapping("/api/commandes")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Commandes", description = "Gestion des commandes de transport")
public class CommandeController {

	private final CommandeService commandeService;

	@PostMapping("/client/{clientId}")
	@Operation(summary = "Créer une nouvelle commande")
	@PreAuthorize("hasRole('CLIENT') and #clientId == authentication.principal.id")
	public ResponseEntity<CommandeDTO.Response> creerCommande(
			@PathVariable Long clientId,
			@Valid @RequestBody CommandeDTO.CreateRequest request) {
		return ResponseEntity.ok(commandeService.creerCommande(clientId, request));
	}

	@PutMapping("/{commandeId}/accepter")
	@Operation(summary = "Accepter une commande (chauffeur)")
	@PreAuthorize("hasRole('CHAUFFEUR')")
	public ResponseEntity<CommandeDTO.Response> accepterCommande(
			@PathVariable Long commandeId,
			@RequestParam Long chauffeurId) {
		return ResponseEntity.ok(commandeService.accepterCommande(commandeId, chauffeurId));
	}

	@PutMapping("/{commandeId}/statut")
	@Operation(summary = "Changer le statut d'une commande")
	@PreAuthorize("hasRole('CHAUFFEUR') or hasRole('CLIENT')")
	public ResponseEntity<CommandeDTO.Response> changerStatut(
			@PathVariable Long commandeId,
			@RequestParam StatutCommande statut) {
		return ResponseEntity.ok(commandeService.changerStatut(commandeId, statut));
	}

	@PostMapping("/{commandeId}/evaluer")
	@Operation(summary = "Évaluer une commande")
	@PreAuthorize("hasRole('CLIENT') or hasRole('CHAUFFEUR')")
	public ResponseEntity<CommandeDTO.Response> evaluerCommande(
			@PathVariable Long commandeId,
			@Valid @RequestBody CommandeDTO.EvaluationRequest request) {
		return ResponseEntity.ok(commandeService.evaluerCommande(
				commandeId,
				request.getNote(),
				request.getCommentaire(),
				request.getTypeEvaluateur()
		));
	}

	@GetMapping("/client/{clientId}")
	@Operation(summary = "Récupérer les commandes d'un client")
	@PreAuthorize("hasRole('CLIENT') and #clientId == authentication.principal.id")
	public ResponseEntity<List<CommandeDTO.Response>> getCommandesClient(@PathVariable Long clientId) {
		return ResponseEntity.ok(commandeService.getCommandesClient(clientId));
	}

	@GetMapping("/chauffeur/{chauffeurId}")
	@Operation(summary = "Récupérer les commandes d'un chauffeur")
	@PreAuthorize("hasRole('CHAUFFEUR') and #chauffeurId == authentication.principal.id")
	public ResponseEntity<List<CommandeDTO.Response>> getCommandesChauffeur(@PathVariable Long chauffeurId) {
		return ResponseEntity.ok(commandeService.getCommandesChauffeur(chauffeurId));
	}

	@GetMapping("/{commandeId}")
	@Operation(summary = "Récupérer le détail d'une commande")
	@PreAuthorize("hasRole('CLIENT') or hasRole('CHAUFFEUR') or hasRole('PROPRIETAIRE_VEHICULE')")
	public ResponseEntity<CommandeDTO.Response> getCommande(@PathVariable Long commandeId) {
		return ResponseEntity.ok(commandeService.getCommandeById(commandeId));
	}

	@GetMapping("/statut/{statut}")
	@Operation(summary = "Récupérer les commandes par statut")
	@PreAuthorize("hasRole('CHAUFFEUR') or hasRole('PROPRIETAIRE_VEHICULE')")
	public ResponseEntity<List<CommandeDTO.Response>> getCommandesParStatut(@PathVariable StatutCommande statut) {
		return ResponseEntity.ok(commandeService.getCommandesParStatut(statut));
	}
}