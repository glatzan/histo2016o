package org.histo.adaptors.printer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.cups4j.CupsClient;
import org.cups4j.CupsPrinter;
import org.cups4j.PrintJob;
import org.histo.model.PDFContainer;
import org.histo.model.transitory.settings.PrinterSettings;
import org.histo.util.HistoUtil;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.Resource;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClinicPrinter extends AbstractPrinter {

	protected PrinterSettings settings;

	public ClinicPrinter() {
	}

	public ClinicPrinter(int id, CupsPrinter cupsPrinter, PrinterSettings settings) {
		this.id = id;
		this.address = cupsPrinter.getPrinterURL().toString();
		this.name = cupsPrinter.getName();
		this.description = cupsPrinter.getDescription();
		this.location = cupsPrinter.getLocation();
		this.settings = settings;
	}

	/**
	 * Prints a file
	 * 
	 * @param cPrinter
	 * @param file
	 */
	public boolean print(File file) {
		CupsClient cupsClient;
		try {
			cupsClient = new CupsClient(settings.getCupsHost(), settings.getCupsPost());
			CupsPrinter printer = cupsClient.getPrinter(new URL(address));
			PrintJob printJob = new PrintJob.Builder(new FileInputStream(file)).build();
			printer.print(printJob);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean print(PDFContainer container) {
		return print(container, null);
	}

	public boolean print(PDFContainer container, int count) {
		return print(container, count, null);
	}

	public boolean print(PDFContainer container, String args) {
		return print(container, 1, args);
	}

	public boolean print(PDFContainer container, int count, String args) {
		logger.debug("Printing xtimes: " + count);
		System.out.println("-------------- duplexys");
		int i = 0;
		boolean result = true;
		while (i < count) {
			logger.debug("Printing " + i);
			CupsClient cupsClient;
			try {
				cupsClient = new CupsClient(settings.getCupsHost(), settings.getCupsPost());
				CupsPrinter printer = cupsClient.getPrinter(new URL(address));

				PrintJob printJob = new PrintJob.Builder(new ByteArrayInputStream(container.getData())).duplex(false)
						.build();

				// args= "sides:keyword:two-sided-long-edge"; duplex
				// args= "d";

				if (HistoUtil.isNotNullOrEmpty(args)) {

					Map<String, String> attribute = new HashMap<String, String>();
					attribute.put("job-attributes", args);

					printJob.setAttributes(attribute);
					logger.debug("Printig with args: " + args);
				}

				printer.print(printJob);

			} catch (Exception e) {
				e.printStackTrace();
				result = false;
			}
			i++;
		}
		return result;
	}

	public boolean printTestPage() {
		ClassPathXmlApplicationContext appContext = new ClassPathXmlApplicationContext();
		Resource resource = appContext.getResource(settings.getTestPage());
		try {
			print(new File(resource.getURI()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			appContext.close();
		}
		return true;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ClinicPrinter && ((ClinicPrinter) obj).getName().equals(name))
			return true;

		return super.equals(obj);
	}
}
