package io.github.ifamneme.mneme_files.application;

import io.github.ifamneme.mneme_files.domain.FileId;
import io.github.ifamneme.mneme_files.domain.StoredFile;

public interface GetFileUseCase {

	StoredFile get(FileId id);

}
