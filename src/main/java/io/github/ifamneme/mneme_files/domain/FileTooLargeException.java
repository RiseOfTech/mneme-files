package io.github.ifamneme.mneme_files.domain;

public class FileTooLargeException extends RuntimeException {

	public FileTooLargeException(long size, long maxSize) {
		super("File size " + size + " exceeds the maximum of " + maxSize + " bytes");
	}

}
