package runwar.gui;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import runwar.logging.RunwarLogger;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;

import static runwar.LaunchUtil.getResourceAsString;

public class Form {
    private static HashMap<String,String> variableMap;

    public static void setVariableMap(HashMap<String,String> vm) {
        variableMap = vm;
    }

    public static void renderFormJson(String path, HashMap<String, String> variableMap) {
        JSONObject schema;
        setVariableMap(variableMap);
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }catch(Exception ex) {
            ex.printStackTrace();
        }
        String json = getResourceAsString(path);
        schema = (JSONObject) JSONValue.parse(json);
        JSONObject properties = (JSONObject) schema.get("properties");

        final JPanel mainPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10)) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(580, 420);
            }
        };

        properties.keySet().forEach(s -> {

            final JPanel fieldPanel = new JPanel(new GridLayout(1, properties.keySet().size()));
            JSONObject property = (JSONObject) properties.get(s);
            String type = getString(property,"type","");
            String title = getString(property,"title","");
            JLabel label = new JLabel(title);
            fieldPanel.add(label);
            JTextField field = new JTextField();
            fieldPanel.add(field);
            fieldPanel.add(new JSeparator(SwingConstants.HORIZONTAL));
            mainPanel.add(fieldPanel);

        });
        System.out.println(properties);

        String formTitle = getString(schema, "title", "defaultTitle");
        JOptionPane.showMessageDialog(null, mainPanel, formTitle, JOptionPane.INFORMATION_MESSAGE);

    }
    private static String getString(JSONObject menu, String key, String defaultValue) {
        String value = menu.get(key) != null ? menu.get(key).toString() : defaultValue;
        return replaceMenuTokens(value);
    }

    private static String replaceMenuTokens(String string) {
        for(String key : variableMap.keySet() ) {
            if(variableMap.get(key) != null) {
                string = string.replace("${" + key + "}", variableMap.get(key));
            } else {
                RunwarLogger.LOG.error("Could not get key: " + key);
            }
        }
        return string;
    }

}

