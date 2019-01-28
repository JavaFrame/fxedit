package ch.sebi.fxedit.runtime.lib.base;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.eclipsesource.v8.JavaVoidCallback;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.V8Value;
import com.eclipsesource.v8.utils.MemoryManager;

import ch.sebi.fxedit.runtime.JsRuntime;
import ch.sebi.fxedit.runtime.reflection.JsClassFactory;


public class BaseLib implements JsClassFactory {
	private final Logger logger = LogManager.getLogger(getClass());
	private JsRuntime runtime;

	protected BaseLib() {
		this.runtime = runtime;
	}

	public void jsPrint(V8Array args) {
		MemoryManager scope = new MemoryManager(runtime.getV8());
		try {
			StringBuffer msg = new StringBuffer();
			for(int i = 0; i < args.length(); i++) {
				Object obj = args.get(i);
				msg.append(getString(obj));
			}
			logger.info(msg.toString());
		} finally {
			scope.release();
		}
	}
	private String getString(Object obj) {
		if(obj instanceof V8Value) {
			V8Value value = (V8Value) obj;
			if(value.getV8Type() == V8Value.V8_OBJECT) {
				return v8ObjectToString((V8Object) obj);
			} 
		}
		if(obj == null) return "null";
		return obj.toString();
	}
	private String v8ObjectToString(V8Object obj) {
		StringBuffer str = new StringBuffer("{ ");
		for(String key : obj.getKeys()) {
			str.append(key + ": " + getString(obj.get(key)) + ", ");
		}
		str.append(" }");
		return str.toString();
	}
	

	@Override
	public void initClass(JsRuntime runtime, V8Object object) {
		this.runtime = runtime;
		runtime.getV8().registerJavaMethod((JavaVoidCallback) (receiver, parameters) -> jsPrint(parameters), "print");
	}

	@Override
	public void initObject(JsRuntime runtime, V8Object object, V8Array args) {
		
	}

	@Override
	public Object createObject(JsRuntime runtime, Object[] args) {
		throw new UnsupportedOperationException("The BaseLib doesn't have a runtime instance");
	}
}
