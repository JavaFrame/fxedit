package ch.sebi.fxedit.model.ui;

import ch.sebi.fxedit.shortcut.ShortcutManager;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 * A model for a buffer
 * @author sebi
 *
 */
public class BufferModel {
	/**
	 * the shortcut manager for the buffer
	 */
	private ShortcutManager shortcuts = new ShortcutManager();
	
	/**
	 * the current editor of this buffer 
	 */
	private ObjectProperty<EditorModel> editor = new SimpleObjectProperty<EditorModel>();
	
}
