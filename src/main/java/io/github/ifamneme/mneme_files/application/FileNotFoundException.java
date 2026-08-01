package io.github.ifamneme.mneme_files.application;

import io.github.ifamneme.mneme_files.domain.FileId;

public class FileNotFoundException extends RuntimeException {

	public FileNotFoundException(FileId id) {
		super("File not found: " + id.value());
	}

}
