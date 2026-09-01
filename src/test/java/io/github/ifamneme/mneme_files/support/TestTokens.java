package io.github.ifamneme.mneme_files.support;

import java.io.IOException;
import java.io.InputStream;
import java.security.interfaces.RSAPrivateKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import org.springframework.security.converter.RsaKeyConverters;

// Test tokens signed here, with a key pair kept in src/test/resources and used for
// nothing else. The app's test configuration points public-key-location at the public
// half; wrongSignatureToken() uses a second, unrelated key pair the app never trusts.
public final class TestTokens {

	public static final String ISSUER = "mneme-auth-test";

	public static final String SUBJECT = "test-subject";

	private TestTokens() {
	}

	public static String validToken() {
		return token(SUBJECT, ISSUER, Instant.now().plus(Duration.ofMinutes(5)), testPrivateKey());
	}

	public static String tokenWithSubject(String subject) {
		return token(subject, ISSUER, Instant.now().plus(Duration.ofMinutes(5)), testPrivateKey());
	}

	public static String expiredToken() {
		return token(SUBJECT, ISSUER, Instant.now().minus(Duration.ofMinutes(5)), testPrivateKey());
	}

	public static String wrongIssuerToken() {
		return token(SUBJECT, "someone-else", Instant.now().plus(Duration.ofMinutes(5)), testPrivateKey());
	}

	public static String wrongSignatureToken() {
		return token(SUBJECT, ISSUER, Instant.now().plus(Duration.ofMinutes(5)), wrongSignerPrivateKey());
	}

	private static String token(String subject, String issuer, Instant expiry, RSAPrivateKey signingKey) {
		try {
			JWTClaimsSet claims = new JWTClaimsSet.Builder().subject(subject)
				.issuer(issuer)
				.issueTime(Date.from(Instant.now().minus(Duration.ofMinutes(1))))
				.expirationTime(Date.from(expiry))
				.build();
			SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
			jwt.sign(new RSASSASigner(signingKey));
			return jwt.serialize();
		}
		catch (JOSEException ex) {
			throw new IllegalStateException("Failed to sign test token", ex);
		}
	}

	private static RSAPrivateKey testPrivateKey() {
		return loadPrivateKey("/jwt/test-private-key.pem");
	}

	private static RSAPrivateKey wrongSignerPrivateKey() {
		return loadPrivateKey("/jwt/wrong-signer-private-key.pem");
	}

	private static RSAPrivateKey loadPrivateKey(String classpathResource) {
		try (InputStream in = TestTokens.class.getResourceAsStream(classpathResource)) {
			return RsaKeyConverters.pkcs8().convert(in);
		}
		catch (IOException ex) {
			throw new IllegalStateException("Failed to load " + classpathResource, ex);
		}
	}

}
