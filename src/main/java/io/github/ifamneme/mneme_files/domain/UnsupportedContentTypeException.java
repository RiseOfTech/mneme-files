package io.github.ifamneme.mneme_files.domain;

public class UnsupportedContentTypeException extends RuntimeException {

	public UnsupportedContentTypeException() {
		super("No permitted content type matched the file's leading bytes");
	}

	public UnsupportedContentTypeException(String value) {
		super("Content type not on the allowlist: " + value);
	}

}
