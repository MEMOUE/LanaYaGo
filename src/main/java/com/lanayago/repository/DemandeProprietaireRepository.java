
package com.lanayago.repository;

import com.lanayago.entity.DemandeProprietaire;
import com.lanayago.enums.StatutDemandeProprietaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DemandeProprietaireRepository extends JpaRepository<DemandeProprietaire, Long> {

	Optional<DemandeProprietaire> findByUserIdAndStatut(Long userId, StatutDemandeProprietaire statut);

	@Query("SELECT d FROM DemandeProprietaire d WHERE d.user.id = :userId ORDER BY d.dateCreation DESC")
	List<DemandeProprietaire> findByUserIdOrderByDateCreationDesc(@Param("userId") Long userId);

	List<DemandeProprietaire> findByStatutOrderByDateCreationAsc(StatutDemandeProprietaire statut);

	@Query("SELECT COUNT(d) FROM DemandeProprietaire d WHERE d.statut = :statut")
	long countByStatut(@Param("statut") StatutDemandeProprietaire statut);

	@Query("SELECT d FROM DemandeProprietaire d WHERE d.statut = 'EN_ATTENTE' ORDER BY d.dateCreation ASC")
	List<DemandeProprietaire> findDemandesEnAttente();

	boolean existsByUserIdAndStatutIn(Long userId, List<StatutDemandeProprietaire> statuts);
}