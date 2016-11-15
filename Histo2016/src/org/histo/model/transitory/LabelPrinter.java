package org.histo.model.transitory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.log4j.Logger;
import org.histo.model.interfaces.GsonAble;
import org.histo.util.FileUtil;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.reflect.TypeToken;

public class LabelPrinter implements GsonAble {

	private static Logger logger = Logger.getLogger(LabelPrinter.class);

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
	 * Default name of the ftp uploaded file.
	 */
	@Expose
	private String fileName;

	/**
	 * Task a zpl command string with wildcards and a hash map to replace them.
	 * Then send the zpl command as ftp file to the printer defined within this
	 * object.
	 * 
	 * @param toPrint
	 * @param args
	 * @return
	 */
	public boolean printViaFtp(String toPrint, HashMap<String, String> args) {
		return printViaFtp(FileUtil.replaceWildcardsInString(toPrint, args), fileName);
	}

	/**
	 * Prints slideLabes takes a string with zpl commands and a filename and
	 * uploads these data to the printer defined within this object.
	 * 
	 * @param toPrint
	 * @return
	 */
	public boolean printViaFtp(String toPrint, String filename) {
		boolean result = false;

		try {

			FTPClient f = new FTPClient();

			logger.debug("Connecting to label printer ftp://" + ip + ":" + String.valueOf(port));

			f.connect(ip);
			f.login(userName, password);
			f.setFileType(FTP.ASCII_FILE_TYPE);

			InputStream stream = new ByteArrayInputStream(toPrint.getBytes(StandardCharsets.UTF_8));

			if (f.storeFile(filename, stream)) {
				logger.debug("Upload " + filename + ", ok");
				result = true;
			} else {
				logger.debug("Upload " + filename + ", ok");
			}

			f.logout();
			f.disconnect();

			logger.debug("Disconnected");

		} catch (Exception e) {
			e.printStackTrace();
		}

		return result;
	}

	public static final LabelPrinter[] factroy(String jsonFile) {

		Type type = new TypeToken<LabelPrinter[]>() {
		}.getType();

		Gson gson = new Gson();
		LabelPrinter[] result = gson.fromJson(FileUtil.loadTextFile(jsonFile), type);

		logger.debug("Created label printer list with " + result.length + " printern");

		return result;
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/

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

	/********************************************************
	 * Getter/Setter
	 ********************************************************/

}
