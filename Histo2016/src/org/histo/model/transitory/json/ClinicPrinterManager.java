package org.histo.model.transitory.json;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.cups4j.CupsClient;
import org.cups4j.CupsPrinter;
import org.cups4j.PrintJob;
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

	private List<ClinicPrinter> printers;

	private ClinicPrinter selectedPrinter;

	public final void print(File file) {
		CupsClient cupsClient;
		try {
			cupsClient = new CupsClient(host, port);
			CupsPrinter printer = cupsClient.getPrinter(selectedPrinter.getPrinterURL());
			PrintJob printJob = new PrintJob.Builder(new FileInputStream(file)).build();
			printer.print(printJob);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public final void printTestPage() {
		ClassPathXmlApplicationContext appContext = new ClassPathXmlApplicationContext();
		Resource resource = appContext.getResource(testPage);
		try {
			print(new File(resource.getURI()));
			System.out.println(resource.getURI());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		print(new File(
//				"D:\\Dropbox\\Projekte\\Histo2016offline\\Histo2016\\WebContent\\resources\\templates\\testPage.pdf"));
	}

	public final boolean initPrinters() {
		CupsClient cupsClient;
		try {
			cupsClient = new CupsClient(host, port);
			List<CupsPrinter> cupsPrinter = cupsClient.getPrinters();
			printers = new ArrayList<ClinicPrinter>();

			// transformin into clinicprinters
			for (CupsPrinter p : cupsPrinter) {
				printers.add(new ClinicPrinter(p));
			}

			// setting default printer
			if (printers != null && printers.size() > 1) {
				setSelectedPrinter(printers.get(0));
			}

			logger.debug("Printers init successful");

			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
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

	public ClinicPrinter getSelectedPrinter() {
		return selectedPrinter;
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

	public void setSelectedPrinter(ClinicPrinter selectedPrinter) {
		this.selectedPrinter = selectedPrinter;
	}

}