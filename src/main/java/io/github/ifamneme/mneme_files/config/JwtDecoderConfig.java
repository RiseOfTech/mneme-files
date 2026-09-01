package io.github.ifamneme.mneme_files.config;

import java.io.IOException;
import java.io.InputStream;
import java.security.interfaces.RSAPublicKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
public class JwtDecoderConfig {

	@Bean
	public JwtDecoder jwtDecoder(
			@Value("${spring.security.oauth2.resourceserver.jwt.public-key-location}") Resource publicKeyLocation,
			JwtProperties jwtProperties) throws IOException {
		RSAPublicKey publicKey;
		try (InputStream in = publicKeyLocation.getInputStream()) {
			publicKey = RsaKeyConverters.x509().convert(in);
		}

		NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(publicKey).build();
		decoder.setJwtValidator(issuerValidator(jwtProperties.getIssuer()));
		return decoder;
	}

	// Extracted so the plain-JUnit issuer test exercises the real validator: expiry is
	// checked by default only when the key is discovered via OIDC metadata, which this
	// service deliberately does not use, so the issuer check must be added explicitly here.
	static OAuth2TokenValidator<Jwt> issuerValidator(String issuer) {
		return JwtValidators.createDefaultWithIssuer(issuer);
	}

}
