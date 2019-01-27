package ch.sebi.fxedit.model.shortcut;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import ch.sebi.fxedit.model.FXEditModel;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventType;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class ShortcutExecutor {
	private ObservableList<String> pressedKeys = FXCollections.observableArrayList();
	private StringBinding pressedKeyStringBinding;

	public ShortcutExecutor() {
		pressedKeys.addListener((ListChangeListener<String>) c -> refresh());
		pressedKeyStringBinding = Bindings.createStringBinding(() -> pressedKeys.stream().collect(Collectors.joining(" ")),
				pressedKeys);
	}

	private boolean refresh() {
		ShortcutManager globalManager = FXEditModel.getFXEditModel().getShortcutManager();
		String[] pressedKeysArr = pressedKeys.toArray(new String[pressedKeys.size()]);

		if (globalManager.execute(pressedKeysArr)) {
			clear();
			return true;
		}
		return false;
	}

	public void addKeyEvent(KeyEvent event) {
		if (event.getEventType() != KeyEvent.KEY_PRESSED) {
			return;
		}
		KeyCode code = event.getCode();
		if(code.isModifierKey()) return;
		pressedKeys.add(getString(event));
		event.consume();
	}

	public void clear() {
		pressedKeys.clear();
	}

	public ObservableList<String> getPressedKeys() {
		return pressedKeys;
	}

	public String getPressedKeysString() {
		return pressedKeyStringBinding.get();
	}

	public StringBinding pressedKeysStringBinding() {
		return pressedKeyStringBinding;
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
