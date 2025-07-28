package com.lanayago.service.photo;

import com.lanayago.entity.Chauffeur;
import com.lanayago.entity.ProprietaireVehicule;
import com.lanayago.entity.User;
import com.lanayago.repository.UserRepository;
import com.lanayago.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Service de migration pour organiser les fichiers existants selon la nouvelle structure
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PhotoMigrationService {

	private final UserRepository userRepository;
	private final FileStorageService fileStorageService;

	/**
	 * Migration automatique au démarrage de l'application
	 */
	@EventListener(ApplicationReadyEvent.class)
	@Transactional
	public void migrateExistingPhotos() {
		log.info("Démarrage de la migration des photos existantes...");

		try {
			migrerPhotosProprietaires();
			migrerPhotosChauffeurs();
			log.info("Migration des photos terminée avec succès");
		} catch (Exception e) {
			log.error("Erreur lors de la migration des photos", e);
		}
	}

	/**
	 * Migrer les photos des propriétaires depuis l'ancien système
	 */
	private void migrerPhotosProprietaires() {
		List<ProprietaireVehicule> proprietaires = userRepository.findAll()
				.stream()
				.filter(user -> user instanceof ProprietaireVehicule)
				.map(user -> (ProprietaireVehicule) user)
				.toList();

		for (ProprietaireVehicule proprietaire : proprietaires) {
			migrerUtilisateur(proprietaire, "proprietaire");
		}

		log.info("Migration de {} propriétaires terminée", proprietaires.size());
	}

	/**
	 * Migrer les photos des chauffeurs depuis l'ancien système
	 */
	private void migrerPhotosChauffeurs() {
		List<Chauffeur> chauffeurs = userRepository.findAll()
				.stream()
				.filter(user -> user instanceof Chauffeur)
				.map(user -> (Chauffeur) user)
				.toList();

		for (Chauffeur chauffeur : chauffeurs) {
			migrerUtilisateur(chauffeur, "chauffeur");
		}

		log.info("Migration de {} chauffeurs terminée", chauffeurs.size());
	}

	/**
	 * Migrer les fichiers d'un utilisateur spécifique
	 */
	private void migrerUtilisateur(User user, String userType) {
		try {
			// Chemins anciens possibles
			Path ancienRepertoire = Paths.get("uploads", user.getId().toString());
			Path nouveauRepertoire = Paths.get("uploads", userType, user.getId().toString());

			if (Files.exists(ancienRepertoire)) {
				// Créer la nouvelle structure
				Files.createDirectories(nouveauRepertoire.resolve("photos"));
				Files.createDirectories(nouveauRepertoire.resolve("documents"));

				// Déplacer les fichiers
				Files.list(ancienRepertoire).forEach(ancienFichier -> {
					try {
						String nomFichier = ancienFichier.getFileName().toString();
						Path nouveauFichier;

						// Déterminer le dossier de destination selon le type de fichier
						if (nomFichier.toLowerCase().contains("photo") ||
								nomFichier.toLowerCase().contains("profile") ||
								nomFichier.toLowerCase().contains("avatar")) {
							nouveauFichier = nouveauRepertoire.resolve("photos").resolve(nomFichier);
						} else {
							nouveauFichier = nouveauRepertoire.resolve("documents").resolve(nomFichier);
						}

						Files.move(ancienFichier, nouveauFichier);
						log.debug("Fichier migré: {} -> {}", ancienFichier, nouveauFichier);
					} catch (Exception e) {
						log.warn("Impossible de migrer le fichier: {}", ancienFichier, e);
					}
				});

				// Supprimer l'ancien répertoire s'il est vide
				if (Files.list(ancienRepertoire).findAny().isEmpty()) {
					Files.delete(ancienRepertoire);
				}
			}
		} catch (Exception e) {
			log.warn("Erreur lors de la migration pour l'utilisateur {}: {}", user.getId(), e.getMessage());
		}
	}

	/**
	 * Migration manuelle pour un utilisateur spécifique
	 */
	@Transactional
	public void migrerUtilisateur(Long userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new RuntimeException("Utilisateur non trouvé: " + userId));

		String userType = switch (user.getTypeUtilisateur()) {
			case PROPRIETAIRE_VEHICULE -> "proprietaire";
			case CHAUFFEUR -> "chauffeur";
			case CLIENT -> "client";
		};

		migrerUtilisateur(user, userType);
		log.info("Migration manuelle terminée pour l'utilisateur {} ({})", userId, userType);
	}

	/**
	 * Nettoyer les fichiers orphelins
	 */
	@Transactional
	public void nettoyerFichiersOrphelins() {
		log.info("Nettoyage des fichiers orphelins...");

		try {
			Path uploadsDir = Paths.get("uploads");
			if (!Files.exists(uploadsDir)) {
				return;
			}

			// Récupérer tous les IDs d'utilisateurs existants
			List<Long> userIds = userRepository.findAll()
					.stream()
					.map(User::getId)
					.toList();

			// Parcourir les dossiers et supprimer ceux sans utilisateur correspondant
			Files.list(uploadsDir)
					.filter(Files::isDirectory)
					.forEach(typeDir -> {
						try {
							Files.list(typeDir)
									.filter(Files::isDirectory)
									.forEach(userDir -> {
										try {
											Long userId = Long.parseLong(userDir.getFileName().toString());
											if (!userIds.contains(userId)) {
												// Supprimer récursivement le dossier orphelin
												supprimerRecursivement(userDir);
												log.info("Dossier orphelin supprimé: {}", userDir);
											}
										} catch (NumberFormatException e) {
											// Ignorer les dossiers avec des noms non numériques
										} catch (Exception e) {
											log.warn("Erreur lors de la suppression du dossier: {}", userDir, e);
										}
									});
						} catch (Exception e) {
							log.warn("Erreur lors du parcours du dossier: {}", typeDir, e);
						}
					});

		} catch (Exception e) {
			log.error("Erreur lors du nettoyage des fichiers orphelins", e);
		}
	}

	private void supprimerRecursivement(Path path) throws Exception {
		if (Files.isDirectory(path)) {
			Files.list(path).forEach(subPath -> {
				try {
					supprimerRecursivement(subPath);
				} catch (Exception e) {
					log.warn("Erreur lors de la suppression: {}", subPath, e);
				}
			});
		}
		Files.deleteIfExists(path);
	}
}