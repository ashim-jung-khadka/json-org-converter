package com.github.ashim.json.parser;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;

import com.github.ashim.json.annotations.JsonType;

/**
 * Handling Annotation Field
 *
 * @author Ashim Jung Khadka
 */
public class ReflectionUtils {

	private ReflectionUtils() {
	}

	/**
	 * Returns all field from a given class that are annotated with provided
	 * annotation type.
	 *
	 * @param clazz
	 *            source class
	 * @param annotation
	 *            target annotation
	 * @return list of fields or empty collection in case no fields were found
	 */
	public static List<Field> getAnnotatedFields(Class<?> clazz, Class<? extends Annotation> annotation) {
		Field[] fields = clazz.getDeclaredFields();

		List<Field> result = new ArrayList<Field>();

		for (Field field : fields) {
			if (field.isAnnotationPresent(annotation)) {
				result.add(field);
			}
		}

		return result;
	}

	/**
	 * Returns the json type name defined using JsonType annotation on provided
	 * class.
	 *
	 * @param clazz
	 *            type class
	 * @return name of the type or <code>null</code> in case JsonType annotation
	 *         is not present
	 */
	public static String getJsonTypeName(Class<?> clazz) {
		JsonType jsonTypeAnnotation = clazz.getAnnotation(JsonType.class);
		return jsonTypeAnnotation != null ? jsonTypeAnnotation.value() : null;
	}

	public static Class<?> getFieldType(Field field) {
		Class<?> targetType = field.getType();

		if (targetType.equals(List.class)) {
			ParameterizedType stringListType = (ParameterizedType) field.getGenericType();
			targetType = (Class<?>) stringListType.getActualTypeArguments()[0];
		}

		return targetType;
	}
}
