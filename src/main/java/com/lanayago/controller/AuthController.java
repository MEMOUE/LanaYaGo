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
@Tag(name = "Authentification", description = "Gestion de l'authentification")
public class AuthController {

	private final AuthService authService;

	@PostMapping("/register")
	@Operation(summary = "Inscription d'un nouvel utilisateur")
	public ResponseEntity<AuthDTO.AuthResponse> register(@Valid @RequestBody AuthDTO.RegisterRequest request) {
		return ResponseEntity.ok(authService.register(request));
	}

	@PostMapping("/login")
	@Operation(summary = "Connexion d'un utilisateur")
	public ResponseEntity<AuthDTO.AuthResponse> login(@Valid @RequestBody AuthDTO.LoginRequest request) {
		return ResponseEntity.ok(authService.login(request));
	}
}