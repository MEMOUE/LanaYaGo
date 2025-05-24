package com.lanayago.repository;

import com.lanayago.entity.Commande;
import com.lanayago.enums.StatutCommande;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CommandeRepository extends JpaRepository<Commande, Long> {

	List<Commande> findByClientIdOrderByDateCreationDesc(Long clientId);
	List<Commande> findByChauffeurIdOrderByDateCreationDesc(Long chauffeurId);
	List<Commande> findByStatut(StatutCommande statut);

	@Query("SELECT c FROM Commande c WHERE c.statut = :statut ORDER BY c.dateCreation ASC")
	List<Commande> findByStatutOrderByDateCreationAsc(@Param("statut") StatutCommande statut);

	@Query("""
        SELECT c FROM Commande c 
        WHERE c.vehicule.proprietaire.id = :proprietaireId 
        ORDER BY c.dateCreation DESC
        """)
	List<Commande> findByProprietaireId(@Param("proprietaireId") Long proprietaireId);

	@Query("""
        SELECT c FROM Commande c 
        WHERE c.dateCreation BETWEEN :debut AND :fin 
        AND c.statut = 'LIVREE'
        """)
	List<Commande> findCommandesLivreesEntreDates(@Param("debut") LocalDateTime debut,
	                                              @Param("fin") LocalDateTime fin);

	@Query("SELECT SUM(c.tarifFinal) FROM Commande c WHERE c.statut = 'LIVREE' AND c.chauffeur.id = :chauffeurId")
	Double getChiffreAffaireChauffeur(@Param("chauffeurId") Long chauffeurId);

	@Query("SELECT COUNT(c) FROM Commande c WHERE c.statut = :statut")
	long countByStatut(@Param("statut") StatutCommande statut);
}