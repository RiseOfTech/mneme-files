package io.github.ifamneme.mneme_files.domain;

// Thrown by FileStorage implementations at the adapter boundary. No SDK exception type
// crosses into the application layer.
public class FileStorageException extends RuntimeException {

	public FileStorageException(String message, Throwable cause) {
		super(message, cause);
	}

}
