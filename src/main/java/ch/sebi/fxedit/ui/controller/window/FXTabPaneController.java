package ch.sebi.fxedit.ui.controller.window;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.sebi.fxedit.exception.EditorLoadException;
import ch.sebi.fxedit.model.FXEditModel;
import ch.sebi.fxedit.model.ui.editor.EditorModel;
import ch.sebi.fxedit.model.ui.window.TabModel;
import ch.sebi.fxedit.model.ui.window.TabPaneModel;
import ch.sebi.fxedit.model.ui.window.WindowModel;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

public class FXTabPaneController {
	private Logger logger = LogManager.getLogger();

	private final static DataFormat NODE_DATA_FORMAT = new DataFormat("node");
	private static TabModel draggedTabModel = null;
	private static Tab draggedTab = null;
	private static FXTabPaneController draggedTabController = null;
	private TabPane tabPane;

	private TabPaneModel model;

	private Map<TabModel, Tab> tabMap = new HashMap<>();

	public FXTabPaneController(TabPaneModel model) {
		this.model = model;
		this.tabPane = new TabPane();
		initBindings();

		tabPane.setOnDragOver(event -> {
			if (draggedTabModel != null && draggedTabController != this) {
				event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
				event.consume();
			} else {
			}
		});
		tabPane.setOnDragDropped(event -> {
			if (draggedTabModel != null && draggedTabController != this) {
				draggedTabController.getModel().getTabs().remove(draggedTabModel);
				model.getTabs().add(draggedTabModel);
				draggedTabModel = null;
				draggedTabController = null;
				event.setDropCompleted(true);
				event.consume();
			}
		});
		tabPane.setOnDragEntered(event -> {
			tabPane.setStyle("-fx-background: red");
		});
		tabPane.setOnDragExited(event -> {
			tabPane.setStyle("-fx-background: white");
		});
		tabPane.getSelectionModel().selectedIndexProperty().addListener((ChangeListener<Number>) (observable, oldValue, newValue) -> {
			int index = newValue.intValue();
			Tab tab = tabPane.getTabs().get(index);
			TabModel tabModel = null;
			// search tab model for selected tab
			for(TabModel key : tabMap.keySet()) {
				if(tab == tabMap.get(key)) {
					tabModel = key;
				}
			}

			if(tabModel != null)
				FXEditModel.getFXEditModel().setCurrentTab(tabModel);
		});
	}

	/**
	 * initializes the listeners for a tab to drag and drop
	 * @param t the tab
	 * @param model the tab model
	 */
	private void initListeners(Tab t, TabModel model) {
		System.out.println("added init listener to " + t.getText());
		Node n = t.getGraphic();
		n.setOnDragDetected(event -> {
			Dragboard db = n.startDragAndDrop(TransferMode.ANY);

			ClipboardContent content = new ClipboardContent();
			content.putString("test");
			db.setContent(content);

			draggedTabController = this;
			draggedTabModel = model;
			draggedTab = t;

			event.consume();

		});
		n.setOnDragDone(e -> {
			if (!e.isAccepted()) {
				if (draggedTabModel == null || draggedTabController == null || draggedTab == null) {
					logger.warn("draggedTabModel, draggedTabController or draggedTab are null");
					return;
				}
				FXEditModel root = FXEditModel.getFXEditModel();
				WindowModel window = WindowModel.createModel(root.getRuntime());
				root.getWindows().add(window);
				window.getCenterTabs().getTabs().add(draggedTabModel);
				draggedTabController.getModel().getTabs().remove(draggedTabModel);
				draggedTabModel = null;
				draggedTabController = null;
				draggedTab = null;
			}
		});
	}

	/**
	 * binds the model to this controller
	 */
	private void initBindings() {
		model.getTabs().addListener((ListChangeListener<TabModel>) c -> {
			while (c.next()) {
				if (c.wasAdded()) {
					for (int i = c.getFrom(); i < c.getTo(); i++) {
						createTab(c.getList().get(i), i);
					}
				}
				if (c.wasRemoved()) {
					for (TabModel tab : c.getRemoved()) {
						if(!tabPane.getTabs().remove(tabMap.get(tab))) {
							logger.warn("Tab dosn't exist in this TabPane");
						}
						tabMap.remove(tab);
					}
				}
			}
		});
	}

	/**
	 * Creates a new tab. If the {@link #draggedTabModel} is the same object
	 * @param tabModel
	 * @param index
	 */
	private void createTab(TabModel tabModel, int index) {
		if (draggedTabModel == tabModel) {
			tabPane.getTabs().add(draggedTab);
			tabPane.getSelectionModel().select(draggedTab);
			tabMap.put(tabModel, draggedTab);
			initListeners(draggedTab, tabModel);
			return;
		}
		Tab tab = new Tab();
		tab.setOnClosed(e -> {
			model.getTabs().remove(tabModel);
			e.consume();
		});
		Label tabLabel = new Label();
		tabLabel.textProperty().bind(tabModel.titleProperty());
		tab.setGraphic(tabLabel);

		tabModel.editorProperty().addListener((o, oldV, newV) -> loadContent(tab, newV));
		loadContent(tab, tabModel.getEditor());

		initListeners(tab, tabModel);
		tabMap.put(tabModel, tab);
		tabPane.getTabs().add(index, tab);
		tabPane.getSelectionModel().select(tab);
	}

	private void loadContent(Tab tab, EditorModel editorModel) {
		if (editorModel == null)
			return;
		try {
			Node view = editorModel.getFactory().createView();
			tab.setContent(view);
		} catch (EditorLoadException e) {
			logger.error("Couldn't create an editor view for the model \"" + editorModel.getClass().getName() + "\"",
					e);
		}
	}

	/**
	 * Returns the tabpane model
	 * 
	 * @return the model
	 */
	public TabPaneModel getModel() {
		return model;
	}

	/**
	 * Returns the tabpane component
	 * 
	 * @return the tabpane
	 */
	public TabPane getTabPane() {
		return tabPane;
	}
}
