package ch.sebi.fxedit.runtime;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;

public class Runtime {
	private V8 v8;
	private long startThreadID;
	
	public Runtime() {
		startThreadID = Thread.currentThread().getId();
		v8 = V8.createV8Runtime();
		init();
	}
	
	private void init() {
		
	}
	
	private void checkThread() {
		if(Thread.currentThread().getId() != startThreadID) {
			throw new IllegalStateException("Runtime can only be called from the thread it was created from");
		}
	}
	
	public Object execString(String script) {
		checkThread();
		return v8.executeScript(script);
	}
	
	public Object executeFile(File f) throws IOException {
		checkThread();
		String script = new String(Files.readAllBytes(f.toPath()));
		return execString(script);
	}
}
