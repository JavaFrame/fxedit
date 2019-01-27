package ch.sebi.fxedit.ui.controller.editor.text;

import ch.sebi.fxedit.exception.EditorLoadException;
import ch.sebi.fxedit.model.ui.editor.TextEditorModel;
import ch.sebi.fxedit.ui.controller.editor.EditorControllerFactory;
import javafx.scene.Node;

/**
 * An factory implementation for the {@link TextEditorController}
 * @author sebi
 *
 */
public class TextEditorControllerFactory implements EditorControllerFactory {
	/**
	 * the model
	 */
	private TextEditorModel model;
	
	/**
	 * constructor
	 * @param model the text editor model
	 */
	public TextEditorControllerFactory(TextEditorModel model) {
		this.model = model;
	}
	
	@Override
	public Node createView() throws EditorLoadException {
		return loadFXML("/ch/sebi/fxedit/ui/editor/texteditor.fxml", new TextEditorController(model));
	}
	
}
