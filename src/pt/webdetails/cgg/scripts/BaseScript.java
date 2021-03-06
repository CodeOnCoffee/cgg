/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.webdetails.cgg.scripts;

import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mozilla.javascript.*;
import pt.webdetails.cgg.datasources.DatasourceFactory;

/**
 *
 * @author pdpi
 */
public abstract class BaseScript implements Script {

    protected static final Log logger = LogFactory.getLog(BaseScript.class);
    protected String source, rootPath;
    protected Scriptable scope;

    BaseScript() {
    }

    BaseScript(String source) {
        this.source = source.replaceAll("\\\\", "/").replaceAll("/+", "/");
        this.rootPath = this.source.replaceAll("(.*/).*", "$1");
    }

    public void initializeObjects() {
        ContextFactory.getGlobal().enter();
        Object wrappedFactory = Context.javaToJS(new DatasourceFactory(), scope);
        ScriptableObject.putProperty(scope, "datasourceFactory", wrappedFactory);
    }

    public void setScope(Scriptable scope) {
        this.scope = scope;
        if (scope instanceof BaseScope) {
            ((BaseScope) scope).setBasePath(this.rootPath);
        }
        initializeObjects();
    }

    protected void executeScript(Map<String, Object> params) {
        Context cx = Context.getCurrentContext();
        // env.js has methods that pass the 64k Java limit, so we can't compile
        // to bytecode. Interpreter mode to the rescue!
        cx.setOptimizationLevel(-1);
        cx.setLanguageVersion(Context.VERSION_1_7);
        OutputStream bytes = new ByteArrayOutputStream();

        Object wrappedParams;
        if (params != null) {
            wrappedParams = Context.javaToJS(params, scope);
        } else {
            wrappedParams = Context.javaToJS(new HashMap<String, Object>(), scope);
        }
        ScriptableObject.defineProperty(scope, "params", wrappedParams, 0);

        try {
            cx.evaluateReader(scope, new FileReader(source), this.source.replaceAll("(.*/)(.*)", "$2"), 1, null);
        } catch (IOException ex) {
            logger.error("Failed to read " + source + ": " + ex.toString());
        }
    }
}
