package ch.sebi.fxedit.model.ui.editor;

import ch.sebi.fxedit.model.source.BufferSource;
import ch.sebi.fxedit.model.source.TextBufferSource;
import ch.sebi.fxedit.runtime.reflection.annotation.JsBinding;
import ch.sebi.fxedit.runtime.reflection.annotation.JsId;
import ch.sebi.fxedit.runtime.reflection.annotation.JsObject;
import ch.sebi.fxedit.ui.controller.editor.EditorControllerFactory;
import ch.sebi.fxedit.ui.controller.editor.text.TextEditorControllerFactory;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 * an abstract model of an editor
 * @author sebi
 *
 */
@JsObject
public class TextEditorModel implements EditorModel {
	/**
	 * js id
	 */
	@JsId
	private long id;

	/**
	 * the source property
	 */
	@JsBinding(type = TextBufferSource.class)
	private ObjectProperty<TextBufferSource> source = new SimpleObjectProperty<>();
	
	/**
	 * the factory to create texte editor views/controllers
	 */
	private TextEditorControllerFactory factory = new TextEditorControllerFactory(this);
	
	
	/**
	 * constructor
	 * @param bufferSourceClass the class  of the {@link BufferSource}
	 */
	private TextEditorModel() {
	}
	
	/**
	 * Returns the current source. If there is not source then this function
	 * returns null
	 * @return the current  source
	 */
	public TextBufferSource getSource() {
		return source.get();
	}
	
	/**
	 * returns the current source property.
	 * @return 
	 */
	public ObjectProperty<TextBufferSource> sourceProperty() {
		return source;
	}
	
	public void setSource(TextBufferSource source) {
		this.source.set(source);
	}

	@Override
	public EditorControllerFactory getFactory() {
		return factory;
	}
	
}
