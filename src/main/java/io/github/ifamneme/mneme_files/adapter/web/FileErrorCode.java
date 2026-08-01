package io.github.ifamneme.mneme_files.adapter.web;

// Literal lowercase codes per the slice spec's error table — not enum.name(), which
// would render uppercase.
public enum FileErrorCode {

	FILE_NOT_FOUND("file_not_found"), FILE_TOO_LARGE("file_too_large"),
	UNSUPPORTED_CONTENT_TYPE("unsupported_content_type"), INVALID_REQUEST("invalid_request");

	private final String code;

	FileErrorCode(String code) {
		this.code = code;
	}

	public String code() {
		return code;
	}

}
