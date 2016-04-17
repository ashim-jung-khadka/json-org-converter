package com.github.ashim.json.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * JsonRelation for Json Model
 *
 * @author Ashim Jung Khadka
 */
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonRelation {

	String value();

	boolean included() default false;

}