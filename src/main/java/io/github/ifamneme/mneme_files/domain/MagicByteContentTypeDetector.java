package io.github.ifamneme.mneme_files.domain;

import java.util.Optional;

// Apache Tika and equivalents are deliberately not used: the task is to confirm one of
// three permitted formats, which this allowlist answers by construction, not to
// identify arbitrary ones.
public final class MagicByteContentTypeDetector implements ContentTypeDetector {

	private static final byte[] JPEG_SIGNATURE = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF };
	private static final byte[] PNG_SIGNATURE = { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A };
	private static final byte[] RIFF_SIGNATURE = { 'R', 'I', 'F', 'F' };
	private static final byte[] WEBP_SIGNATURE = { 'W', 'E', 'B', 'P' };
	private static final int WEBP_SIGNATURE_OFFSET = 8;

	@Override
	public Optional<ContentType> detect(byte[] header) {
		if (matches(header, 0, JPEG_SIGNATURE)) {
			return Optional.of(new ContentType("image/jpeg"));
		}
		if (matches(header, 0, PNG_SIGNATURE)) {
			return Optional.of(new ContentType("image/png"));
		}
		if (matches(header, 0, RIFF_SIGNATURE) && matches(header, WEBP_SIGNATURE_OFFSET, WEBP_SIGNATURE)) {
			return Optional.of(new ContentType("image/webp"));
		}
		return Optional.empty();
	}

	private static boolean matches(byte[] header, int offset, byte[] signature) {
		if (header.length < offset + signature.length) {
			return false;
		}
		for (int i = 0; i < signature.length; i++) {
			if (header[offset + i] != signature[i]) {
				return false;
			}
		}
		return true;
	}

}
