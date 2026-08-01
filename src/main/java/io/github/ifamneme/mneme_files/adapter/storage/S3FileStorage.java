package io.github.ifamneme.mneme_files.adapter.storage;

import java.io.IOException;
import java.util.Optional;

import org.springframework.stereotype.Component;

import io.github.ifamneme.mneme_files.config.StorageProperties;
import io.github.ifamneme.mneme_files.domain.ContentType;
import io.github.ifamneme.mneme_files.domain.FileContent;
import io.github.ifamneme.mneme_files.domain.FileId;
import io.github.ifamneme.mneme_files.domain.FileStorage;
import io.github.ifamneme.mneme_files.domain.FileStorageException;
import io.github.ifamneme.mneme_files.domain.StoredFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
public class S3FileStorage implements FileStorage {

	private final S3Client s3Client;
	private final StorageProperties properties;

	public S3FileStorage(S3Client s3Client, StorageProperties properties) {
		this.s3Client = s3Client;
		this.properties = properties;
	}

	@Override
	public void store(FileId id, FileContent content) {
		PutObjectRequest request = PutObjectRequest.builder()
			.bucket(properties.getBucket())
			.key(key(id))
			.contentType(content.contentType().value())
			.contentLength(content.size())
			.build();
		try (var data = content.data()) {
			s3Client.putObject(request, RequestBody.fromInputStream(data, content.size()));
		}
		catch (SdkException ex) {
			throw new FileStorageException("Failed to store file " + id.value(), ex);
		}
		catch (IOException ex) {
			throw new FileStorageException("Failed to read file " + id.value() + " for storage", ex);
		}
	}

	@Override
	public Optional<StoredFile> load(FileId id) {
		GetObjectRequest request = GetObjectRequest.builder().bucket(properties.getBucket()).key(key(id)).build();
		try {
			var response = s3Client.getObject(request);
			GetObjectResponse metadata = response.response();
			return Optional
				.of(new StoredFile(id, new ContentType(metadata.contentType()), metadata.contentLength(),
						metadata.eTag(), response));
		}
		catch (NoSuchKeyException ex) {
			return Optional.empty();
		}
		catch (SdkException ex) {
			throw new FileStorageException("Failed to load file " + id.value(), ex);
		}
	}

	// Flat, no prefix, no extension: an extension in the key would be a second source
	// of truth about content type, competing with the object's own metadata.
	private static String key(FileId id) {
		return id.value().toString();
	}

}
