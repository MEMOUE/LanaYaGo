package com.lanayago.service;

import com.lanayago.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

	@Value("${file.upload.dir:uploads}")
	private String uploadDir;

	@Value("${file.max-size:10485760}") // 10MB par défaut
	private Long maxFileSize;

	private final List<String> allowedExtensions = Arrays.asList(".jpg", ".jpeg", ".png", ".pdf", ".doc", ".docx");

	/**
	 * Stocke un fichier dans le système de fichiers
	 */
	public String storeFile(MultipartFile file, String subfolder) {
		if (file.isEmpty()) {
			throw new BusinessException("Le fichier est vide");
		}

		// Validation de la taille
		if (file.getSize() > maxFileSize) {
			throw new BusinessException("Le fichier est trop volumineux. Taille maximum: " + (maxFileSize / 1024 / 1024) + "MB");
		}

		// Validation de l'extension
		String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
		String fileExtension = getFileExtension(originalFilename).toLowerCase();

		if (!allowedExtensions.contains(fileExtension)) {
			throw new BusinessException("Type de fichier non autorisé. Extensions autorisées: " + String.join(", ", allowedExtensions));
		}

		try {
			// Création du nom de fichier unique
			String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
			String uniqueId = UUID.randomUUID().toString().substring(0, 8);
			String filename = timestamp + "_" + uniqueId + fileExtension;

			// Création du répertoire si nécessaire
			Path uploadPath = Paths.get(uploadDir, subfolder);
			if (!Files.exists(uploadPath)) {
				Files.createDirectories(uploadPath);
			}

			// Sauvegarde du fichier
			Path targetLocation = uploadPath.resolve(filename);
			Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

			// Retour du chemin relatif
			String relativePath = subfolder + "/" + filename;
			log.info("Fichier sauvegardé: {}", relativePath);

			return relativePath;

		} catch (IOException ex) {
			log.error("Erreur lors de la sauvegarde du fichier", ex);
			throw new BusinessException("Erreur lors de la sauvegarde du fichier: " + ex.getMessage());
		}
	}

	/**
	 * Supprime un fichier
	 */
	public void deleteFile(String filePath) {
		try {
			Path path = Paths.get(uploadDir, filePath);
			if (Files.exists(path)) {
				Files.delete(path);
				log.info("Fichier supprimé: {}", filePath);
			}
		} catch (IOException ex) {
			log.error("Erreur lors de la suppression du fichier: {}", filePath, ex);
		}
	}

	/**
	 * Vérifie si un fichier existe
	 */
	public boolean fileExists(String filePath) {
		Path path = Paths.get(uploadDir, filePath);
		return Files.exists(path);
	}

	/**
	 * Obtient le chemin absolu d'un fichier
	 */
	public Path getFilePath(String relativePath) {
		return Paths.get(uploadDir, relativePath);
	}

	/**
	 * Obtient l'extension d'un fichier
	 */
	private String getFileExtension(String filename) {
		if (filename == null || filename.isEmpty()) {
			return "";
		}
		int lastDotIndex = filename.lastIndexOf('.');
		if (lastDotIndex == -1) {
			return "";
		}
		return filename.substring(lastDotIndex);
	}

	/**
	 * Valide si un fichier est une image
	 */
	public boolean isImageFile(MultipartFile file) {
		String contentType = file.getContentType();
		return contentType != null && contentType.startsWith("image/");
	}

	/**
	 * Valide si un fichier est un PDF
	 */
	public boolean isPdfFile(MultipartFile file) {
		String contentType = file.getContentType();
		return "application/pdf".equals(contentType);
	}

	/**
	 * Obtient l'URL publique d'un fichier
	 */
	public String getFileUrl(String relativePath) {
		if (relativePath == null || relativePath.isEmpty()) {
			return null;
		}
		return "/api/files/" + relativePath;
	}
}