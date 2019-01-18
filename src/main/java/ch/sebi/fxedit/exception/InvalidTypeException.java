package ch.sebi.fxedit.exception;

/**
 * This exception is thrown if the type isn't valid
 * @author sebi
 *
 */
public class InvalidTypeException extends Exception {

	/**
	 * constructor
	 * @param msg the message
	 * @param cause the cause 
	 */
	public InvalidTypeException(String msg, Throwable cause) {
		super(msg, cause);
	}

	/**
	 * constructor
	 * @param msg the message
	 */
	public InvalidTypeException(String message) {
		super(message);
	}

}
