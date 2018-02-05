package org.histo.adaptors.printer;

import java.io.File;

import org.histo.model.PDFContainer;

public class ClinicPrinterDummy extends ClinicPrinter {

	private static final long serialVersionUID = 3965169880380400939L;

	public ClinicPrinterDummy() {
		this.name = "Drucker Auswählen";
		this.id = this.getName().hashCode();
	}

	@Override
	public boolean print(File file) {
		logger.debug("Dummy printer, printin...");
		return true;
	}

	@Override
	public boolean print(PDFContainer container) {
		return print(container, 1, null);
	}

	@Override
	public boolean print(PDFContainer container, int count) {
		return print(container, count, null);
	}

	@Override
	public boolean print(PDFContainer container, int count, String args) {
		logger.debug("Dummy printer, printin...");
		return true;
	}

	@Override
	public boolean printTestPage() {
		logger.debug("Dummy printer, printin...");
		return true;
	}

	@Override
	public boolean print(PDFContainer container, String args) {
		logger.debug("Dummy printer, printin...");
		return true;
	}

}
