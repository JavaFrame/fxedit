package ch.sebi.fxedit.model.ui.window;

import ch.sebi.fxedit.exception.FactoryNotFoundException;
import ch.sebi.fxedit.exception.FailedObjectCreationException;
import ch.sebi.fxedit.model.shortcut.ShortcutManager;
import ch.sebi.fxedit.model.ui.editor.TextEditorModel;
import ch.sebi.fxedit.runtime.JsRuntime;
import ch.sebi.fxedit.runtime.reflection.annotation.JsBinding;
import ch.sebi.fxedit.runtime.reflection.annotation.JsId;
import ch.sebi.fxedit.runtime.reflection.annotation.JsObject;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * A model for a buffer
 * 
 * @author sebi
 *
 */
@JsObject
public class TabModel {
	/**
	 * the js id
	 */
	@JsId
	private long id;
	
	/**
	 * the title of the tab
	 */
	@JsBinding(type = String.class)
	private StringProperty title = new SimpleStringProperty();

	/**
	 * the shortcut manager for the buffer
	 */
	@JsBinding(type = ShortcutManager.class)
	private ObjectProperty<ShortcutManager> shortcuts = new SimpleObjectProperty<>();

	/**
	 * the current editor of this buffer
	 */
	@JsBinding(type = TextEditorModel.class)
	private ObjectProperty<TextEditorModel> editor = new SimpleObjectProperty();

	private TabModel(JsRuntime runtime) throws FactoryNotFoundException, FailedObjectCreationException {
		shortcuts.set(runtime.createObject(ShortcutManager.class));
	}
	
	/**
	* Returns title
	* @return the title
	*/
	public String getTitle() {
		return title.get();
	}

	/**
	* Returns property of title
	* @return the title
	*/
	public StringProperty titleProperty() {
		return title;
	}

	/**
	* Sets the value of title 
	* @param value the new value
	*/
	public void setTitle(String value) {
		title.set(value);
	}

	/**
	 * Returns editor
	 * 
	 * @return the editor
	 */
	public TextEditorModel getEditor() {
		return editor.get();
	}

	/**
	 * Returns property of editor
	 * 
	 * @return the editor
	 */
	public ObjectProperty<TextEditorModel> editorProperty() {
		return editor;
	}

	/**
	 * Returns the shortcut manager
	 * 
	 * @return the manager
	 */
	public ShortcutManager getShortcuts() {
		return shortcuts.get();
	}

	/**
	 * Sets the value of editor
	 * 
	 * @param value the new value
	 */
	public void setEditor(TextEditorModel value) {
		editor.set(value);
	}

	public static TabModel createTabModel(JsRuntime runtime) {
		try {
			return runtime.createObject(TabModel.class);
		} catch (FactoryNotFoundException | FailedObjectCreationException e) {
			throw new IllegalStateException("Couldn't create a TabModel", e);
		}
	}
}
