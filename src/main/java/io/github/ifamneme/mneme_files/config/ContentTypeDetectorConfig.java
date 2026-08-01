package io.github.ifamneme.mneme_files.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.ifamneme.mneme_files.domain.ContentTypeDetector;
import io.github.ifamneme.mneme_files.domain.MagicByteContentTypeDetector;

// The domain package must import nothing outside java.*, so its implementation classes
// can't self-register with @Component; they are wired here instead.
@Configuration
public class ContentTypeDetectorConfig {

	@Bean
	public ContentTypeDetector contentTypeDetector() {
		return new MagicByteContentTypeDetector();
	}

}
