package ch.sebi.fxedit;

import java.io.IOException;
import java.io.ObjectInputStream.GetField;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.sebi.fxedit.runtime.JsRuntime;
import ch.sebi.fxedit.runtime.reflection.ObjectPool;
import ch.sebi.fxedit.ui.controller.window.FXApplication;

public class FXEditStartup {
	private static Logger logger = LogManager.getLogger(FXEditStartup.class);
	public static void main(String[] args) throws IOException {
		logger.info("Start FXEdit");
		FXApplication.startup(args);
/*		logger.traceEntry();

		JsRuntime runtime = new JsRuntime();
		runtime.executeRC();
		ObjectPool pool = runtime.getObjectPool();*/
	}
}
