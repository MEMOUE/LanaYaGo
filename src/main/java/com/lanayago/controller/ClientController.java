package com.lanayago.controller;

import com.lanayago.dto.CommandeDTO;
import com.lanayago.dto.UserDTO;
import com.lanayago.service.ClientService;
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
@RequestMapping("/api/clients")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Clients", description = "Fonctionnalités spécifiques aux clients")
public class ClientController {

	private final ClientService clientService;

	@GetMapping("/{clientId}/profil")
	@Operation(summary = "Récupérer le profil du client")
	@PreAuthorize("hasRole('CLIENT') and #clientId == authentication.principal.id")
	public ResponseEntity<UserDTO> getProfil(@PathVariable Long clientId) {
		return ResponseEntity.ok(clientService.getProfilClient(clientId));
	}

	@PutMapping("/{clientId}/profil")
	@Operation(summary = "Mettre à jour le profil du client")
	@PreAuthorize("hasRole('CLIENT') and #clientId == authentication.principal.id")
	public ResponseEntity<UserDTO> mettreAJourProfil(
			@PathVariable Long clientId,
			@Valid @RequestBody UserDTO request) {
		return ResponseEntity.ok(clientService.mettreAJourProfil(clientId, request));
	}

	@GetMapping("/{clientId}/historique")
	@Operation(summary = "Récupérer l'historique des commandes")
	@PreAuthorize("hasRole('CLIENT') and #clientId == authentication.principal.id")
	public ResponseEntity<List<CommandeDTO.Response>> getHistoriqueCommandes(
			@PathVariable Long clientId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {
		return ResponseEntity.ok(clientService.getHistoriqueCommandes(clientId, page, size));
	}

	@GetMapping("/{clientId}/statistiques")
	@Operation(summary = "Récupérer les statistiques du client")
	@PreAuthorize("hasRole('CLIENT') and #clientId == authentication.principal.id")
	public ResponseEntity<Map<String, Object>> getStatistiques(@PathVariable Long clientId) {
		return ResponseEntity.ok(clientService.getStatistiquesClient(clientId));
	}

	@GetMapping("/{clientId}/adresses-favorites")
	@Operation(summary = "Récupérer les adresses favorites")
	@PreAuthorize("hasRole('CLIENT') and #clientId == authentication.principal.id")
	public ResponseEntity<List<Map<String, Object>>> getAdressesFavorites(@PathVariable Long clientId) {
		return ResponseEntity.ok(clientService.getAdressesFavorites(clientId));
	}

	@PostMapping("/{clientId}/adresses-favorites")
	@Operation(summary = "Ajouter une adresse favorite")
	@PreAuthorize("hasRole('CLIENT') and #clientId == authentication.principal.id")
	public ResponseEntity<Void> ajouterAdresseFavorite(
			@PathVariable Long clientId,
			@RequestBody Map<String, Object> adresse) {
		clientService.ajouterAdresseFavorite(clientId, adresse);
		return ResponseEntity.ok().build();
	}
}