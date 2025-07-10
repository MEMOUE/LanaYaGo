package com.lanayago.service;

import com.lanayago.dto.AuthDTO;
import com.lanayago.dto.UserDTO;
import com.lanayago.entity.Client;
import com.lanayago.entity.User;
import com.lanayago.enums.StatutDemandeProprietaire;
import com.lanayago.enums.TypeUtilisateur;
import com.lanayago.exception.BusinessException;
import com.lanayago.repository.DemandeProprietaireRepository;
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

import java.util.Arrays;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

	private final UserRepository userRepository;
	private final DemandeProprietaireRepository demandeProprietaireRepository;
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

		// Création du client (tous les utilisateurs s'inscrivent comme clients)
		Client client = new Client();
		client.setNom(request.getNom());
		client.setPrenom(request.getPrenom());
		client.setEmail(request.getEmail());
		client.setTelephone(request.getTelephone());
		client.setMotDePasse(passwordEncoder.encode(request.getMotDePasse()));
		client.setTypeUtilisateur(TypeUtilisateur.CLIENT);
		client.setAdresse(request.getAdresse());
		client.setVille(request.getVille());
		client.setCodePostal(request.getCodePostal());

		client = (Client) userRepository.save(client);
		log.info("Client créé avec succès: {}", client.getId());

		// Génération des tokens
		String token = jwtTokenProvider.createToken(client.getEmail(), client.getTypeUtilisateur());
		String refreshToken = jwtTokenProvider.createRefreshToken(client.getEmail());

		UserDTO userDTO = userMapperService.toDTO(client);

		AuthDTO.AuthResponse response = new AuthDTO.AuthResponse();
		response.setToken(token);
		response.setRefreshToken(refreshToken);
		response.setUser(userDTO);
		response.setExpiresIn(jwtTokenProvider.getTokenValidityInMilliseconds());
		response.setPeutDemanderProprietaire(peutDemanderProprietaire(client.getId()));

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
		response.setPeutDemanderProprietaire(peutDemanderProprietaire(user.getId()));

		log.info("Connexion réussie pour l'utilisateur: {}", user.getId());
		return response;
	}

	/**
	 * Vérifie si un utilisateur peut faire une demande pour devenir propriétaire
	 */
	private boolean peutDemanderProprietaire(Long userId) {
		// L'utilisateur ne peut pas demander s'il a déjà une demande en cours ou approuvée
		return !demandeProprietaireRepository.existsByUserIdAndStatutIn(
				userId,
				Arrays.asList(
						StatutDemandeProprietaire.EN_ATTENTE,
						StatutDemandeProprietaire.EN_REVISION,
						StatutDemandeProprietaire.APPROUVEE
				)
		);
	}

	@Transactional(readOnly = true)
	public Boolean verifierStatutDemandeProprietaire(Long userId) {
		return peutDemanderProprietaire(userId);
	}
}