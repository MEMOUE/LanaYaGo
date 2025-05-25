package com.lanayago.service;

import com.lanayago.dto.DocumentDTO;
import com.lanayago.entity.Document;
import com.lanayago.entity.User;
import com.lanayago.enums.TypeDocument;
import com.lanayago.enums.TypeUtilisateur;
import com.lanayago.exception.BusinessException;
import com.lanayago.repository.DocumentRepository;
import com.lanayago.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

	private final DocumentRepository documentRepository;
	private final UserRepository userRepository;
	private final UserMapperService userMapperService;

	@Value("${documents.upload-dir:./uploads}")
	private String uploadDir;

	@Value("${documents.allowed-types:image/jpeg,image/png,application/pdf}")
	private String allowedTypes;

	@Transactional
	public DocumentDTO.Response uploadDocument(Long userId, MultipartFile file, DocumentDTO.UploadRequest request) {
		User utilisateur = userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException("Utilisateur non trouvé"));

		// Validation du fichier
		validerFichier(file);

		try {
			// Création du dossier si nécessaire
			Path uploadPath = Paths.get(uploadDir);
			if (!Files.exists(uploadPath)) {
				Files.createDirectories(uploadPath);
			}

			// Génération d'un nom unique
			String filename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
			Path filePath = uploadPath.resolve(filename);

			// Sauvegarde du fichier
			Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

			// Création de l'entité Document
			Document document = new Document();
			document.setNom(request.getNom());
			document.setCheminFichier(filePath.toString());
			document.setTypeDocument(request.getTypeDocument());
			document.setUtilisateur(utilisateur);
			document.setTailleFichier(file.getSize());
			document.setTypeContenu(file.getContentType());
			document.setObligatoire(request.getObligatoire());
			document.setDateExpiration(request.getDateExpiration());

			document = documentRepository.save(document);
			log.info("Document uploadé avec succès: {} pour l'utilisateur {}", filename, userId);

			return mapToDTO(document);

		} catch (IOException e) {
			throw new BusinessException("Erreur lors de l'upload du fichier: " + e.getMessage());
		}
	}

	@Transactional(readOnly = true)
	public List<DocumentDTO.Response> getDocumentsUtilisateur(Long userId) {
		return documentRepository.findByUtilisateurIdOrderByDateCreationDesc(userId)
				.stream()
				.map(this::mapToDTO)
				.toList();
	}

	@Transactional(readOnly = true)
	public ResponseEntity<Resource> downloadDocument(Long documentId) {
		Document document = documentRepository.findById(documentId)
				.orElseThrow(() -> new BusinessException("Document non trouvé"));

		try {
			Path filePath = Paths.get(document.getCheminFichier());
			Resource resource = new UrlResource(filePath.toUri());

			if (!resource.exists()) {
				throw new BusinessException("Fichier non trouvé");
			}

			return ResponseEntity.ok()
					.contentType(MediaType.parseMediaType(document.getTypeContenu()))
					.header(HttpHeaders.CONTENT_DISPOSITION,
							"attachment; filename=\"" + document.getNom() + "\"")
					.body(resource);

		} catch (Exception e) {
			throw new BusinessException("Erreur lors du téléchargement: " + e.getMessage());
		}
	}

	@Transactional
	public void supprimerDocument(Long documentId) {
		Document document = documentRepository.findById(documentId)
				.orElseThrow(() -> new BusinessException("Document non trouvé"));

		try {
			// Suppression du fichier physique
			Path filePath = Paths.get(document.getCheminFichier());
			Files.deleteIfExists(filePath);

			// Suppression en base
			documentRepository.delete(document);

			log.info("Document {} supprimé avec succès", documentId);

		} catch (IOException e) {
			log.error("Erreur lors de la suppression du fichier physique", e);
			// On supprime quand même l'entrée en base
			documentRepository.delete(document);
		}
	}

	@Transactional(readOnly = true)
	public List<String> getTypesDocumentsRequis(String typeUtilisateur) {
		TypeUtilisateur type = TypeUtilisateur.valueOf(typeUtilisateur);

		return switch (type) {
			case CLIENT -> List.of(
					TypeDocument.CARTE_IDENTITE.name(),
					TypeDocument.JUSTIFICATIF_DOMICILE.name()
			);
			case CHAUFFEUR -> List.of(
					TypeDocument.CARTE_IDENTITE.name(),
					TypeDocument.PERMIS_CONDUIRE.name(),
					TypeDocument.JUSTIFICATIF_DOMICILE.name()
			);
			case PROPRIETAIRE_VEHICULE -> List.of(
					TypeDocument.CARTE_IDENTITE.name(),
					TypeDocument.KBIS.name(),
					TypeDocument.JUSTIFICATIF_DOMICILE.name()
			);
		};
	}

	private void validerFichier(MultipartFile file) {
		if (file.isEmpty()) {
			throw new BusinessException("Le fichier est vide");
		}

		List<String> typesAutorises = Arrays.asList(allowedTypes.split(","));
		if (!typesAutorises.contains(file.getContentType())) {
			throw new BusinessException("Type de fichier non autorisé: " + file.getContentType());
		}

		// Limite de 10MB
		if (file.getSize() > 10 * 1024 * 1024) {
			throw new BusinessException("Le fichier est trop volumineux (max 10MB)");
		}
	}

	private DocumentDTO.Response mapToDTO(Document document) {
		DocumentDTO.Response dto = new DocumentDTO.Response();
		dto.setId(document.getId());
		dto.setNom(document.getNom());
		dto.setTypeDocument(document.getTypeDocument());
		dto.setStatut(document.getStatut());
		dto.setUtilisateur(userMapperService.toDTO(document.getUtilisateur()));
		dto.setCommentaireValidation(document.getCommentaireValidation());
		if (document.getValidateur() != null) {
			dto.setValidateur(userMapperService.toDTO(document.getValidateur()));
		}
		dto.setDateCreation(document.getDateCreation());
		dto.setDateValidation(document.getDateValidation());
		dto.setDateExpiration(document.getDateExpiration());
		dto.setTailleFichier(document.getTailleFichier());
		dto.setTypeContenu(document.getTypeContenu());
		dto.setObligatoire(document.getObligatoire());
		dto.setUrlTelecharger("/api/documents/" + document.getId() + "/download");
		return dto;
	}
}