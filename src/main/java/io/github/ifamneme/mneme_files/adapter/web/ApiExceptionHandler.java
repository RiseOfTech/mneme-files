package io.github.ifamneme.mneme_files.adapter.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import io.github.ifamneme.mneme_files.application.FileNotFoundException;
import io.github.ifamneme.mneme_files.domain.FileTooLargeException;
import io.github.ifamneme.mneme_files.domain.UnsupportedContentTypeException;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(FileNotFoundException.class)
	public ProblemDetail handleFileNotFound(FileNotFoundException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND,
				"The requested file was not found.");
		problem.setProperty("code", FileErrorCode.FILE_NOT_FOUND.code());
		return problem;
	}

	@ExceptionHandler(UnsupportedContentTypeException.class)
	public ProblemDetail handleUnsupportedContentType(UnsupportedContentTypeException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
				"The uploaded file's content type is not supported.");
		problem.setProperty("code", FileErrorCode.UNSUPPORTED_CONTENT_TYPE.code());
		return problem;
	}

	@ExceptionHandler({ FileTooLargeException.class, MaxUploadSizeExceededException.class })
	public ProblemDetail handleFileTooLarge(Exception ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONTENT_TOO_LARGE,
				"The uploaded file exceeds the maximum allowed size.");
		problem.setProperty("code", FileErrorCode.FILE_TOO_LARGE.code());
		return problem;
	}

	@ExceptionHandler({ MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class,
			MissingServletRequestPartException.class, MultipartException.class })
	public ProblemDetail handleInvalidRequest(Exception ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				"The request is malformed.");
		problem.setProperty("code", FileErrorCode.INVALID_REQUEST.code());
		return problem;
	}

}
