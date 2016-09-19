package org.histo.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.stereotype.Component;

import javax.faces.context.FacesContext;
import java.util.HashMap;

@Component(value = "msg")
public class ResourceBundle extends HashMap<Object, Object> {

	private static final long serialVersionUID = 1668009329184453712L;
	
	@Autowired
    private MessageSource messageSource;

    @Override
    public String get(Object key) {
        String message;
        try {
            message = messageSource.getMessage((String) key, null,  FacesContext.getCurrentInstance().getViewRoot().getLocale());
        }
        catch (NoSuchMessageException e) {
            message = "???" + key + "???";
        }
        return message;
    }
}