package ch.sebi.fxedit.ui.controller.editor.text;

import java.net.URL;
import java.util.ResourceBundle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.sebi.fxedit.model.source.TextBufferSource;
import ch.sebi.fxedit.model.ui.editor.TextEditorModel;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextArea;

public class TextEditorController implements Initializable {
	/**
	 * logger
	 */
	private Logger logger = LogManager.getLogger();

	@FXML
	private TextArea textarea;
	
	private TextEditorModel model;
	
	public TextEditorController(TextEditorModel model) {
		this.model = model;
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		model.sourceProperty().addListener((o, oldV, newV) -> setSource(newV));
	}

	private void setSource(TextBufferSource newV) {
		try {
			String text = newV.getText();
			textarea.setText(text);
		} catch (Exception e) {
			logger.error("Couldn't load text from the source \"" + newV.getClass().getName() + "\"", e);
		}
	}

}
