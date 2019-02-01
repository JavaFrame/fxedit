package ch.sebi.fxedit.runtime.lib.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.eclipsesource.v8.JavaVoidCallback;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.V8ScriptExecutionException;

import ch.sebi.fxedit.exception.FactoryNotFoundException;
import ch.sebi.fxedit.exception.FailedObjectCreationException;
import ch.sebi.fxedit.exception.InvalidTypeException;
import ch.sebi.fxedit.exception.NoIdFoundException;
import ch.sebi.fxedit.exception.SerializeException;
import ch.sebi.fxedit.runtime.JsRuntime;
import ch.sebi.fxedit.runtime.reflection.ObjectPool;
import ch.sebi.fxedit.runtime.reflection.annotation.JsBinding;
import ch.sebi.fxedit.runtime.reflection.annotation.JsFunction;
import ch.sebi.fxedit.runtime.reflection.annotation.JsId;
import ch.sebi.fxedit.runtime.reflection.annotation.JsObject;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class BindingPropertyTest {
	private Logger logger = LogManager.getLogger(getClass());
	private JsRuntime runtime;
	private V8 v8;

	@BeforeEach
	void setup() {
		runtime = new JsRuntime();
		runtime.getRequireLib().addRequirePath("./jstestruntime");
		v8 = runtime.getV8();
	}

	/**
	 * Tests if the the java property and js binding are linked correctly and
	 * listeners are called correctly. This tests uses Strings. Types like int,
	 * float long, ... should work too. If complex data structure work is tested in
	 * a different Test
	 * 
	 * @throws InterruptedException
	 * @throws SerializeException
	 */
	@Test
	void testSetJavaProp() throws InterruptedException, SerializeException {
		CountDownLatch latch = new CountDownLatch(1);
		AtomicInteger counter = new AtomicInteger();
		StringProperty javaProp = new SimpleStringProperty("val1");
		javaProp.addListener((ob, oldV, newV) -> {
			logger.info("java prop changed from " + oldV + " to " + newV);
			latch.countDown();
			counter.incrementAndGet();
		});
		AtomicInteger stateCounter = new AtomicInteger(0);
		v8.registerJavaMethod((JavaVoidCallback) (receiver, parameters) -> {
			assertEquals(parameters.length(), 2, "Invalid call to _bindingChanged");
			Object oldV = parameters.get(0);
			Object newV = parameters.get(1);
			logger.info("javascript binding changed from " + oldV + " to " + newV);
			if (stateCounter.get() == 0) {
				assertEquals("jsInitValue", oldV);
				assertEquals("val1", newV);
			} else if (stateCounter.get() == 1) {
				assertEquals("val1", oldV);
				assertEquals("val2", newV);
			}
			stateCounter.incrementAndGet();
			counter.incrementAndGet();
			latch.countDown();
		}, "_bindingChanged");
		V8Object jsProp = v8.executeObjectScript(
				"let binding = new (require('util.binding').Binding)('jsInitValue');"
						+ "binding.addListener((oldV, newV) => _bindingChanged(oldV, newV)); binding;",
				"jsPropTest", 0);
		assertFalse(jsProp.isUndefined(), "Js Binding is undefined");
		BindingUtils.bindProperty(javaProp, jsProp, String.class, runtime);
		javaProp.set("val2");
		V8Array setValueArgs = new V8Array(v8);
		setValueArgs.push("hello");
		jsProp.executeVoidFunction("setValue", setValueArgs);
		setValueArgs.release();

		latch.await(1000, TimeUnit.MILLISECONDS);
		assertEquals(0, latch.getCount(), "Latch has not reached a countdown of zero; Missing calls to listeners");
		assertEquals(5, counter.get(), "Not the right amount of listener calls");
	}

	/**
	 * Tests if an exception is thrown, if the js binding is set to a data type
	 * which isn't supported by the java property.
	 * 
	 * @throws InterruptedException
	 * @throws SerializeException
	 */
	@Test
	void testWrongType() throws InterruptedException, SerializeException {
		StringProperty javaProp = new SimpleStringProperty("val1");
		V8Object jsProp = v8.executeObjectScript(
				"let binding = new (require('util.binding').Binding)('jsInitValue');binding;", "jsPropTest", 0);
		assertFalse(jsProp.isUndefined(), "Js Binding is undefined");
		BindingUtils.bindProperty(javaProp, jsProp, String.class, runtime);
		V8Array setValueArgs = new V8Array(v8);
		V8ScriptExecutionException exception = assertThrows(V8ScriptExecutionException.class, () -> {
			setValueArgs.push(10);
			jsProp.executeVoidFunction("setValue", setValueArgs);
			setValueArgs.release();
		});
		assertThrows(InvalidTypeException.class, () -> {
			throw exception.getCause().getCause();
		});
	}

	/**
	 * Tests if serialization of complex object works and property and bindings are
	 * linked correctly
	 * 
	 * @throws InterruptedException
	 * @throws FactoryNotFoundException
	 * @throws SerializeException
	 * @throws FailedObjectCreationException
	 */
	@Test
	void testSerializeBinding()
			throws InterruptedException, FactoryNotFoundException, SerializeException, FailedObjectCreationException {
		runtime.getObjectPool().registerClass("test.complex-binding", TestComplexBinding.class);

		CountDownLatch latch = new CountDownLatch(1);
		AtomicInteger stateCounter = new AtomicInteger(0);

		TestComplexBinding testObj = runtime.createObject(TestComplexBinding.class);
		TestComplexBinding testObj2 = runtime.createObject(TestComplexBinding.class);

		ObjectProperty<TestComplexBinding> javaProp = new SimpleObjectProperty<>(testObj);

		v8.registerJavaMethod((JavaVoidCallback) (receiver, parameters) -> {
			assertEquals(parameters.length(), 2, "Invalid call to _bindingChanged");
			Object oldV = parameters.get(0);
			Object newV = parameters.get(1);
			logger.info("javascript binding changed from " + oldV + " to " + newV);
			if (stateCounter.get() == 0) {
				assertEquals("jsInitValue", oldV);
				try {
					assertEquals(runtime.getObjectPool().getJsObj(testObj), newV);
				} catch (NoIdFoundException e) {
					fail("Could not found test object's id");
				}
			} else if (stateCounter.get() == 1) {
				try {
					assertEquals(runtime.getObjectPool().getJsObj(testObj), oldV);
					assertEquals(runtime.getObjectPool().getJsObj(testObj2), newV);
					latch.countDown();
				} catch (NoIdFoundException e) {
					fail("Could not found test object's id");
				}
			}
			stateCounter.incrementAndGet();
		}, "_bindingChanged");
		V8Object jsProp = v8.executeObjectScript(
				"let binding = new (require('util.binding').Binding)('jsInitValue');"
						+ "binding.addListener((oldV, newV) => _bindingChanged(oldV, newV)); binding;",
				"jsPropTest", 0);
		assertFalse(jsProp.isUndefined(), "Js Binding is undefined");
		BindingUtils.bindProperty(javaProp, jsProp, TestComplexBinding.class, runtime);

		javaProp.set(testObj2);

		latch.await(1000, TimeUnit.MILLISECONDS);
		assertEquals(0, latch.getCount(), "Not all listeners where called");
	}

	/**
	 * Tests if deserialization of complex object works and property and bindings
	 * are linked correctly
	 * 
	 * @throws InterruptedException
	 * @throws FactoryNotFoundException
	 * @throws SerializeException
	 * @throws FailedObjectCreationException
	 */
	@Test
	void testDeserializeBinding()
			throws InterruptedException, FactoryNotFoundException, SerializeException, FailedObjectCreationException {
		runtime.getObjectPool().registerClass("test.complex-binding", TestComplexBinding.class);

		CountDownLatch latch = new CountDownLatch(1);
		AtomicInteger stateCounter = new AtomicInteger(0);

		TestComplexBinding testObj = runtime.createObject(TestComplexBinding.class);
		TestComplexBinding testObj2 = null;

		ObjectProperty<TestComplexBinding> javaProp = new SimpleObjectProperty<>(testObj);
		javaProp.addListener((o, oldV, newV) -> {
			latch.countDown();
		});
		V8Object jsProp = v8.executeObjectScript("binding = new (require('util.binding').Binding)('jsInitValue');"
				+ "testObj = new (require('test.complex-binding'))();" + "binding;", "jsPropTest", 0);
		assertFalse(jsProp.isUndefined(), "Js Binding is undefined");
		BindingUtils.bindProperty(javaProp, jsProp, TestComplexBinding.class, runtime);
		long id = v8.executeIntegerScript("testObj._id");
		testObj2 = (TestComplexBinding) runtime.getObjectPool().getJavaObj(id);
		v8.executeScript("binding.value = testObj");

		latch.await(1000, TimeUnit.MILLISECONDS);
		assertEquals(0, latch.getCount(), "Not all listeners where called");
	}

	@AfterEach
	void teardown() throws IOException {
		runtime.close();
	}

	@JsObject("new (require('test.complex-binding'))()")
	private static class TestComplexBinding {
		private Logger logger = LogManager.getLogger();
		@JsId
		private long id;

		@JsBinding(type = TestComplexBinding.class)
		private ObjectProperty<TestComplexBinding> ref = new SimpleObjectProperty<>();

		public TestComplexBinding() {
		}

		@JsFunction
		private void printTest(String text) {
			logger.info("TEST: " + text);
		}

		public ObjectProperty<TestComplexBinding> refProperty() {
			return ref;
		}

		@Override
		public boolean equals(Object obj) {
			// TODO Auto-generated method stub
			return super.equals(obj);
		}

	}
}
