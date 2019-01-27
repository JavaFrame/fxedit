package ch.sebi.fxedit.runtime.reflection;

import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;

import ch.sebi.fxedit.exception.FailedObjectCreationException;
import ch.sebi.fxedit.runtime.JsRuntime;

/**
 * A class which is used to initialize classes and objects which exist in both java and js
 * @author sebi
 *
 */
public interface JsClassFactory {
	/**
	 * called when a class definition is initialized. The given {@link V8Object} is
	 * the class object
	 * @param runtime the js runtime
	 * @param object the class object
	 */
	public void initClass(JsRuntime runtime, V8Object object);
	/**
	 * called when an object is initialized. 
	 * @param runtime the runtime
	 * @param object the object which is initialized
	 * @param args the arguments to the constructor of the java object
	 */
	public void initObject(JsRuntime runtime, V8Object object, V8Array args);
	
	/**
	 * Creates and java and its js object and returns the java object
	 * @param runtime the runtime
	 * @param args the arguments to the constructor
	 * @return the java object
	 */
	public Object createObject(JsRuntime runtime, Object[] args) throws FailedObjectCreationException;
}
