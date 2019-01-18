package ch.sebi.fxedit.runtime.reflection;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.eclipsesource.v8.JavaCallback;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.V8ResultUndefined;
import com.eclipsesource.v8.V8RuntimeException;
import com.eclipsesource.v8.utils.V8ObjectUtils;

import ch.sebi.fxedit.exception.NoIdFoundException;
import ch.sebi.fxedit.exception.SerializeException;
import ch.sebi.fxedit.runtime.JsRuntime;
import ch.sebi.fxedit.runtime.lib.binding.BindingUtils;
import ch.sebi.fxedit.runtime.reflection.annotation.JsBinding;
import ch.sebi.fxedit.runtime.reflection.annotation.JsFunction;
import ch.sebi.fxedit.runtime.reflection.annotation.JsId;
import ch.sebi.fxedit.runtime.reflection.annotation.JsObject;
import ch.sebi.fxedit.utils.Annotations;
import ch.sebi.fxedit.utils.Annotations.AnnotationMatch;
import javafx.beans.property.Property;

public class JsAnnotationClassFactory implements JsClassFactory {
	private final Logger logger = LogManager.getLogger();

	private Class<?> clazz;
	private AnnotationMatch<JsId> jsId;
	private AnnotationMatch<JsObject> jsObject;
	private List<AnnotationMatch<JsBinding>> bindingAnnotations;
	private List<AnnotationMatch<JsFunction>> functionAnnotations;
	private ObjectPool pool;
	private String modulePath;

	public JsAnnotationClassFactory(Class<?> clazz, AnnotationMatch<JsObject> jsObject, AnnotationMatch<JsId> jsId,
			List<AnnotationMatch<JsBinding>> bindingAnnotations, List<AnnotationMatch<JsFunction>> functionAnnotations,
			String modulePath, ObjectPool pool) {

		this.clazz = clazz;
		this.jsObject = jsObject;
		this.jsId = jsId;
		this.bindingAnnotations = bindingAnnotations;
		this.functionAnnotations = functionAnnotations;
		this.modulePath = modulePath;
		this.pool = pool;
	}

	@Override
	public void initClass(JsRuntime runtime, V8Object object) {
		V8 v8 = runtime.getV8();
		for (AnnotationMatch<JsFunction> fun : functionAnnotations) {
			JsFunction annotation = fun.getAnnotation();
			// it is save to cast, because JsFunction can only occure on methods (see
			// @Target)
			Method method = (Method) fun.getFoundOn();

			String name = annotation.name();
			if (name.isEmpty()) {
				name = method.getName();
			}
			registerFunction(object, name, method);
		}
	}

	/**
	 * Registers a js function in the given object with the given name for the given
	 * method
	 * 
	 * @param object the js object on which the function is registered
	 * @param name   the name of the function
	 * @param method the java method which should be called. It doesn't matter if it
	 *               is private or not.
	 */
	private void registerFunction(V8Object object, String name, Method method) {
		object.registerJavaMethod((JavaCallback) (receiver, parameters) -> {
			try {
				long id = ObjectPool.getId(receiver);
				Object obj = pool.getJavaObj(id);
				if (obj == null) {
					throw new NullPointerException("No java object with the id \"" + id + "\" was found");
				}

				// checks if the object returned from the pool has the right type
				Class<?> clazz = method.getDeclaringClass();
				if (!obj.getClass().isAssignableFrom(clazz)) {
					throw new IllegalStateException(
							"The class \"" + obj.getClass().getName() + "\" the object with the id \"" + id
									+ "\" is not assignable from the class \"" + clazz.getName() + "\"");
				}

				// maps the parameter types to the parameter objects
				Class<?>[] parametherTypes = method.getParameterTypes();
				Object[] args = Arrays.stream(method.getParameterTypes()).map(typeObj -> {
					if (V8Object.class.isAssignableFrom(typeObj)) {
						return receiver;
					}
					throw new IllegalStateException("Cannot call the function \"" + method.toGenericString()
							+ "\" because of the argument type \"" + typeObj.getName() + "\"");
				}).toArray(Object[]::new);

				// calls the method
				method.setAccessible(true);
				return method.invoke(obj, args);
			} catch (RuntimeException e) {
				throw e; // runtime exception don't have to be wrapped by an other runtime exception
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}, name);
	}

	/**
	 * called when an object is initialized.
	 * 
	 * @param runtime the runtime
	 * @param object  the object which is initialized
	 */
	public void initObject(JsRuntime runtime, V8Object object) {
		if (object.isUndefined()) {
			throw new NullPointerException("V8Object is undefined");
		}
		try {
			Constructor<?> constructor = clazz.getConstructor();
			constructor.setAccessible(true);
			Object obj = constructor.newInstance();
			constructor.setAccessible(false);
			// it is save to cast, because JsId can only occur on fields (see @Target)
			Field jsIdField = (Field) jsId.getFoundOn();
			long id = pool.requestId();
			jsIdField.setAccessible(true);
			jsIdField.set(obj, id);
			jsIdField.setAccessible(false);
			pool.putObject(id, object.twin(), obj, this);

			object.add("_id", id);

			for (AnnotationMatch<JsBinding> binding : bindingAnnotations) {
				JsBinding annotation = binding.getAnnotation();
				// it is save to cast, because JsBinding can only occur on fields (see @Target)
				Field field = (Field) binding.getFoundOn();

				String name = annotation.name();
				if (name.isEmpty()) {
					name = field.getName();
				}
				// no generics because BindingLib.bindProperty expects an impossible generic
				Class javaPropGenericType = annotation.type();

				// checks the type of the field
				Class<?> fieldType = field.getType();
				if (!Property.class.isAssignableFrom(fieldType)) {
					throw new IllegalStateException("the type of the JsBinding field \"" + field.toGenericString()
							+ "\" has to be of the type \"" + Property.class.getName() + "\"");
				}

				// sets up the binding between js and java
				field.setAccessible(true);
				Property<?> javaProp = (Property<?>) field.get(obj);
				field.setAccessible(false);
				V8Object jsBindingObj = BindingUtils.createBinding(runtime);
				BindingUtils.bindProperty(javaProp, jsBindingObj, javaPropGenericType, runtime);
				object.add(name, jsBindingObj);
			}
		} catch (InstantiationException | IllegalAccessException | SerializeException | IllegalArgumentException
				| InvocationTargetException | NoSuchMethodException | SecurityException e) {
			throw new IllegalStateException(
					"An java object which belongs to a js object must have a zero-argument constructor", e);
		}
	}

	public Object createObject(JsRuntime runtime) {
		String jsConstructorCode = jsObject.getAnnotation().jsConstructorCode();
		if (jsConstructorCode.isEmpty()) {
			jsConstructorCode = "new (require('" + getModulePath() + "'))();";
		}
		V8Object jsObj = runtime.getV8().executeObjectScript(jsConstructorCode, clazz.getName() + " constructor code",
				0);
		if (jsObj.isUndefined()) {
			throw new IllegalStateException("Module returned by jsConstructorCode is undefined");
		}
		try {
			long objId = ObjectPool.getId(jsObj);
			return runtime.getObjectPool().getJavaObj(objId);
		} catch (NoIdFoundException e) {
			logger.error("Could not retrieve _id from created object: ", e);
			return null;
		}
	}

	/**
	 * Returns the module path of the js file
	 * 
	 * @return the module path
	 */
	public String getModulePath() {
		return modulePath;
	}
}
