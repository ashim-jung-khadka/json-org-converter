package com.github.ashim.json.error;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utility class providing methods needed for parsing JSON API Spec errors.
 *
 * @author Ashim Jung Khadka
 */
public class ErrorUtils {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	static {
		MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
	}

	private ErrorUtils() {
		// Private constructor
	}

	/**
	 * Parses provided JsonNode and returns it as ErrorResponse.
	 *
	 * @param errorResponse
	 *            error response body
	 * @return ErrorResponse collection
	 * @throws JsonProcessingException
	 *             thrown in case JsonNode is not parseable
	 */
	public static ErrorResponse parseError(JsonNode errorResponse) throws JsonProcessingException {
		return MAPPER.treeToValue(errorResponse, ErrorResponse.class);
	}

}
