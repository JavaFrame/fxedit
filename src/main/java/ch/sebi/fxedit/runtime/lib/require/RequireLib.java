package ch.sebi.fxedit.runtime.lib.require;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.V8ResultUndefined;

import ch.sebi.fxedit.exception.ScriptNotFoundException;
import ch.sebi.fxedit.runtime.JsRuntime;

/**
 * The require library which loads js modules
 * @author sebi
 *
 */
public class RequireLib {
	/**
	 * logger
	 */
	private final Logger logger = LogManager.getLogger(getClass());
	
	/**
	 * module cache
	 */
	private Map<String, V8Object> moduleCache = new HashMap<>();

	/**
	 * the js runtime
	 */
	private JsRuntime runtime;
	/**
	 * the search path in which a module is searched in
	 */
	private V8Array requirePath;

	
	/**
	 * Initializes the lib
	 * @param runtime the runtime 
	 * @param requirePath the requirepath v8 array object
	 */
	public void init(JsRuntime runtime, V8Array requirePath) {
		this.runtime = runtime;
		this.requirePath = requirePath;
	}
	
	/**
	 * Loads the given path and returns the module
	 * @param path the path of the module
	 * @return the loaded module
	 * @throws IOException if the module can't be found or an other io error occured
	 */
	public V8Object jsRequire(String path) throws IOException {
		File f = findFile(path);
		V8Object moduleObj = loadFile(f);
		return moduleObj;
	}
	
	/**
	 * Finds the file in the {@link #getRequirePath()}
	 * @param jsPath the module path
	 * @return the file of the module
	 * @throws ScriptNotFoundException 
	 */
	protected File findFile(String jsPath) throws ScriptNotFoundException {
		String oldJsPath = jsPath;
		String[] paths = getRequirePath();
		//because we replace '.' to '/' we have to remove the ".js" to later add it again
		if(jsPath.endsWith(".js")) {
			jsPath = jsPath.substring(0, jsPath.length()-3);
		}
		String filePath = jsPath.replace('.', '/') + ".js";
		for(String dirPath : paths) {
			File dir = new File(dirPath);
			if(!dir.exists()) {
				logger.warn("Path \"" + dir.getAbsolutePath() + "\" doesn't exist");
				continue;
			}
			if(!dir.isDirectory()) {
				logger.warn("Path \"" + dir.getAbsolutePath() + "\" isn't a directory");
				continue;
			}
			File scriptFile = new File(dir.getAbsoluteFile() + "/" + filePath);
			if(scriptFile.exists()) {
				logger.debug("Found script \"" + scriptFile.getAbsolutePath() + "\" for jsPath \"" + oldJsPath + 
						"\" (requirePath: " + Arrays.toString(paths) + ")");
				return scriptFile;
			}
		}
		throw new ScriptNotFoundException(oldJsPath, paths);
	}
	
	/**
	 * Loads the given module file and returns the loaded module
	 * @param f the file of the module
	 * @return the loaded module
	 * @throws IOException
	 */
	protected V8Object loadFile(File f) throws IOException {
		//caching
		if(moduleCache.containsKey(f.getAbsolutePath())) {
			V8Object cachedModule = moduleCache.get(f.getAbsolutePath());
			//checks if another part of the code already released the v8 object
			if(!cachedModule.isReleased()) {
				logger.debug("Loaded module \"" + f.getPath() + "\" from cache");
				return cachedModule;
			} else {
				logger.warn("Modle \"" + f.getPath() + "\" was already released");
			}
		}
		String scriptBody = new String(Files.readAllBytes(f.toPath()));
		String script = "(function() {\n" + scriptBody + "\n})()";
		V8 v8 = runtime.getV8();
		V8Object module;
		try {
			module = v8.executeObjectScript(script, f.getPath(), 0);
		} catch(V8ResultUndefined ex) {
			module = new V8Object(runtime.getV8());
		}
		moduleCache.put(f.getAbsolutePath(), module.twin());
		return module;
	}
	
	/**
	 * Returns the requirePath. In this path are all paths where modules can be found.
	 * If a path is earlier than it has a higher priority.
	 * @return the paths
	 */
	public String[] getRequirePath() {
		List<String> paths = new ArrayList<>();
		int length = requirePath.length();
		for(int i = 0; i < length; i++) {
			Object path = requirePath.get(i); 
			if(path != null) {
				paths.add(path.toString());
			}
		}
		return paths.toArray(new String[paths.size()]);
	}
	
	/**
	 * Adds a require path to the require path array
	 * @param path the path to add
	 */
	public void addRequirePath(String path) {
		requirePath.push(path);
	}
		

}
