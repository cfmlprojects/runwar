package runwar;

import static org.junit.Assert.*;

import java.util.HashMap;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

import org.junit.Test;

public class TestGetTrayConfig {
   
    public TestGetTrayConfig() {
    }

    @Test
    public void testAliasMapProcessed() {
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
        menu = LaunchUtil.getTrayConfig( menuJSON , statusText, variableMap );
        assertEquals(statusText, menu.get("title").toString());
        assertEquals(statusText, menu.get("tooltip").toString());
        assertEquals(3, ((JSONArray) menu.get("items")) .size() );
        assertEquals("http://localhost:8099/", ((JSONObject) ((JSONArray) menu.get("items")) .get(2)).get("url").toString() );
        assertEquals("true", ((JSONObject) ((JSONArray) menu.get("items")) .get(0)).get("disabled").toString() );

        menu = LaunchUtil.getTrayConfig( menuJSONTooltip , statusText, variableMap );
        assertEquals("Cooltool localhost", menu.get("tooltip").toString());
        assertEquals(statusText, menu.get("title").toString());
        assertEquals(3, ((JSONArray) menu.get("items")) .size() );

        menu = LaunchUtil.getTrayConfig( legacyMenuJSON , statusText, variableMap );
        assertEquals(statusText, menu.get("title").toString());
        assertEquals(2, ((JSONArray) menu.get("items")) .size() );
        
        menu = LaunchUtil.getTrayConfig( menuJSONnoActions , statusText, variableMap );
        assertEquals(statusText, menu.get("title").toString());
        assertEquals(2, ((JSONArray) menu.get("items")) .size() );
        assertEquals("http://localhost:8099/", ((JSONObject) ((JSONArray) menu.get("items")) .get(1)).get("url").toString() );
        assertEquals("openbrowser", ((JSONObject) ((JSONArray) menu.get("items")) .get(1)).get("action").toString() );

    
    }
    

}
