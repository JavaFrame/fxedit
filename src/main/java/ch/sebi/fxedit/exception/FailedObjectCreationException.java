package ch.sebi.fxedit.exception;

/**
 * This exception is used to indicate that an js/java object couldn't be
 * created, because of an error
 * 
 * @author sebi
 *
 */
public class FailedObjectCreationException extends Exception {

	/**
	 * constructor
	 * @param message the message
	 * @param cause the cause
	 */
	public FailedObjectCreationException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * constructor
	 * @param message the message
	 */
	public FailedObjectCreationException(String message) {
		super(message);
	}

}
