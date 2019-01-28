package ch.sebi.fxedit.model;

import ch.sebi.fxedit.exception.FactoryNotFoundException;
import ch.sebi.fxedit.exception.FailedObjectCreationException;
import ch.sebi.fxedit.model.shortcut.ShortcutManager;
import ch.sebi.fxedit.model.ui.window.TabModel;
import ch.sebi.fxedit.model.ui.window.WindowModel;
import ch.sebi.fxedit.runtime.JsRuntime;
import ch.sebi.fxedit.runtime.reflection.annotation.JsBinding;
import ch.sebi.fxedit.runtime.reflection.annotation.JsFunction;
import ch.sebi.fxedit.runtime.reflection.annotation.JsId;
import ch.sebi.fxedit.runtime.reflection.annotation.JsObject;
import javafx.application.Application;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * The root model
 * @author sebi
 *
 */
@JsObject("require('window.root')")
public class FXEditModel {
	/**
	 * the js id
	 */
	@JsId
	private long id;

	/**
	 * all windows of the fx editor
	 */
	@JsBinding(type = WindowModel.class)
	private ObservableList<WindowModel> windows = FXCollections.observableArrayList();
	
	/**
	 * the currently selected window or null
	 */
	@JsBinding(type = WindowModel.class)
	private ObjectProperty<WindowModel> currentWindow = new SimpleObjectProperty<>();

	/**
	 * the currently selected  tab or null
	 */
	@JsBinding(type = TabModel.class)
	private ObjectProperty<TabModel> currentTab = new SimpleObjectProperty<>();

	/**
	 * the path to the user agent style
	 */
	@JsBinding(type = String.class)
	private StringProperty stylePath = new SimpleStringProperty("./themes/dark.theme");
	
	/**
	 * the javascript runtime
	 */
	private JsRuntime runtime;
	
	/**
	 * the root manager.
	 * It is only accessible through the function #getShortcutManager()
	 */
	private ShortcutManager rootManager;
	
	/**
	 * constructor
	 */
	private FXEditModel(JsRuntime runtime) {
		if(this.runtime != null) {
			throw new IllegalStateException("FXEditModel is already initalized");
		}
		this.runtime = runtime;
		rootManager = ShortcutManager.createShortcutManager(runtime);
	}
	
	/**
	 * Returns the window observable list. 
	 * All changes to that list are represented in real windows.
	 * @return all window model instances
	 */
	public ObservableList<WindowModel> getWindows() {
		return windows;
	}
	
	public WindowModel getCurrentWindow() {
		return currentWindow.get();
	}
	
	public ObjectProperty<WindowModel> currentWindowProperty() {
		return currentWindow;
	}
	
	public void setCurrentWindow(WindowModel model) {
		currentWindow.set(model);
	}

	public TabModel getCurrentTab() {
		return currentTab.get();
	}
	
	public ObjectProperty<TabModel> currentTabProperty() {
		return currentTab;
	}
	
	public void setCurrentTab(TabModel model) {
		currentTab.set(model);
	}
	
	/**
	 * Returns the path to the current style
	 * @return the current style path
	 * @see Application#setUserAgentStylesheet(String)
	 */
	public String getStylePath() {
		return stylePath.get();
	}
	
	/**
	 * Returns the property for the current style
	 * @return the poperty
	 * @see Application#setUserAgentStylesheet(String)
	 */
	public StringProperty stylePathProperty() {
		return stylePath;
	}
	
	/**
	 * Sets the path to the current style
	 * @param path the path
	 * @see Application#setUserAgentStylesheet(String)
	 */
	public void setStylePath(String path) {
		stylePath.set(path);
	}
	
	/**
	 * Returns the js runtime of this application
	 * @return the js runtime
	 */
	public JsRuntime getRuntime() {
		return runtime;
	}
	

	/**
	 * Returns the root/global shortcut manager
	 * @return the root/global shortcut manager
	 */
	@JsFunction
	public ShortcutManager getShortcutManager() {
		return rootManager;
	}
	
	/**
	 * singelton instance
	 */
	private static FXEditModel instance;
	
	/**
	 * Returns the single {@link FXEditModel} in existencee.
	 * @return
	 */
	public static FXEditModel getFXEditModel() {
		if(instance == null) {
			JsRuntime runtime = new JsRuntime();
			try {
				instance = runtime.createObject(FXEditModel.class);
			} catch (FactoryNotFoundException | FailedObjectCreationException e) {
				throw new IllegalStateException("Cannot create a FXEditModel instance", e);
			}
		}
		return instance;
	}
}
