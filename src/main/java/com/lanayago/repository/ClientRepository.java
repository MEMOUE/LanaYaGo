package com.lanayago.repository;

import com.lanayago.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClientRepository extends JpaRepository<Client, Long> {

	@Query("SELECT c FROM Client c WHERE c.actif = true ORDER BY c.nombreCommandes DESC")
	List<Client> findTopClients();

	@Query("SELECT c FROM Client c WHERE c.ville = :ville AND c.actif = true")
	List<Client> findByVille(@Param("ville") String ville);

	@Query("SELECT AVG(c.noteMoyenne) FROM Client c WHERE c.actif = true")
	Double getNoteMoyenneGlobale();
}