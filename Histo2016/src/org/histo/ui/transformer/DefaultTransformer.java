package org.histo.ui.transformer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;

import org.histo.model.interfaces.HasID;

public class DefaultTransformer<T extends HasID> implements Converter {

	private List<T> objects;

	public DefaultTransformer(T[] objects) {
		this.objects = Arrays.asList(objects);
	}
	
	public DefaultTransformer(List<T> objects) {
		this.objects = objects;
	}

	public Object getAsObject(FacesContext fc, UIComponent uic, String value) {
		if (value != null && value.trim().length() > 0) {
			try {
				long id = Long.valueOf(value);
				for (T diagnosisPrototype : objects) {
					if (diagnosisPrototype.getId() == id)
						return diagnosisPrototype;
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
		if (object != null && object instanceof HasID) {
			return String.valueOf(((T) object).getId());
		} else {
			return "";
		}
	}
}