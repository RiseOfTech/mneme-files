package io.github.ifamneme.mneme_files.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class FileUploadTest {

	@Test
	void acceptsSizeAtBoundary() {
		assertThatCode(() -> FileUpload.validateSize(10, 10)).doesNotThrowAnyException();
	}

	@Test
	void rejectsSizeAboveLimit() {
		assertThatThrownBy(() -> FileUpload.validateSize(11, 10)).isInstanceOf(FileTooLargeException.class);
	}

}
