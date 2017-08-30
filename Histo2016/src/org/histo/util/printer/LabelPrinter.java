package org.histo.util.printer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.histo.config.enums.DocumentType;
import org.histo.model.patient.Slide;
import org.histo.template.DocumentTemplate;
import org.histo.util.HistoUtil;

import com.google.gson.annotations.Expose;

/**
 * Zebra AbstractPrinter with ftp printing function. Buffer can be filled
 * without opening a connection with the printer.
 * 
 * @author andi
 *
 */
public class LabelPrinter extends AbstractPrinter {

	/**
	 * Default name of the ftp uploaded file. Should contain %counter% in order
	 * to print multiple files.
	 */
	@Expose
	private String fileName;

	@Expose
	private int timeout;

	/**
	 * Saves the current number of printed files.
	 */
	private int fileNameCounter;

	/**
	 * In order to print faster only one
	 */
	private HashMap<String, String> printBuffer;

	/**
	 * Connection Object for ftp connection
	 */
	private FTPClient connection;

	/**
	 * Default constructor
	 */
	public LabelPrinter() {
		printBuffer = new HashMap<String, String>();
	}

	/**
	 * Adds a zpl command to the buffer
	 * 
	 * @param toPrint
	 */
	public void addTaskToBuffer(String toPrint) {
		printBuffer.put(fileName.replace("%count%", String.valueOf(++fileNameCounter)), toPrint);
	}

	public final void print(DocumentTemplate printTemplate, Slide slide, String date) {
		String taskID = slide.getTask().getTaskID();

		logger.debug("Using printer " + getName());

		printTemplate.prepareTemplate();
		
		String toPrint = printTemplate.getFileContent();

		HashMap<String, String> args = new HashMap<String, String>();
		args.put("%slideNumber%", taskID + HistoUtil.fitString(slide.getUniqueIDinBlock(), 3, '0'));
		args.put("%slideName%", taskID + " " + slide.getSlideID());
		args.put("%slideText%", slide.getCommentary());
		args.put("%date%", date);

		addTaskToBuffer(HistoUtil.replaceWildcardsInString(toPrint, args));

	}

	public boolean printTestPage() {

		DocumentTemplate test = DocumentTemplate
				.getDefaultTemplate(DocumentTemplate.getTemplates(DocumentType.TEST_LABLE));

		test.prepareTemplate();
		
		String toPrint = test.getFileContent();

		if (toPrint == null)
			return false;

		logger.debug("Printing Testpage an flushing, printer " + getFileName());
		addTaskToBuffer(toPrint);
		flushPrints();
		return true;
	}

	/**
	 * Flushes all prints of the given printer
	 * 
	 * @param printer
	 * @return
	 */
	public boolean flushPrints() {
		if (isBufferNotEmpty()) {
			try {
				openConnection();
				flushBuffer();
				closeConnection();
			} catch (IOException e) {
				logger.error(e);
			}
			logger.debug("Flushing prints of printer " + getName());
			return true;
		} else
			logger.debug("Nothing in buffer of printer " + getName());

		return false;
	}

	/**
	 * Print the whole buffer and clears it if successful.
	 * 
	 * @return
	 * @throws IOException
	 */
	public boolean flushBuffer() throws IOException {
		boolean result = true;

		for (Map.Entry<String, String> entry : printBuffer.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();
			if (!print(value, key)) {
				result = false;
			}
		}

		printBuffer.clear();

		return result;
	}

	/**
	 * Sends a file to the zpl printer
	 * 
	 * @param content
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public boolean print(String content, String file) throws IOException {
		InputStream stream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));

		if (connection.storeFile(file, stream)) {
			logger.debug("Upload " + file + ", ok");
			return true;
		} else {
			logger.debug("Upload " + file + ", failed");
			return false;
		}
	}

	/**
	 * Opens a ftp connection to the zpl printer
	 * 
	 * @throws SocketException
	 * @throws IOException
	 */
	public void openConnection() throws SocketException, IOException {
		connection = new FTPClient();

		logger.debug("Connecting to label printer ftp://" + address + ":" + port);

		connection.setConnectTimeout(getTimeout());
		connection.connect(address, Integer.valueOf(getPort()));
		connection.login(userName, password);
		connection.setFileType(FTP.ASCII_FILE_TYPE);
	}

	/**
	 * closes the conncteion with the zpl printer
	 * 
	 * @throws IOException
	 */
	public void closeConnection() throws IOException {
		connection.logout();
		connection.disconnect();
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/

	/**
	 * Returns true if the butter is not empty
	 * 
	 * @return
	 */
	public boolean isBufferNotEmpty() {
		return !printBuffer.isEmpty();
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/

}
