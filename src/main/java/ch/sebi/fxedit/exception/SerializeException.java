package ch.sebi.fxedit.exception;

/**
 * A class which is thrown if the serialization of a object failed
 * 
 * @author sebi
 *
 */
public class SerializeException extends Exception {

	/**
	 * constructor
	 * 
	 * @param message the message
	 * @param cause   the cause
	 */
	public SerializeException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * constructor
	 * 
	 * @param message the message
	 */
	public SerializeException(String message) {
		super(message);
	}

}
