package io.github.ifamneme.mneme_files.domain;

import java.util.Objects;
import java.util.UUID;

public record FileId(UUID value) {

	public FileId {
		Objects.requireNonNull(value, "value");
	}

	// Reads are unauthenticated, so a file's id is the only thing protecting it.
	// UUID.randomUUID() draws from SecureRandom and is version 4 - never swap this for a
	// time-ordered generator, which would share a long prefix with every id minted in the
	// same instant and shrink the space an attacker must search.
	public static FileId generate() {
		return new FileId(UUID.randomUUID());
	}

	public static FileId of(UUID value) {
		return new FileId(value);
	}

}
