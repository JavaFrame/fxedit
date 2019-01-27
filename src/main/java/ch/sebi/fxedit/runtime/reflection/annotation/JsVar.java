package ch.sebi.fxedit.runtime.reflection.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field as a js field. It is set to the js object,
 * but isn't a binding and thus can't change
 * @author sebi
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface JsVar {
	/**
	 * optional the name of the variable. It it is omitted (or an empty string)
	 * than the name of the field is used
	 * @return
	 */
	String name() default "";
}
