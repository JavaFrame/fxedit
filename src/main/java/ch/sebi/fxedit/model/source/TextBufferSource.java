package ch.sebi.fxedit.model.source;

public interface TextBufferSource extends BufferSource {
	/**
	 * Returns the text portion from the range.
	 * @param from from where (inclusive)
	 * @param to to where (exclusive)
	 * @return the returned text
	 */
	String getText(int from, int to) throws Exception;

	/**
	 * Returns the total length of the text source.
	 * @return
	 */
	int length();
	
	/**
	 * Returns true if the source is done loading. By default it returns true. 
	 * @return if the file is done loading
	 */
	default boolean doneLoading() {
		return true;
	}
	
	/**
	 * Returns all text.<br>
	 * <b>if the source is async (for example a network resource) then this
	 * will try to load every thing (until {@link #doneLoading()} returns true)</b>
	 * @return the entire file 
	 * @throws Exception
	 */
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
}
