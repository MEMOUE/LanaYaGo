package com.lanayago.controller;

import com.lanayago.dto.CommandeDTO;
import com.lanayago.dto.RechercheTransportDTO;
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
@Tag(name = "📦 Commandes", description = "Gestion des commandes de transport")
public class CommandeController {

	private final CommandeService commandeService;

	@PostMapping("/depuis-recherche")
	@Operation(
			summary = "Créer une commande depuis une recherche de transport",
			description = "Le client choisit un chauffeur/véhicule et crée une commande à partir de sa recherche"
	)
	@PreAuthorize("hasRole('CLIENT')")
	public ResponseEntity<CommandeDTO.Response> creerCommandeDepuisRecherche(
			@Valid @RequestBody RechercheTransportDTO.DemandeTransportRequest request) {
		return ResponseEntity.ok(commandeService.creerCommandeDepuisRecherche(request));
	}

	@PutMapping("/{commandeId}/accepter/{chauffeurId}")
	@Operation(
			summary = "Accepter une commande (chauffeur)",
			description = "Le chauffeur accepte définitivement la commande après avoir reçu la notification"
	)
	@PreAuthorize("hasRole('CHAUFFEUR') and #chauffeurId == authentication.principal.id")
	public ResponseEntity<CommandeDTO.Response> accepterCommande(
			@PathVariable Long commandeId,
			@PathVariable Long chauffeurId) {
		return ResponseEntity.ok(commandeService.accepterCommande(commandeId, chauffeurId));
	}

	@PutMapping("/{commandeId}/refuser/{chauffeurId}")
	@Operation(
			summary = "Refuser une commande (chauffeur)",
			description = "Le chauffeur refuse la commande avec un motif"
	)
	@PreAuthorize("hasRole('CHAUFFEUR') and #chauffeurId == authentication.principal.id")
	public ResponseEntity<Void> refuserCommande(
			@PathVariable Long commandeId,
			@PathVariable Long chauffeurId,
			@RequestParam String motifRefus) {
		commandeService.refuserCommande(commandeId, chauffeurId, motifRefus);
		return ResponseEntity.ok().build();
	}

	@PutMapping("/{commandeId}/statut")
	@Operation(
			summary = "Changer le statut d'une commande",
			description = "Met à jour le statut de la commande (EN_COURS, RAMASSAGE, EN_LIVRAISON, LIVREE)"
	)
	@PreAuthorize("hasRole('CHAUFFEUR') or hasRole('CLIENT')")
	public ResponseEntity<CommandeDTO.Response> changerStatut(
			@PathVariable Long commandeId,
			@RequestParam StatutCommande statut) {
		return ResponseEntity.ok(commandeService.changerStatut(commandeId, statut));
	}

	@PostMapping("/{commandeId}/evaluer")
	@Operation(
			summary = "Évaluer une commande",
			description = "Le client ou le chauffeur évalue la commande après livraison"
	)
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

	// === ENDPOINTS DE CONSULTATION ===

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

	// === ENDPOINTS POUR LES PROPRIÉTAIRES ===

	@GetMapping("/proprietaire/{proprietaireId}")
	@Operation(
			summary = "Récupérer les commandes des véhicules d'un propriétaire",
			description = "Toutes les commandes effectuées par les véhicules du propriétaire"
	)
	@PreAuthorize("hasRole('PROPRIETAIRE_VEHICULE') and #proprietaireId == authentication.principal.id")
	public ResponseEntity<List<CommandeDTO.Response>> getCommandesProprietaire(@PathVariable Long proprietaireId) {
		return ResponseEntity.ok(commandeService.getCommandesProprietaire(proprietaireId));
	}
}