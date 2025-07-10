package com.lanayago.controller;

import com.lanayago.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "📁 Gestion Fichiers", description = "Accès aux fichiers uploadés")
public class FileController {

	private final FileStorageService fileStorageService;

	@GetMapping("/**")
	@Operation(
			summary = "Télécharger un fichier",
			description = "Récupère un fichier uploadé par son chemin relatif"
	)
	public ResponseEntity<Resource> downloadFile(HttpServletRequest request) {
		// Extraire le chemin du fichier depuis l'URL
		String requestURL = request.getRequestURL().toString();
		String contextPath = request.getContextPath();
		String servletPath = request.getServletPath();
		String filePath = requestURL.substring(requestURL.indexOf(contextPath + servletPath) + (contextPath + servletPath).length() + "/files/".length());

		try {
			Path path = fileStorageService.getFilePath(filePath);
			Resource resource = new UrlResource(path.toUri());

			if (!resource.exists() || !resource.isReadable()) {
				log.warn("Fichier non trouvé ou non lisible: {}", filePath);
				return ResponseEntity.notFound().build();
			}

			// Déterminer le type de contenu
			String contentType = determineContentType(filePath);

			return ResponseEntity.ok()
					.contentType(MediaType.parseMediaType(contentType))
					.header(HttpHeaders.CONTENT_DISPOSITION,
							"inline; filename=\"" + resource.getFilename() + "\"")
					.body(resource);

		} catch (MalformedURLException ex) {
			log.error("Erreur lors de l'accès au fichier: {}", filePath, ex);
			return ResponseEntity.badRequest().build();
		}
	}

	private String determineContentType(String filePath) {
		String extension = filePath.substring(filePath.lastIndexOf('.') + 1).toLowerCase();

		return switch (extension) {
			case "pdf" -> "application/pdf";
			case "jpg", "jpeg" -> "image/jpeg";
			case "png" -> "image/png";
			case "gif" -> "image/gif";
			case "doc" -> "application/msword";
			case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
			default -> "application/octet-stream";
		};
	}
}