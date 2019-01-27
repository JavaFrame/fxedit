package ch.sebi.fxedit.ui.controller.window;

import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.javafx.stage.StageHelper;

import ch.sebi.fxedit.model.FXEditModel;
import ch.sebi.fxedit.model.ui.window.WindowModel;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

public class FXApplication extends Application  {
	private static Logger logger = LogManager.getLogger(FXApplication.class);
	public static final String 	MAIN_WINDOW_URI = "/ch/sebi/fxedit/ui/main.fxml";
	
	public static void startup(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage stage) throws Exception {
		try {
			initFXEditModel();
		} catch(Exception e) {
			logger.error("Couldn't start application", e);
			System.exit(1);
		}
		
		FXEditModel rootModel = FXEditModel.getFXEditModel();
		//creates window
		WindowModel window = WindowModel.createModel(rootModel.getRuntime());
		FXEditModel.getFXEditModel().getWindows().add(window);

		// runs rc.js
		try {
			rootModel.getRuntime().executeRC();
		} catch (Exception e) {
			logger.error("Cannot execute the rc file", e);
		}
	}
	
	/**
	 * initializes the {@link FXEditModel} and binds its property
	 */
	private static void initFXEditModel() {
		FXEditModel rootModel = FXEditModel.getFXEditModel();
		rootModel.getWindows().addListener((ListChangeListener<WindowModel>) c -> {
			while(c.next()) {
				if(c.wasAdded()) {
					c.getAddedSubList().forEach(FXApplication::createWindow);
				} 
				if(c.wasRemoved()) {
					c.getRemoved().forEach(WindowModel::close);
				}
			}
		});
	}
	
	
	/**
	 * Creates a new window from the given window model
	 * @param model the model the model
	 */
	private static void createWindow(WindowModel model) {
		try {
			FXWindowController controller = new FXWindowController(model);

			FXMLLoader loader = new FXMLLoader(FXApplication.class.getResource(MAIN_WINDOW_URI));
			loader.setController(controller);
			Parent parent = loader.load();
			Scene scene = new Scene(parent);
			Stage stage = new Stage();
			stage.setScene(scene);
			stage.show();
		} catch (IOException e) {
			throw new IllegalStateException("Could not load the fxml", e);
		}
	}
	
}
