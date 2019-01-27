package ch.sebi.fxedit.model.ui.editor;

import ch.sebi.fxedit.ui.controller.editor.EditorControllerFactory;

/**
 * An editor model
 * @author sebi
 *
 */
public interface EditorModel {
	/**
	 * The factory for this model
	 * @return the factory
	 */
	EditorControllerFactory getFactory();
}
