package org.histo.adaptors.printer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.histo.action.handler.GlobalSettings;
import org.histo.config.exception.CustomUserNotificationExcepetion;
import org.histo.template.DocumentTemplate;
import org.histo.template.documents.SlideLable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.google.gson.annotations.Expose;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * Zebra AbstractPrinter with ftp printing function. Buffer can be filled
 * without opening a connection with the printer.
 * 
 * @author andi
 *
 */
@Getter
@Setter
@Configurable
public class LabelPrinter extends AbstractPrinter {

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private GlobalSettings globalSettings;

	/**
	 * Default name of the ftp uploaded file. Should contain %counter% in order to
	 * print multiple files.
	 */

	@Expose
	private int timeout;

	/**
	 * Default constructor
	 */
	public LabelPrinter() {
		// printBuffer = new HashMap<String, String>();
	}

	public void print(SlideLable tempalte) throws CustomUserNotificationExcepetion {
		List<SlideLable> toPrint = new ArrayList<SlideLable>();
		toPrint.add(tempalte);
		print(toPrint);
	}

	public void print(List<SlideLable> tempaltes) throws CustomUserNotificationExcepetion {

		try {
			FTPClient connection = openConnection();
			for (SlideLable documentTemplate : tempaltes) {
				print(connection, documentTemplate.getFileContent(), generateUnqiueName(6));
			}
			closeConnection(connection);
		} catch (SocketTimeoutException e) {
			throw new CustomUserNotificationExcepetion("growl.error", "growl.error.priter.timeout");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public boolean printTestPage() {

		SlideLable label = DocumentTemplate
				.getTemplateByID(globalSettings.getDefaultDocuments().getSlideLableTestDocument());

		label.prepareTemplate();
		
		String toPrint = label.getFileContent();

		if (toPrint == null)
			return false;

		try {
			FTPClient connection = openConnection();
			print(connection, toPrint, generateUnqiueName(6));
			closeConnection(connection);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return true;
	}

	/**
	 * Sends a file to the zpl printer
	 * 
	 * @param content
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public boolean print(FTPClient connection, String content, String file) throws IOException {
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
	public FTPClient openConnection() throws SocketException, IOException {
		FTPClient connection = new FTPClient();

		logger.debug("Connecting to label printer ftp://" + address + ":" + port);

		connection.setConnectTimeout(getTimeout());
		connection.connect(address, Integer.valueOf(getPort()));
		connection.login(userName, password);
		connection.setFileType(FTP.ASCII_FILE_TYPE);

		return connection;
	}

	/**
	 * closes the conncteion with the zpl printer
	 * 
	 * @throws IOException
	 */
	public void closeConnection(FTPClient connection) throws IOException {
		connection.logout();
		connection.disconnect();
	}

	public static String generateUnqiueName(int length) {
		return RandomStringUtils.randomAlphanumeric(length) + ".zpl";
	}
}
