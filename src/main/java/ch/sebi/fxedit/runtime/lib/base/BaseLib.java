package ch.sebi.fxedit.runtime.lib.base;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.eclipsesource.v8.JavaVoidCallback;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;

import ch.sebi.fxedit.runtime.JsRuntime;
import ch.sebi.fxedit.runtime.reflection.JsClassFactory;


public class BaseLib implements JsClassFactory {
	private final Logger logger = LogManager.getLogger(getClass());
	private JsRuntime runtime;

	protected BaseLib() {
		this.runtime = runtime;
	}

	public void jsPrint(V8Array args) {
		logger.info(args.toString());
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
