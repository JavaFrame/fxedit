package ch.sebi.fxedit.runtime.reflection.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import ch.sebi.fxedit.runtime.reflection.ObjectPool;

/**
 * An annotations which is needed for the {@link ObjectPool} to parse the objects annotations
 * @author sebi
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface JsObject {
	/**
	 * Returns the java script code necessary to create
	 * the javascript object (with the _id field).
	 * @return the js code 
	 */
	String jsConstructorCode() default "";
}
