package io.github.ifamneme.mneme_files.domain;

import java.util.Objects;
import java.util.Set;

public record ContentType(String value) {

	private static final Set<String> ALLOWED = Set.of("image/jpeg", "image/png", "image/webp");

	public ContentType {
		Objects.requireNonNull(value, "value");
		if (!ALLOWED.contains(value)) {
			throw new UnsupportedContentTypeException(value);
		}
	}

	public static Set<String> allowedValues() {
		return ALLOWED;
	}

}
