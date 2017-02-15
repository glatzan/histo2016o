package org.histo.ui.transformer;

import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;

import org.histo.model.transitory.json.ClinicPrinter;

public class ClinicPrinterTransformer implements Converter {

	private List<ClinicPrinter> objects;

	public ClinicPrinterTransformer(List<ClinicPrinter> objects) {
		this.objects = objects;
	}

	public Object getAsObject(FacesContext fc, UIComponent uic, String value) {
		if (value != null && value.trim().length() > 0) {
			try {
				for (ClinicPrinter printer : objects) {
					System.out.println(printer.getPrinterURL());
					if (printer.getId().equals(value))
						return printer;
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
		if (object != null && object instanceof ClinicPrinter) {
			return ((ClinicPrinter) object).getId();
		} else {
			return "";
		}
	}
}