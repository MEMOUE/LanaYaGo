package com.lanayago.entity;

import com.lanayago.enums.StatutCommande;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "commandes")
@EntityListeners(AuditingEntityListener.class)
@Data
@EqualsAndHashCode(of = "id")
public class Commande {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 20)
	private String numeroCommande;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "client_id", nullable = false)
	private Client client;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "chauffeur_id")
	private Chauffeur chauffeur;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "vehicule_id")
	private Vehicule vehicule;

	// Localisation départ
	@Column(nullable = false, columnDefinition = "DECIMAL(10,8)")
	private Double latitudeDepart;

	@Column(nullable = false, columnDefinition = "DECIMAL(11,8)")
	private Double longitudeDepart;

	@Column(nullable = false, length = 255)
	private String adresseDepart;

	// Localisation arrivée
	@Column(nullable = false, columnDefinition = "DECIMAL(10,8)")
	private Double latitudeArrivee;

	@Column(nullable = false, columnDefinition = "DECIMAL(11,8)")
	private Double longitudeArrivee;

	@Column(nullable = false, length = 255)
	private String adresseArrivee;

	// Détails du transport
	@Column(nullable = false, columnDefinition = "DECIMAL(8,2)")
	private BigDecimal poidsMarchandise; // en kg

	@Column(columnDefinition = "DECIMAL(8,2)")
	private BigDecimal volumeMarchandise; // en m³

	@Column(length = 500)
	private String descriptionMarchandise;

	@Column(nullable = false)
	private Boolean urgent = false;

	// Dates
	@CreatedDate
	@Column(nullable = false, updatable = false)
	private LocalDateTime dateCreation;

	@LastModifiedDate
	private LocalDateTime dateModification;

	private LocalDateTime dateRamassageSouhaitee;
	private LocalDateTime dateRamassageEffective;
	private LocalDateTime dateLivraisonSouhaitee;
	private LocalDateTime dateLivraisonEffective;

	// Tarification
	@Column(nullable = false)
	private Double distance; // en km

	@Column(nullable = false, columnDefinition = "DECIMAL(10,2)")
	private BigDecimal tarifCalcule;

	@Column(columnDefinition = "DECIMAL(10,2)")
	private BigDecimal tarifFinal;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private StatutCommande statut = StatutCommande.EN_ATTENTE;

	// Évaluations
	@Column(columnDefinition = "DECIMAL(2,1)")
	private BigDecimal noteClient; // Note donnée par le client

	@Column(columnDefinition = "DECIMAL(2,1)")
	private BigDecimal noteChauffeur; // Note donnée par le chauffeur

	@Column(length = 500)
	private String commentaireClient;

	@Column(length = 500)
	private String commentaireChauffeur;

	@PrePersist
	protected void onCreate() {
		if (numeroCommande == null) {
			numeroCommande = generateNumeroCommande();
		}
	}

	private String generateNumeroCommande() {
		return "CMD" + System.currentTimeMillis();
	}
}