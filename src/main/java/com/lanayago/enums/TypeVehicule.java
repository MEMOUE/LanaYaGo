package com.lanayago.enums;

public enum TypeVehicule {
	CAMIONNETTE("Camionnette", 3.5, 15.0),
	CAMION_LEGER("Camion Léger", 7.5, 25.0),
	CAMION_MOYEN("Camion Moyen", 19.0, 40.0),
	CAMION_LOURD("Camion Lourd", 40.0, 80.0),
	CAMION_FRIGORIFIQUE("Camion Frigorifique", 19.0, 45.0),
	CAMION_BENNE("Camion Benne", 30.0, 35.0);

	private final String libelle;
	private final Double capaciteMaxPoids; // en tonnes
	private final Double capaciteMaxVolume; // en m³

	TypeVehicule(String libelle, Double capaciteMaxPoids, Double capaciteMaxVolume) {
		this.libelle = libelle;
		this.capaciteMaxPoids = capaciteMaxPoids;
		this.capaciteMaxVolume = capaciteMaxVolume;
	}

	public String getLibelle() { return libelle; }
	public Double getCapaciteMaxPoids() { return capaciteMaxPoids; }
	public Double getCapaciteMaxVolume() { return capaciteMaxVolume; }
}