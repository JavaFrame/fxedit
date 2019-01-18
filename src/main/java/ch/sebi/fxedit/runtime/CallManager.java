package ch.sebi.fxedit.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.eclipsesource.v8.V8Array;

public class CallManager {
	private Map<String, Consumer<V8Array>> registeredCallMap = new HashMap<>();

	protected CallManager() {
		
	}

	public void call(String functionId, V8Array args) {
		Consumer<V8Array> consumer = registeredCallMap.get(functionId);
		if(consumer == null) {
			
		}
		consumer.accept(args);
	}
	
	public void registerCall(String functionId, Consumer<V8Array> callable) {
		registeredCallMap.put(functionId, callable);
	}
}
