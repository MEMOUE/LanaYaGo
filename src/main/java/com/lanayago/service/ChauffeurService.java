package com.lanayago.service;

import com.lanayago.dto.AuthDTO;
import com.lanayago.dto.UserDTO;
import com.lanayago.entity.Chauffeur;
import com.lanayago.entity.ProprietaireVehicule;
import com.lanayago.entity.Vehicule;
import com.lanayago.enums.TypeUtilisateur;
import com.lanayago.exception.BusinessException;
import com.lanayago.repository.ChauffeurRepository;
import com.lanayago.repository.UserRepository;
import com.lanayago.repository.VehiculeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChauffeurService {

	private final ChauffeurRepository chauffeurRepository;
	private final UserRepository userRepository;
	private final VehiculeRepository vehiculeRepository;
	private final PasswordEncoder passwordEncoder;
	private final UserMapperService userMapperService;

	public static class ChauffeurDTO {
		public static class CreateRequest {
			private String nom;
			private String prenom;
			private String email;
			private String telephone;
			private String motDePasse;
			private String numeroPermis;
			private LocalDate dateExpirationPermis;

			// Getters et setters
			public String getNom() { return nom; }
			public void setNom(String nom) { this.nom = nom; }
			public String getPrenom() { return prenom; }
			public void setPrenom(String prenom) { this.prenom = prenom; }
			public String getEmail() { return email; }
			public void setEmail(String email) { this.email = email; }
			public String getTelephone() { return telephone; }
			public void setTelephone(String telephone) { this.telephone = telephone; }
			public String getMotDePasse() { return motDePasse; }
			public void setMotDePasse(String motDePasse) { this.motDePasse = motDePasse; }
			public String getNumeroPermis() { return numeroPermis; }
			public void setNumeroPermis(String numeroPermis) { this.numeroPermis = numeroPermis; }
			public LocalDate getDateExpirationPermis() { return dateExpirationPermis; }
			public void setDateExpirationPermis(LocalDate dateExpirationPermis) { this.dateExpirationPermis = dateExpirationPermis; }
		}
	}

	@Transactional
	public UserDTO creerChauffeur(Long proprietaireId, ChauffeurDTO.CreateRequest request) {
		log.info("Création d'un chauffeur par le propriétaire: {}", proprietaireId);

		ProprietaireVehicule proprietaire = (ProprietaireVehicule) userRepository.findById(proprietaireId)
				.orElseThrow(() -> new BusinessException("Propriétaire non trouvé"));

		// Vérifications
		if (userRepository.existsByEmail(request.getEmail())) {
			throw new BusinessException("Un utilisateur avec cet email existe déjà");
		}

		if (userRepository.existsByTelephone(request.getTelephone())) {
			throw new BusinessException("Un utilisateur avec ce téléphone existe déjà");
		}

		if (chauffeurRepository.existsByNumeroPermis(request.getNumeroPermis())) {
			throw new BusinessException("Un chauffeur avec ce numéro de permis existe déjà");
		}

		// Validation de la date d'expiration du permis
		if (request.getDateExpirationPermis().isBefore(LocalDate.now().plusMonths(3))) {
			throw new BusinessException("Le permis doit être valide pendant au moins 3 mois");
		}

		// Créer le chauffeur
		Chauffeur chauffeur = new Chauffeur();
		chauffeur.setNom(request.getNom());
		chauffeur.setPrenom(request.getPrenom());
		chauffeur.setEmail(request.getEmail());
		chauffeur.setTelephone(request.getTelephone());
		chauffeur.setMotDePasse(passwordEncoder.encode(request.getMotDePasse()));
		chauffeur.setTypeUtilisateur(TypeUtilisateur.CHAUFFEUR);
		chauffeur.setNumeroPermis(request.getNumeroPermis());
		chauffeur.setDateExpirationPermis(request.getDateExpirationPermis());
		chauffeur.setProprietaire(proprietaire);

		chauffeur = (Chauffeur) userRepository.save(chauffeur);
		log.info("Chauffeur créé avec succès: {}", chauffeur.getId());

		return userMapperService.toDTO(chauffeur);
	}

	@Transactional
	public void affecterVehicule(Long proprietaireId, Long chauffeurId, Long vehiculeId) {
		log.info("Affectation du véhicule {} au chauffeur {} par le propriétaire {}",
				vehiculeId, chauffeurId, proprietaireId);

		ProprietaireVehicule proprietaire = (ProprietaireVehicule) userRepository.findById(proprietaireId)
				.orElseThrow(() -> new BusinessException("Propriétaire non trouvé"));

		Chauffeur chauffeur = chauffeurRepository.findById(chauffeurId)
				.orElseThrow(() -> new BusinessException("Chauffeur non trouvé"));

		Vehicule vehicule = vehiculeRepository.findById(vehiculeId)
				.orElseThrow(() -> new BusinessException("Véhicule non trouvé"));

		// Vérifications
		if (!chauffeur.getProprietaire().getId().equals(proprietaireId)) {
			throw new BusinessException("Ce chauffeur n'appartient pas à votre entreprise");
		}

		if (!vehicule.getProprietaire().getId().equals(proprietaireId)) {
			throw new BusinessException("Ce véhicule ne vous appartient pas");
		}

		// Libérer l'ancien véhicule du chauffeur si existant
		if (chauffeur.getVehiculeActuel() != null) {
			chauffeur.getVehiculeActuel().setDisponible(true);
			vehiculeRepository.save(chauffeur.getVehiculeActuel());
		}

		// Affecter le nouveau véhicule
		chauffeur.setVehiculeActuel(vehicule);
		chauffeurRepository.save(chauffeur);

		log.info("Véhicule {} affecté au chauffeur {} avec succès", vehiculeId, chauffeurId);
	}

	@Transactional
	public void libererVehicule(Long proprietaireId, Long chauffeurId) {
		log.info("Libération du véhicule du chauffeur {} par le propriétaire {}", chauffeurId, proprietaireId);

		Chauffeur chauffeur = chauffeurRepository.findById(chauffeurId)
				.orElseThrow(() -> new BusinessException("Chauffeur non trouvé"));

		// Vérifications
		if (!chauffeur.getProprietaire().getId().equals(proprietaireId)) {
			throw new BusinessException("Ce chauffeur n'appartient pas à votre entreprise");
		}

		if (chauffeur.getVehiculeActuel() == null) {
			throw new BusinessException("Ce chauffeur n'a pas de véhicule affecté");
		}

		// Vérifier qu'il n'y a pas de commande en cours
		if (!chauffeur.getDisponible()) {
			throw new BusinessException("Impossible de libérer le véhicule : le chauffeur a une commande en cours");
		}

		// Libérer le véhicule
		chauffeur.getVehiculeActuel().setDisponible(true);
		vehiculeRepository.save(chauffeur.getVehiculeActuel());

		chauffeur.setVehiculeActuel(null);
		chauffeurRepository.save(chauffeur);

		log.info("Véhicule libéré du chauffeur {} avec succès", chauffeurId);
	}

	@Transactional(readOnly = true)
	public List<UserDTO> getChauffeursProprietaire(Long proprietaireId) {
		return chauffeurRepository.findByProprietaireId(proprietaireId)
				.stream()
				.map(userMapperService::toDTO)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<UserDTO> getChauffeursDisponibles() {
		return chauffeurRepository.findChauffeursDisponibles()
				.stream()
				.map(userMapperService::toDTO)
				.toList();
	}

	@Transactional
	public void changerDisponibilite(Long chauffeurId, Boolean disponible) {
		Chauffeur chauffeur = chauffeurRepository.findById(chauffeurId)
				.orElseThrow(() -> new BusinessException("Chauffeur non trouvé"));

		chauffeur.setDisponible(disponible);
		chauffeurRepository.save(chauffeur);

		log.info("Disponibilité du chauffeur {} changée vers: {}", chauffeurId, disponible);
	}

	@Transactional
	public UserDTO mettreAJourChauffeur(Long proprietaireId, Long chauffeurId, ChauffeurDTO.CreateRequest request) {
		log.info("Mise à jour du chauffeur {} par le propriétaire {}", chauffeurId, proprietaireId);

		Chauffeur chauffeur = chauffeurRepository.findById(chauffeurId)
				.orElseThrow(() -> new BusinessException("Chauffeur non trouvé"));

		// Vérifications
		if (!chauffeur.getProprietaire().getId().equals(proprietaireId)) {
			throw new BusinessException("Ce chauffeur n'appartient pas à votre entreprise");
		}

		// Vérifier l'unicité de l'email si modifié
		if (!chauffeur.getEmail().equals(request.getEmail()) &&
				userRepository.existsByEmail(request.getEmail())) {
			throw new BusinessException("Un utilisateur avec cet email existe déjà");
		}

		// Vérifier l'unicité du téléphone si modifié
		if (!chauffeur.getTelephone().equals(request.getTelephone()) &&
				userRepository.existsByTelephone(request.getTelephone())) {
			throw new BusinessException("Un utilisateur avec ce téléphone existe déjà");
		}

		// Vérifier l'unicité du permis si modifié
		if (!chauffeur.getNumeroPermis().equals(request.getNumeroPermis()) &&
				chauffeurRepository.existsByNumeroPermis(request.getNumeroPermis())) {
			throw new BusinessException("Un chauffeur avec ce numéro de permis existe déjà");
		}

		// Mise à jour
		chauffeur.setNom(request.getNom());
		chauffeur.setPrenom(request.getPrenom());
		chauffeur.setEmail(request.getEmail());
		chauffeur.setTelephone(request.getTelephone());

		if (request.getMotDePasse() != null && !request.getMotDePasse().isEmpty()) {
			chauffeur.setMotDePasse(passwordEncoder.encode(request.getMotDePasse()));
		}

		chauffeur.setNumeroPermis(request.getNumeroPermis());
		chauffeur.setDateExpirationPermis(request.getDateExpirationPermis());

		chauffeur = (Chauffeur) userRepository.save(chauffeur);
		log.info("Chauffeur {} mis à jour avec succès", chauffeurId);

		return userMapperService.toDTO(chauffeur);
	}

	@Transactional
	public void supprimerChauffeur(Long proprietaireId, Long chauffeurId) {
		log.info("Suppression du chauffeur {} par le propriétaire {}", chauffeurId, proprietaireId);

		Chauffeur chauffeur = chauffeurRepository.findById(chauffeurId)
				.orElseThrow(() -> new BusinessException("Chauffeur non trouvé"));

		// Vérifications
		if (!chauffeur.getProprietaire().getId().equals(proprietaireId)) {
			throw new BusinessException("Ce chauffeur n'appartient pas à votre entreprise");
		}

		if (!chauffeur.getDisponible()) {
			throw new BusinessException("Impossible de supprimer le chauffeur : il a une commande en cours");
		}

		// Libérer le véhicule si affecté
		if (chauffeur.getVehiculeActuel() != null) {
			chauffeur.getVehiculeActuel().setDisponible(true);
			vehiculeRepository.save(chauffeur.getVehiculeActuel());
		}

		// Désactiver plutôt que supprimer (pour conserver l'historique)
		chauffeur.setActif(false);
		chauffeurRepository.save(chauffeur);

		log.info("Chauffeur {} désactivé avec succès", chauffeurId);
	}
}