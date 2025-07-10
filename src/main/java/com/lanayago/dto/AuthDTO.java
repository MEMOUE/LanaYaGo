package com.lanayago.dto;

import lombok.Data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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

		// Adresse du client (optionnelle à l'inscription)
		private String adresse;
		private String ville;
		private String codePostal;
	}

	@Data
	public static class DemandeProprietaireRequest {
		@NotBlank(message = "Le nom de l'entreprise est obligatoire")
		@Size(max = 150, message = "Le nom de l'entreprise ne peut pas dépasser 150 caractères")
		private String nomEntreprise;

		@Size(max = 14, message = "Le numéro SIRET ne peut pas dépasser 14 caractères")
		private String numeroSiret;

		@NotBlank(message = "L'adresse de l'entreprise est obligatoire")
		@Size(max = 255, message = "L'adresse ne peut pas dépasser 255 caractères")
		private String adresseEntreprise;

		@NotBlank(message = "La ville est obligatoire")
		@Size(max = 100, message = "La ville ne peut pas dépasser 100 caractères")
		private String villeEntreprise;

		@NotBlank(message = "Le code postal est obligatoire")
		@Size(max = 10, message = "Le code postal ne peut pas dépasser 10 caractères")
		private String codePostalEntreprise;

		// URLs des documents (seront gérées par upload séparé)
		private String pieceIdentiteUrl;
		private String extraitUrl;
		private String justificatifAdresseUrl;
	}

	@Data
	public static class AuthResponse {
		private String token;
		private String refreshToken;
		private UserDTO user;
		private Long expiresIn;
		private Boolean peutDemanderProprietaire; // Indique si l'utilisateur peut faire une demande
	}
}