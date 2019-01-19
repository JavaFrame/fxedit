package ch.sebi.fxedit.runtime.lib.binding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.naming.OperationNotSupportedException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.eclipsesource.v8.JavaCallback;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Function;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.utils.MemoryManager;
import com.eclipsesource.v8.utils.V8ObjectUtils;

import ch.sebi.fxedit.exception.InvalidTypeException;
import ch.sebi.fxedit.exception.SerializeException;
import ch.sebi.fxedit.runtime.JsRuntime;
import ch.sebi.fxedit.runtime.reflection.ObjectPool;
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
		V8Object binding = v8.executeObjectScript("new (require('binding').Binding)();");
		return binding;
	}

	/**
	 * Creates a new js ArrayBinding and returns it
	 * 
	 * @param runtime the runtime
	 * @return the binding in a v8 object
	 */
	public static <T> V8Object createArrayBinding(JsRuntime runtime) {
		V8 v8 = runtime.getV8();
		V8Object binding = v8.executeObjectScript("new (require('binding').ArrayBinding)();");
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

	/**
	 * Binds a {@link ObservableList} to a ArrayBinding object. The ArrayBinding is
	 * reseted to the current value of this observable list. This means on binding
	 * all listeners are called with the reset type
	 * 
	 * @param list         the observable list
	 * @param arrayBinding the js Arraybinding
	 * @param clazz        the type of the observable list
	 * @param runtime      the js runtime which should be used
	 * @throws SerializeException
	 * @throws InvalidTypeException
	 */
	public static <T> void bindObservableList(ObservableList<T> list, V8Object arrayBinding, Class<T> clazz,
			JsRuntime runtime) {
		if (arrayBinding.isUndefined())
			throw new NullPointerException("Binding V8Object is undefined");

		// init variables used
		V8 v8 = runtime.getV8();
		ObjectPool pool = runtime.getObjectPool();
		MemoryManager scope = new MemoryManager(v8);

		resetArrayBinding(list, arrayBinding, runtime);

		V8Array addListenerArgs = new V8Array(v8);
		addListenerArgs.push(new V8Function(v8, new JavaCallback() {

			@Override
			public Object invoke(V8Object receiver, V8Array parameters) {
				MemoryManager scope = new MemoryManager(v8);
				V8Array oldArray = parameters.getArray(0);
				V8Array newArray = parameters.getArray(1);
				V8Array changes = parameters.getArray(2);
				for (int changeI = 0; changeI < changes.length(); changeI++) {
					V8Object change = changes.getObject(changeI);
					String type = change.getString("type");
					int from = change.getInteger("from");
					int to = change.getInteger("to");
					switch (type) {
					case "add":
						for (int i = from; i < to; i++) {
							try {
								T obj = pool.deserialize(clazz, newArray.get(i));
								list.add(obj);
							} catch (SerializeException | InvalidTypeException e) {
								logger.error("Cannot deserialize id \"" + i + "\" from newArray!", e);
							}
						}
						break;
					case "permutation":
						throw new UnsupportedOperationException("The 'permutation' type isn't");
					case "remove":
						// starts from to, to not change the ids of the list
						for (int i = to - 1; i >= from; i--) {
							list.remove(i);
						}
						break;
					case "update":
						for (int i = from; i < to; i++) {
							try {
								T obj = pool.deserialize(clazz, newArray.get(i));
								list.set(i, obj);
							} catch (SerializeException | InvalidTypeException e) {
								logger.error("Cannot deserialize id \"" + i + "\" from newArray!", e);
							}
						}
						break;
					case "reset":
						throw new UnsupportedOperationException("The 'reset' type isn't");
					default:
						throw new UnsupportedOperationException("The \"" + type + "\" type isn't");
					}
				}
				scope.release();
				return null;
			}
		}));

		list.addListener((ListChangeListener<T>) c -> {
			MemoryManager changeListenerScope = new MemoryManager(v8);
			try {
				while (c.next()) {
					// fetches the array. It is in the while loop, so that it is up to date
					V8Array _array = arrayBinding.getArray("_value");
					String type = null;
					if (c.wasAdded()) {
						type = "add";
						// deserializing list
						// args for js function Array.prototype.splice(start [, deleteCount[,
						// items...]])
						V8Array pushArgs = new V8Array(v8);
						pushArgs.push(c.getFrom()); // start
						pushArgs.push(0); // deleteCount
						// items
						for (T obj : c.getAddedSubList()) {
							try {
								pushArgs.push(pool.serialize(clazz, obj));
							} catch (SerializeException e) {
								logger.error("Couldn't deserialize object", e);
								pushArgs.pushNull();
							}
						}
						_array.executeVoidFunction("splice", pushArgs);
					} else if (c.wasRemoved()) {
						type = "remove";
						// deserializing list
						// args for js function Array.prototype.slice(start [, deleteCount])
						V8Array pushArgs = new V8Array(v8);
						pushArgs.push(c.getFrom()); // start
						pushArgs.push(c.getRemovedSize()); // deleteCount
						_array.executeVoidFunction("splice", pushArgs);
					} else if (c.wasPermutated()) {
						type = "permutation";
						throw new UnsupportedOperationException("Permutations are not supported");
					} else if (c.wasUpdated()) {
						type = "update";
						// removes all elements in the update range
						V8Array spliceArgs = new V8Array(v8);
						spliceArgs.push(c.getFrom());
						spliceArgs.push(c.getTo() - c.getFrom());
						List<? extends T> updateSublist = c.getList().subList(c.getFrom(), c.getTo());
						for (T obj : updateSublist) {
							try {
								spliceArgs.push(pool.serialize(clazz, obj));
							} catch (SerializeException e) {
								logger.error("Couldn't deserialize object", e);
								spliceArgs.pushNull();
							}
						}
					} else {
						throw new IllegalStateException("Unkown change: " + c);
					}
					V8Array fireListenersArgs = new V8Array(v8);
					fireListenersArgs.push(_array);
					fireListenersArgs.push(arrayBinding.get("_value")); // fetches the new array after the operations
					V8Object change = createChange(type, c.getFrom(), c.getTo(), runtime);
					fireListenersArgs.push(change);
					arrayBinding.executeVoidFunction("_fireListeners", fireListenersArgs);
				}
			} catch (Exception e) {
				//if an error occured, reset the array binding, so it is the same as the java
				//this should prevent out of sync arrays
				logger.error("An error occurred; reset ArrayBinding", e);
				resetArrayBinding(list, arrayBinding, runtime);
			} finally {
				// this ensures that the scope is always released
				changeListenerScope.release();
			}
		});

		scope.release();
	}

	/**
	 * Resets the array binding values to the values of the list. All v8 values
	 * created in this function are released.
	 * 
	 * @param list         the observable list
	 * @param arrayBinding the v8 ArrayBinding object
	 * @param runtime      the js runtime
	 * @throws SerializeException
	 */
	private static void resetArrayBinding(ObservableList<?> list, V8Object arrayBinding, JsRuntime runtime) {
		V8 v8 = runtime.getV8();
		ObjectPool pool = runtime.getObjectPool();

		MemoryManager scope = new MemoryManager(v8);

		// serialize object list
		List<Object> serializedList = new ArrayList<>();
		for (Object obj : list) {
			if (obj == null) {
				serializedList.add(null);
			} else {
				try {
					serializedList.add(pool.serialize((Class) obj.getClass(), obj));
				} catch (SerializeException e) {
					logger.error("Couldn't serialize object of \"" + obj.getClass().getName() + "\"", e);
					serializedList.add(null);
				}
			}
		}

		V8Array _arrayNew = V8ObjectUtils.toV8Array(v8, serializedList);
		V8Array _array = arrayBinding.getArray("_value");

		V8Object change = createChange("reset", 0, list.size(), runtime);
		V8Array changes = new V8Array(v8);
		changes.push(change);

		// set _value variable
		arrayBinding.add("_value", _arrayNew);

		// call _fireChanges function
		V8Array fireChangesArgs = new V8Array(runtime.getV8());
		fireChangesArgs.push(_array);
		fireChangesArgs.push(_arrayNew);
		fireChangesArgs.push(changes);
		arrayBinding.executeFunction("_fireChanges", fireChangesArgs);

		// release created v8 objects
		scope.release();
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
		switch (type) {
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
