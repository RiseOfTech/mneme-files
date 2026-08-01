package io.github.ifamneme.mneme_files.domain;

import java.util.Optional;

public interface FileStorage {

	void store(FileId id, FileContent content);

	/**
	 * Returns {@link Optional#empty()} if no object exists for the id. The returned
	 * {@link StoredFile#data()} is backed by a live connection from the storage client's
	 * pool — the caller must close it.
	 */
	Optional<StoredFile> load(FileId id);

}
