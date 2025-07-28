package com.lanayago.security;

import com.lanayago.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtTokenProvider jwtTokenProvider;
	private final CustomUserDetailsService userDetailsService;

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		return http
				.csrf(AbstractHttpConfigurer::disable)
				.cors(cors -> cors.configurationSource(corsConfigurationSource()))
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						// === ENDPOINTS PUBLICS ===
						.requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
						.requestMatchers("/api/geolocation/distance").permitAll()
						.requestMatchers("/swagger-ui/**", "/api-docs/**", "/swagger-ui.html").permitAll()
						.requestMatchers("/ws/**").permitAll()
						.requestMatchers("api").permitAll()
						.requestMatchers("/api/files/**").permitAll() // Accès aux fichiers

						// === AUTHENTIFICATION ===
						.requestMatchers("/api/auth/statut-demande-proprietaire/**").hasRole("CLIENT")

						// === DEMANDES PROPRIÉTAIRE ===
						.requestMatchers("/api/demandes-proprietaire/user/**").hasRole("CLIENT")
						.requestMatchers("/api/demandes-proprietaire/*/documents/**").hasRole("CLIENT")
						.requestMatchers("/api/demandes-proprietaire/admin/**").hasRole("ADMIN")

						// ✅ NOUVEAU SYSTÈME UNIFIÉ DE PHOTOS
						.requestMatchers("/api/photos/**").hasAnyRole("PROPRIETAIRE_VEHICULE", "CHAUFFEUR", "CLIENT")

						// 🔄 ANCIENS ENDPOINTS (pour rétrocompatibilité - à supprimer plus tard)
						.requestMatchers("/api/proprietaires/*/photo").hasRole("PROPRIETAIRE_VEHICULE")
						.requestMatchers("/api/proprietaires/*/carte-identite").hasRole("PROPRIETAIRE_VEHICULE")
						.requestMatchers("/api/chauffeurs/*/photo").hasAnyRole("CHAUFFEUR", "PROPRIETAIRE_VEHICULE", "CLIENT")
						.requestMatchers("/api/chauffeurs/*/carte-identite").hasAnyRole("CHAUFFEUR", "PROPRIETAIRE_VEHICULE")

						// === RECHERCHE ET TRANSPORT ===
						.requestMatchers("/api/recherche-transport/client/**").hasRole("CLIENT")
						.requestMatchers("/api/recherche-transport/vehicules-disponibles").hasRole("CLIENT")
						.requestMatchers("/api/recherche-transport/session/**").hasRole("CLIENT")
						.requestMatchers("/api/recherche-transport/*/desactiver").hasRole("CLIENT")

						// === SUIVI TRANSPORT ===
						.requestMatchers("/api/suivi-transport/commande/**").hasAnyRole("CLIENT", "CHAUFFEUR")
						.requestMatchers("/api/suivi-transport/chauffeur/**").hasRole("CHAUFFEUR")

						// === COMMANDES ===
						.requestMatchers("/api/commandes/depuis-recherche").hasRole("CLIENT")
						.requestMatchers("/api/commandes/*/accepter/**").hasRole("CHAUFFEUR")
						.requestMatchers("/api/commandes/*/refuser/**").hasRole("CHAUFFEUR")
						.requestMatchers("/api/commandes/*/statut").hasAnyRole("CLIENT", "CHAUFFEUR")
						.requestMatchers("/api/commandes/*/evaluer").hasAnyRole("CLIENT", "CHAUFFEUR")
						.requestMatchers("/api/commandes/client/**").hasRole("CLIENT")
						.requestMatchers("/api/commandes/chauffeur/**").hasRole("CHAUFFEUR")
						.requestMatchers("/api/commandes/proprietaire/**").hasRole("PROPRIETAIRE_VEHICULE")
						.requestMatchers("/api/commandes/**").hasAnyRole("CLIENT", "CHAUFFEUR", "PROPRIETAIRE_VEHICULE")

						// === GESTION CHAUFFEURS ===
						.requestMatchers("/api/chauffeurs/proprietaire/**").hasRole("PROPRIETAIRE_VEHICULE")
						.requestMatchers("/api/chauffeurs/*/disponibilite").hasAnyRole("CHAUFFEUR", "PROPRIETAIRE_VEHICULE")
						.requestMatchers("/api/chauffeurs/disponibles").hasAnyRole("CLIENT", "PROPRIETAIRE_VEHICULE")

						// === GESTION VÉHICULES ===
						.requestMatchers("/api/vehicules/proprietaire/**").hasRole("PROPRIETAIRE_VEHICULE")
						.requestMatchers("/api/vehicules/disponibles").hasAnyRole("CLIENT", "CHAUFFEUR", "PROPRIETAIRE_VEHICULE")
						.requestMatchers("/api/vehicules/*/disponibilite").hasAnyRole("CHAUFFEUR", "PROPRIETAIRE_VEHICULE")

						// === GÉOLOCALISATION ===
						.requestMatchers("/api/geolocation/chauffeur/**").hasRole("CHAUFFEUR")

						// === ADMINISTRATION ===
						.requestMatchers("/api/admin/**").hasRole("ADMIN")

						// Tout le reste nécessite une authentification
						.anyRequest().authenticated())
				.addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider, userDetailsService),
						UsernamePasswordAuthenticationFilter.class)
				.build();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
		return config.getAuthenticationManager();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOriginPatterns(Arrays.asList("*"));
		configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(Arrays.asList("*"));
		configuration.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
}