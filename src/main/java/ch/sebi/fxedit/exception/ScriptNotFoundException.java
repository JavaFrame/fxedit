package ch.sebi.fxedit.exception;

import java.io.IOException;
import java.util.Arrays;

/**
 * A script could not be found
 * @author sebi
 *
 */
public class ScriptNotFoundException extends IOException {
	/**
	 * constructor
	 * @param scriptPath the script path for the script which wasn't found
	 * @param requirePaths the requirePaths which where used
	 */
	public ScriptNotFoundException(String scriptPath, String[] requirePaths) {
		super("Could not find script \"" + scriptPath + "\" in requirePath \"" + Arrays.toString(requirePaths) + "\"");
	}
}
