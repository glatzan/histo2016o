package org.histo.model.transitory.json;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.cups4j.CupsClient;
import org.cups4j.CupsPrinter;
import org.histo.config.HistoSettings;
import org.histo.model.patient.Slide;
import org.histo.ui.StainingTableChooser;
import org.histo.util.HistoUtil;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.Resource;

public class LabelPrinterManager {

	private static Logger logger = Logger.getLogger("org.histo");

	private String testPage;

	private List<LabelPrinter> printers;

	public final void print(String uuid, PrintTemplate printTemplate, Slide slide, String date) {
		print(getPrinterByUuid(uuid), printTemplate, slide, date);
	}

	public final void print(LabelPrinter printer, PrintTemplate printTemplate, Slide slide, String date) {
		String taskID = slide.getTask().getTaskID();

		ClassPathXmlApplicationContext appContext = new ClassPathXmlApplicationContext();
		Resource resource = appContext.getResource(testPage);

		HashMap<String, String> args = new HashMap<String, String>();
		args.put("%slideNumber%", taskID + HistoUtil.fitString(slide.getUniqueIDinBlock(), 2, '0'));
		args.put("%slideName%", taskID + " " + slide.getSlideID());
		args.put("%slideText%", slide.getCommentary());
		args.put("%date%", date);

		try {
			String toPrint = IOUtils.toString(resource.getInputStream(), "UTF-8");
			logger.debug("Printing Testpage an flushing, printer " + printer.getFileName());
			printer.addTaskToBuffer(HistoUtil.replaceWildcardsInString(toPrint, args));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			appContext.close();
		}

	}

	public final void printTestPage(String printer) {
		printTestPage(getPrinterByUuid(printer));
	}

	public final void printTestPage(LabelPrinter printer) {
		if(printer == null){
			logger.debug("No printer given");
			return;
		}
		ClassPathXmlApplicationContext appContext = new ClassPathXmlApplicationContext();
		Resource resource = appContext.getResource(testPage);

		try {
			String toPrint = IOUtils.toString(resource.getInputStream(), "UTF-8");
			logger.debug("Printing Testpage an flushing, printer " + printer.getFileName());
			printer.addTaskToBuffer(toPrint);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			appContext.close();
		}
	}

	/**
	 * Flushes all prints of the given printer
	 * 
	 * @param printer
	 * @return
	 */
	public boolean flushPrints(LabelPrinter printer) {
		if (printer.isBufferNotEmpty()) {
			try {
				printer.openConnection();
				printer.flushBuffer();
				printer.closeConnection();
			} catch (IOException e) {
				logger.error(e);
			}
			logger.debug("Flushing prints of printer " + printer.getName());
			return true;
		} else
			logger.debug("Nothing in buffer of printer " + printer.getName());

		return false;
	}

	/**
	 * Returns a labelPrinter matching the given uuid
	 * 
	 * @param uuid
	 * @return
	 */
	public LabelPrinter getPrinterByUuid(String name) {
		for (LabelPrinter labelPrinter : printers) {
			if (labelPrinter.getName().equals(name))
				return labelPrinter;
		}
		return null;
	}

	/**
	 * Initializes all labelPrinter from json config file
	 * 
	 * @return
	 */
	public final boolean initPrinters() {
		LabelPrinter[] printers = LabelPrinter.factroy(HistoSettings.LABEL_PRINTER_JSON);
		setPrinters(new ArrayList<LabelPrinter>(Arrays.asList(printers)));

		return true;
	}

	public List<LabelPrinter> getPrinters() {
		return printers;
	}

	public void setPrinters(List<LabelPrinter> printers) {
		this.printers = printers;
	}

}
