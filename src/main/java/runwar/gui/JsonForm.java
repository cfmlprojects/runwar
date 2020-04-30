package runwar.gui;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.HashMap;
import java.util.Map;

import javax.swing.*;

import static runwar.LaunchUtil.displayMessage;
import static runwar.LaunchUtil.getResourceAsString;

public class JsonForm extends JPanel {

    private HashMap<String, String> variableMap;
    private Map<String,JTextField> fields;

    private JsonForm(JSONObject properties, HashMap<String, String> values, HashMap<String, String> variableMap) {
        super(new BorderLayout());
        this.variableMap = variableMap;
        assert variableMap != null;
        fields = new HashMap<>();
        int propertyCount = properties.keySet().size();
        JPanel labelPanel = new JPanel(new GridLayout(propertyCount, 1));
        JPanel fieldPanel = new JPanel(new GridLayout(propertyCount, 1));

        add(labelPanel, BorderLayout.WEST);
        add(fieldPanel, BorderLayout.CENTER);

        properties.keySet().forEach(propertyName -> {
            JTextField field = new JTextField();
            JSONObject property = (JSONObject) properties.get(propertyName);
            assert property != null;
            String tooltip = getString(property,"type","");
            String title = getString(property,"title","");
            String width = getString(property,"width","15");
            String defaultValue = getString(property,"default","");
            String mnemonic = getString(property,"mnemonic","");

            if (tooltip.length() > 0)
                field.setToolTipText(tooltip);
            if (width.length() > 0)
                field.setColumns(Integer.parseInt(width));

            if(values.get(propertyName) != null){
                field.setText(values.get(propertyName));
            } else if(defaultValue != ""){
                field.setText(defaultValue);
            }

            JLabel lab = new JLabel(title, JLabel.RIGHT);
            lab.setLabelFor(field);
            if (mnemonic.length() > 0)
                lab.setDisplayedMnemonic(mnemonic.charAt(0));

            labelPanel.add(lab);
            JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
            p.add(field);
            fieldPanel.add(p);
            fields.put(propertyName, field);
        });
    }

    public String getFieldValue(String propertyName) {
        if(fields.get(propertyName) == null){
            displayMessage("error","no field: " + propertyName + "(" + fields.keySet().toString() + ")");
            return "";
        }
        return fields.get(propertyName).getText();
    }

    public Map<String,JTextField> getFields() {
        return fields;
    }

    private String getString(JSONObject menu, String key, String defaultValue) {
        assert menu != null && key != null && defaultValue !=null;
        String value = menu.get(key) != null ? menu.get(key).toString() : defaultValue;
        return replaceMenuTokens(value);
    }

    private String replaceMenuTokens(String string) {
        for(String key : variableMap.keySet() ) {
            if(variableMap.get(key) != null) {
                string = string.replace("${" + key + "}", variableMap.get(key));
            } else {
                displayMessage("error","Could not get key: " + key);
            }
        }
        return string;
    }

    public static void renderFormJson(String path, HashMap<String, String> values, HashMap<String, String> variableMap, SubmitActionlistioner actionListener) {
        JSONObject schema;
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        String json = getResourceAsString(path);
        schema = (JSONObject) JSONValue.parse(json);
        JSONObject properties = (JSONObject) schema.get("properties");

        final JsonForm form = new JsonForm(properties,values,variableMap);

        JButton submit = new JButton("Submit Form");

        actionListener.setForm(form);
        submit.addActionListener(actionListener);

        JFrame f = new JFrame(schema.get("title") != null ? schema.get("title").toString() : "Form");
        f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        f.getContentPane().add(form, BorderLayout.NORTH);
        JPanel p = new JPanel();
        p.add(submit);
        f.getContentPane().add(p, BorderLayout.SOUTH);
        f.pack();
        f.setVisible(true);
    }


}
