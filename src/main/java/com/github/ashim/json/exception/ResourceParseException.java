package com.github.ashim.json.exception;

import com.github.ashim.json.error.ErrorResponse;

/**
 * ResourceParseException implementation. <br />
 * This exception is thrown from ResourceConverter in case parsed body contains
 * 'errors' node.
 *
 * @author Ashim Jung Khadka
 */
public class ResourceParseException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	private ErrorResponse errorResponse;

	public ResourceParseException(ErrorResponse errorResponse) {
		super(errorResponse.toString());
		this.errorResponse = errorResponse;
	}

	/**
	 * Returns ErrorResponse or <code>null</code>
	 *
	 * @return
	 */
	public ErrorResponse getErrorResponse() {
		return errorResponse;
	}

}