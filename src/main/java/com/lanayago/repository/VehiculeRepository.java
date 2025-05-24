package com.lanayago.repository;

import com.lanayago.entity.Vehicule;
import com.lanayago.enums.TypeVehicule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VehiculeRepository extends JpaRepository<Vehicule, Long> {

	@Query("SELECT v FROM Vehicule v WHERE v.disponible = true")
	List<Vehicule> findVehiculesDisponibles();

	@Query("SELECT v FROM Vehicule v WHERE v.proprietaire.id = :proprietaireId")
	List<Vehicule> findByProprietaireId(@Param("proprietaireId") Long proprietaireId);

	@Query("""
        SELECT v FROM Vehicule v 
        WHERE v.disponible = true 
        AND v.typeVehicule = :type 
        AND v.capacitePoids >= :poidsMin
        """)
	List<Vehicule> findVehiculesCompatibles(@Param("type") TypeVehicule type,
	                                        @Param("poidsMin") Double poidsMin);

	boolean existsByImmatriculation(String immatriculation);

	@Query("SELECT COUNT(v) FROM Vehicule v WHERE v.proprietaire.id = :proprietaireId")
	long countByProprietaireId(@Param("proprietaireId") Long proprietaireId);
}