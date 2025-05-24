package com.lanayago.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
		log.error("Erreur métier: {}", ex.getMessage());

		ErrorResponse error = new ErrorResponse(
				"BUSINESS_ERROR",
				ex.getMessage(),
				LocalDateTime.now()
		);

		return ResponseEntity.badRequest().body(error);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
		Map<String, String> errors = new HashMap<>();
		ex.getBindingResult().getAllErrors().forEach(error -> {
			String fieldName = ((FieldError) error).getField();
			String errorMessage = error.getDefaultMessage();
			errors.put(fieldName, errorMessage);
		});

		ErrorResponse error = new ErrorResponse(
				"VALIDATION_ERROR",
				"Erreurs de validation",
				LocalDateTime.now(),
				errors
		);

		return ResponseEntity.badRequest().body(error);
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex) {
		ErrorResponse error = new ErrorResponse(
				"ACCESS_DENIED",
				"Accès refusé",
				LocalDateTime.now()
		);

		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
		log.error("Erreur inattendue", ex);

		ErrorResponse error = new ErrorResponse(
				"INTERNAL_ERROR",
				"Une erreur inattendue s'est produite",
				LocalDateTime.now()
		);

		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
	}

	public static class ErrorResponse {
		private String code;
		private String message;
		private LocalDateTime timestamp;
		private Map<String, String> details;

		public ErrorResponse(String code, String message, LocalDateTime timestamp) {
			this.code = code;
			this.message = message;
			this.timestamp = timestamp;
		}

		public ErrorResponse(String code, String message, LocalDateTime timestamp, Map<String, String> details) {
			this(code, message, timestamp);
			this.details = details;
		}

		// Getters et setters
		public String getCode() { return code; }
		public void setCode(String code) { this.code = code; }
		public String getMessage() { return message; }
		public void setMessage(String message) { this.message = message; }
		public LocalDateTime getTimestamp() { return timestamp; }
		public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
		public Map<String, String> getDetails() { return details; }
		public void setDetails(Map<String, String> details) { this.details = details; }
	}
}