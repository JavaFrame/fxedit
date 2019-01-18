package ch.sebi.fxedit.runtime.lib.test;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.sebi.fxedit.runtime.reflection.annotation.JsBinding;
import ch.sebi.fxedit.runtime.reflection.annotation.JsFunction;
import ch.sebi.fxedit.runtime.reflection.annotation.JsId;
import ch.sebi.fxedit.runtime.reflection.annotation.JsObject;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

@JsObject
public class TestLib {
	private Logger logger = LogManager.getLogger(getClass());
	@JsId
	private long id;

	@JsBinding(type = String.class)
	private StringProperty testStr = new SimpleStringProperty("test");
	
	public TestLib() {
		testStr.addListener((o, oldV, newV) -> {
			logger.info("changed testStr " + oldV + " -> " + newV);
		});
	}
	
	public String getTestStr() {
		return testStr.get();
	}
	
	public StringProperty testStrProperty() {
		return testStr;
	}

	@JsFunction
	public void sayHelloWorld() {
		System.out.println("hello world");
		testStr.set("hello evening");
		testStr.set("hello evening 2");
	}

}
