package com.lanayago.service;

import com.lanayago.entity.Chauffeur;
import com.lanayago.entity.Commande;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

	private final SimpMessagingTemplate messagingTemplate;

	public void notifierNouvelleCommande(List<Chauffeur> chauffeurs, Commande commande) {
		chauffeurs.forEach(chauffeur -> {
			try {
				messagingTemplate.convertAndSend(
						"/topic/chauffeur/" + chauffeur.getId() + "/commandes",
						createCommandeNotification(commande, "NOUVELLE_COMMANDE")
				);
				log.info("Notification envoyée au chauffeur {}", chauffeur.getId());
			} catch (Exception e) {
				log.error("Erreur lors de l'envoi de notification au chauffeur {}", chauffeur.getId(), e);
			}
		});
	}

	public void notifierChangementStatut(Commande commande) {
		try {
			// Notification au client
			messagingTemplate.convertAndSend(
					"/topic/client/" + commande.getClient().getId() + "/commandes",
					createCommandeNotification(commande, "CHANGEMENT_STATUT")
			);

			// Notification au chauffeur si assigné
			if (commande.getChauffeur() != null) {
				messagingTemplate.convertAndSend(
						"/topic/chauffeur/" + commande.getChauffeur().getId() + "/commandes",
						createCommandeNotification(commande, "CHANGEMENT_STATUT")
				);
			}

			log.info("Notifications de changement de statut envoyées pour la commande {}", commande.getId());
		} catch (Exception e) {
			log.error("Erreur lors de l'envoi des notifications de changement de statut", e);
		}
	}

	private Object createCommandeNotification(Commande commande, String type) {
		return new Object() {
			public String getType() { return type; }
			public Long getCommandeId() { return commande.getId(); }
			public String getNumeroCommande() { return commande.getNumeroCommande(); }
			public String getStatut() { return commande.getStatut().name(); }
			public String getMessage() {
				return switch(type) {
					case "NOUVELLE_COMMANDE" -> "Nouvelle commande disponible";
					case "CHANGEMENT_STATUT" -> "Statut de votre commande mis à jour : " + commande.getStatut().getLibelle();
					default -> "Notification";
				};
			}
		};
	}
}