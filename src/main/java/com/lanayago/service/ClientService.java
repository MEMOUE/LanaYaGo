package com.lanayago.service;

import com.lanayago.dto.CommandeDTO;
import com.lanayago.dto.UserDTO;
import com.lanayago.entity.Client;
import com.lanayago.exception.BusinessException;
import com.lanayago.repository.ClientRepository;
import com.lanayago.repository.CommandeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClientService {

	private final ClientRepository clientRepository;
	private final CommandeRepository commandeRepository;
	private final UserMapperService userMapperService;

	@Transactional(readOnly = true)
	public UserDTO getProfilClient(Long clientId) {
		Client client = clientRepository.findById(clientId)
				.orElseThrow(() -> new BusinessException("Client non trouvé"));
		return userMapperService.toDTO(client);
	}

	@Transactional
	public UserDTO mettreAJourProfil(Long clientId, UserDTO request) {
		Client client = clientRepository.findById(clientId)
				.orElseThrow(() -> new BusinessException("Client non trouvé"));

		client.setNom(request.getNom());
		client.setPrenom(request.getPrenom());
		client.setTelephone(request.getTelephone());
		client.setAdresse(request.getAdresse());
		client.setVille(request.getVille());
		client.setCodePostal(request.getCodePostal());

		client = clientRepository.save(client);
		log.info("Profil client {} mis à jour", clientId);

		return userMapperService.toDTO(client);
	}

	@Transactional(readOnly = true)
	public List<CommandeDTO.Response> getHistoriqueCommandes(Long clientId, int page, int size) {
		return commandeRepository.findByClientIdOrderByDateCreationDesc(clientId)
				.stream()
				.skip((long) page * size)
				.limit(size)
				.map(commande -> {
					// Utilisation simplifiée du mapping
					CommandeDTO.Response response = new CommandeDTO.Response();
					response.setId(commande.getId());
					response.setNumeroCommande(commande.getNumeroCommande());
					response.setStatut(commande.getStatut());
					response.setDateCreation(commande.getDateCreation());
					response.setTarifFinal(commande.getTarifFinal());
					return response;
				})
				.toList();
	}

	@Transactional(readOnly = true)
	public Map<String, Object> getStatistiquesClient(Long clientId) {
		Client client = clientRepository.findById(clientId)
				.orElseThrow(() -> new BusinessException("Client non trouvé"));

		Map<String, Object> stats = new HashMap<>();
		stats.put("nombreCommandes", client.getNombreCommandes());
		stats.put("noteMoyenne", client.getNoteMoyenne());
		stats.put("montantTotalDepense", calculerMontantTotalDepense(clientId));
		stats.put("economiesMoyennes", calculerEconomiesMoyennes(clientId));

		return stats;
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> getAdressesFavorites(Long clientId) {
		// Simulation - à remplacer par une vraie entité AdresseFavorite
		return List.of(
				Map.of("nom", "Domicile", "adresse", "123 Rue de la Paix, Paris", "latitude", 48.8566, "longitude", 2.3522),
				Map.of("nom", "Bureau", "adresse", "456 Avenue des Champs, Paris", "latitude", 48.8584, "longitude", 2.2945)
		);
	}

	@Transactional
	public void ajouterAdresseFavorite(Long clientId, Map<String, Object> adresse) {
		// TODO: Implémenter l'ajout d'adresse favorite
		log.info("Adresse favorite ajoutée pour le client {}: {}", clientId, adresse.get("nom"));
	}

	private BigDecimal calculerMontantTotalDepense(Long clientId) {
		return commandeRepository.findByClientIdOrderByDateCreationDesc(clientId)
				.stream()
				.filter(commande -> commande.getTarifFinal() != null)
				.map(commande -> commande.getTarifFinal())
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private BigDecimal calculerEconomiesMoyennes(Long clientId) {
		// Simulation du calcul d'économies
		return BigDecimal.valueOf(15.50);
	}
}