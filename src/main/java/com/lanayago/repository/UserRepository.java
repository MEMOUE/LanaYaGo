package com.lanayago.repository;

import com.lanayago.entity.User;
import com.lanayago.enums.TypeUtilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByEmail(String email);
	Optional<User> findByEmailAndActifTrue(String email);
	boolean existsByEmail(String email);
	boolean existsByTelephone(String telephone);

	@Query("SELECT COUNT(u) FROM User u WHERE u.typeUtilisateur = :type AND u.actif = true")
	long countByTypeUtilisateurAndActifTrue(@Param("type") TypeUtilisateur type);
}