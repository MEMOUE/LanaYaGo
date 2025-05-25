package com.lanayago.repository;

import com.lanayago.entity.Document;
import com.lanayago.enums.StatutDocument;
import com.lanayago.enums.TypeDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {

	List<Document> findByUtilisateurIdOrderByDateCreationDesc(Long utilisateurId);

	Page<Document> findByStatutOrderByDateCreationDesc(StatutDocument statut, Pageable pageable);

	Page<Document> findAllByOrderByDateCreationDesc(Pageable pageable);

	List<Document> findByUtilisateurIdAndTypeDocument(Long utilisateurId, TypeDocument typeDocument);

	@Query("SELECT d FROM Document d WHERE d.dateExpiration < :now AND d.statut = 'VALIDE'")
	List<Document> findDocumentsExpires(@Param("now") LocalDateTime now);

	@Query("SELECT COUNT(d) FROM Document d WHERE d.statut = :statut")
	long countByStatut(@Param("statut") StatutDocument statut);

	boolean existsByUtilisateurIdAndTypeDocumentAndStatut(Long utilisateurId, TypeDocument typeDocument, StatutDocument statut);
}