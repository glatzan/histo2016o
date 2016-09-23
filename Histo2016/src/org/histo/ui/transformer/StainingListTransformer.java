package org.histo.ui;

import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;

import org.histo.model.StainingPrototypeList;

public class StainingListTransformer implements Converter {

    private List<StainingPrototypeList> allAvailableStainingLists;

    public StainingListTransformer(List<StainingPrototypeList> allAvailableStainingLists) {
	this.allAvailableStainingLists = allAvailableStainingLists;
    }

    public Object getAsObject(FacesContext fc, UIComponent uic, String value) {
	System.out.println("--------------!count");
	if (value != null && value.trim().length() > 0) {
	    try {
		System.out.println(value);
		long id = Long.valueOf(value);
		for (StainingPrototypeList stainingPrototypeList : allAvailableStainingLists) {
		    if (stainingPrototypeList.getId() == id)
			return stainingPrototypeList;
		}
		System.out.println("found nothing");
		return null;
	    } catch (NumberFormatException e) {
	    }
	} else {
	    return null;
	}
	return null;
    }

    public String getAsString(FacesContext fc, UIComponent uic, Object object) {
	System.out.println("--------------!geting");
	if (object != null && object instanceof StainingPrototypeList) {
	    return String.valueOf(((StainingPrototypeList) object).getId());
	} else {
	    return "";
	}
    }
}