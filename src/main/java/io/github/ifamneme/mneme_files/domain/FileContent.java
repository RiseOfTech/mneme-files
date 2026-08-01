package io.github.ifamneme.mneme_files.domain;

import java.io.InputStream;
import java.util.Objects;

public record FileContent(InputStream data, long size, ContentType contentType) {

	public FileContent {
		Objects.requireNonNull(data, "data");
		Objects.requireNonNull(contentType, "contentType");
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
