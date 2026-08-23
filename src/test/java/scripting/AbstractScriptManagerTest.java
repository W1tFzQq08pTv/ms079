package scripting;

import org.junit.Test;

import javax.script.ScriptEngine;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import static org.junit.Assert.assertEquals;

public class AbstractScriptManagerTest {

    @Test
    public void supportsLegacyImportPackageScripts() throws Exception {
        final ScriptEngine engine = AbstractScriptManager.createScriptEngine();

        assertEquals(0, ((Number) engine.eval("importPackage(Packages.java.util); new ArrayList().size();")).intValue());
    }

    @Test
    public void compilesLargeGachaponScriptsWithoutChangingTheirItemCounts() throws Exception {
        assertGachaponItemCount("9050000", 1686);
        assertGachaponItemCount("9050001", 1688);
        assertGachaponItemCount("9050002", 1688);
        assertGachaponItemCount("9050003", 1686);
    }

    private void assertGachaponItemCount(String npcId, int expectedCount) throws Exception {
        final File scriptFile = new File("scripts/npc/" + npcId + ".js");
        final ScriptEngine engine = AbstractScriptManager.createScriptEngine();
        try (Reader reader = new InputStreamReader(new FileInputStream(scriptFile), EncodingDetect.getJavaEncode(scriptFile))) {
            engine.eval(reader);
        }

        assertEquals(expectedCount, ((Number) engine.eval("itemList.length")).intValue());
    }
}
