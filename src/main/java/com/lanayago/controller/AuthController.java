package com.lanayago.controller;

import com.lanayago.dto.AuthDTO;
import com.lanayago.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "🔐 Authentification", description = "Gestion de l'authentification et inscription")
public class AuthController {

	private final AuthService authService;

	@PostMapping("/register")
	@Operation(
			summary = "Inscription d'un nouvel utilisateur",
			description = "Tous les utilisateurs s'inscrivent initialement comme clients. " +
					"Ils peuvent ensuite faire une demande pour devenir propriétaire de véhicules."
	)
	public ResponseEntity<AuthDTO.AuthResponse> register(@Valid @RequestBody AuthDTO.RegisterRequest request) {
		return ResponseEntity.ok(authService.register(request));
	}

	@PostMapping("/login")
	@Operation(summary = "Connexion d'un utilisateur")
	public ResponseEntity<AuthDTO.AuthResponse> login(@Valid @RequestBody AuthDTO.LoginRequest request) {
		return ResponseEntity.ok(authService.login(request));
	}

	@GetMapping("/statut-demande-proprietaire/{userId}")
	@Operation(
			summary = "Vérifier si un utilisateur peut faire une demande de propriétaire",
			description = "Retourne true si l'utilisateur peut faire une demande de propriétaire"
	)
	public ResponseEntity<Boolean> verifierStatutDemandeProprietaire(@PathVariable Long userId) {
		return ResponseEntity.ok(authService.verifierStatutDemandeProprietaire(userId));
	}
}