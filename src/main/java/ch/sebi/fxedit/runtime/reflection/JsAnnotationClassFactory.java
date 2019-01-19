package ch.sebi.fxedit.runtime.reflection;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.eclipsesource.v8.JavaCallback;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.utils.MemoryManager;

import ch.sebi.fxedit.exception.InvalidTypeException;
import ch.sebi.fxedit.exception.NoIdFoundException;
import ch.sebi.fxedit.exception.SerializeException;
import ch.sebi.fxedit.runtime.JsRuntime;
import ch.sebi.fxedit.runtime.lib.binding.BindingUtils;
import ch.sebi.fxedit.runtime.reflection.annotation.JsBinding;
import ch.sebi.fxedit.runtime.reflection.annotation.JsFunction;
import ch.sebi.fxedit.runtime.reflection.annotation.JsId;
import ch.sebi.fxedit.runtime.reflection.annotation.JsObject;
import ch.sebi.fxedit.utils.Annotations.AnnotationMatch;
import javafx.beans.property.Property;
import javafx.collections.ObservableList;

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
			try {
				registerFunction(object, name, method, annotation.raw());
			} catch (InvalidTypeException e) {
				logger.error("Could not register function", e);
			}
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
	 * @throws InvalidTypeException
	 */
	private void registerFunction(V8Object object, String name, Method method, boolean raw)
			throws InvalidTypeException {

		Class<?>[] types = method.getParameterTypes();
		if (raw) {
			if (types.length != 2 || !V8Object.class.isAssignableFrom(types[0])
					|| !V8Array.class.isAssignableFrom(types[1])) {
				throw new InvalidTypeException("Cannot call the method \"" + method.toGenericString() + "\" because "
						+ "the signature doesn't match (V8Object receiver, V8Array parameters)");
			}
		}
		object.registerJavaMethod((JavaCallback) (receiver, parameters) -> {
			MemoryManager scope = new MemoryManager(receiver.getRuntime());
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

				if (raw) {
					method.setAccessible(true);
					return method.invoke(obj, receiver, parameters);
				}

				// checks parameter length
				if (parameters.length() > method.getParameterCount()) {
					throw new InvalidTypeException("The js function called with " + parameters.length()
							+ " parameters, " + "but the java method \"" + method.getName() + "\"only supports "
							+ method.getParameterCount());
				}
				
				// deserializes parameters
				Object[] args = new Object[method.getParameterCount()];
				for(int i = 0; i < parameters.length(); i++) {
					Object jsArg = parameters.get(i);
					args[i] = pool.deserialize(types[i], jsArg);
				}

				// calls the method
				method.setAccessible(true);
				return method.invoke(obj, args);
			} catch (RuntimeException e) {
				throw e; // runtime exception don't have to be wrapped by an other runtime exception
			} catch (Exception e) {
				throw new RuntimeException(e);
			} finally {
				scope.release();
				method.setAccessible(true);
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

				field.setAccessible(true);
				Object fieldObj = field.get(obj);
				field.setAccessible(false);
				Class<?> fieldType = field.getType();
				// checks the type of the field
				if (ObservableList.class.isAssignableFrom(fieldType)) {
					V8Object jsArrayBindingObj = BindingUtils.createArrayBinding(runtime);
					BindingUtils.bindObservableList((ObservableList<?>) fieldObj, jsArrayBindingObj,
							javaPropGenericType, runtime);
					object.add(name, jsArrayBindingObj);
				} else if (Property.class.isAssignableFrom(fieldType)) {
					V8Object jsBindingObj = BindingUtils.createBinding(runtime);
					BindingUtils.bindProperty((Property<?>) fieldObj, jsBindingObj, javaPropGenericType, runtime);
					object.add(name, jsBindingObj);
				} else {
					throw new IllegalStateException("the type of the JsBinding field \"" + field.toGenericString()
							+ "\" has to be of the type \"" + Property.class.getName() + "\" or \""
							+ ObservableList.class.getName() + "\"");
				}

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
