package ch.sebi.fxedit.model.shortcut;

import java.util.Arrays;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.utils.V8ObjectUtils;

import ch.sebi.fxedit.exception.FactoryNotFoundException;
import ch.sebi.fxedit.exception.FailedObjectCreationException;
import ch.sebi.fxedit.runtime.JsRuntime;
import ch.sebi.fxedit.runtime.reflection.annotation.JsId;
import ch.sebi.fxedit.runtime.reflection.annotation.JsObject;

@JsObject
public class ShortcutManager {
	@JsId
	private long id;

	private JsRuntime runtime;

	private ShortcutManager(JsRuntime runtime) {
		this.runtime = runtime;
	}

	public boolean execute(String[] keys) {
		V8Object shortcutManagerJsObj = runtime.getObjectPool().getJsObj(id);
		V8 v8 = runtime.getV8();
		V8Array args = new V8Array(v8);
		try {
			args.push(V8ObjectUtils.toV8Array(v8, Arrays.asList(keys)));
			return shortcutManagerJsObj.executeBooleanFunction("execute", args);
		} finally {
			args.release();
		}
	}

	public static ShortcutManager createShortcutManager(JsRuntime runtime) {
		try {
			ShortcutManager manager = runtime.createObject(ShortcutManager.class);
			return manager;
		} catch (FactoryNotFoundException | FailedObjectCreationException e) {
			throw new IllegalStateException("Couldn't create new ShortcutManager", e);
		}
	}
}
