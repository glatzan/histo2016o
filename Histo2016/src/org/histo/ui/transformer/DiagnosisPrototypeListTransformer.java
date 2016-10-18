package org.histo.ui.transformer;

import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;

import org.histo.model.DiagnosisPreset;

public class DiagnosisPrototypeListTransformer implements Converter {

	private List<DiagnosisPreset> allAvailableDiagnosisPrototypes;

	public DiagnosisPrototypeListTransformer(List<DiagnosisPreset> allAvailableDiagnosisPrototypes) {
		this.allAvailableDiagnosisPrototypes = allAvailableDiagnosisPrototypes;
	}

	public Object getAsObject(FacesContext fc, UIComponent uic, String value) {
		if (value != null && value.trim().length() > 0) {
			try {
				long id = Long.valueOf(value);
				for (DiagnosisPreset diagnosisPreset : allAvailableDiagnosisPrototypes) {
					if (diagnosisPreset.getId() == id)
						return diagnosisPreset;
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
		if (object != null && object instanceof DiagnosisPreset) {
			return String.valueOf(((DiagnosisPreset) object).getId());
		} else {
			return "";
		}
	}
}