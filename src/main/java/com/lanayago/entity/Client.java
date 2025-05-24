package com.lanayago.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "clients")
@Data
@EqualsAndHashCode(callSuper = true)
public class Client extends User {

	@Column(length = 255)
	private String adresse;

	@Column(columnDefinition = "DECIMAL(3,2) DEFAULT 0.0")
	private BigDecimal noteMoyenne = BigDecimal.ZERO;

	@Column(nullable = false)
	private Integer nombreCommandes = 0;

	@Column(length = 100)
	private String ville;

	@Column(length = 10)
	private String codePostal;

	@OneToMany(mappedBy = "client", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private List<Commande> commandes;
}