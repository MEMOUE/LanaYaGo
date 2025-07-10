package com.lanayago.entity;

import com.lanayago.enums.TypeVehicule;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "recherches_transport")
@EntityListeners(AuditingEntityListener.class)
@Data
@EqualsAndHashCode(of = "id")
public class RechercheTransport {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "client_id", nullable = false)
	private Client client;

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

	// Caractéristiques de la marchandise
	@Column(nullable = false, columnDefinition = "DECIMAL(8,2)")
	private BigDecimal poidsMarchandise; // en kg

	@Column(columnDefinition = "DECIMAL(8,2)")
	private BigDecimal volumeMarchandise; // en m³

	@Column(length = 500)
	private String descriptionMarchandise;

	@Enumerated(EnumType.STRING)
	private TypeVehicule typeVehiculeRecommande;

	@Column(nullable = false)
	private Double distance; // en km

	@Column(nullable = false, columnDefinition = "DECIMAL(10,2)")
	private BigDecimal tarifEstime;

	@Column(nullable = false)
	private Boolean urgent = false;

	private LocalDateTime dateRamassageSouhaitee;
	private LocalDateTime dateLivraisonSouhaitee;

	@CreatedDate
	@Column(nullable = false, updatable = false)
	private LocalDateTime dateCreation;

	// Statut de la recherche
	@Column(nullable = false)
	private Boolean active = true;

	// Session ID pour le suivi temps réel
	@Column(length = 100)
	private String sessionId;
}