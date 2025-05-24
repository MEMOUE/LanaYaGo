package com.lanayago.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Entity
@Table(name = "proprietaires_vehicules")
@Data
@EqualsAndHashCode(callSuper = true)
public class ProprietaireVehicule extends User {

	@Column(length = 150)
	private String nomEntreprise;

	@Column(unique = true, length = 14)
	private String numeroSiret;

	@Column(length = 255)
	private String adresseEntreprise;

	@OneToMany(mappedBy = "proprietaire", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private List<Vehicule> vehicules;

	@OneToMany(mappedBy = "proprietaire", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private List<Chauffeur> chauffeurs;
}