package ch.sebi.fxedit.runtime.reflection;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.eclipsesource.v8.V8Object;

import ch.sebi.fxedit.exception.FactoryNotFoundException;
import ch.sebi.fxedit.runtime.JsRuntime;

public class JsClassFactoryManager {
	private final Logger logger = LogManager.getLogger();
	
	/**
	 * the js runtime which initialized this class
	 */
	private JsRuntime runtime;

	/**
	 * the js factories with their tag/name
	 */
	private Map<String, JsClassFactory> factories = new HashMap<>();
	
	/**
	 * 
	 * @param runtime
	 * @param factories
	 */
	public JsClassFactoryManager(JsRuntime runtime) {
		this.runtime = runtime;
	}

	/**
	 * Puts a factory with the given classFactoryId in to the factory map
	 * 
	 * @param classFactoryId the factory id
	 * @param factory        the factory itself
	 */
	public void registerFactory(String classFactoryId, JsClassFactory factory) {
		if (factories.containsKey(classFactoryId)) {
			throw new IllegalStateException("The factory with the id \"" + classFactoryId + "\" already exists");
		}
		logger.info("register factory \"" + classFactoryId + "\" with the java path \"" + factory.getClass().getName()
				+ "\"");
		factories.put(classFactoryId, factory);
	}

	/**
	 * Returns a factory with the given id or throws a {@link FactoryNotFoundException} exception
	 * @param id the id of the factory
	 * @return the factory
	 * @throws FactoryNotFoundException
	 */
	public JsClassFactory getFactory(String id) throws FactoryNotFoundException {
		JsClassFactory factory = factories.get(id);
		if (factory == null) {
			throw new FactoryNotFoundException(id);
		}
		return factory;
	}
	
	/**
	 * Checks if there is a factory with the given id
	 * @param id the id of the factory
	 * @return
	 */
	public boolean doesFactoryExist(String id) {
		return factories.containsKey(id);
	}

	public void initClass(String id, V8Object jsClassObj) throws FactoryNotFoundException {
		getFactory(id).initClass(runtime, jsClassObj);
	}

	public void initObject(String id, V8Object jsObj) throws FactoryNotFoundException {
		getFactory(id).initObject(runtime, jsObj);
	}
	
}
