package io.github.ifamneme.mneme_files.domain;

import java.util.Optional;

public interface ContentTypeDetector {

	// Long enough to cover every signature this detector checks: the WebP check reads
	// up to offset 8 + 4 bytes ("WEBP").
	int HEADER_LENGTH_BYTES = 12;

	/**
	 * Detects a {@link ContentType} from a file's leading bytes. {@code header} may be
	 * shorter than {@link #HEADER_LENGTH_BYTES} for a truncated file; that is treated as
	 * no match rather than an error.
	 */
	Optional<ContentType> detect(byte[] header);

}
