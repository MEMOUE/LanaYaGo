package com.lanayago.service;

import com.lanayago.entity.User;
import com.lanayago.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		User user = userRepository.findByEmailAndActifTrue(email)
				.orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé : " + email));

		List<GrantedAuthority> authorities = List.of(
				new SimpleGrantedAuthority("ROLE_" + user.getTypeUtilisateur().name())
		);

		return org.springframework.security.core.userdetails.User.builder()
				.username(user.getEmail())
				.password(user.getMotDePasse())
				.authorities(authorities)
				.accountExpired(false)
				.accountLocked(!user.getActif())
				.credentialsExpired(false)
				.disabled(!user.getActif())
				.build();
	}
}