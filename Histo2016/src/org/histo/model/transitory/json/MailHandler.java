package org.histo.model.transitory.json;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.activation.DataSource;
import javax.mail.util.ByteArrayDataSource;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.MultiPartEmail;
import org.apache.commons.mail.SimpleEmail;
import org.apache.log4j.Logger;
import org.histo.model.PDFContainer;
import org.histo.model.interfaces.GsonAble;

public class MailHandler implements GsonAble {

	private static Logger logger = Logger.getLogger("org.histo");

	private String server;

	private int port;

	private boolean ssl;

	private boolean debug;

	private String fromMail;

	private String fromName;

	private String defaultReportEmailSubject;

	private String defaultReportEmailText;

	/**
	 * Sends a mail with the standard sender to several addresses
	 * 
	 * @param to
	 * @param subject
	 * @param text
	 * @return
	 */
	public final boolean sendMail(String to[], String subject, String text) {
		return sendMail(to, getFromMail(), getFromName(), subject, text);
	}

	/**
	 * Sends plain text mails to several addresses
	 * 
	 * @param to
	 * @param from
	 * @param fromName
	 * @param subject
	 * @param text
	 * @return
	 */
	public final boolean sendMail(String to[], String from, String fromName, String subject, String text) {
		boolean success = true;

		for (int i = 0; i < to.length; i++) {
			if (!sendMail(to[i], from, from, subject, text))
				success = false;
		}

		return success;
	}

	/**
	 * Sends a plain text mail, using the standard sender mail address to on
	 * email address
	 * 
	 * @param to
	 * @param subject
	 * @param text
	 * @return
	 */
	public final boolean sendMail(String to, String subject, String text) {
		return sendMail(to, getFromMail(), getFromName(), subject, text);
	}

	/**
	 * Sends plain text mails to one email address
	 * 
	 * @param to
	 * @param from
	 * @param fromName
	 * @param subject
	 * @param text
	 * @return
	 */
	public final boolean sendMail(String to, String from, String fromName, String subject, String text) {
		SimpleEmail email = new SimpleEmail();

		email.setHostName(getServer());
		email.setDebug(isDebug());
		email.setSmtpPort(getPort());
		email.setSSLOnConnect(isSsl());

		try {
			email.addTo(to);
			email.setFrom(from, fromName);
			email.setSubject(subject);
			email.setMsg(text);
			email.send();
		} catch (EmailException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Sends a mail with an pdf attachment to one mail address
	 * 
	 * @param to
	 * @param from
	 * @param fromName
	 * @param subject
	 * @param text
	 * @param container
	 * @return
	 */
	public final boolean sendMail(String to, String from, String fromName, String subject, String text,
			PDFContainer container) {
		// Create the email message
		MultiPartEmail email = new MultiPartEmail();

		email.setHostName(getServer());
		email.setDebug(isDebug());
		email.setSmtpPort(getPort());
		email.setSSLOnConnect(isSsl());

		try {
			email.addTo(to);
			email.setFrom(from, fromName);
			email.setSubject(subject);
			email.setMsg(text);

			InputStream is = new ByteArrayInputStream(container.getData());
			DataSource source = new ByteArrayDataSource(is, "application/pdf");

			// add the attachment
			email.attach(source, container.getName(), "");

			// send the email
			email.send();
		} catch (EmailException | IOException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/
	public String getServer() {
		return server;
	}

	public void setServer(String server) {
		this.server = server;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public boolean isSsl() {
		return ssl;
	}

	public void setSsl(boolean ssl) {
		this.ssl = ssl;
	}

	public boolean isDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public String getFromMail() {
		return fromMail;
	}

	public void setFromMail(String fromMail) {
		this.fromMail = fromMail;
	}

	public String getFromName() {
		return fromName;
	}

	public void setFromName(String fromName) {
		this.fromName = fromName;
	}

	public String getDefaultReportEmailSubject() {
		return defaultReportEmailSubject;
	}

	public void setDefaultReportEmailSubject(String defaultReportEmailSubject) {
		this.defaultReportEmailSubject = defaultReportEmailSubject;
	}

	public String getDefaultReportEmailText() {
		return defaultReportEmailText;
	}

	public void setDefaultReportEmailText(String defaultReportEmailText) {
		this.defaultReportEmailText = defaultReportEmailText;
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/

}
