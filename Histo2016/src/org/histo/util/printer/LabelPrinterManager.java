package org.histo.util.printer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.histo.config.HistoSettings;
import org.histo.config.enums.DocumentType;
import org.histo.model.patient.Slide;
import org.histo.util.HistoUtil;

public class LabelPrinterManager {

	private static Logger logger = Logger.getLogger("org.histo");

	private String testPage;

	private List<LabelPrinter> printers;
	
	private LabelPrinter printerToUse;

	public final boolean loadPrinter(String name){
		setPrinterToUse(getPrinterByUuid(name));
		return getPrinterToUse() == null ? false : true;
	}

	public final void print(PrintTemplate printTemplate, Slide slide, String date) {
		String taskID = slide.getTask().getTaskID();

		logger.debug("Using printer " + getPrinterToUse().getName());

		String toPrint = printTemplate.getContentOfFile();

		HashMap<String, String> args = new HashMap<String, String>();
		args.put("%slideNumber%", taskID + HistoUtil.fitString(slide.getUniqueIDinBlock(), 2, '0'));
		args.put("%slideName%", taskID + " " + slide.getSlideID());
		args.put("%slideText%", slide.getCommentary());
		args.put("%date%", date);

		getPrinterToUse().addTaskToBuffer(HistoUtil.replaceWildcardsInString(toPrint, args));

	}


	public final void printTestPage() {

		PrintTemplate test = PrintTemplate.getDefaultTemplate(PrintTemplate.getTemplatesByType(DocumentType.TEST_LABLE));
		
		String toPrint = test.getContentOfFile();

		if (toPrint == null)
			return;

		logger.debug("Printing Testpage an flushing, printer " + getPrinterToUse().getFileName());
		getPrinterToUse().addTaskToBuffer(toPrint);
		flushPrints();
	}

	/**
	 * Flushes all prints of the given printer
	 * 
	 * @param printer
	 * @return
	 */
	public boolean flushPrints() {
		if (getPrinterToUse().isBufferNotEmpty()) {
			try {
				getPrinterToUse().openConnection();
				getPrinterToUse().flushBuffer();
				getPrinterToUse().closeConnection();
			} catch (IOException e) {
				logger.error(e);
			}
			logger.debug("Flushing prints of printer " + getPrinterToUse().getName());
			return true;
		} else
			logger.debug("Nothing in buffer of printer " + getPrinterToUse().getName());

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
			if (labelPrinter.getName().equals(name)){
				logger.debug("Printer found, " + name);
				return labelPrinter;
			}
		}
		logger.debug("No printer found for " + name);
		return null;
	}

	/**
	 * Initializes all labelPrinter from json config file
	 * 
	 * @return
	 */
	public final boolean loadFtpPrinters() {
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

	public LabelPrinter getPrinterToUse() {
		return printerToUse;
	}

	public void setPrinterToUse(LabelPrinter printerToUse) {
		this.printerToUse = printerToUse;
	}

	
}
