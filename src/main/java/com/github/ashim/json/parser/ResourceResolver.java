package com.github.ashim.json.parser;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.ashim.json.annotations.JsonId;
import com.github.ashim.json.annotations.JsonRelation;
import com.github.ashim.json.annotations.JsonType;
import com.github.ashim.json.common.Constants;

/**
 * JSON API data converter. <br />
 *
 * Provides methods for conversion between JSON API resources to java POJOs and
 * vice versa.
 *
 * @author Ashim Jung Khadka
 */
public class ResourceResolver {
	private static final Map<String, Class<?>> TYPE_TO_CLASS_MAPPING = new HashMap<>();
	private static final Map<Class<?>, JsonType> TYPE_ANNOTATIONS = new HashMap<>();
	private static final Map<Class<?>, Field> ID_MAP = new HashMap<>();
	private static final Map<Class<?>, List<Field>> RELATIONSHIPS_MAP = new HashMap<>();
	private static final Map<Class<?>, Map<String, Class<?>>> RELATIONSHIP_TYPE_MAP = new HashMap<>();
	private static final Map<Class<?>, Map<String, Field>> RELATIONSHIP_FIELD_MAP = new HashMap<>();

	private ObjectMapper objectMapper;

	public ResourceResolver(Class<?>... classes) {
		this(null, classes);
	}

	public ResourceResolver(ObjectMapper mapper, Class<?>... classes) {

		for (Class<?> clazz : classes) {

			if (clazz.isAnnotationPresent(JsonType.class)) {
				JsonType annotation = clazz.getAnnotation(JsonType.class);
				TYPE_TO_CLASS_MAPPING.put(annotation.value(), clazz);
				TYPE_ANNOTATIONS.put(clazz, annotation);
				RELATIONSHIP_TYPE_MAP.put(clazz,
						new HashMap<String, Class<?>>());
				RELATIONSHIP_FIELD_MAP.put(clazz, new HashMap<String, Field>());

				// collecting JsonRelation fields
				List<Field> relationshipFields = ReflectionUtils
						.getAnnotatedFields(clazz, JsonRelation.class);

				for (Field relationshipField : relationshipFields) {
					relationshipField.setAccessible(true);

					JsonRelation jsonRelation = relationshipField
							.getAnnotation(JsonRelation.class);
					Class<?> targetType = ReflectionUtils
							.getFieldType(relationshipField);
					RELATIONSHIP_TYPE_MAP.get(clazz).put(jsonRelation.value(),
							targetType);
					RELATIONSHIP_FIELD_MAP.get(clazz).put(jsonRelation.value(),
							relationshipField);

				}

				RELATIONSHIPS_MAP.put(clazz, relationshipFields);

				// collecting Id fields
				List<Field> idAnnotatedFields = ReflectionUtils
						.getAnnotatedFields(clazz, JsonId.class);

				if (!idAnnotatedFields.isEmpty()) {
					Field idField = idAnnotatedFields.get(0);
					idField.setAccessible(true);
					ID_MAP.put(clazz, idField);
				} else {
					throw new IllegalArgumentException(
							"All resource classes must have a field annotated with the "
									+ "@JsonId annotation");
				}

			} else {
				throw new IllegalArgumentException(
						"All resource classes must be annotated with JsonType annotation!");
			}
		}

		// Set custom mapper if provided
		if (mapper != null) {
			objectMapper = mapper;
		} else {
			objectMapper = new ObjectMapper();
		}

		objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
	}

	/**
	 * Converts raw data input into requested target type.
	 *
	 * @param data
	 *            data
	 * @param clazz
	 *            target object
	 * @param <T>
	 * @return converted object
	 * @throws RuntimeException
	 *             in case conversion fails
	 */
	public <T> T readJson(byte[] data, Class<T> clazz) {
		try {
			JsonNode rootNode = objectMapper.readTree(data);

			// Validate
			ValidationUtils.ensureNotError(rootNode);
			ValidationUtils.ensureObject(rootNode);

			Map<String, Object> included = parseIncluded(rootNode);

			JsonNode dataNode = rootNode.get(Constants.DATA);

			T result = readJson(dataNode, clazz, included);

			return result;
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Converts raw-data input into a collection of requested output objects.
	 *
	 * @param data
	 *            data
	 * @param clazz
	 *            target type
	 * @param <T>
	 * @return collection of converted elements
	 * @throws RuntimeException
	 *             in case conversion fails
	 */
	public <T> List<T> readJsonCollection(byte[] data, Class<T> clazz) {

		try {
			JsonNode rootNode = objectMapper.readTree(data);

			// Validate
			ValidationUtils.ensureNotError(rootNode);
			ValidationUtils.ensureCollection(rootNode);

			Map<String, Object> included = parseIncluded(rootNode);

			List<T> result = new ArrayList<>();

			for (JsonNode element : rootNode.get(Constants.DATA)) {
				T pojo = readJson(element, clazz, included);
				result.add(pojo);
			}

			return result;
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	/**
	 * Converts provided input into a target object. After conversion completes
	 * any relationships defined are resolved.
	 *
	 * @param source
	 *            JSON source
	 * @param clazz
	 *            target type
	 * @param cache
	 *            resolved objects (either from included element or already
	 *            parsed objects)
	 * @param <T>
	 * @return converted target object
	 * @throws IOException
	 * @throws IllegalAccessException
	 */
	private <T> T readJson(JsonNode source, Class<T> clazz,
			Map<String, Object> cache) throws IOException,
	IllegalAccessException, InstantiationException {
		T result;

		if (source.has(Constants.ATTRIBUTES)) {
			result = objectMapper.treeToValue(source.get(Constants.ATTRIBUTES),
					clazz);
		} else {
			result = clazz.newInstance();
		}

		// Set object id
		setIdValue(result, source.get(Constants.ID));

		if (cache != null) {
			// Handle relationships
			handleRelationships(source, result, cache);

			// Add parsed object to cache
			cache.put(createIdentifier(source), result);
		}

		return result;
	}

	/**
	 * Converts included data and returns it as pairs of its unique identifiers
	 * and converted types.
	 *
	 * @param parent
	 *            data source
	 * @return identifier/object pairs
	 * @throws IOException
	 * @throws IllegalAccessException
	 */
	private Map<String, Object> parseIncluded(JsonNode parent)
			throws IOException, IllegalAccessException, InstantiationException {
		Map<String, Object> result = new HashMap<>();

		if (parent.has(Constants.INCLUDED)) {
			// Get resources
			List<Resource> includedResources = getIncludedResources(parent);

			if (!includedResources.isEmpty()) {
				// Add to result
				for (Resource includedResource : includedResources) {
					result.put(includedResource.getIdentifier(),
							includedResource.getObject());
				}

				ArrayNode includedArray = (ArrayNode) parent
						.get(Constants.INCLUDED);

				for (int i = 0; i < includedResources.size(); i++) {
					Resource resource = includedResources.get(i);

					// Handle relationships
					JsonNode node = includedArray.get(i);
					handleRelationships(node, resource.getObject(), result);
				}
			}
		}

		return result;
	}

	/**
	 * Parses out included resources excluding relationships.
	 *
	 * @param parent
	 *            root node
	 * @return map of identifier/resource pairs
	 * @throws IOException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	private List<Resource> getIncludedResources(JsonNode parent)
			throws IOException, IllegalAccessException, InstantiationException {
		List<Resource> result = new ArrayList<>();

		if (parent.has(Constants.INCLUDED)) {
			for (JsonNode jsonNode : parent.get(Constants.INCLUDED)) {
				String type = jsonNode.get(Constants.TYPE).asText();

				if (type != null) {
					Class<?> clazz = TYPE_TO_CLASS_MAPPING.get(type);

					if (clazz != null) {
						Object object = readJson(jsonNode, clazz, null);
						result.add(new Resource(createIdentifier(jsonNode),
								object));
					}
				}
			}
		}

		return result;
	}

	private void handleRelationships(JsonNode source, Object object,
			Map<String, Object> includedData) throws IllegalAccessException,
			IOException, InstantiationException {
		JsonNode relationships = source.get(Constants.RELATIONSHIPS);

		if (relationships != null) {
			Iterator<String> fields = relationships.fieldNames();

			while (fields.hasNext()) {
				String field = fields.next();

				JsonNode relationship = relationships.get(field);
				Field relationshipField = RELATIONSHIP_FIELD_MAP.get(
						object.getClass()).get(field);

				if (relationshipField != null) {
					// Get target type
					Class<?> type = RELATIONSHIP_TYPE_MAP
							.get(object.getClass()).get(field);

					// In case type is not defined, relationship object cannot
					// be processed
					if (type == null) {
						continue;
					}

					if (isCollection(relationship)) {

						List<Object> elements = new ArrayList<>();

						for (JsonNode element : relationship
								.get(Constants.DATA)) {
							Object relationshipObject = parseRelationship(
									element, type, includedData);
							if (relationshipObject != null) {
								elements.add(relationshipObject);
							}
						}
						relationshipField.set(object, elements);
					} else {
						Object relationshipObject = parseRelationship(
								relationship.get(Constants.DATA), type,
								includedData);
						if (relationshipObject != null) {
							relationshipField.set(object, relationshipObject);
						}
					}
				}
			}
		}
	}

	/**
	 * Creates relationship object by consuming provided 'data' node.
	 *
	 * @param relationshipDataNode
	 *            relationship data node
	 * @param type
	 *            object type
	 * @param cache
	 *            object cache
	 * @return created object or <code>null</code> in case data node is not
	 *         valid
	 * @throws IOException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	private Object parseRelationship(JsonNode relationshipDataNode,
			Class<?> type, Map<String, Object> cache) throws IOException,
			IllegalAccessException, InstantiationException {
		if (ValidationUtils.isRelationshipParsable(relationshipDataNode)) {
			String identifier = createIdentifier(relationshipDataNode);

			if (cache.containsKey(identifier)) {
				return cache.get(identifier);
			} else {
				return readJson(relationshipDataNode, type, cache);
			}
		}

		return null;
	}

	/**
	 * Generates unique resource identifier by combining resource type and
	 * resource id fields. <br />
	 * By specification id/type combination guarantees uniqueness.
	 *
	 * @param object
	 *            data object
	 * @return concatenated id and type values
	 */
	private String createIdentifier(JsonNode object) {

		JsonNode idValue = object.get(Constants.ID);
		Object id = "";

		if (idValue != null && idValue.asText() != null
				&& idValue.asText() != "") {
			id = object.get(Constants.ID);
		}

		String type = object.get(Constants.TYPE).asText();
		return type.concat(id.toString());
	}

	/**
	 * Sets an id attribute value to a target object.
	 *
	 * @param target
	 *            target POJO
	 * @param idValue
	 *            id node
	 * @throws IllegalAccessException
	 *             thrown in case target field is not accessible
	 */
	private void setIdValue(Object target, JsonNode idValue)
			throws IllegalAccessException {
		Field idField = ID_MAP.get(target.getClass());

		// By specification, id value is always a String type

		if (idValue != null && idValue.asText() != null
				&& idValue.asText() != "") {
			idField.set(target, Integer.parseInt(idValue.asText()));
		}

	}

	/**
	 * Checks if <code>data</code> object is an array or just single object
	 * holder.
	 *
	 * @param source
	 *            data node
	 * @return <code>true</code> if data node is an array else
	 *         <code>false</code>
	 */
	private boolean isCollection(JsonNode source) {
		JsonNode data = source.get(Constants.DATA);
		return data != null && data.isArray();
	}

	/**
	 * Converts input object to String.
	 *
	 * @param object
	 *            input object
	 * @return json in String
	 * @throws JsonProcessingException
	 * @throws IllegalAccessException
	 */
	public String writeJson(Object object) {
		String json = "";

		try {
			ObjectNode dataNode = getDataNode(object);
			ObjectNode result = objectMapper.createObjectNode();

			result.set(Constants.DATA, dataNode);

			// Include Field
			List<Field> includeFields = RELATIONSHIPS_MAP
					.get(object.getClass());
			ObjectNode includedNode = objectMapper.createObjectNode();

			for (Field includeField : includeFields) {
				boolean isInclude = includeField.getAnnotation(
						JsonRelation.class).included();

				if (!isInclude) {
					continue;
				}

				Object relationshipObject = includeField.get(object);

				if (relationshipObject != null) {
					JsonRelation jsonRelation = includeField
							.getAnnotation(JsonRelation.class);

					String relationshipName = jsonRelation.value();

					if (relationshipObject instanceof List) {
						ArrayNode dataArrayNode = objectMapper
								.createArrayNode();

						for (Object element : (List<?>) relationshipObject) {
							String relationshipType = TYPE_ANNOTATIONS.get(
									element.getClass()).value();
							Integer idValue = (Integer) ID_MAP.get(
									element.getClass()).get(element);

							ObjectNode identifierNode = objectMapper
									.createObjectNode();

							identifierNode
									.put(Constants.TYPE, relationshipType);
							identifierNode
									.put(Constants.ID, idValue.toString());

							// Included Attribute
							ObjectNode elementNode = objectMapper
									.valueToTree(element);
							Field elementIdField = ID_MAP.get(element
									.getClass());
							elementNode.remove(elementIdField.getName());
							identifierNode.set(Constants.ATTRIBUTES,
									elementNode);

							dataArrayNode.add(identifierNode);
						}

						ObjectNode relationshipDataNode = objectMapper
								.createObjectNode();
						relationshipDataNode.set(Constants.DATA, dataArrayNode);
						includedNode
								.set(relationshipName, relationshipDataNode);

					} else {
						String relationshipType = TYPE_ANNOTATIONS.get(
								relationshipObject.getClass()).value();
						String idValue = (String) ID_MAP.get(
								relationshipObject.getClass()).get(
								relationshipObject);

						ObjectNode identifierNode = objectMapper
								.createObjectNode();
						identifierNode.put(Constants.TYPE, relationshipType);
						identifierNode.put(Constants.ID, idValue);

						ObjectNode relationshipDataNode = objectMapper
								.createObjectNode();
						relationshipDataNode
								.set(Constants.DATA, identifierNode);

						includedNode
								.set(relationshipName, relationshipDataNode);
					}
				}
			}

			if (includedNode.size() > 0) {
				result.set(Constants.INCLUDED, includedNode);
			}

			json = objectMapper.writeValueAsString(result);
		} catch (JsonProcessingException ex) {
			System.out.println("Error in writeObject");
		} catch (IllegalAccessException ex) {
			System.out.println("Error in writeObject");
		}

		return json;
	}

	private ObjectNode getDataNode(Object object) throws IllegalAccessException {

		// Perform initial conversion
		ObjectNode attributesNode = objectMapper.valueToTree(object);

		// Remove id, meta and relationship fields
		Field idField = ID_MAP.get(object.getClass());
		attributesNode.remove(idField.getName());

		// Handle resource identifier
		ObjectNode dataNode = objectMapper.createObjectNode();
		dataNode.put(Constants.TYPE, TYPE_ANNOTATIONS.get(object.getClass())
				.value());

		Integer resourceId = (Integer) idField.get(object);
		if (resourceId != null) {
			dataNode.put(Constants.ID, resourceId.toString());
		}
		dataNode.set(Constants.ATTRIBUTES, attributesNode);

		// Handle relationships (remove from base type and add as relationships)
		List<Field> relationshipFields = RELATIONSHIPS_MAP.get(object
				.getClass());

		if (relationshipFields != null) {
			ObjectNode relationshipsNode = objectMapper.createObjectNode();

			for (Field relationshipField : relationshipFields) {
				Object relationshipObject = relationshipField.get(object);

				boolean isList = relationshipObject instanceof List;
				if (isList) {
					@SuppressWarnings("unchecked")
					List<Object> relationList = (List<Object>) relationshipObject;

					if (relationList.size() == 0) {
						continue;
					}
				}

				if (relationshipObject != null) {
					attributesNode.remove(relationshipField.getName());

					JsonRelation jsonRelation = relationshipField
							.getAnnotation(JsonRelation.class);

					String relationshipName = jsonRelation.value();

					if (relationshipObject instanceof List) {
						ArrayNode dataArrayNode = objectMapper
								.createArrayNode();

						for (Object element : (List<?>) relationshipObject) {
							String relationshipType = TYPE_ANNOTATIONS.get(
									element.getClass()).value();
							Integer idValue = (Integer) ID_MAP.get(
									element.getClass()).get(element);

							ObjectNode identifierNode = objectMapper
									.createObjectNode();
							identifierNode
									.put(Constants.TYPE, relationshipType);
							identifierNode
									.put(Constants.ID, idValue.toString());
							dataArrayNode.add(identifierNode);
						}

						ObjectNode relationshipDataNode = objectMapper
								.createObjectNode();
						relationshipDataNode.set(Constants.DATA, dataArrayNode);
						relationshipsNode.set(relationshipName,
								relationshipDataNode);

					} else {
						String relationshipType = TYPE_ANNOTATIONS.get(
								relationshipObject.getClass()).value();
						String idValue = (String) ID_MAP.get(
								relationshipObject.getClass()).get(
								relationshipObject);

						ObjectNode identifierNode = objectMapper
								.createObjectNode();
						identifierNode.put(Constants.TYPE, relationshipType);
						identifierNode.put(Constants.ID, idValue);

						ObjectNode relationshipDataNode = objectMapper
								.createObjectNode();
						relationshipDataNode
								.set(Constants.DATA, identifierNode);

						relationshipsNode.set(relationshipName,
								relationshipDataNode);
					}
				}

			}

			if (relationshipsNode.size() > 0) {
				dataNode.set(Constants.RELATIONSHIPS, relationshipsNode);
			}
		}
		return dataNode;
	}

	/**
	 * Converts input object to String.
	 *
	 * @param objects
	 *            List of input objects
	 * @return json in String
	 * @throws JsonProcessingException
	 * @throws IllegalAccessException
	 */
	public <T> String writeJsonCollection(Iterable<T> objects) {

		String json = "";

		try {
			ArrayNode results = objectMapper.createArrayNode();

			for (T object : objects) {
				results.add(getDataNode(object));
			}

			ObjectNode result = objectMapper.createObjectNode();
			result.set(Constants.DATA, results);
			return objectMapper.writeValueAsString(result);
		} catch (JsonProcessingException ex) {
			System.out.println("Error in writeObject");
		} catch (IllegalAccessException ex) {
			System.out.println("Error in writeObject");
		}

		return json;
	}

	/**
	 * Checks if provided type is registered with this converter instance.
	 *
	 * @param type
	 *            class to check
	 * @return returns <code>true</code> if type is registered, else
	 *         <code>false</code>
	 */
	public boolean isRegisteredType(Class<?> type) {
		return TYPE_ANNOTATIONS.containsKey(type);
	}

	private static class Resource {
		private String identifier;
		private Object object;

		public Resource(String identifier, Object resource) {
			this.identifier = identifier;
			this.object = resource;
		}

		public String getIdentifier() {
			return identifier;
		}

		public Object getObject() {
			return object;
		}
	}

}