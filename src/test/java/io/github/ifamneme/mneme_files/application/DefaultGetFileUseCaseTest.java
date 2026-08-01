package io.github.ifamneme.mneme_files.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.ifamneme.mneme_files.domain.ContentType;
import io.github.ifamneme.mneme_files.domain.FileId;
import io.github.ifamneme.mneme_files.domain.FileStorage;
import io.github.ifamneme.mneme_files.domain.StoredFile;

@ExtendWith(MockitoExtension.class)
class DefaultGetFileUseCaseTest {

	@Mock
	private FileStorage fileStorage;

	private DefaultGetFileUseCase useCase;

	@BeforeEach
	void setUp() {
		useCase = new DefaultGetFileUseCase(fileStorage);
	}

	@Test
	void unknownId_producesNotFoundOutcome() {
		FileId id = FileId.generate();
		when(fileStorage.load(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> useCase.get(id)).isInstanceOf(FileNotFoundException.class);
	}

	@Test
	void knownId_returnsStoredFile() {
		FileId id = FileId.generate();
		StoredFile stored = new StoredFile(id, new ContentType("image/png"), 10, "\"etag\"",
				InputStream.nullInputStream());
		when(fileStorage.load(id)).thenReturn(Optional.of(stored));

		assertThat(useCase.get(id)).isEqualTo(stored);
	}

}
