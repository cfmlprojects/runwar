package runwar.gui;

import java.awt.event.ActionListener;

public abstract class SubmitActionlistioner implements ActionListener {
    private JsonForm jsonForm = null;

    public JsonForm setForm(JsonForm form){
        jsonForm = form;
        return jsonForm;
    }

    public JsonForm getForm(){
        return jsonForm;
    }


}
