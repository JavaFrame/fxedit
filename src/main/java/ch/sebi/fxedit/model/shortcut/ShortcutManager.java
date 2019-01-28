package ch.sebi.fxedit.model.shortcut;

import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.utils.V8ObjectUtils;

import ch.sebi.fxedit.exception.FactoryNotFoundException;
import ch.sebi.fxedit.exception.FailedObjectCreationException;
import ch.sebi.fxedit.runtime.JsRuntime;
import ch.sebi.fxedit.runtime.reflection.annotation.JsBinding;
import ch.sebi.fxedit.runtime.reflection.annotation.JsId;
import ch.sebi.fxedit.runtime.reflection.annotation.JsObject;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

@JsObject
public class ShortcutManager {
	public static final String FOUND_MATCH = "found match";
	public static final String NO_MATCH = "no match";
	public static final String POSSIBLE_MATCH = "possible match";


	private Logger logger = LogManager.getLogger();

	@JsId
	private long id;
	
	@JsBinding(type = Boolean.class)
	private BooleanProperty consumesEvents = new SimpleBooleanProperty();

	@JsBinding(type = String.class)
	private StringProperty modeName = new SimpleStringProperty();

	private JsRuntime runtime;

	private ShortcutManager(JsRuntime runtime) {
		this.runtime = runtime;
	}

	public String execute(String[] keys) {
		V8Object shortcutManagerJsObj = runtime.getObjectPool().getJsObj(id);
		V8 v8 = runtime.getV8();
		V8Array args = new V8Array(v8);
		try {
			args.push(V8ObjectUtils.toV8Array(v8, Arrays.asList(keys)));
			return shortcutManagerJsObj.executeStringFunction("execute", args);
		} catch(Exception e) {
			new Alert(AlertType.ERROR, "Couldn't execute shortcut: \n" + e).show();
			logger.error("Couldn't execute shortcut: ", e);
			return NO_MATCH;
		} finally {
			args.release();
		}
	}
	
	public boolean isConsumingEvents() {
		return consumesEvents.get();
	}
	
	public BooleanProperty consumesEventsProperty() {
		return consumesEvents;
	}
	
	public String getModeName() {
		return modeName.get();
	}
	
	public StringProperty modeNameProperty() {
		return modeName;
	}

	public static ShortcutManager createShortcutManager(JsRuntime runtime) {
		try {
			ShortcutManager manager = runtime.createObject(ShortcutManager.class);
			return manager;
		} catch (FactoryNotFoundException | FailedObjectCreationException e) {
			throw new IllegalStateException("Couldn't create new ShortcutManager", e);
		}
	}

	public static String getString(KeyEvent event) {
		StringBuffer buf = new StringBuffer();
		if (event.isShortcutDown())
			buf.append("C-");
		if (event.isAltDown())
			buf.append("A-");
		KeyCode code = event.getCode();
		if (code.isModifierKey()) {
			return code.getName();
		}
		if (code.isLetterKey()) {
			if (event.isShiftDown()) {
				buf.append(code.getName().toUpperCase());
			} else {
				buf.append(code.getName().toLowerCase());
			}
		} else {
			buf.append(code.getName());
		}

		return buf.toString();
	}
}
