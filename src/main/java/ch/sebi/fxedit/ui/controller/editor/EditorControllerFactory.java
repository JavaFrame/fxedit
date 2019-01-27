package ch.sebi.fxedit.ui.controller.editor;

import java.io.IOException;

import ch.sebi.fxedit.exception.EditorLoadException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;

/**
 * A factory for creating new editor views
 * @author sebi
 *
 */
public interface EditorControllerFactory {
	/**
	 * Creates a new editor view
	 * @return the view/node
	 * @throws EditorLoadException
	 */
	Node createView() throws EditorLoadException;

	/**
	 * Loads the fxml with the given path and sets the controller
	 * @param path the path of the fxml
	 * @param controller the controller
	 * @return the loaded node
	 * @throws EditorLoadException 
	 */
	default Node loadFXML(String path, Object controller) throws EditorLoadException {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
			loader.setController(controller);
			return loader.load();
		} catch (IOException e) {
			throw new EditorLoadException("Couldn't load fxml", e);
		}
	}
}
