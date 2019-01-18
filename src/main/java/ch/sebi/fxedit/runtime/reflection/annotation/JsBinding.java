package ch.sebi.fxedit.runtime.reflection.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javafx.beans.property.Property;

/**
 * Declares a {@link Property} to be bound in the js space.
 * The field has to be a Property or a subclass of a {@link Property}
 * @author sebi
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface JsBinding {
	/**
	 * the name of the js binding.
	 * If the name is empty, then the java name is used
	 * @return the name of the js binding
	 */
	String name() default "";
	/**
	 * the type of the {@link Property}
	 * @return the type of the property
	 */
	Class<?> type();
}
