package com.lanayago.service;

import com.lanayago.dto.CommandeDTO;
import com.lanayago.enums.TypeVehicule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@Slf4j
public class TarificationService {

	@Value("${tarification.tarif-base-km:2.5}")
	private Double tarifBaseParKm;

	@Value("${tarification.tarif-minimum:25.0}")
	private Double tarifMinimum;

	public BigDecimal calculerTarif(CommandeDTO.CreateRequest request, Double distance, TypeVehicule typeVehicule) {
		log.info("Calcul du tarif pour une distance de {} km avec véhicule {}", distance, typeVehicule);

		// Tarif de base selon la distance
		BigDecimal tarifBase = BigDecimal.valueOf(distance * tarifBaseParKm);

		// Coefficient selon le type de véhicule
		BigDecimal coefficientVehicule = getCoefficientTypeVehicule(typeVehicule);

		// Coefficient selon le poids
		BigDecimal coefficientPoids = getCoefficientPoids(request.getPoidsMarchandise());

		// Coefficient urgence
		BigDecimal coefficientUrgence = request.getUrgent() ?
				BigDecimal.valueOf(1.5) : BigDecimal.ONE;

		// Coefficient horaire (heures pleines/creuses)
		BigDecimal coefficientHoraire = getCoefficientHoraire(LocalDateTime.now());

		// Calcul final
		BigDecimal tarifCalcule = tarifBase
				.multiply(coefficientVehicule)
				.multiply(coefficientPoids)
				.multiply(coefficientUrgence)
				.multiply(coefficientHoraire);

		// Application du tarif minimum
		BigDecimal tarifMinimumBD = BigDecimal.valueOf(tarifMinimum);
		tarifCalcule = tarifCalcule.max(tarifMinimumBD);

		// Arrondi à 2 décimales
		tarifCalcule = tarifCalcule.setScale(2, RoundingMode.HALF_UP);

		log.info("Tarif calculé: {} €", tarifCalcule);
		return tarifCalcule;
	}

	private BigDecimal getCoefficientTypeVehicule(TypeVehicule type) {
		return switch (type) {
			case CAMIONNETTE -> BigDecimal.valueOf(1.0);
			case CAMION_LEGER -> BigDecimal.valueOf(1.3);
			case CAMION_MOYEN -> BigDecimal.valueOf(1.6);
			case CAMION_LOURD -> BigDecimal.valueOf(2.0);
			case CAMION_FRIGORIFIQUE -> BigDecimal.valueOf(1.8);
			case CAMION_BENNE -> BigDecimal.valueOf(1.7);
		};
	}

	private BigDecimal getCoefficientPoids(BigDecimal poids) {
		double poidsDouble = poids.doubleValue();
		if (poidsDouble <= 100) return BigDecimal.valueOf(1.0);
		if (poidsDouble <= 500) return BigDecimal.valueOf(1.1);
		if (poidsDouble <= 1000) return BigDecimal.valueOf(1.2);
		if (poidsDouble <= 5000) return BigDecimal.valueOf(1.4);
		return BigDecimal.valueOf(1.6);
	}

	private BigDecimal getCoefficientHoraire(LocalDateTime dateTime) {
		int heure = dateTime.getHour();
		// Heures pleines : 7h-9h et 17h-19h
		if ((heure >= 7 && heure <= 9) || (heure >= 17 && heure <= 19)) {
			return BigDecimal.valueOf(1.2);
		}
		// Heures creuses : 22h-6h
		if (heure >= 22 || heure <= 6) {
			return BigDecimal.valueOf(0.9);
		}
		// Heures normales
		return BigDecimal.valueOf(1.0);
	}
}