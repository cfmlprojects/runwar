package runwar;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import runwar.tray.Tray;


public class TestGetTrayConfig {
   
    public TestGetTrayConfig() {
    }

    @Test
    public void testGetTrayConfig() {
        final String statusText = " Awesome server on 127.0.0.1:8099 PID:323423";

        HashMap<String,String> variableMap = new HashMap<String,String>();
        variableMap.put("defaultTitle", statusText);
        variableMap.put("runwar.port", "8099");
        variableMap.put("runwar.processName", "Awesome");
        variableMap.put("runwar.host", "localhost");
        variableMap.put("runwar.stopsocket", "2343");

        final String menuJSON = "{\"title\" : \"${defaultTitle}\", \"items\": ["
                + "{label:\"Blank Item\", disabled:\"true\"}"
                + "{label:\"Stop Server (${runwar.processName})\", action:\"stopserver\"}"
                + ",{label:\"Open Browser\", action:\"openbrowser\", url:\"http://${runwar.host}:${runwar.port}/\"}"
                + "{label:\"Advanced\",\"items\": [" +
                ",{label:\"Version\", action:\"getversion\", image:\"dialog-reload.png\"}" +
                ",{label:\"Start on boot ${runwar.processName} http://${runwar.host}:${runwar.port}\", checkbox:\"true\", image:\"dialog-information.png\", disabled:\"true\"}" +
                ",{label:\"Browse File System\", hotkey:\"B\", action:\"openfilesystem\", path:\"\", image:\"resources/dialog-information.png\"}" +
                "]}"
                + "]}";

        final String menuJSONTooltip = "{\"title\" : \"${defaultTitle}\",\"tooltip\" : \"Cooltool ${runwar.host}\", \"items\": ["
                + "{label:\"Blank Item\", disabled:\"true\"}"
                + "{label:\"Stop Server (${runwar.processName})\", action:\"stopserver\"}"
                + ",{label:\"Open Browser\", action:\"openbrowser\", url:\"http://${runwar.host}:${runwar.port}/\"}"
                + "]}";

        final String legacyMenuJSON = "["
                + "{label:\"Stop Server (${runwar.processName})\", action:\"stopserver\"}"
                + ",{label:\"Open Browser\", action:\"openbrowser\", url:\"http://${runwar.host}:${runwar.port}/\"}"
                + "]";

        final String menuJSONnoActions = "{\"title\" : \"${defaultTitle}\", \"items\": ["
                + "{label:\"Stop Server (${runwar.processName})\"}"
                + ",{label:\"Open Browser\", url:\"http://${runwar.host}:${runwar.port}/\"}"
                + "]}";

        JSONObject menu;
        menu = Tray.getTrayConfig( menuJSON , statusText, variableMap );
        assertEquals(statusText, menu.get("title").toString());
        assertEquals(statusText, menu.get("tooltip").toString());
        JSONArray menuArray = (JSONArray) menu.get("items");
        assertEquals(4, menuArray .size() );
        assertEquals("http://localhost:8099/", ((JSONObject) menuArray .get(2)).get("url").toString() );
        assertEquals("true", ((JSONObject) menuArray .get(0)).get("disabled").toString() );

        JSONArray submenuArray = (JSONArray) ((JSONObject) menuArray .get(3)).get("items");
        assertEquals(3, submenuArray.size());
        assertEquals("Start on boot Awesome http://localhost:8099", ((JSONObject)submenuArray.get(1)).get("label"));

        menu = Tray.getTrayConfig( menuJSONTooltip , statusText, variableMap );
        menuArray = (JSONArray) menu.get("items");
        assertEquals("Cooltool localhost", menu.get("tooltip").toString());
        assertEquals(statusText, menu.get("title").toString());
        assertEquals(3, menuArray .size() );

        menu = Tray.getTrayConfig( legacyMenuJSON , statusText, variableMap );
        menuArray = (JSONArray) menu.get("items");
        assertEquals(statusText, menu.get("title").toString());
        assertEquals(2, menuArray .size() );
        
        menu = Tray.getTrayConfig( menuJSONnoActions , statusText, variableMap );
        menuArray = (JSONArray) menu.get("items");
        assertEquals(statusText, menu.get("title").toString());
        assertEquals(2, menuArray .size() );
        assertEquals("http://localhost:8099/", ((JSONObject) menuArray .get(1)).get("url").toString() );
        assertEquals("openbrowser", ((JSONObject) menuArray .get(1)).get("action").toString() );

    
    }
    
    @Test
    public void testGetTrayConfigLongTooltip() {
        final String statusText = " Awesome server on 127.0.0.1:8099 PID:323423";
        
        HashMap<String,String> variableMap = new HashMap<String,String>();
        variableMap.put("defaultTitle", statusText);
        variableMap.put("runwar.port", "8099");
        variableMap.put("runwar.processName", "Awesome");
        variableMap.put("runwar.host", "localhost");
        variableMap.put("runwar.stopsocket", "2343");
        
        final String menuJSON = "{\"title\" : \"${defaultTitle}\",\"tooltip\" : \"Cooltool ${runwar.host} that is longer than 64 characters by several characters at least\", \"items\": ["
                + "{label:\"Blank Item\", disabled:\"true\"}"
                + "{label:\"Stop Server (${runwar.processName})\", action:\"stopserver\"}"
                + ",{label:\"Open Browser\", action:\"openbrowser\", url:\"http://${runwar.host}:${runwar.port}/\"}"
                + "]}";

        JSONObject menu;
        menu = Tray.getTrayConfig( menuJSON , statusText, variableMap );
        assertNotEquals(statusText, menu.get("tooltip").toString());
        assertTrue(menu.get("tooltip").toString().length() < 65);
        assertEquals("Cooltool localhost that is longer than 64 characters by...", menu.get("tooltip").toString());
        
        
    }
    

}
