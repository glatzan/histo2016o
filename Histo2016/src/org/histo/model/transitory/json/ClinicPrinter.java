package org.histo.model.transitory.json;

import java.io.Serializable;
import java.net.URL;

import org.cups4j.CupsPrinter;
import org.histo.model.interfaces.LogAble;

public class ClinicPrinter implements Serializable {

	private static final long serialVersionUID = 5960177965663431521L;

	private URL printerURL;

	private String name;

	private String description;

	private String location;

	public ClinicPrinter(CupsPrinter cupsPrinter) {
		printerURL = cupsPrinter.getPrinterURL();
		name = cupsPrinter.getName();
		description = cupsPrinter.getDescription();
		location = cupsPrinter.getLocation();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ClinicPrinter && ((ClinicPrinter) obj).getPrinterURL().equals(printerURL))
			return true;

		return super.equals(obj);
	}

	public String getId(){
		return getPrinterURL().toString();
	}

	public URL getPrinterURL() {
		return printerURL;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public String getLocation() {
		return location;
	}

	public void setPrinterURL(URL printerURL) {
		this.printerURL = printerURL;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setLocation(String location) {
		this.location = location;
	}

}
