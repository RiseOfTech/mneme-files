package io.github.ifamneme.mneme_files.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class MagicByteContentTypeDetectorTest {

	private final MagicByteContentTypeDetector detector = new MagicByteContentTypeDetector();

	@Test
	void detectsJpegFromRealFixture() throws IOException {
		assertThat(detect("valid.jpg")).contains(new ContentType("image/jpeg"));
	}

	@Test
	void detectsPngFromRealFixture() throws IOException {
		assertThat(detect("valid.png")).contains(new ContentType("image/png"));
	}

	@Test
	void detectsWebpFromRealFixture() throws IOException {
		assertThat(detect("valid.webp")).contains(new ContentType("image/webp"));
	}

	@Test
	void rejectsTextFile() throws IOException {
		assertThat(detect("text.txt")).isEmpty();
	}

	@Test
	void rejectsSvgDocument() throws IOException {
		assertThat(detect("image.svg")).isEmpty();
	}

	@Test
	void rejectsTruncatedHeader() throws IOException {
		byte[] truncated = Arrays.copyOf(readFixture("valid.jpg"), 2);

		assertThat(detector.detect(truncated)).isEmpty();
	}

	@Test
	void detectionHasNoParameterForADeclaredContentType() throws IOException {
		// detect(byte[]) has no declared-content-type parameter at all, so a JPEG's
		// detection is structurally unaffected by whatever the client claimed - there is
		// nothing here for a declared value to influence.
		assertThat(detect("valid.jpg")).contains(new ContentType("image/jpeg"));
	}

	private Optional<ContentType> detect(String fixture) throws IOException {
		return detector.detect(readFixture(fixture));
	}

	private byte[] readFixture(String name) throws IOException {
		try (InputStream in = getClass().getResourceAsStream("/fixtures/" + name)) {
			return in.readAllBytes();
		}
	}

}
