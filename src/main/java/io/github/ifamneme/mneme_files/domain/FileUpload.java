package io.github.ifamneme.mneme_files.domain;

import java.io.InputStream;
import java.util.Objects;

public record FileUpload(InputStream data, long size, ContentType contentType, String uploader) {

	public FileUpload {
		Objects.requireNonNull(data, "data");
		Objects.requireNonNull(contentType, "contentType");
		Objects.requireNonNull(uploader, "uploader");
		if (size < 0) {
			throw new IllegalArgumentException("size must not be negative");
		}
	}

	public static void validateSize(long size, long maxSize) {
		if (size > maxSize) {
			throw new FileTooLargeException(size, maxSize);
		}
	}

}
