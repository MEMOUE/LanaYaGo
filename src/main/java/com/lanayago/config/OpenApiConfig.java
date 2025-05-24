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
                        - 📍 **Géolocalisation** temps réel
                        - 📦 **Commandes** de transport avec géolocalisation
                        - 🚛 **Véhicules** et gestion de flotte
                       
                        - ⭐ **Évaluations** et système de notation
                        - 💰 **Tarification** dynamique
                        
                        ## 🔐 Authentification
                        
                        L'API utilise **JWT Bearer Token** pour l'authentification.
                        
                        ### Étapes pour s'authentifier :
                        1. **Inscription** : `POST /api/auth/register`
                        2. **Connexion** : `POST /api/auth/login`
                        3. **Utiliser le token** : Ajouter `Authorization: Bearer <token>` dans les headers
                        
                        ## 👥 Types d'utilisateurs
                        
                        - **CLIENT** : Peut créer et suivre des commandes
                        - **CHAUFFEUR** : Peut accepter et traiter des commandes
                        - **PROPRIETAIRE_VEHICULE** : Peut gérer sa flotte et ses chauffeurs
                        
                        ## 📱 Intégration Mobile
                        
                        Cette API est conçue pour être consommée par :
                        - Applications mobiles (iOS/Android)
                        - Applications web (Angular/React)
                        - Systèmes tiers
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

	// Tags pour organiser les endpoints
//	private Tag authTag() {
//		return new Tag()
//				.name("🔐 Authentification")
//				.description("Inscription, connexion et gestion des utilisateurs");
//	}
//
//	private Tag geolocationTag() {
//		return new Tag()
//				.name("📍 Géolocalisation")
//				.description("Services de géolocalisation et cartographie");
//	}
//
//	private Tag commandesTag() {
//		return new Tag()
//				.name("📦 Commandes")
//				.description("Création, gestion et suivi des commandes de transport");
//	}
//
//	private Tag vehiculesTag() {
//		return new Tag()
//				.name("🚛 Véhicules")
//				.description("Gestion des véhicules et flottes");
//	}
//
//
//
//	private Tag adminTag() {
//		return new Tag()
//				.name("⚙️ Administration")
//				.description("Fonctionnalités d'administration (propriétaires de flotte)");
//	}

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
                        """);
	}

	private SecurityRequirement securityRequirement() {
		return new SecurityRequirement()
				.addList("bearerAuth");
	}
}