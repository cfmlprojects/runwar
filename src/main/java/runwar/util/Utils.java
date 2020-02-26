/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package runwar.util;

import java.net.MalformedURLException;
import java.net.URL;

/**
 *
 * @author pyro
 */
public class Utils {
    
        public static String replaceHost(String openbrowserURL, String oldHost, String newHost) {
        String url = openbrowserURL;
        try {
            URL address = new URL(openbrowserURL);
            String host = address.getHost();
            if (host.equalsIgnoreCase(oldHost)) {
                URL ob = new URL(address.getProtocol(), newHost, address.getPort(), address.getFile());
                openbrowserURL = ob.toString();
            }
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
            openbrowserURL = url;
        }
        return openbrowserURL;
    }
    
}
