package org.histo.ui.transformer;

import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;

import org.histo.model.transitory.PDFTemplate;

public class PdfTemplateTransformer implements Converter {

	private List<PDFTemplate> templates;

	public PdfTemplateTransformer(List<PDFTemplate> templates) {
		this.templates = templates;
	}

	public Object getAsObject(FacesContext fc, UIComponent uic, String value) {
		if (value != null && value.trim().length() > 0) {
			try {
				for (PDFTemplate pdfTemplate : templates) {
					if (pdfTemplate.getName().equals(value))
						return pdfTemplate;
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
		if (object != null && object instanceof PDFTemplate) {
			return ((PDFTemplate)object).getName();
		} else {
			return "";
		}
	}
}
