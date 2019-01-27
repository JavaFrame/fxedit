package ch.sebi.fxedit.model.source;

import ch.sebi.fxedit.runtime.reflection.annotation.JsFunction;

public interface TextBufferSource extends BufferSource {
	/**
	 * Returns the text portion from the range.
	 * @param from from where (inclusive)
	 * @param to to where (exclusive)
	 * @return the returned text
	 */
	@JsFunction
	String getText(int from, int to) throws Exception;
	
	/**
	 * Sets the text. It starts at the from position and writes to the position to.
	 * If the length of the given text is smaller than to, then the remaining characters
	 * will be deleted.
	 * @param from where the text should be inserted
	 * @param to until where text should be overriden. If nothing then use the same value as from
	 * @param text the text to inserted 
	 */
	@JsFunction
	void setText(int from, int to, String text) throws Exception;
	
	/**
	 * Saves the text. 
	 * @throws Exception
	 */
	@JsFunction
	void save() throws Exception;

	/**
	 * Returns the total length of the text source.
	 * @return the lenght of the text
	 * @throws Exception 
	 */
	@JsFunction
	int length() throws Exception;
	
	/**
	 * Returns true if the source is done loading. By default it returns true. 
	 * @return if the file is done loading
	 */
	@JsFunction
	default boolean doneLoading() {
		return true;
	}
	
	/**
	 * Returns if the source can be saved
	 * @return if the source is readonly
	 */
	@JsFunction
	default boolean isReadonly() {
		return false;
	}
	
	/**
	 * Returns all text.<br>
	 * <b>if the source is async (for example a network resource) then this
	 * will try to load every thing (until {@link #doneLoading()} returns true)</b>
	 * @return the entire file 
	 * @throws Exception
	 */
	@JsFunction(name = "getFullText")
	default String getText() throws Exception {
		StringBuffer buffer = new StringBuffer();
		int from = 0;
		int to = length();
		do {
			buffer.append(getText(from, to));
			from = to;
			to = length();
		} while(!doneLoading());
		return buffer.toString();
	}
	
	@JsFunction(name="setFullText")
	default void setText(String text) throws Exception {
		setText(0, length(), text);
	}
}
