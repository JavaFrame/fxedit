package ch.sebi.fxedit.runtime;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.eclipsesource.v8.JavaCallback;
import com.eclipsesource.v8.JavaVoidCallback;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;

import ch.sebi.fxedit.exception.FactoryNotFoundException;
import ch.sebi.fxedit.exception.ScriptNotFoundException;
import ch.sebi.fxedit.runtime.lib.require.RequireLib;
import ch.sebi.fxedit.runtime.reflection.JsClassFactoryManager;
import ch.sebi.fxedit.runtime.reflection.ObjectPool;
import ch.sebi.fxedit.runtime.reflection.annotation.JsFunction;

public class JsRuntime implements Closeable {
	private final Logger logger = LogManager.getLogger(getClass());
	private V8 v8;

	private RequireLib requireLib;

	private JsClassFactoryManager factoryManager; 
	private ObjectPool objectPool;


	public JsRuntime() {
		v8 = V8.createV8Runtime();
		factoryManager = new JsClassFactoryManager(this);
		objectPool = new ObjectPool(this);
		initRequireLib();
		initObjectPoolLib();
	}

	/**
	 * initializes the require lib
	 */
	private void initRequireLib() {
		requireLib = new RequireLib();
		V8Array requirePathArray = new V8Array(getV8());
		requirePathArray.push(".");
		requirePathArray.push("./jsruntime");
		requireLib.init(this, requirePathArray);

		// v8.registerJavaMethod(requireLib, "jsRequire", "require", new Class[]
		// {String.class});
		v8.registerJavaMethod(new JavaCallback() {

			@Override
			public Object invoke(V8Object receiver, V8Array parameters) {
				if (parameters.length() != 1) {
					throw new IllegalArgumentException(
							"require(path) expectes 1 parameters");
				}
				String path = parameters.getString(0);
				try {
					return requireLib.jsRequire(path);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}, "require");
		v8.add("requirePaths", requirePathArray);
	}

	/**
	 * initializes the object pool lib
	 */
	private void initObjectPoolLib() {
		v8.registerJavaMethod((JavaVoidCallback) (receiver, parameters) -> {
			if (parameters.length() != 2 && parameters.length() != 3) {
				throw new IllegalArgumentException(
						"_initClass(id, classObj[, usePrototype=true]) expectes at least 2 parameters");
			}
			String id = parameters.getString(0);
			V8Object jsClassObj = parameters.getObject(1);
			boolean usePrototype = true;
			if (parameters.length() >= 3) {
				usePrototype = parameters.getBoolean(2);
			}
			V8 v8 = getV8();
			V8Object objectClass = v8.getObject("Object");
			if (usePrototype) {
				V8Array args = new V8Array(v8);
				args.push(jsClassObj);
				V8Object prototypeObj = objectClass.getObject("prototype");
				args.release();
				jsClassObj.release();
				jsClassObj = prototypeObj;
			}
			try {
				factoryManager.initClass(id, jsClassObj);
			} catch (FactoryNotFoundException e) {
				throw new RuntimeException(e);
			} finally {
				objectClass.release();
				jsClassObj.release();
			}
		}, "_initClass");
		v8.registerJavaMethod((JavaVoidCallback) (receiver, parameters) -> {
			if (parameters.length() != 2) {
				throw new IllegalArgumentException("_initObject(id, obj) expectes 2 parameters");
			}
			String id = parameters.getString(0);
			V8Object jsObj = parameters.getObject(1);
			V8 v8 = getV8();
			try {
				factoryManager.initObject(id, jsObj);
			} catch (FactoryNotFoundException e) {
				throw new RuntimeException(e);
			} finally {
				jsObj.release();
			}
		}, "_initObj");
		File classPropertiesFile = new File("./classes.properties");
		try {
			objectPool.processClassProperties(classPropertiesFile);
		} catch (IOException e) {
			logger.warn("Cannot load the \"" + classPropertiesFile.getAbsolutePath() + "\"");
		}

	}

	/**
	 * Executes the rc.js file
	 * 
	 * @throws IOException if the file wasn't found or couldn't be read
	 */
	public void executeRC() throws IOException {
		File rcFile = new File("./rc.js");
		if (!rcFile.exists()) {
			throw new ScriptNotFoundException("rc.js", new String[0]);
		}
		executeFile(rcFile);
	}

	/**
	 * Executes the given file
	 * 
	 * @param f
	 * @return
	 * @throws IOException
	 */
	public Object executeFile(File f) throws IOException {
		String script = new String(Files.readAllBytes(f.toPath()));
		return v8.executeScript(script, f.getPath(), 0);
	}

	/**
	 * Returns the v8 js engine of this js runtime
	 * 
	 * @return the v8 object
	 */
	public V8 getV8() {
		return v8;
	}

	/**
	 * Returns the object pool object
	 * 
	 * @return the object pool object
	 */
	public ObjectPool getObjectPool() {
		return objectPool;
	}
	
	/**
	 * Returns the factory manager
	 * @return the factory manager
	 */
	public JsClassFactoryManager getFactoryManager() {
		return factoryManager;
	}
	
	/**
	 * Creates a new object with the {@link JsAnnotationClassFactory} which is registered for
	 * the given class. If the factory is not found, then a {@link FactoryNotFoundException} is
	 * thrown
	 * @param clazz the class which should be created
	 * @return the created object
	 * @throws FactoryNotFoundException
	 * @see {@link ObjectPool#createObject(Class, JsRuntime)}
	 */
	public <T> T createObject(Class<T> clazz) throws FactoryNotFoundException {
		return getObjectPool().createObject(clazz);
	}
	
	/**
	 * Returns the require lib object
	 * @return the require lib
	 */
	public RequireLib getRequireLib() {
		return requireLib;
	}

	@Override
	public void close() throws IOException {
		getObjectPool().close();
	}
}
