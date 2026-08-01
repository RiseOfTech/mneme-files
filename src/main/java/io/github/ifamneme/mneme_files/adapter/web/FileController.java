package io.github.ifamneme.mneme_files.adapter.web;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.UUID;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.github.ifamneme.mneme_files.application.GetFileUseCase;
import io.github.ifamneme.mneme_files.application.UploadFileCommand;
import io.github.ifamneme.mneme_files.application.UploadFileUseCase;
import io.github.ifamneme.mneme_files.application.UploadedFile;
import io.github.ifamneme.mneme_files.domain.ContentTypeDetector;
import io.github.ifamneme.mneme_files.domain.FileId;
import io.github.ifamneme.mneme_files.domain.StoredFile;

@RestController
@RequestMapping("/v1/files")
public class FileController {

	private final UploadFileUseCase uploadFileUseCase;
	private final GetFileUseCase getFileUseCase;

	public FileController(UploadFileUseCase uploadFileUseCase, GetFileUseCase getFileUseCase) {
		this.uploadFileUseCase = uploadFileUseCase;
		this.getFileUseCase = getFileUseCase;
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<FileResponseDto> upload(@RequestParam("file") MultipartFile file) throws IOException {
		byte[] header;
		try (InputStream headerStream = file.getInputStream()) {
			header = headerStream.readNBytes(ContentTypeDetector.HEADER_LENGTH_BYTES);
		}

		UploadFileCommand command = new UploadFileCommand(header, file.getInputStream(), file.getSize());
		UploadedFile uploaded = uploadFileUseCase.upload(command);

		URI location = URI.create("/v1/files/" + uploaded.id().value());
		return ResponseEntity.created(location).body(FileResponseDto.from(uploaded));
	}

	@GetMapping("/{id}")
	public ResponseEntity<InputStreamResource> download(@PathVariable UUID id) {
		StoredFile file = getFileUseCase.get(FileId.of(id));
		return ResponseEntity.ok()
			.contentType(MediaType.parseMediaType(file.contentType().value()))
			.contentLength(file.size())
			.eTag(file.etag())
			.cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
			.body(new InputStreamResource(file.data()));
	}

}
