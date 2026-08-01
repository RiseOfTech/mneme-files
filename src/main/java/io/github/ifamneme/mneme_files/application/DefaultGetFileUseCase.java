package io.github.ifamneme.mneme_files.application;

import org.springframework.stereotype.Service;

import io.github.ifamneme.mneme_files.domain.FileId;
import io.github.ifamneme.mneme_files.domain.FileStorage;
import io.github.ifamneme.mneme_files.domain.StoredFile;

@Service
public class DefaultGetFileUseCase implements GetFileUseCase {

	private final FileStorage fileStorage;

	public DefaultGetFileUseCase(FileStorage fileStorage) {
		this.fileStorage = fileStorage;
	}

	@Override
	public StoredFile get(FileId id) {
		return fileStorage.load(id).orElseThrow(() -> new FileNotFoundException(id));
	}

}
