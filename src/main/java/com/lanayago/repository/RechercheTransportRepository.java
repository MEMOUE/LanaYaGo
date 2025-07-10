// RechercheTransportRepository
package com.lanayago.repository;

import com.lanayago.entity.RechercheTransport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RechercheTransportRepository extends JpaRepository<RechercheTransport, Long> {

	@Query("SELECT r FROM RechercheTransport r WHERE r.client.id = :clientId AND r.active = true ORDER BY r.dateCreation DESC")
	List<RechercheTransport> findActiveByClientId(@Param("clientId") Long clientId);

	Optional<RechercheTransport> findBySessionId(String sessionId);

	@Query("SELECT r FROM RechercheTransport r WHERE r.active = true AND r.dateCreation >= :since")
	List<RechercheTransport> findActiveRecherches(@Param("since") LocalDateTime since);

	@Query("UPDATE RechercheTransport r SET r.active = false WHERE r.dateCreation < :expiredDate")
	void desactiverRecherchesPerirees(@Param("expiredDate") LocalDateTime expiredDate);
}