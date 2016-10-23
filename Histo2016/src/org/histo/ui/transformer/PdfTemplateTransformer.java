package org.histo.ui.transformer;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;

import org.histo.config.enums.PdfTemplate;

public class PdfTemplateTransformer implements Converter {

	private PdfTemplate[] templates;

	public PdfTemplateTransformer(PdfTemplate[] templates) {
		this.templates = templates;
	}

	public Object getAsObject(FacesContext fc, UIComponent uic, String value) {
		if (value != null && value.trim().length() > 0) {
			try {
				for (PdfTemplate pdfTemplate : templates) {
					if (pdfTemplate.getType().equals(value)){
						System.out.println("found");
						return pdfTemplate;
					}
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
		if (object != null && object instanceof PdfTemplate) {
			return ((PdfTemplate) object).getType();
		} else {
			System.out.println("nop ");
			return "";
		}
	}
}
