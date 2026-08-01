package io.github.ifamneme.mneme_files.application;

import io.github.ifamneme.mneme_files.domain.ContentType;
import io.github.ifamneme.mneme_files.domain.FileId;

public record UploadedFile(FileId id, ContentType contentType, long size) {

}
