package ch.sebi.fxedit.model.ui;

import ch.sebi.fxedit.model.source.BufferSource;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 * an abstract model of an editor
 * @author sebi
 *
 */
public abstract class EditorModel<T extends BufferSource> {
	/**
	 * the source property
	 */
	private ObjectProperty<T> source = new SimpleObjectProperty<>();
	/**
	 * the class of the source of the editor
	 */
	private Class<T> bufferSourceClass;
	
	
	/**
	 * constructor
	 * @param bufferSourceClass the class  of the {@link BufferSource}
	 */
	public EditorModel(Class<T> bufferSourceClass) {
		this.bufferSourceClass = bufferSourceClass;
	}
	
	/**
	 * Returns the current source. If there is not source then this function
	 * returns null
	 * @return the current  source
	 */
	public T getSource() {
		return source.get();
	}
	
	/**
	 * returns the current source property.
	 * @return 
	 */
	public ObjectProperty<T> sourceProperty() {
		return source;
	}
	
	public void setSource(T source) {
		this.source.set(source);
	}
	
	public Class<T> getBufferSourceClass() {
		return bufferSourceClass;
	}
}
