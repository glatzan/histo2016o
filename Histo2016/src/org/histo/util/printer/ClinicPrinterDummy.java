package org.histo.util.printer;

import java.io.File;

import org.histo.model.PDFContainer;

public class ClinicPrinterDummy extends ClinicPrinter {

	private static final long serialVersionUID = 3965169880380400939L;

	public ClinicPrinterDummy(long id) {
		this.name = "Dummy AbstractPrinter";
		this.id = id;
	}

	@Override
	public boolean print(File file) {
		logger.debug("Dummy printer, printin...");
		return true;
	}

	@Override
	public boolean print(PDFContainer container) {
		logger.debug("Dummy printer, printin...");
		return true;
	}

	@Override
	public boolean printTestPage() {
		logger.debug("Dummy printer, printin...");
		return true;
	}

}
