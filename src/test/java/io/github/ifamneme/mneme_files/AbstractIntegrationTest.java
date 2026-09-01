package io.github.ifamneme.mneme_files;

import java.net.URI;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

import io.github.ifamneme.mneme_files.support.TestTokens;

@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

	protected static final String BUCKET = "mneme-files-test";
	protected static final String ACCESS_KEY = "test-access-key";
	protected static final String SECRET_KEY = "test-secret-key";
	private static final int S3_PORT = 8333;

	// SeaweedFS rejects signed (SigV4) requests unless an explicit S3 identity config is
	// supplied - the AWS SDK always signs, so without this every call fails with
	// "Signed request requires setting up SeaweedFS S3 authentication".
	private static final String S3_CONFIG_JSON = """
			{
			  "identities": [
			    {
			      "name": "test",
			      "credentials": [
			        { "accessKey": "%s", "secretKey": "%s" }
			      ],
			      "actions": ["Admin", "Read", "Write"]
			    }
			  ]
			}
			""".formatted(ACCESS_KEY, SECRET_KEY);

	// Started once per JVM (Testcontainers' "singleton container" pattern), not via
	// @Container/@Testcontainers, whose lifecycle is scoped per test class and would
	// stop and recreate this container between every subclass.
	private static final GenericContainer<?> seaweedFs = new GenericContainer<>(
			DockerImageName.parse("chrislusf/seaweedfs:4.40"))
		.withCommand("server", "-dir=/data", "-s3", "-s3.config=/etc/seaweedfs/s3.json")
		.withCopyToContainer(Transferable.of(S3_CONFIG_JSON), "/etc/seaweedfs/s3.json")
		.withExposedPorts(S3_PORT)
		.waitingFor(Wait.forListeningPort());

	static {
		seaweedFs.start();
		// The bucket is provisioned by test setup, never by application code - the
		// service assumes it exists and fails fast on startup if it does not.
		try (S3Client bootstrapClient = rawS3Client()) {
			bootstrapClient.createBucket(CreateBucketRequest.builder().bucket(BUCKET).build());
		}
	}

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add("mneme.files.storage.endpoint", AbstractIntegrationTest::endpoint);
		registry.add("mneme.files.storage.bucket", () -> BUCKET);
		registry.add("mneme.files.storage.region", () -> "us-east-1");
		registry.add("mneme.files.storage.access-key", () -> ACCESS_KEY);
		registry.add("mneme.files.storage.secret-key", () -> SECRET_KEY);
		// Every test context is a resource server now; point it at the dedicated test
		// signing key pair (never the dev classpath default, whose private half doesn't
		// exist anywhere) and the issuer TestTokens signs with.
		registry.add("spring.security.oauth2.resourceserver.jwt.public-key-location",
				() -> "classpath:jwt/test-public-key.pem");
		registry.add("mneme.security.jwt.issuer", () -> TestTokens.ISSUER);
	}

	protected static String endpoint() {
		return "http://" + seaweedFs.getHost() + ":" + seaweedFs.getMappedPort(S3_PORT);
	}

	private static S3Client rawS3Client() {
		return S3Client.builder()
			.endpointOverride(URI.create(endpoint()))
			.region(Region.of("us-east-1"))
			.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(ACCESS_KEY, SECRET_KEY)))
			.serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
			.build();
	}

}
