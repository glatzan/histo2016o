package org.histo.ui;

import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;

import org.histo.model.DiagnosisPrototype;

public class DiagnosisPrototypeListTransformer implements Converter {

	private List<DiagnosisPrototype> allAvailableDiagnosisPrototypes;

	public DiagnosisPrototypeListTransformer(List<DiagnosisPrototype> allAvailableDiagnosisPrototypes) {
		this.allAvailableDiagnosisPrototypes = allAvailableDiagnosisPrototypes;
	}

	public Object getAsObject(FacesContext fc, UIComponent uic, String value) {
		if (value != null && value.trim().length() > 0) {
			try {
				long id = Long.valueOf(value);
				for (DiagnosisPrototype diagnosisPrototype : allAvailableDiagnosisPrototypes) {
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
		if (object != null && object instanceof DiagnosisPrototype) {
			return String.valueOf(((DiagnosisPrototype) object).getId());
		} else {
			return "";
		}
	}
}