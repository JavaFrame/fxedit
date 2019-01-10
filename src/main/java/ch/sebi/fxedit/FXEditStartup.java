package ch.sebi.fxedit;

import java.io.File;
import java.io.IOException;

import ch.sebi.fxedit.runtime.Runtime;

public class FXEditStartup {
	public static void main(String[] args) throws IOException {
		Runtime runtime = new Runtime();
		String path = FXEditStartup.class.getResource("/jsruntime/test.js").getFile();
		runtime.executeFile(new File(path));
	}
}
