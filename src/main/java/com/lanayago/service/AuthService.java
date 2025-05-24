package com.lanayago.service;

import com.lanayago.dto.AuthDTO;
import com.lanayago.dto.UserDTO;
import com.lanayago.entity.Client;
import com.lanayago.entity.Chauffeur;
import com.lanayago.entity.ProprietaireVehicule;
import com.lanayago.entity.User;
import com.lanayago.enums.TypeUtilisateur;
import com.lanayago.exception.BusinessException;
import com.lanayago.repository.UserRepository;
import com.lanayago.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;
	private final AuthenticationManager authenticationManager;
	private final UserMapperService userMapperService;

	@Transactional
	public AuthDTO.AuthResponse register(AuthDTO.RegisterRequest request) {
		log.info("Tentative d'inscription pour l'email: {}", request.getEmail());

		// Vérifications
		if (userRepository.existsByEmail(request.getEmail())) {
			throw new BusinessException("Un utilisateur avec cet email existe déjà");
		}

		if (userRepository.existsByTelephone(request.getTelephone())) {
			throw new BusinessException("Un utilisateur avec ce téléphone existe déjà");
		}

		// Création de l'utilisateur selon le type
		User user = createUserByType(request);
		user.setMotDePasse(passwordEncoder.encode(request.getMotDePasse()));

		user = userRepository.save(user);
		log.info("Utilisateur créé avec succès: {}", user.getId());

		// Génération des tokens
		String token = jwtTokenProvider.createToken(user.getEmail(), user.getTypeUtilisateur());
		String refreshToken = jwtTokenProvider.createRefreshToken(user.getEmail());

		UserDTO userDTO = userMapperService.toDTO(user);

		AuthDTO.AuthResponse response = new AuthDTO.AuthResponse();
		response.setToken(token);
		response.setRefreshToken(refreshToken);
		response.setUser(userDTO);
		response.setExpiresIn(jwtTokenProvider.getTokenValidityInMilliseconds());

		return response;
	}

	public AuthDTO.AuthResponse login(AuthDTO.LoginRequest request) {
		log.info("Tentative de connexion pour l'email: {}", request.getEmail());

		// Authentification
		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(request.getEmail(), request.getMotDePasse())
		);

		User user = userRepository.findByEmailAndActifTrue(request.getEmail())
				.orElseThrow(() -> new BusinessException("Utilisateur non trouvé ou inactif"));

		// Génération des tokens
		String token = jwtTokenProvider.createToken(user.getEmail(), user.getTypeUtilisateur());
		String refreshToken = jwtTokenProvider.createRefreshToken(user.getEmail());

		UserDTO userDTO = userMapperService.toDTO(user);

		AuthDTO.AuthResponse response = new AuthDTO.AuthResponse();
		response.setToken(token);
		response.setRefreshToken(refreshToken);
		response.setUser(userDTO);
		response.setExpiresIn(jwtTokenProvider.getTokenValidityInMilliseconds());

		log.info("Connexion réussie pour l'utilisateur: {}", user.getId());
		return response;
	}

	private User createUserByType(AuthDTO.RegisterRequest request) {
		User user = switch (request.getTypeUtilisateur()) {
			case CLIENT -> {
				Client client = new Client();
				client.setAdresse(request.getAdresse());
				client.setVille(request.getVille());
				client.setCodePostal(request.getCodePostal());
				yield client;
			}
			case CHAUFFEUR -> {
				Chauffeur chauffeur = new Chauffeur();
				chauffeur.setNumeroPermis(request.getNumeroPermis());
				// TODO: Gérer l'affectation du propriétaire
				yield chauffeur;
			}
			case PROPRIETAIRE_VEHICULE -> {
				ProprietaireVehicule proprietaire = new ProprietaireVehicule();
				proprietaire.setNomEntreprise(request.getNomEntreprise());
				proprietaire.setNumeroSiret(request.getNumeroSiret());
				yield proprietaire;
			}
		};

		// Propriétés communes
		user.setNom(request.getNom());
		user.setPrenom(request.getPrenom());
		user.setEmail(request.getEmail());
		user.setTelephone(request.getTelephone());
		user.setTypeUtilisateur(request.getTypeUtilisateur());

		return user;
	}
}