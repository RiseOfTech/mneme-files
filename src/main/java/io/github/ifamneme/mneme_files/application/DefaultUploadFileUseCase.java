package io.github.ifamneme.mneme_files.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;

import io.github.ifamneme.mneme_files.domain.ContentType;
import io.github.ifamneme.mneme_files.domain.ContentTypeDetector;
import io.github.ifamneme.mneme_files.domain.FileId;
import io.github.ifamneme.mneme_files.domain.FileUpload;
import io.github.ifamneme.mneme_files.domain.FileStorage;
import io.github.ifamneme.mneme_files.domain.UnsupportedContentTypeException;

@Service
public class DefaultUploadFileUseCase implements UploadFileUseCase {

	private final FileStorage fileStorage;
	private final ContentTypeDetector contentTypeDetector;
	private final long maxSizeBytes;

	public DefaultUploadFileUseCase(FileStorage fileStorage, ContentTypeDetector contentTypeDetector,
			@Value("${mneme.files.upload.max-size}") DataSize maxSize) {
		this.fileStorage = fileStorage;
		this.contentTypeDetector = contentTypeDetector;
		this.maxSizeBytes = maxSize.toBytes();
	}

	@Override
	public UploadedFile upload(UploadFileCommand command) {
		ContentType contentType = contentTypeDetector.detect(command.header())
			.orElseThrow(UnsupportedContentTypeException::new);
		FileUpload.validateSize(command.size(), maxSizeBytes);

		FileId id = FileId.generate();
		fileStorage.store(id, new FileUpload(command.data(), command.size(), contentType, command.uploader()));
		return new UploadedFile(id, contentType, command.size());
	}

}
