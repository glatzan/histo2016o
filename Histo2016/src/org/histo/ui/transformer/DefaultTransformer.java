package org.histo.ui.transformer;

import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;

import org.histo.model.DiagnosisPreset;
import org.histo.model.interfaces.LogAble;

public class DefaultTransformer<T extends LogAble> implements Converter {

	private List<T> objects;

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
		if (object != null && object instanceof LogAble) {
			return String.valueOf(((T) object).getId());
		} else {
			return "";
		}
	}
}