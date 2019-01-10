package ch.sebi.fxedit.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class FXApplication extends Application {


	@Override
	public void start(Stage stage) throws Exception {
		Parent parent = FXMLLoader.load(getClass().getResource("main.fxml"));
		Scene scene = new Scene(parent, 800, 800);
		
		stage.setTitle("FXEdit");
		stage.setScene(scene);
		stage.show();
	}

}
