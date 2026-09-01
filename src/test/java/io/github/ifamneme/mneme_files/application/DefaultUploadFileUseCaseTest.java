package io.github.ifamneme.mneme_files.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.unit.DataSize;

import io.github.ifamneme.mneme_files.domain.ContentType;
import io.github.ifamneme.mneme_files.domain.ContentTypeDetector;
import io.github.ifamneme.mneme_files.domain.FileStorage;
import io.github.ifamneme.mneme_files.domain.FileTooLargeException;
import io.github.ifamneme.mneme_files.domain.UnsupportedContentTypeException;

@ExtendWith(MockitoExtension.class)
class DefaultUploadFileUseCaseTest {

	@Mock
	private FileStorage fileStorage;

	@Mock
	private ContentTypeDetector contentTypeDetector;

	private DefaultUploadFileUseCase useCase;

	@BeforeEach
	void setUp() {
		useCase = new DefaultUploadFileUseCase(fileStorage, contentTypeDetector, DataSize.ofMegabytes(5));
	}

	@Test
	void unsupportedContentType_rejectsBeforeTouchingStorage() {
		when(contentTypeDetector.detect(any())).thenReturn(Optional.empty());
		UploadFileCommand command = new UploadFileCommand(new byte[] { 1, 2, 3 }, InputStream.nullInputStream(), 10,
				"test-subject");

		assertThatThrownBy(() -> useCase.upload(command)).isInstanceOf(UnsupportedContentTypeException.class);

		verifyNoInteractions(fileStorage);
	}

	@Test
	void oversizedFile_rejectsBeforeTouchingStorage() {
		when(contentTypeDetector.detect(any())).thenReturn(Optional.of(new ContentType("image/jpeg")));
		UploadFileCommand command = new UploadFileCommand(new byte[] { 1, 2, 3 }, InputStream.nullInputStream(),
				10 * 1024 * 1024, "test-subject");

		assertThatThrownBy(() -> useCase.upload(command)).isInstanceOf(FileTooLargeException.class);

		verifyNoInteractions(fileStorage);
	}

	@Test
	void validUpload_storesContentAndReturnsResult() {
		when(contentTypeDetector.detect(any())).thenReturn(Optional.of(new ContentType("image/png")));
		UploadFileCommand command = new UploadFileCommand(new byte[] { 1, 2, 3 }, InputStream.nullInputStream(), 100,
				"test-subject");

		UploadedFile result = useCase.upload(command);

		assertThat(result.contentType()).isEqualTo(new ContentType("image/png"));
		assertThat(result.size()).isEqualTo(100);
		verify(fileStorage).store(org.mockito.ArgumentMatchers.eq(result.id()), any());
	}

}
