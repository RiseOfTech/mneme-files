package io.github.ifamneme.mneme_files.adapter.web;

import java.util.UUID;

import io.github.ifamneme.mneme_files.application.UploadedFile;

public record FileResponseDto(UUID id, String contentType, long size) {

	public static FileResponseDto from(UploadedFile uploaded) {
		return new FileResponseDto(uploaded.id().value(), uploaded.contentType().value(), uploaded.size());
	}

}
