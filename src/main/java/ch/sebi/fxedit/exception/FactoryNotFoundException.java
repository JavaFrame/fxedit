package ch.sebi.fxedit.exception;


/**
 * A exception which is thrown if a {@link LibFactory} wasn't found by the {@link LibFactoryManager}
 * @author sebi
 *
 */
public class FactoryNotFoundException extends Exception {

	/**
	 * constructor
	 * @param id the id of the {@link LibFactory} which wasn't found
	 */
	public FactoryNotFoundException(String id) {
		super("Factory \"" + id + "\" not found");
	}

	/**
	 * constructor
	 * @param id the id of the {@link LibFactory} which wasn't found
	 * @param cause the cause of this exception
	 */
	public FactoryNotFoundException(String id, Throwable cause) {
		super("Factory \"" + id + "\" not found", cause);
	}
}
