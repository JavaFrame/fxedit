package ch.sebi.fxedit.shortcut;

import java.util.HashMap;

import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

public class ShortcutManager {
	private ObservableMap<String, ShortcutMode> modes = FXCollections.observableHashMap();
	
	public ShortcutManager() {
	}
	
	public void addMode(String id, ShortcutMode mode) {
		modes.put(id, mode);
	}
	
	public ShortcutMode getMode(String id) {
		return modes.get(id);
	}
	
	public ShortcutMode[] getModes() {
		return modes.entrySet().toArray(new ShortcutMode[modes.size()]);
	}
	
	public ObservableMap<String, ShortcutMode> modesProperty() {
		return modes;
	}
}
