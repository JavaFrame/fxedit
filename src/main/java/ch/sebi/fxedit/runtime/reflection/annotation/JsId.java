package ch.sebi.fxedit.runtime.reflection.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import ch.sebi.fxedit.runtime.reflection.ObjectPool;

/**
 * Declares a field as a js id. This field is used to uniquly identify
 * the object and is managed by the {@link ObjectPool}
 * @author sebi
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface JsId {
}
