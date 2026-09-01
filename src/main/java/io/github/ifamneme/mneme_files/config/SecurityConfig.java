package io.github.ifamneme.mneme_files.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import io.github.ifamneme.mneme_files.adapter.web.ProblemJsonAuthenticationEntryPoint;

// Every valid token is equally acceptable: no role or authority checks, no scopes, no
// custom JwtAuthenticationConverter. This service verifies signature, expiry, and issuer only -
// it has no domain knowledge to authorize against.
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http,
			ProblemJsonAuthenticationEntryPoint problemJsonAuthenticationEntryPoint) throws Exception {
		http.csrf(csrf -> csrf.disable())
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(authorize -> authorize.requestMatchers(HttpMethod.GET, "/v1/files/*")
				.permitAll()
				.requestMatchers("/actuator/health")
				.permitAll()
				.anyRequest()
				.authenticated())
			.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults())
				.authenticationEntryPoint(problemJsonAuthenticationEntryPoint));
		return http.build();
	}

}
