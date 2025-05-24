package com.lanayago.repository;

import com.lanayago.entity.Chauffeur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChauffeurRepository extends JpaRepository<Chauffeur, Long> {

	@Query("SELECT c FROM Chauffeur c WHERE c.disponible = true AND c.actif = true")
	List<Chauffeur> findChauffeursDisponibles();

	@Query("SELECT c FROM Chauffeur c WHERE c.proprietaire.id = :proprietaireId AND c.actif = true")
	List<Chauffeur> findByProprietaireId(@Param("proprietaireId") Long proprietaireId);

	@Query("""
        SELECT c FROM Chauffeur c 
        WHERE c.disponible = true 
        AND c.actif = true 
        AND c.latitudeActuelle IS NOT NULL 
        AND c.longitudeActuelle IS NOT NULL
        AND (6371 * acos(cos(radians(:latitude)) * cos(radians(c.latitudeActuelle)) 
        * cos(radians(c.longitudeActuelle) - radians(:longitude)) 
        + sin(radians(:latitude)) * sin(radians(c.latitudeActuelle)))) <= :rayon
        """)
	List<Chauffeur> findChauffeursProches(@Param("latitude") Double latitude,
	                                      @Param("longitude") Double longitude,
	                                      @Param("rayon") Double rayon);

	boolean existsByNumeroPermis(String numeroPermis);
}