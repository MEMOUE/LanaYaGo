package com.lanayago.entity;

import com.lanayago.enums.TypeUtilisateur;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
@EntityListeners(AuditingEntityListener.class)
@Data
@EqualsAndHashCode(of = "id")
public abstract class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String nom;

	@Column(nullable = false, length = 100)
	private String prenom;

	@Column(nullable = false, unique = true, length = 150)
	private String email;

	@Column(nullable = false, length = 20)
	private String telephone;

	@Column(nullable = false)
	private String motDePasse;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TypeUtilisateur typeUtilisateur;

	@Column(nullable = false)
	private Boolean actif = true;

	@CreatedDate
	@Column(nullable = false, updatable = false)
	private LocalDateTime dateCreation;

	@LastModifiedDate
	private LocalDateTime dateModification;

	private String photoUrl;

	@PrePersist
	protected void onCreate() {
		if (dateCreation == null) {
			dateCreation = LocalDateTime.now();
		}
	}

	@PreUpdate
	protected void onUpdate() {
		dateModification = LocalDateTime.now();
	}
}