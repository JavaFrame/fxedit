package ch.sebi.fxedit.model.ui.window;

import ch.sebi.fxedit.exception.FactoryNotFoundException;
import ch.sebi.fxedit.exception.FailedObjectCreationException;
import ch.sebi.fxedit.runtime.JsRuntime;
import ch.sebi.fxedit.runtime.reflection.ObjectPool;
import ch.sebi.fxedit.runtime.reflection.annotation.JsBinding;
import ch.sebi.fxedit.runtime.reflection.annotation.JsFunction;
import ch.sebi.fxedit.runtime.reflection.annotation.JsId;
import ch.sebi.fxedit.runtime.reflection.annotation.JsObject;
import ch.sebi.fxedit.runtime.reflection.annotation.JsVar;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * The model for a window
 * @author sebi
 *
 */
@JsObject
public class WindowModel {
	/**
	 * the js id
	 */
	@JsId
	private long id;
	
	/**
	 * the title of the window
	 */
	@JsBinding(type = String.class)
	private StringProperty title = new SimpleStringProperty("FXEdit");

	/**
	 * the closed property
	 */
	@JsBinding(type = Boolean.class)
	private ReadOnlyBooleanWrapper closed = new ReadOnlyBooleanWrapper(false);
	
	/**
	 * the center pane tabs
	 */
	@JsBinding(type = TabPaneModel.class)
	private ReadOnlyObjectWrapper<TabPaneModel> centerTabs = new ReadOnlyObjectWrapper<>();
	
	
	/**
	 * constructor
	 */
	private WindowModel(JsRuntime runtime) {
		centerTabs.set(TabPaneModel.createTabPaneModel(runtime));
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
	 * Returns if the window is open or closed
	 * @return if the window is closed
	 */
	public boolean isClosed() {
		return closed.get();
	}
	
	/**
	 * returns the property if the window is closed.
	 * use {@link #close()} to close the window
	 * @return the closed property
	 */
	public ReadOnlyBooleanProperty closedProperty() {
		return closed.getReadOnlyProperty();
	}
	
	/**
	 * closes the window
	 */
	@JsFunction
	public void close() {
		if(!isClosed()) {
			closed.set(true);
		}
	}
	
	/**
	 * Returns the center tab pane model
	 * @return the tab pane model for the center
	 */
	public TabPaneModel getCenterTabs() {
		return centerTabs.get();
	}
	
	
	/**
	 * convenient function for {@link ObjectPool#createObject(Class)}
	 * @param runtime the js runtime to use
	 * @return the created window model
	 */
	public static WindowModel createModel(JsRuntime runtime) {
		try {
			return runtime.createObject(WindowModel.class);
		} catch (FactoryNotFoundException | FailedObjectCreationException e) {
			throw new IllegalStateException("Unable to create a new WindowModel", e);
		}
	}

}
