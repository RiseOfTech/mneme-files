package io.github.ifamneme.mneme_files.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class FileIdTest {

	@Test
	void generateProducesVersion4Uuids() {
		FileId id = FileId.generate();

		assertThat(id.value().version()).isEqualTo(4);
	}

	@Test
	void generateProducesDistinctValues() {
		Set<FileId> ids = new HashSet<>();
		for (int i = 0; i < 1000; i++) {
			ids.add(FileId.generate());
		}

		assertThat(ids).hasSize(1000);
	}

}
