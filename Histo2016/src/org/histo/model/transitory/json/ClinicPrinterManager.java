package org.histo.model.transitory.json;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.cups4j.CupsClient;
import org.cups4j.CupsPrinter;
import org.cups4j.PrintJob;
import org.histo.model.PDFContainer;
import org.histo.model.interfaces.GsonAble;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.Resource;

import com.google.gson.annotations.Expose;

public class ClinicPrinterManager implements GsonAble {

	private static Logger logger = Logger.getLogger("org.histo");

	@Expose
	private String host;

	@Expose
	private int port;

	@Expose
	private String testPage;

	@Expose
	private boolean offline;

	private List<ClinicPrinter> printers;

	private ClinicPrinter printerToUse;

	public final boolean loadPrinter(String name) {
		setPrinterToUse(getPrinterByName(name));
		return getPrinterToUse() == null ? false : true;
	}

	/**
	 * Prints a file
	 * 
	 * @param cPrinter
	 * @param file
	 */
	public final void print(File file) {
		if (!isOffline()) {
			CupsClient cupsClient;
			try {
				cupsClient = new CupsClient(host, port);
				CupsPrinter printer = cupsClient.getPrinter(getPrinterToUse().getPrinterURL());
				PrintJob printJob = new PrintJob.Builder(new FileInputStream(file)).build();
				printer.print(printJob);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			logger.debug("Is offline do nothing");
		}

	}

	public final void print(PDFContainer container) {
		if (!isOffline()) {
			CupsClient cupsClient;
			try {
				cupsClient = new CupsClient(host, port);
				CupsPrinter printer = cupsClient.getPrinter(getPrinterToUse().getPrinterURL());
				PrintJob printJob = new PrintJob.Builder(new ByteArrayInputStream(container.getData())).build();
				printer.print(printJob);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			logger.debug("Is offline do nothing");
		}
	}

	public final void printTestPage() {
		if (!isOffline()) {
			ClassPathXmlApplicationContext appContext = new ClassPathXmlApplicationContext();
			Resource resource = appContext.getResource(testPage);
			try {
				print(new File(resource.getURI()));
				System.out.println(resource.getURI());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				appContext.close();
			}
		} else {
			logger.debug("Is offline do nothing");
		}
	}

	public final boolean loadCupsPrinters() {
		CupsClient cupsClient;
		try {
			cupsClient = new CupsClient(host, port);
			List<CupsPrinter> cupsPrinter = cupsClient.getPrinters();
			printers = new ArrayList<ClinicPrinter>();

			// transformin into clinicprinters
			for (CupsPrinter p : cupsPrinter) {
				printers.add(new ClinicPrinter(p));
			}

			logger.debug("Printers init successful");

			return true;
		} catch (Exception e) {
			setOffline(true);
			logger.error("Printers failed");
			logger.error(e);
			return false;
		}
	}

	public ClinicPrinter getPrinterByName(String name) {
		for (ClinicPrinter clinicPrinter : printers) {
			if (clinicPrinter.getName().equals(name))
				return clinicPrinter;
		}
		return null;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public List<ClinicPrinter> getPrinters() {
		return printers;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setPrinters(List<ClinicPrinter> printers) {
		this.printers = printers;
	}

	public ClinicPrinter getPrinterToUse() {
		return printerToUse;
	}

	public void setPrinterToUse(ClinicPrinter printerToUse) {
		this.printerToUse = printerToUse;
	}

	public boolean isOffline() {
		return offline;
	}

	public void setOffline(boolean offline) {
		this.offline = offline;
	}
	
}