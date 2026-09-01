package io.github.ifamneme.mneme_files.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

// Plain JUnit, no Spring context: exercises JwtDecoderConfig.issuerValidator(...) directly,
// so this fails if that method is ever weakened to fall back on JwtValidators.createDefault()
// (i.e. drops the issuer check) - which the framework's defaults do not provide on their own
// when the key is configured directly rather than resolved through OIDC discovery metadata.
class JwtIssuerValidationTest {

	private static final String ISSUER = "mneme-auth";

	private final OAuth2TokenValidator<Jwt> validator = JwtDecoderConfig.issuerValidator(ISSUER);

	@Test
	void acceptsTokenWithConfiguredIssuer() {
		OAuth2TokenValidatorResult result = validator.validate(token(ISSUER));

		assertThat(result.hasErrors()).isFalse();
	}

	@Test
	void rejectsTokenWithDifferentIssuer() {
		OAuth2TokenValidatorResult result = validator.validate(token("someone-else"));

		assertThat(result.hasErrors()).isTrue();
	}

	private Jwt token(String issuer) {
		return Jwt.withTokenValue("token")
			.header("alg", "RS256")
			.claim("sub", "test-subject")
			.issuer(issuer)
			.issuedAt(Instant.now())
			.expiresAt(Instant.now().plusSeconds(300))
			.build();
	}

}
