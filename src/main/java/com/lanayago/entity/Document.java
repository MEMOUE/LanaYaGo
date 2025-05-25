package com.lanayago.entity;

import com.lanayago.enums.TypeDocument;
import com.lanayago.enums.StatutDocument;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
@EntityListeners(AuditingEntityListener.class)
@Data
@EqualsAndHashCode(of = "id")
public class Document {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String nom;

	@Column(nullable = false)
	private String cheminFichier;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TypeDocument typeDocument;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private StatutDocument statut = StatutDocument.EN_ATTENTE;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "utilisateur_id", nullable = false)
	private User utilisateur;

	@Column(length = 500)
	private String commentaireValidation;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "validateur_id")
	private User validateur;

	@CreatedDate
	@Column(nullable = false, updatable = false)
	private LocalDateTime dateCreation;

	private LocalDateTime dateValidation;

	@Column(nullable = false)
	private Long tailleFichier;

	@Column(nullable = false)
	private String typeContenu;

	@Column(nullable = false)
	private Boolean obligatoire = false;

	private LocalDateTime dateExpiration;
}