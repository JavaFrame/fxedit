package ch.sebi.fxedit.model.ui;

import ch.sebi.fxedit.exception.FactoryNotFoundException;
import ch.sebi.fxedit.runtime.JsRuntime;
import ch.sebi.fxedit.runtime.reflection.annotation.JsId;
import ch.sebi.fxedit.runtime.reflection.annotation.JsObject;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * a model for a tab pane
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
	 * the buffers which are shown in this tab
	 */
	private ObservableList<BufferModel> buffers = FXCollections.observableArrayList();
	
	/**
	 * if this tab pane is visible
	 */
	private BooleanProperty visible = new SimpleBooleanProperty();
	
	/**
	 * constructor<br>
	 * use {@link #createTabModel(JsRuntime)} to create an instance
	 * 
	 * @see #createTabModel(JsRuntime)
	 */
	private TabModel() {
	}

	/**
	 * if the tab pane is visible or not
	 * @return if it is visible
	 */
	public boolean isVisible() {
		return visible.get();
	}
	
	/**
	 * Hides or shows the tab pane to which this model is bound
	 * @return the property
	 */
	public BooleanProperty visibleProperty() {
		return visible;
	}
	
	/**
	 * sets the tab model visible
	 * @param visible if it is visble or not
	 */
	public void setVisible(boolean visible)  {
		this.visible.set(visible);
	}
	
	/**
	 * Returns the buffer observable list
	 * @return the buffers on this tab pane
	 */
	public ObservableList<BufferModel> getBuffers() {
		return buffers;
	}
	
	/**
	 * Creates a new tab model in the java and the js space
	 * @param runtime the js runtime
	 * @return the tab model
	 */
	public static TabModel createTabModel(JsRuntime runtime) {
		try {
			return runtime.getObjectPool().createObject(TabModel.class);
		} catch (FactoryNotFoundException e) {
			throw new IllegalStateException("Could not create a TabModel object", e);
		}
	}
}
