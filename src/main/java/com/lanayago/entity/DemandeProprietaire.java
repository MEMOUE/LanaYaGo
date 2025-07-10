package com.lanayago.entity;

import com.lanayago.enums.StatutDemandeProprietaire;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "demandes_proprietaires")
@EntityListeners(AuditingEntityListener.class)
@Data
@EqualsAndHashCode(of = "id")
public class DemandeProprietaire {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false, length = 150)
	private String nomEntreprise;

	@Column(unique = true, length = 14)
	private String numeroSiret;

	@Column(nullable = false, length = 255)
	private String adresseEntreprise;

	@Column(nullable = false, length = 100)
	private String villeEntreprise;

	@Column(nullable = false, length = 10)
	private String codePostalEntreprise;

	// Documents fournis
	@Column(length = 500)
	private String pieceIdentiteUrl;

	@Column(length = 500)
	private String extraitUrl;

	@Column(length = 500)
	private String justificatifAdresseUrl;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private StatutDemandeProprietaire statut = StatutDemandeProprietaire.EN_ATTENTE;

	@Column(length = 1000)
	private String commentaireAdmin;

	@CreatedDate
	@Column(nullable = false, updatable = false)
	private LocalDateTime dateCreation;

	@LastModifiedDate
	private LocalDateTime dateModification;

	private LocalDateTime dateTraitement;

	@Column(name = "admin_id")
	private Long adminId; // ID de l'admin qui a traité la demande
}