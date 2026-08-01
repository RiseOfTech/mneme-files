package io.github.ifamneme.mneme_files.domain;

import java.io.InputStream;
import java.util.Objects;

public record StoredFile(FileId id, ContentType contentType, long size, String etag, InputStream data) {

	public StoredFile {
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(contentType, "contentType");
		Objects.requireNonNull(etag, "etag");
		Objects.requireNonNull(data, "data");
	}

}
