package com.lanayago.entity;

import com.lanayago.enums.TypeVehicule;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "vehicules")
@EntityListeners(AuditingEntityListener.class)
@Data
@EqualsAndHashCode(of = "id")
public class Vehicule {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 15)
	private String immatriculation;

	@Column(nullable = false, length = 50)
	private String marque;

	@Column(nullable = false, length = 50)
	private String modele;

	@Column(nullable = false)
	private Integer annee;

	@Column(nullable = false, columnDefinition = "DECIMAL(5,2)")
	private BigDecimal capacitePoids; // en tonnes

	@Column(nullable = false, columnDefinition = "DECIMAL(6,2)")
	private BigDecimal capaciteVolume; // en m³

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TypeVehicule typeVehicule;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "proprietaire_id", nullable = false)
	private ProprietaireVehicule proprietaire;

	@Column(nullable = false)
	private Boolean disponible = true;

	@Column(columnDefinition = "DECIMAL(10,8)")
	private Double latitudeActuelle;

	@Column(columnDefinition = "DECIMAL(11,8)")
	private Double longitudeActuelle;

	@CreatedDate
	@Column(nullable = false, updatable = false)
	private LocalDateTime dateCreation;

	@OneToMany(mappedBy = "vehicule", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private List<Commande> commandes;

	private String photoUrl;
	private String numeroAssurance;
	private String numeroCarteGrise;
}