package ch.sebi.fxedit.runtime.reflection;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.V8ResultUndefined;
import com.eclipsesource.v8.V8Value;

import ch.sebi.fxedit.exception.FactoryNotFoundException;
import ch.sebi.fxedit.exception.FailedObjectCreationException;
import ch.sebi.fxedit.exception.InvalidTypeException;
import ch.sebi.fxedit.exception.NoIdFoundException;
import ch.sebi.fxedit.exception.SerializeException;
import ch.sebi.fxedit.runtime.JsRuntime;
import ch.sebi.fxedit.runtime.reflection.annotation.JsBinding;
import ch.sebi.fxedit.runtime.reflection.annotation.JsFunction;
import ch.sebi.fxedit.runtime.reflection.annotation.JsId;
import ch.sebi.fxedit.runtime.reflection.annotation.JsObject;
import ch.sebi.fxedit.utils.Annotations;
import ch.sebi.fxedit.utils.Annotations.AnnotationMatch;

/**
 * manages js and java objects
 * 
 * @author sebi
 *
 */
public class ObjectPool implements Closeable {
	/**
	 * logger
	 */
	private Logger logger = LogManager.getLogger(getClass());

	/**
	 * the runtime which initialized the class
	 */
	private JsRuntime runtime;

	/**
	 * the next id counter for the object ids
	 */
	private AtomicLong nextId = new AtomicLong();

	/**
	 * the object entries with their ids
	 */
	private Map<Long, ObjectPoolEntry> objectPool = new HashMap<>();

	/**
	 * the registered Classes which use the {@link JsAnnotationClassFactory}. This
	 * map is used for instantiating objects from Java instead of js
	 */
	private Map<Class<?>, JsAnnotationClassFactory> annotationClassFactories = new HashMap<>();

	public ObjectPool(JsRuntime runtime) {
		this.runtime = runtime;
	}

	public void registerClass(String modulePath, Class<?> objClass) {
		List<AnnotationMatch<JsObject>> objects = Annotations.findAnnotations(objClass, JsObject.class);
		if (objects.size() == 0) {
			throw new IllegalArgumentException(
					"The class \"" + objClass.getName() + "\" to have a @JsObject annotation");
		}
		AnnotationMatch<JsObject> object = objects.get(0);
		Class<?> foundOn = (Class<?>) object.getFoundOn();
		scanClass(modulePath, foundOn);
	}

	/**
	 * Scans the given class and (if successful) creates and adds a
	 * {@link JsAnnotationClassFactory}
	 * 
	 * @param moudlePath  the name of the factory and the path to the js file
	 * @param searchClazz the class to search in
	 */
	private void scanClass(String moudlePath, Class<?> searchClazz) {
		if (runtime.getFactoryManager().doesFactoryExist(moudlePath))
			return;

		JsAnnotationClassFactory factory = new JsAnnotationClassFactory(searchClazz, moudlePath, this);

		runtime.getFactoryManager().registerFactory(moudlePath, factory);
		annotationClassFactories.put(searchClazz, factory);
	}

	/**
	 * Returns a new unique id
	 * 
	 * @return the id
	 */
	protected long requestId() {
		if (nextId.get() > Long.MAX_VALUE - 10) {
			throw new IllegalStateException(
					"The nextId of the ObjectPool is near the maxId. Please restart the editor to resolve this issue");
		}
		return nextId.incrementAndGet();
	}

	/**
	 * Puts a new object into the pool
	 * 
	 * @param id  the id (has to be unique)
	 * @param obj the object
	 */
	protected void putObject(long id, V8Object jsObj, Object javaObj, JsAnnotationClassFactory factory) {
		if (objectPool.containsKey(id)) {
			throw new IllegalStateException("There can only be one object with the id \"" + id + "\"");
		}
		ObjectPoolEntry entry = new ObjectPoolEntry(id, jsObj, javaObj, factory);
		objectPool.put(id, entry);
	}

	public Object getJavaObj(long id) {
		return objectPool.get(id).getJavaObj();
	}

	public V8Object getJsObj(long id) {
		if (!objectPool.containsKey(id))
			return null;
		return objectPool.get(id).getJsObj();
	}

	public V8Object getJsObj(Object obj) throws NoIdFoundException {
		return getJsObj(getId(obj));
	}

	public JsAnnotationClassFactory getFactory(long id) {
		if (!objectPool.containsKey(id))
			return null;
		return objectPool.get(id).getFactory();
	}

	/**
	 * Loads the properties file from the given path and processes a properties
	 * object the keys are the paths to the js files and the value the java path to
	 * the class (like java.lang.String).<br>
	 * There cannot be a library which has the module paths "initLibs", because the
	 * value to this key is treated as a semicolon (;) terminated list where every
	 * entry is a library/class which is loaded at startup of the {@link JsRuntime}
	 * 
	 * @param propertiesFile the properties file
	 * 
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	public void processClassProperties(File propertiesFile) throws IOException {
		Properties properties = new Properties();
		properties.load(new FileInputStream(propertiesFile));
		processClassProperties(properties);
	}

	/**
	 * processes a properties object the keys are the paths to the the js files and
	 * the value the java path to the class (like java.lang.String).<br>
	 * There cannot be a library which has the module paths "initLibs", because the
	 * value to this key is treated as a semicolon (;) terminated list where every
	 * entry is a library/class which is loaded at startup of the {@link JsRuntime}
	 * 
	 * @param properties the properties
	 * 
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	public void processClassProperties(Properties properties) {
		HashMap<String, Object> map = new HashMap(properties);
		String initLibsStr = (String) map.remove("initLibs");
		for (String key : map.keySet()) {
			Object value = map.get(key);
			if (!(value instanceof String)) {
				return;
			}
			try {
				loadClass(key, (String) value);
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException
					| SecurityException | IllegalArgumentException | InvocationTargetException e) {
				logger.error("An error occured while loading class factory \"" + key + "\" (path: \"" + value + "\") ",
						e);
			}
		}
		String[] initLibs = Arrays.stream(initLibsStr.split(";")).filter(lib -> !lib.trim().isEmpty())
				.toArray(String[]::new);
		for (String lib : initLibs) {
			try {
				V8Object obj = new V8Object(runtime.getV8());
				runtime.getFactoryManager().initClass(lib, obj);
				obj.release();
			} catch (Exception e) {
				logger.warn("An error occured while requiring \"" + lib + "\":", e);
			}
		}
	}

	/**
	 * Loads the given class under the given factory id. If there is a
	 * {@link JsObject} annotation present on the given class, then it is loaded via
	 * the {@link #put(Class)} function else it is instatiated and added as a
	 * factory via {@link #registerFactory(String, JsClassFactory)}
	 * 
	 * @param factoryId        the id of the factory
	 * @param factoryClassPath the path to the class
	 * 
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws IllegalArgumentException
	 */
	private void loadClass(String factoryId, String factoryClassPath)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException,
			SecurityException, IllegalArgumentException, InvocationTargetException {

		ClassLoader loader = getClass().getClassLoader();
		Class<?> factoryClass = loader.loadClass(factoryClassPath);
		if (Annotations.findAnnotations(factoryClass, JsObject.class).size() > 0) {
			registerClass(factoryId, factoryClass);
		} else if (JsClassFactory.class.isAssignableFrom(factoryClass)) {
			Constructor<?> constructor = factoryClass.getDeclaredConstructor();
			constructor.setAccessible(true);
			JsClassFactory factoryObj = (JsClassFactory) constructor.newInstance();
			constructor.setAccessible(false);
			runtime.getFactoryManager().registerFactory(factoryId, factoryObj);
		} else {
			throw new IllegalArgumentException(
					"Given factory \"" + factoryId + "\" (path: \"" + factoryClassPath + "\" isn't a factory");
		}
	}

	/**
	 * Creates a new object with the {@link JsAnnotationClassFactory} which is
	 * registered for the given class. If the factory is not found, then a
	 * {@link FactoryNotFoundException} is thrown
	 * 
	 * @param clazz the class which should be instantiated
	 * @return the created object
	 * @throws FactoryNotFoundException
	 * @throws FailedObjectCreationException
	 */
	public <T> T createObject(Class<T> clazz, Object[] args) throws FactoryNotFoundException, FailedObjectCreationException {
		JsAnnotationClassFactory factory = annotationClassFactories.get(clazz);
		if (factory == null) {
			throw new FactoryNotFoundException(clazz.getName());
		}
		Object obj = factory.createObject(runtime, args);
		if (!clazz.isInstance(obj)) {
			throw new IllegalArgumentException("Wrong class: \"" + clazz.getName() + "\" isn't compatible with \""
					+ obj.getClass().getName() + "\"");
		}
		return (T) obj;
	}

	/**
	 * Serializes an object to an type which is supported by the 2jv8 library.
	 * Primitives like Double, Integer, Float, Number, Boolean or Strings are
	 * returned without any conversion, so is any V8Value and any null value. For
	 * any other regular object, the associated js object is returned. This requires
	 * a {@link JsId} annotation to work and the object has to be created using
	 * {@link #createObject(Class)}
	 * 
	 * @param clazz the type of object
	 * @param obj   the object to serialize
	 * @return the serialized object
	 * @throws SerializeException
	 */
	public <T> Object serialize(Class<T> clazz, T obj) throws SerializeException {
		if (obj instanceof Double || obj instanceof Integer || obj instanceof Float || obj instanceof Number
				|| obj instanceof Boolean || obj instanceof String || obj instanceof V8Value) {
			return obj;
		} else if (obj == null) {
			return null;
		} else {
			try {
				long id = getId(obj);
				V8Object jsObj = getJsObj(id);
				if (jsObj.isReleased()) {
					throw new SerializeException(
							"Couldn't serialize object, because V8Value was already released (clazz: \""
									+ clazz.getName() + "\")");
				}
				return jsObj;
			} catch (NoIdFoundException e) {
				throw new SerializeException("Couldn't serialize object, because no Id was found in the pool", e);
			}
		}
	}

	/**
	 * Deserializes an object from the j2v8 library. Primitives such as Double,
	 * Integer, Float, Number, Booleans or Strings are casted returned. If there was
	 * an error while casting, an {@link InvalidTypeException} is thrown. For V8,
	 * the associated java object is returned using the id. For this to work and
	 * {@link JsId} annotation is required and the v8 object has to have a java
	 * representation
	 * 
	 * @param clazz the clas which is expected
	 * @param obj   the object to deserialize to the given class
	 * @return the deserialized object
	 * @throws SerializeException
	 * @throws InvalidTypeException
	 */
	public <T> T deserialize(Class<T> clazz, Object obj) throws SerializeException, InvalidTypeException {
		if (obj instanceof Double || obj instanceof Integer || obj instanceof Float || obj instanceof Number
				|| obj instanceof Boolean || obj instanceof String) {
			if (!clazz.isInstance(obj)) {
				throw new InvalidTypeException(
						"Cannot deserialize \"" + obj.getClass().getName() + "\" to \"" + clazz.getName() + "\"");
			}
			return (T) obj;
		} else if (obj == null) {
			return null;
		} else if (obj instanceof V8Object) {
			try {
				V8Object v8Obj = (V8Object) obj;
				long id = getId(v8Obj);
				return (T) getJavaObj(id);
			} catch (NoIdFoundException e) {
				throw new SerializeException("Couldn't serialize object, because no Id was found in the pool", e);
			}
		} else {
			throw new IllegalArgumentException(
					"Class \"" + obj.getClass().getName() + "\" cannot be deserialized to a java object");
		}
	}

	@Override
	public void close() throws IOException {
		for (ObjectPoolEntry entry : objectPool.values()) {
			Object javaObj = entry.getJavaObj();
			if (javaObj instanceof Closeable) {
				((Closeable) javaObj).close();
			}
			entry.getJsObj().release();
		}

	}

	private static class ObjectPoolEntry {
		private long id;
		private V8Object jsObj;
		private Object javaObj;
		private JsAnnotationClassFactory factory;

		/**
		 * constructor
		 * 
		 * @param id      the id
		 * @param jsObj   the js v8 object
		 * @param javaObj the java object
		 * @param factory the factory used for the java object
		 */
		public ObjectPoolEntry(long id, V8Object jsObj, Object javaObj, JsAnnotationClassFactory factory) {
			this.id = id;
			this.jsObj = jsObj;
			this.javaObj = javaObj;
			this.factory = factory;
		}

		public long getId() {
			return id;
		}

		public V8Object getJsObj() {
			return jsObj;
		}

		public Object getJavaObj() {
			return javaObj;
		}

		public JsAnnotationClassFactory getFactory() {
			return factory;
		}
	}

	/**
	 * Returns the id of a v8 object or throws NoIdFoundException
	 * 
	 * @param obj the object of which the id is returned
	 * @return the id
	 * @throws NoIdFoundException
	 */
	public static long getId(V8Object obj) throws NoIdFoundException {
		try {
			return obj.getInteger("_id");
		} catch (V8ResultUndefined e) {
			throw new NoIdFoundException();
		}
	}

	/**
	 * Returns the id of a java object or throws NoIdFoundException
	 * 
	 * @param obj the object of which the id is returned
	 * @return the id
	 * @throws NoIdFoundException
	 */
	public static long getId(Object obj) throws NoIdFoundException {
		if (obj == null)
			throw new NullPointerException("Given obj is null");
		List<AnnotationMatch<JsId>> jsIds = Annotations.findAnnotations(obj.getClass(), JsId.class);
		if (jsIds.size() == 0) {
			throw new NoIdFoundException("No JsId annotation found on class \"" + obj.getClass().getName());
		}
		AnnotationMatch<JsId> match = jsIds.get(0);
		// save to cast, because @JsId can only occur on fields
		Field field = (Field) match.getFoundOn();
		field.setAccessible(true);
		long id;
		try {
			id = field.getLong(obj);
			return id;
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new IllegalStateException("Cannot retrieve id because of: ", e);
		} finally {
			field.setAccessible(false);
		}
	}
}
