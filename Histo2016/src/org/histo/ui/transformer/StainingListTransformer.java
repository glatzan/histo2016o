package org.histo.ui.transformer;

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
		if (value != null && value.trim().length() > 0) {
			try {
				System.out.println(value);
				long id = Long.valueOf(value);
				for (StainingPrototypeList stainingPrototypeList : allAvailableStainingLists) {
					if (stainingPrototypeList.getId() == id)
						return stainingPrototypeList;
				}
				return null;
			} catch (NumberFormatException e) {
			}
		} else {
			return null;
		}
		return null;
	}

	public String getAsString(FacesContext fc, UIComponent uic, Object object) {
		if (object != null && object instanceof StainingPrototypeList) {
			return String.valueOf(((StainingPrototypeList) object).getId());
		} else {
			return "";
		}
	}
}