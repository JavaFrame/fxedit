package ch.sebi.fxedit.model.source;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.sebi.fxedit.exception.FactoryNotFoundException;
import ch.sebi.fxedit.exception.FailedObjectCreationException;
import ch.sebi.fxedit.runtime.JsRuntime;
import ch.sebi.fxedit.runtime.reflection.annotation.JsConstructor;
import ch.sebi.fxedit.runtime.reflection.annotation.JsId;
import ch.sebi.fxedit.runtime.reflection.annotation.JsObject;
import ch.sebi.fxedit.runtime.reflection.annotation.JsVar;

@JsObject
public class FileTextBufferSource implements TextBufferSource {
	private Logger logger = LogManager.getLogger();

	
	@JsId
	private long id;

	@JsVar
	private String path;

	/**
	 * the content
	 */
	private String content = null;

	@JsConstructor
	private FileTextBufferSource(String path) {
		this.path = path;
	}

	private void loadContent() throws IOException {
		File file = new File(path);
		if(!file.exists()) file.createNewFile();
		BufferedReader reader = new BufferedReader(new FileReader(file));
		content = "";
		try {
			String input;
			while ((input = reader.readLine()) != null) {
				content += input + "\n";
			}
		} finally {
			reader.close();
		}
	}

	@Override
	public String getText(int from, int to) throws Exception {
		if (content == null)
			loadContent();
		return content.substring(from, to);
	}

	@Override
	public int length() throws Exception {
		if (content == null)
			loadContent();
		return content.length();
	}

	@Override
	public void setText(int from, int to, String text) throws Exception {
		String startStr = content.substring(0, from);
		String endStr = content.substring(to, length());
		content = startStr + text + endStr;
	}

	@Override
	public void save() throws Exception {
		logger.info("Saved content to file \"" + path + "\"");
		File file = new File(path);
		if (!file.exists()) {
			file.createNewFile();
		}

		PrintWriter writter = new PrintWriter(file);
		try {
			writter.println(content);
			writter.flush();
		} finally {
			writter.close();
		}
	}

	public static FileTextBufferSource createSource(String path, JsRuntime runtime) {
		try {
			return runtime.createObject(FileTextBufferSource.class, path);
		} catch (FactoryNotFoundException | FailedObjectCreationException e) {
			throw new IllegalStateException("Unable to create a new WindowModel", e);
		}
	}
}
