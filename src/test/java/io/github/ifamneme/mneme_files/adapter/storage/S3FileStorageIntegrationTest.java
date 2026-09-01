package io.github.ifamneme.mneme_files.adapter.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import io.github.ifamneme.mneme_files.AbstractIntegrationTest;
import io.github.ifamneme.mneme_files.config.StorageProperties;
import io.github.ifamneme.mneme_files.domain.ContentType;
import io.github.ifamneme.mneme_files.domain.FileId;
import io.github.ifamneme.mneme_files.domain.FileStorage;
import io.github.ifamneme.mneme_files.domain.FileUpload;
import io.github.ifamneme.mneme_files.domain.StoredFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;

class S3FileStorageIntegrationTest extends AbstractIntegrationTest {

	@Autowired
	private FileStorage fileStorage;

	@Autowired
	private S3Client s3Client;

	@Autowired
	private StorageProperties storageProperties;

	@Test
	void storesAndLoadsFileWithIdenticalBytesContentTypeAndSize() throws IOException {
		byte[] bytes = readFixture("valid.png");
		FileId id = FileId.generate();

		fileStorage.store(id, new FileUpload(new ByteArrayInputStream(bytes), bytes.length,
				new ContentType("image/png"), "test-subject"));

		StoredFile loaded = fileStorage.load(id).orElseThrow();
		try {
			assertThat(loaded.contentType()).isEqualTo(new ContentType("image/png"));
			assertThat(loaded.size()).isEqualTo(bytes.length);
			assertThat(loaded.data().readAllBytes()).isEqualTo(bytes);
		}
		finally {
			loaded.data().close();
		}
	}

	@Test
	void loadingAbsentIdYieldsEmptyOptional() {
		assertThat(fileStorage.load(FileId.generate())).isEmpty();
	}

	@Test
	void objectIsStoredUnderTheFlatIdKey() throws IOException {
		byte[] bytes = readFixture("valid.jpg");
		FileId id = FileId.generate();

		fileStorage.store(id, new FileUpload(new ByteArrayInputStream(bytes), bytes.length,
				new ContentType("image/jpeg"), "test-subject"));

		// No prefix, no directory segment, no extension: the key is exactly the id's
		// string form.
		var head = s3Client
			.headObject(HeadObjectRequest.builder().bucket(storageProperties.getBucket())
				.key(id.value().toString())
				.build());
		assertThat(head.contentLength()).isEqualTo((long) bytes.length);
	}

	@Test
	void objectCarriesUploaderAsUserMetadata() throws IOException {
		byte[] bytes = readFixture("valid.webp");
		FileId id = FileId.generate();

		fileStorage.store(id, new FileUpload(new ByteArrayInputStream(bytes), bytes.length,
				new ContentType("image/webp"), "test-subject-42"));

		var head = s3Client
			.headObject(HeadObjectRequest.builder().bucket(storageProperties.getBucket()).key(id.value().toString())
				.build());
		assertThat(head.metadata()).containsEntry("uploader", "test-subject-42");
	}

	private byte[] readFixture(String name) throws IOException {
		try (InputStream in = getClass().getResourceAsStream("/fixtures/" + name)) {
			return in.readAllBytes();
		}
	}

}
