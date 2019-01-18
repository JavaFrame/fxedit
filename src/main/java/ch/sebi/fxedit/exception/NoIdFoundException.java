package ch.sebi.fxedit.exception;

/**
 * An exception for when no _id was found on a js object
 * @author sebi
 *
 */
public class NoIdFoundException extends Exception {

	public NoIdFoundException() {
		super("No _id property was found on js object");
	}
	/**
	 * constructor
	 * @param message the message
	 */
	public NoIdFoundException(String message) {
		super(message);
	}
	
}
