package com.lanayago.dto;

import com.lanayago.enums.TypeUtilisateur;
import lombok.Data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class AuthDTO {

	@Data
	public static class LoginRequest {
		@NotBlank(message = "L'email est obligatoire")
		@Email(message = "Format d'email invalide")
		private String email;

		@NotBlank(message = "Le mot de passe est obligatoire")
		private String motDePasse;
	}

	@Data
	public static class RegisterRequest {
		@NotBlank(message = "Le nom est obligatoire")
		@Size(max = 100, message = "Le nom ne peut pas dépasser 100 caractères")
		private String nom;

		@NotBlank(message = "Le prénom est obligatoire")
		@Size(max = 100, message = "Le prénom ne peut pas dépasser 100 caractères")
		private String prenom;

		@NotBlank(message = "L'email est obligatoire")
		@Email(message = "Format d'email invalide")
		private String email;

		@NotBlank(message = "Le téléphone est obligatoire")
		@Size(max = 20, message = "Le téléphone ne peut pas dépasser 20 caractères")
		private String telephone;

		@NotBlank(message = "Le mot de passe est obligatoire")
		@Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caractères")
		private String motDePasse;

		@NotNull(message = "Le type d'utilisateur est obligatoire")
		private TypeUtilisateur typeUtilisateur;

		// Champs spécifiques selon le type
		private String adresse;
		private String ville;
		private String codePostal;
		private String numeroPermis;
		private String nomEntreprise;
		private String numeroSiret;
		private Long proprietaireId;
	}

	@Data
	public static class AuthResponse {
		private String token;
		private String refreshToken;
		private UserDTO user;
		private Long expiresIn;
	}
}