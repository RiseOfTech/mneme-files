package io.github.ifamneme.mneme_files;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

// The domain must know nothing of Spring, the AWS SDK, or Jakarta - its value is in the
// boundaries it draws, and this is a checkable invariant, not left to code review.
class DomainPurityTest {

	private static final List<String> FORBIDDEN_IMPORT_PREFIXES = List.of("org.springframework", "software.amazon",
			"jakarta");

	@Test
	void domainPackageImportsNoFrameworkOrSdkTypes() throws IOException {
		Path domainDir = Path.of("src/main/java/io/github/ifamneme/mneme_files/domain");

		try (Stream<Path> paths = Files.walk(domainDir)) {
			List<Path> javaFiles = paths.filter(p -> p.toString().endsWith(".java")).toList();
			assertThat(javaFiles).isNotEmpty();

			for (Path file : javaFiles) {
				for (String line : Files.readAllLines(file)) {
					String trimmed = line.strip();
					if (trimmed.startsWith("import ")) {
						for (String forbidden : FORBIDDEN_IMPORT_PREFIXES) {
							assertThat(trimmed).as("Forbidden import in %s: %s", file, trimmed)
								.doesNotContain(forbidden);
						}
					}
				}
			}
		}
	}

}
