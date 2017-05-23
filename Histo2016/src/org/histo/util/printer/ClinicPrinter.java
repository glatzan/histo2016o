package org.histo.util.printer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;

import org.cups4j.CupsClient;
import org.cups4j.CupsPrinter;
import org.cups4j.PrintJob;
import org.histo.model.PDFContainer;
import org.histo.model.transitory.json.settings.PrinterSettings;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.Resource;

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

	/**
	 * Prints a pdfContainer
	 * 
	 * @param container
	 * @return
	 */
	public boolean print(PDFContainer container) {
		CupsClient cupsClient;
		try {
			cupsClient = new CupsClient(settings.getCupsHost(), settings.getCupsPost());
			CupsPrinter printer = cupsClient.getPrinter(new URL(address));
			PrintJob printJob = new PrintJob.Builder(new ByteArrayInputStream(container.getData())).build();
			printer.print(printJob);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean printTestPage() {
		ClassPathXmlApplicationContext appContext = new ClassPathXmlApplicationContext();
		Resource resource = appContext.getResource(settings.getTestPage());
		try {
			print(new File(resource.getURI()));
			System.out.println(resource.getURI());
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

	// ************************ Getter/Setter ************************

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
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
