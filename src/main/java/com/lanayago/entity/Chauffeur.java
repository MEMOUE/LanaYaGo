package com.lanayago.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "chauffeurs")
@Data
@EqualsAndHashCode(callSuper = true)
public class Chauffeur extends User {

	@Column(nullable = false, unique = true, length = 20)
	private String numeroPermis;

	@Column(nullable = false)
	private LocalDate dateExpirationPermis;

	@Column(columnDefinition = "DECIMAL(3,2) DEFAULT 0.0")
	private BigDecimal noteMoyenne = BigDecimal.ZERO;

	@Column(nullable = false)
	private Boolean disponible = true;

	@Column(columnDefinition = "DECIMAL(10,8)")
	private Double latitudeActuelle;

	@Column(columnDefinition = "DECIMAL(11,8)")
	private Double longitudeActuelle;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "proprietaire_id")
	private ProprietaireVehicule proprietaire;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "vehicule_actuel_id")
	private Vehicule vehiculeActuel;

	@OneToMany(mappedBy = "chauffeur", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private List<Commande> commandes;

	@Column(nullable = false)
	private Integer nombreCourses = 0;
}