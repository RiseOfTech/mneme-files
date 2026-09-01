package io.github.ifamneme.mneme_files.application;

import java.io.InputStream;

/**
 * @param header   the file's leading bytes, used for content-type detection only
 * @param data     the full file content, stored as-is; the caller closes this stream
 * @param uploader the authenticated caller's token subject, recorded as write-only object
 *                 metadata
 */
public record UploadFileCommand(byte[] header, InputStream data, long size, String uploader) {

}
