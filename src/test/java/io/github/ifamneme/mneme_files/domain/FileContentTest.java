package io.github.ifamneme.mneme_files.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class FileContentTest {

	@Test
	void acceptsSizeAtBoundary() {
		assertThatCode(() -> FileContent.validateSize(10, 10)).doesNotThrowAnyException();
	}

	@Test
	void rejectsSizeAboveLimit() {
		assertThatThrownBy(() -> FileContent.validateSize(11, 10)).isInstanceOf(FileTooLargeException.class);
	}

}
