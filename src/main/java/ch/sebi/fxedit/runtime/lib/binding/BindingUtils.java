package ch.sebi.fxedit.runtime.lib.binding;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.eclipsesource.v8.JavaCallback;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Function;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.V8Value;
import com.eclipsesource.v8.utils.MemoryManager;
import com.eclipsesource.v8.utils.V8ObjectUtils;

import ch.sebi.fxedit.exception.InvalidTypeException;
import ch.sebi.fxedit.exception.SerializeException;
import ch.sebi.fxedit.runtime.JsRuntime;
import ch.sebi.fxedit.runtime.reflection.ObjectPool;
import ch.sebi.fxedit.runtime.reflection.annotation.JsFunction;
import javafx.beans.Observable;
import javafx.beans.property.Property;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

/**
 * A class with util functions for bindings in js and java
 * 
 * @author sebi
 *
 */
public class BindingUtils {
	/**
	 * logger
	 */
	private static final Logger logger = LogManager.getLogger(BindingUtils.class);

	/**
	 * Creates a new js binding and returns it
	 * 
	 * @param runtime the runtime
	 * @return the binding in a v8 object
	 */
	public static <T> V8Object createBinding(JsRuntime runtime) {
		V8 v8 = runtime.getV8();
		V8Object binding = v8.executeObjectScript("new (require('binding'))();");
		return binding;
	}

	/**
	 * Binds the given property to the given js binding. The type has to be
	 * compatible.
	 * 
	 * @param property the javafx property
	 * @param binding  the js property
	 * @param clazz    the type of the javafx property
	 * @throws SerializeException
	 */
	public static <T> void bindProperty(Property<T> property, final V8Object binding, Class<T> clazz, JsRuntime runtime)
			throws SerializeException {
		if (binding.isUndefined()) {
			throw new NullPointerException("Binding V8Object is undefined");
		}
		boolean[] recentlyChanged = new boolean[] { false };
		// set the js value to the java value
		V8Array initSetValueArgs = new V8Array(runtime.getV8());
		try {
			initSetValueArgs.push(runtime.getObjectPool().serialize(clazz, property.getValue()));
			binding.executeVoidFunction("setValue", initSetValueArgs);
		} finally {
			initSetValueArgs.release();
		}

		// register listener on js side
		V8Array args = new V8Array(runtime.getV8());
		args.push(new V8Function(runtime.getV8(), new JavaCallback() {

			@Override
			public Object invoke(V8Object receiver, V8Array parameters) {
				if (recentlyChanged[0]) {
					recentlyChanged[0] = false;
					return null;
				}
				Object newV8Value = parameters.get(1);
				T newValue;
				try {
					newValue = runtime.getObjectPool().deserialize(clazz, newV8Value);
				} catch (SerializeException | InvalidTypeException e) {
					throw new RuntimeException(e);
				}
				if (!clazz.isAssignableFrom(newValue.getClass())) {
					try {
						String msg = "Invalid type \"" + newValue.getClass().getName() + "\" set to Propert<"
								+ clazz.getName() + ">";
						throw new InvalidTypeException(msg);
					} catch (InvalidTypeException e) {
						logger.error("An exception occured: ", e);
						throw new RuntimeException(e);
					}
				}
				// prevents recursion loop java listener sets js listener -> js listener sets
				// java listener -> ...
				recentlyChanged[0] = true;
				property.setValue(clazz.cast(newValue));
				return null;
			}
		}));
		binding.executeFunction("addListener", args);
		args.release();

		binding.setWeak();

		property.addListener((ChangeListener<T>) (observable, oldValue, newValue) -> {
			// prevents recursion loop java listener sets js listener -> js listener sets
			// java listener -> ...
			if (recentlyChanged[0]) {
				recentlyChanged[0] = false;
				return;
			}
			if (binding.isReleased()) {
				throw new IllegalStateException("Js Binding was already released");
			}
			V8Array args1 = new V8Array(runtime.getV8());
			try {
				recentlyChanged[0] = true;
				args1.push(runtime.getObjectPool().serialize(clazz, newValue));
				binding.executeVoidFunction("setValue", args1);
			} catch (RuntimeException e) {
				recentlyChanged[0] = false;
				throw e;
			} catch (SerializeException e) {
				throw new RuntimeException(e);
			} finally {
				args1.release();
			}
		});
	}

	public static <T> void bindObservableList(ObservableList<T> list, V8Object arrayBinding, Class<T> clazz,
			JsRuntime runtime) throws SerializeException, InvalidTypeException {
		if (arrayBinding.isUndefined())
			throw new NullPointerException("Binding V8Object is undefined");

		V8 v8 = runtime.getV8();
		ObjectPool pool = runtime.getObjectPool();

		MemoryManager scope = new MemoryManager(v8);

		// serialize object list
		List<Object> serializedList = new ArrayList<>();
		for (Object obj : list) {
			if (obj == null) {
				serializedList.add(null);
			} else {
				serializedList.add(pool.serialize((Class) obj.getClass(), obj));
			}
		}

		V8Array _arrayNew = V8ObjectUtils.toV8Array(v8, serializedList);
		V8Array _array = arrayBinding.getArray("_value");
		V8Array changes = getChanges(list, _array, runtime);

		// set _value variable
		arrayBinding.add("_value", _arrayNew);

		// call _fireChanges function
		V8Array fireChangesArgs = new V8Array(runtime.getV8());
		fireChangesArgs.push(_array);
		fireChangesArgs.push(_arrayNew);
		fireChangesArgs.push(changes);
		arrayBinding.executeFunction("_fireChanges", fireChangesArgs);

		V8Array addListenerArgs = new V8Array(v8);
		addListenerArgs.push(new V8Function(v8, new JavaCallback() {

			@Override
			public Object invoke(V8Object receiver, V8Array parameters) {
				MemoryManager scope = new MemoryManager(v8);
				V8Array oldArray = parameters.getArray(0);
				V8Array newArray = parameters.getArray(1);
				V8Array changes = parameters.getArray(2);
				for (int i = 0; i < changes.length(); i++) {
					V8Object change = changes.getObject(i);
					String type = change.getString("type");
					int from = change.getInteger("from");
					int to = change.getInteger("to");
				}
				scope.release();
				return null;
			}
		}));

		list.addListener((ListChangeListener<T>) c -> {
		});

		scope.release();
	}

	private static V8Array getChanges(ObservableList list, V8Array array, JsRuntime runtime)
			throws SerializeException, InvalidTypeException {
		V8 v8 = runtime.getV8();
		ObjectPool pool = runtime.getObjectPool();
		V8Array changes = new V8Array(v8);

		for (int i = 0; i < array.length() && i < list.size(); i++) {
			Object javaObj = list.get(i);
			Object jsObj = pool.deserialize(Object.class, array.get(i));

			if ((javaObj == null && jsObj != null) || javaObj != jsObj || javaObj.equals(jsObj)) {
				changes.push(createChange("update", i, i + 1, runtime));
			}
		}

		if (list.size() > array.length()) {
			changes.push(createChange("add", array.length(), list.size(), runtime));
		}
		if (array.length() > list.size()) {
			changes.push(createChange("remove", list.size(), array.length(), runtime));
		}
		return changes;
	}

	private static V8Object createChange(String type, int from, int to, JsRuntime runtime) {
		V8 v8 = runtime.getV8();
		V8Object change = v8.executeObjectScript(
				"new (require('binding).Change)('" + type + "', " + from + ", " + to + ");", "createChangeScript", 0);
		return change;
	}

	private static <T> void replayChangeOnJavaList(String type, int from, int to, ObservableList<T> list,
			V8Array oldArray, V8Array newArray, JsRuntime runtime) {

		V8 v8 = runtime.getV8();
		ObjectPool pool = runtime.getObjectPool();
		MemoryManager scope = new MemoryManager(v8);
		switch(type) {
		case "add":
			break;
		case "remove":
			break;
		case "update":
			break;
		case "permutation":
			break;
		default:
			throw new IllegalStateException("Illegal type \"" + type + "\"");
		}
	}
}
