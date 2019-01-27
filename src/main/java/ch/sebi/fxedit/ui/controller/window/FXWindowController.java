package ch.sebi.fxedit.ui.controller.window;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.ResourceBundle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.sebi.fxedit.model.FXEditModel;
import ch.sebi.fxedit.model.ui.window.WindowModel;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class FXWindowController implements Initializable {
	private Logger logger = LogManager.getLogger();
	/**
	 * the root pane where the tabPanes are
	 */
	@FXML
	private BorderPane rootPane;

	/**
	 * the menubar of this window
	 */
	@FXML
	private MenuBar menuBar;
	
	@FXML
	private Label typedL;

	/**
	 * the model of this window controller
	 */
	private WindowModel model;
	
	/**
	 * the stage
	 */
	private Stage stage;

	/**
	 * constructor<br>
	 * Don't use this constructor. Add a {@link WindowModel} to the
	 * {@link FXEditModel#getWindows()} to create a new window
	 * 
	 * @param model the model of this window controller
	 */
	protected FXWindowController(WindowModel model) {
		this.model = model;
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		FXEditModel rootModel = FXEditModel.getFXEditModel();
		//sets the stage property as soon as it is set
		rootPane.sceneProperty().addListener((o, oldV, newV) -> {
			if(newV != null) {
				if(newV.getWindow() != null) {
					setStage((Stage) newV.getWindow());
				} else {
					newV.windowProperty().addListener((windowO, oldWindow, newWindow) -> {
						if(newV != null) {
							setStage((Stage) newWindow);
						}
					});
				}
			}
		});
		rootPane.addEventFilter(KeyEvent.ANY, event -> {
			rootModel.getShortcutExecutor().addKeyEvent(event);
		});

		//closes the window if the closed property changes
		model.closedProperty().addListener((o, oldV, newV) -> stage.close());
		rootPane.setCenter(new FXTabPaneController(model.getCenterTabs()).getTabPane());
		
		typedL.textProperty().bind(rootModel.getShortcutExecutor().pressedKeysStringBinding());

	}
	
	/**
	 * Sets the stage and initializes whats dependend on the stage
	 * @param stage the stage
	 */
	private void setStage(Stage stage) {
		this.stage = stage;
		loadTheme(FXEditModel.getFXEditModel().getStylePath());
		
		stage.titleProperty().unbind();
		stage.titleProperty().bind(model.titleProperty());
	}
	

	/**
	 * loads a theme from the given path
	 * @param path the theme
	 */
	private void loadTheme(String path) {
		File themeFile = new File(path);
		logger.info("loading theme \"" + themeFile.toURI().toString() + "\"");
		stage.getScene().getStylesheets().add(themeFile.toURI().toString());
	}
	
	
	@FXML
	public void exitApplication() {
		//syncs the model closed state
		model.close();
	}
	
	/**
	 * Returns the window model used in this controller
	 * @return
	 */
	public WindowModel getModel() {
		return model;
	}

}
