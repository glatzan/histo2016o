package org.histo.adaptors.printer;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.cups4j.CupsClient;
import org.cups4j.CupsPrinter;
import org.histo.action.handler.GlobalSettings;
import org.histo.model.transitory.settings.DefaultDocuments;
import org.histo.model.transitory.settings.PrinterSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Configurable
public class CupsPrinterLoader {

	private static Logger logger = Logger.getLogger("org.histo");

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private GlobalSettings globalSettings;

	public List<ClinicPrinter> loadCupsPrinters(PrinterSettings settings, DefaultDocuments defaultDocuments) {
		ArrayList<ClinicPrinter> result = new ArrayList<>();
		CupsClient cupsClient;

		if (!globalSettings.getProgramSettings().isOffline()) {
			try {
				cupsClient = new CupsClient(settings.getCupsHost(), settings.getCupsPost());
				List<CupsPrinter> cupsPrinter = cupsClient.getPrinters();
				// transformin into clinicprinters
				for (CupsPrinter p : cupsPrinter) {
					result.add(new ClinicPrinter(p, settings, defaultDocuments));
				}

			} catch (Exception e) {
				logger.error("Retriving printers failed" + e);
				e.printStackTrace();
			}
		}

		result.add(0, new ClinicPrinterDummy());

		return result;
	}
}
