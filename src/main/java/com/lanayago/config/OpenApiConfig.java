package com.lanayago.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

	@Value("${server.port:8080}")
	private String serverPort;

	@Value("${app.version:1.0.0}")
	private String appVersion;

	@Bean
	public OpenAPI customOpenAPI() {
		return new OpenAPI()
				.info(apiInfo())
				.servers(List.of(
						localServer(),
						productionServer()
				))
				.tags(List.of(
//						authTag(),
//						commandesTag(),
//						vehiculesTag(),
//						geolocationTag(),
//						clientsTag(),
//						chauffeursTag(),
//						documentsTag(),
//						adminTag()
				))
				.components(securityComponents())
				.addSecurityItem(securityRequirement());
	}

	private Info apiInfo() {
		return new Info()
				.title("🚚 LanaYaGo API")
				.description("""
                        **API REST pour la plateforme de transport LanaYaGo**
                        
                        Cette API permet de gérer :
                        - 👤 **Authentification** des clients, chauffeurs et propriétaires
                        - 📍 **Géolocalisation** temps réel avec Google Maps
                        - 📦 **Commandes** de transport avec géolocalisation
                        - 🚛 **Véhicules** et gestion de flotte
                        - 👥 **Gestion des utilisateurs** (clients et chauffeurs)
                        - 📄 **Documents** et validation administrative
                        - ⭐ **Évaluations** et système de notation
                        - 💰 **Tarification** dynamique
                        - ⚙️ **Administration** et statistiques
                        
                        ## 🔐 Authentification
                        
                        L'API utilise **JWT Bearer Token** pour l'authentification.
                        
                        ### Étapes pour s'authentifier :
                        1. **Inscription** : `POST /api/auth/register`
                        2. **Connexion** : `POST /api/auth/login`
                        3. **Utiliser le token** : Ajouter `Authorization: Bearer <token>` dans les headers
                        
                        ## 👥 Types d'utilisateurs
                        
                        - **CLIENT** : Peut créer et suivre des commandes, gérer son profil
                        - **CHAUFFEUR** : Peut accepter et traiter des commandes, gérer sa disponibilité
                        - **PROPRIETAIRE_VEHICULE** : Peut gérer sa flotte, ses chauffeurs et accéder aux fonctions admin
                        
                        ## 🔄 Flux d'utilisation
                        
                        ### Pour un client :
                        1. S'inscrire comme CLIENT
                        2. Créer une commande avec géolocalisation
                        3. Suivre l'évolution de la commande
                        4. Évaluer le service
                        
                        ### Pour un chauffeur :
                        1. S'inscrire comme CHAUFFEUR
                        2. Être assigné à un propriétaire de véhicule
                        3. Accepter des commandes disponibles
                        4. Mettre à jour sa position et le statut des commandes
                        
                        ### Pour un propriétaire :
                        1. S'inscrire comme PROPRIETAIRE_VEHICULE
                        2. Ajouter des véhicules à sa flotte
                        3. Gérer ses chauffeurs
                        4. Accéder aux statistiques et à l'administration
                        
                        ## 📱 Intégration Mobile
                        
                        Cette API est conçue pour être consommée par :
                        - Applications mobiles (iOS/Android)
                        - Applications web (Angular/React/Vue.js)
                        - Systèmes tiers via REST
                        - Notifications temps réel via WebSocket
                        
                        ## 🌐 WebSocket
                        
                        Connexion WebSocket disponible sur `/ws` pour :
                        - Notifications de nouvelles commandes
                        - Mises à jour de statut en temps réel
                        - Suivi géolocalisation temps réel
                        """)
				.version(appVersion)
				.contact(apiContact())
				.license(apiLicense());
	}

	private Contact apiContact() {
		return new Contact()
				.name("Équipe LanaYaGo")
				.email("contact@lanayago.com")
				.url("https://lanayago.com");
	}

	private License apiLicense() {
		return new License()
				.name("Propriétaire - LanaYaGo")
				.url("https://lanayago.com/licence");
	}

	private Server localServer() {
		return new Server()
				.url("http://localhost:" + serverPort)
				.description("🖥️ Serveur de développement local");
	}

	private Server productionServer() {
		return new Server()
				.url("https://api.lanayago.com")
				.description("🌐 Serveur de production");
	}



	// Configuration de la sécurité JWT
	private Components securityComponents() {
		return new Components()
				.addSecuritySchemes("bearerAuth", jwtSecurityScheme());
	}

	private SecurityScheme jwtSecurityScheme() {
		return new SecurityScheme()
				.type(SecurityScheme.Type.HTTP)
				.scheme("bearer")
				.bearerFormat("JWT")
				.description("""
                        **Token JWT Bearer**
                        
                        Pour obtenir un token :
                        1. Créez un compte : `POST /api/auth/register`
                        2. Connectez-vous : `POST /api/auth/login`
                        3. Utilisez le token retourné dans le header Authorization
                        
                        Format : `Authorization: Bearer <votre-token>`
                        
                        **Durée de validité** : 24 heures
                        **Refresh Token** : 7 jours
                        
                        **Exemple d'utilisation** :
                        ```
                        Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
                        ```
                        """);
	}

	private SecurityRequirement securityRequirement() {
		return new SecurityRequirement()
				.addList("bearerAuth");
	}
}