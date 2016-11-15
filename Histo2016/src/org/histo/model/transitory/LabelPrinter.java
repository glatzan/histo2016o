package org.histo.model.transitory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.log4j.Logger;
import org.histo.model.interfaces.GsonAble;
import org.histo.util.HistoUtil;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.reflect.TypeToken;

/**
 * Zebra Printer with ftp printing function. Buffer can be filled without
 * opening a connection with the printer.
 * 
 * @author andi
 *
 */
public class LabelPrinter implements GsonAble {

	private static Logger logger = Logger.getRootLogger();

	@Expose
	private String name;

	@Expose
	private String location;

	@Expose
	private String commentary;

	@Expose
	private String ip;

	@Expose
	private int port;

	@Expose
	private String userName;

	@Expose
	private String password;

	/**
	 * Default name of the ftp uploaded file. Should contain %counter% in order
	 * to print multiple files.
	 */
	@Expose
	private String fileName;

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
			} else
				printBuffer.remove(entry);
		}

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
			logger.debug("Upload " + file + ", ok");
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

		logger.debug("Connecting to label printer ftp://" + ip + ":" + String.valueOf(port));

		// TODO port
		connection.connect(ip);
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

	/**
	 * Loads printer from json
	 * 
	 * @param jsonFile
	 * @return
	 */
	public static final LabelPrinter[] factroy(String jsonFile) {

		Type type = new TypeToken<LabelPrinter[]>() {
		}.getType();

		Gson gson = new Gson();
		LabelPrinter[] result = gson.fromJson(HistoUtil.loadTextFile(jsonFile), type);

		logger.debug("Created label printer list with " + result.length + " printern");

		return result;
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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getCommentary() {
		return commentary;
	}

	public void setCommentary(String commentary) {
		this.commentary = commentary;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	/********************************************************
	 * Getter/Setter
	 ********************************************************/

}
