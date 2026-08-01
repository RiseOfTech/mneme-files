package io.github.ifamneme.mneme_files.config;

import java.net.URI;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

@Configuration
public class S3ClientConfig {

	@Bean
	public S3Client s3Client(StorageProperties properties) {
		return S3Client.builder()
			.endpointOverride(URI.create(properties.getEndpoint()))
			.region(Region.of(properties.getRegion()))
			.credentialsProvider(StaticCredentialsProvider
				.create(AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey())))
			// Against a non-AWS endpoint the default virtual-host style resolves
			// bucket.host, which does not exist.
			.serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
			.build();
	}

}
