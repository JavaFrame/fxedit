package ch.sebi.fxedit.shortcut;

import java.util.HashMap;
import java.util.concurrent.Callable;

import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.event.Event;
import javafx.event.EventHandler;

public class ShortcutMode {
	private ObservableMap<Shortcut, EventHandler<Event>> shortcuts = FXCollections.observableHashMap();
	
	public ShortcutMode() {
		
	}
	
	public void add(Shortcut shortcut, EventHandler<Event> listener) {
		shortcuts.put(shortcut, listener);
	}

	public void remove(Shortcut shortcut) {
		shortcuts.remove(shortcut);
	}
	
	public EventHandler<Event> get(Shortcut shortcut) {
		return shortcuts.get(shortcut);
	}
	
	public void execute(Shortcut shortcut, Event event) {
		EventHandler<Event> handler = get(shortcut);
		handler.handle(event);
	}
	
	public ObservableMap<Shortcut, EventHandler<Event>> shortcutsProperty() {
		return shortcuts;
	}
}
