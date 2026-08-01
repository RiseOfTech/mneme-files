package io.github.ifamneme.mneme_files.config;

import java.util.List;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

import io.github.ifamneme.mneme_files.domain.ContentType;

@ConfigurationProperties(prefix = "mneme.files.upload")
@Validated
public class UploadProperties {

	@NotNull
	private DataSize maxSize;

	@NotEmpty
	private List<String> allowedTypes = List.of();

	public DataSize getMaxSize() {
		return maxSize;
	}

	public void setMaxSize(DataSize maxSize) {
		this.maxSize = maxSize;
	}

	public List<String> getAllowedTypes() {
		return allowedTypes;
	}

	public void setAllowedTypes(List<String> allowedTypes) {
		this.allowedTypes = allowedTypes;
	}

	// The allowed types actually enforced at detection time are fixed in ContentType;
	// this property exists so misconfiguration (a typo, a stray legacy type) fails fast
	// at startup instead of silently drifting from what the domain actually accepts.
	@AssertTrue(message = "mneme.files.upload.allowed-types must be a subset of the supported content types")
	private boolean isAllowedTypesSupported() {
		return ContentType.allowedValues().containsAll(allowedTypes);
	}

}
