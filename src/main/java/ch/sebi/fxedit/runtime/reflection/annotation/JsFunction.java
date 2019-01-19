package ch.sebi.fxedit.runtime.reflection.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.eclipsesource.v8.JavaCallback;

/**
 * Declares the method as a js function which is callable and it will be installed 
 * on the object
 * @author sebi
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface JsFunction {
	/**
	 * the name of the function in the js space.
	 * If it is empty, then the name of the java function is used
	 * @return the name of the js function
	 */
	String name() default "";
	
	/**
	 * If the paramters should be the raw V8Values received by the 
	 * {@link JavaCallback} or if the parameters should already be unpacked.
	 * @return if the raw arguments should be passed down
	 */
	boolean raw() default false;
}
