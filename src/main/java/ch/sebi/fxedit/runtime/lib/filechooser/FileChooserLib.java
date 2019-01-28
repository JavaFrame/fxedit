package ch.sebi.fxedit.runtime.lib.filechooser;

import java.io.File;

import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Function;
import com.eclipsesource.v8.utils.MemoryManager;

import ch.sebi.fxedit.runtime.reflection.annotation.JsFunction;
import ch.sebi.fxedit.runtime.reflection.annotation.JsId;
import ch.sebi.fxedit.runtime.reflection.annotation.JsObject;
import javafx.application.Platform;
import javafx.stage.FileChooser;

@JsObject
public class FileChooserLib {
	@JsId
	private long id;

	private FileChooserLib() {
	}

	@JsFunction
	private void openFileChooser(Boolean saveDialog, String title, V8Function callback) {
		FileChooser chooser = new FileChooser();
		chooser.setTitle(title);
		V8Function callbackTwined = callback.twin();
		if (callbackTwined == null || callbackTwined.isUndefined()) {
			return;
		}
		Platform.runLater(() -> {
			File choosenFile;
			if (saveDialog) {
				choosenFile = chooser.showSaveDialog(null);
			} else {
				choosenFile = chooser.showOpenDialog(null);
			}
			MemoryManager scope = new MemoryManager(callbackTwined.getRuntime());
			try {
				V8Array args = new V8Array(callbackTwined.getRuntime());
				if(choosenFile == null) {
					args.pushNull();
				} else {
					args.push(choosenFile.getAbsolutePath());
				}
				callback.call(null, args);
			} finally {
				scope.release();
			}
		});
		callbackTwined.release();
	}
}
