package io.github.ifamneme.mneme_files.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ContentTypeTest {

	@Test
	void acceptsEachPermittedType() {
		assertThat(new ContentType("image/jpeg").value()).isEqualTo("image/jpeg");
		assertThat(new ContentType("image/png").value()).isEqualTo("image/png");
		assertThat(new ContentType("image/webp").value()).isEqualTo("image/webp");
	}

	@Test
	void rejectsValueOutsideAllowlist() {
		assertThatThrownBy(() -> new ContentType("image/gif")).isInstanceOf(UnsupportedContentTypeException.class);
		assertThatThrownBy(() -> new ContentType("image/svg+xml"))
			.isInstanceOf(UnsupportedContentTypeException.class);
		assertThatThrownBy(() -> new ContentType("application/octet-stream"))
			.isInstanceOf(UnsupportedContentTypeException.class);
	}

}
