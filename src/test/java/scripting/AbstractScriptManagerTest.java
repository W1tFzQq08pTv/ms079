package scripting;

import org.junit.Test;

import javax.script.ScriptEngine;

import static org.junit.Assert.assertEquals;

public class AbstractScriptManagerTest {

    @Test
    public void supportsLegacyImportPackageScripts() throws Exception {
        final ScriptEngine engine = AbstractScriptManager.createScriptEngine();

        assertEquals(0, ((Number) engine.eval("importPackage(Packages.java.util); new ArrayList().size();")).intValue());
    }
}
