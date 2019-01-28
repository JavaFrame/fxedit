package ch.sebi.fxedit.ui.controller.window;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.sebi.fxedit.model.FXEditModel;
import ch.sebi.fxedit.model.shortcut.ShortcutManager;
import ch.sebi.fxedit.model.ui.window.WindowModel;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.value.ChangeListener;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.util.Duration;

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
	
	@FXML
	private Label modeL;

	/**
	 * the model of this window controller
	 */
	private WindowModel model;

	/**
	 * the stage
	 */
	private Stage stage;

	private List<KeyEvent> unprocessedEvents = new ArrayList<>();

	private boolean reemittingEvents = false;

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

		// sets the stage property as soon as it is set
		rootPane.sceneProperty().addListener((o, oldV, newV) -> {
			if (newV != null) {
				if (newV.getWindow() != null) {
					setStage((Stage) newV.getWindow());
				} else {
					newV.windowProperty().addListener((windowO, oldWindow, newWindow) -> {
						if (newV != null) {
							setStage((Stage) newWindow);
						}
					});
				}
			}
		});

		// closes the window if the closed property changes
		model.closedProperty().addListener((o, oldV, newV) -> {
			stage.close();
		});
		rootPane.setCenter(new FXTabPaneController(model.getCenterTabs()).getTabPane());
		
		modeL.textProperty().bind(rootModel.getShortcutManager().modeNameProperty());

	}

	private void processEvents() {
		String[] keysArray = unprocessedEvents.stream().filter(e -> e.getEventType() == KeyEvent.KEY_RELEASED)
				.map(ShortcutManager::getString).toArray(String[]::new);
		ShortcutManager rootManager = FXEditModel.getFXEditModel().getShortcutManager();
		boolean consumingEvents = rootManager.isConsumingEvents();
		String result = rootManager.execute(keysArray);

		String status = unprocessedEvents.stream().filter(e -> e.getEventType() == KeyEvent.KEY_RELEASED)
				.map(ShortcutManager::getString).collect(Collectors.joining(" "));
		if (!status.isEmpty())
			typedL.setText(status + " [" + result.toUpperCase() + "]");
		

		if (result.equals(ShortcutManager.NO_MATCH)) {
			if (!consumingEvents) {
				reemittingEvents = true;
				for (KeyEvent event : unprocessedEvents) {
					((Node) event.getTarget()).fireEvent(event);
				}
				reemittingEvents = false;
			}
			unprocessedEvents.clear();
		}
		if (result.equals(ShortcutManager.FOUND_MATCH)) {
			unprocessedEvents.clear();
		}
	}

	/**
	 * Sets the stage and initializes whats dependend on the stage
	 * 
	 * @param stage the stage
	 */
	private void setStage(Stage stage) {
		this.stage = stage;
		loadTheme(FXEditModel.getFXEditModel().getStylePath());

		stage.titleProperty().unbind();
		stage.titleProperty().bind(model.titleProperty());

		ShortcutManager rootManager = FXEditModel.getFXEditModel().getShortcutManager();
		stage.addEventFilter(KeyEvent.ANY, event -> {
			if (!reemittingEvents) {
				unprocessedEvents.add((KeyEvent) event.clone());
				processEvents();
				event.consume();
			}
		});
		stage.focusedProperty().addListener((ChangeListener<Boolean>) (observable, oldValue, newValue) -> {
			if(newValue) {
				FXEditModel.getFXEditModel().setCurrentWindow(model);
			}
		});
	}

	/**
	 * loads a theme from the given path
	 * 
	 * @param path the theme
	 */
	private void loadTheme(String path) {
		File themeFile = new File(path);
		logger.info("loading theme \"" + themeFile.toURI().toString() + "\"");
		stage.getScene().getStylesheets().add(themeFile.toURI().toString());
	}

	@FXML
	public void exitApplication() {
		// syncs the model closed state
		model.close();
	}

	/**
	 * Returns the window model used in this controller
	 * 
	 * @return
	 */
	public WindowModel getModel() {
		return model;
	}

}
