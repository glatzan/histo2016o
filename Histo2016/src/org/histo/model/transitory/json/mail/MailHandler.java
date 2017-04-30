package org.histo.model.transitory.json.mail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import javax.activation.DataSource;
import javax.mail.util.ByteArrayDataSource;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.MultiPartEmail;
import org.apache.commons.mail.SimpleEmail;
import org.apache.log4j.Logger;
import org.histo.config.HistoSettings;
import org.histo.config.enums.MailType;
import org.histo.model.PDFContainer;
import org.histo.model.interfaces.GsonAble;
import org.histo.util.HistoUtil;

public class MailHandler implements GsonAble {

	private static Logger logger = Logger.getLogger("org.histo");

	private String server;

	private int port;

	private boolean ssl;

	private boolean debug;

	private String systemMail;

	private String systemName;

	public final boolean sendTempalteMail(String to[], MailType preset, HashMap<String, String> subjectReplace,
			HashMap<String, String> contentReplace) {

		boolean result = true;
		for (int i = 0; i < to.length; i++) {
			if (!sendTempalteMail(to[i], preset, subjectReplace, contentReplace))
				result = false;
		}

		return result;
	}

	public final boolean sendTempalteMail(String to, MailType preset, HashMap<String, String> subjectReplace,
			HashMap<String, String> contentReplace) {

		MailTemplate template = MailTemplate.factroy(HistoSettings.MAIL_TEMPLATE_JSON, preset);
		
		if (template == null) {
			logger.debug("No TemplateUtil found for " + preset.toString());
			return false;
		}

		logger.debug("TemplateUtil found for " + preset.toString());

		// replacing wildcards if hashmap is provided
		String subject = subjectReplace != null
				? HistoUtil.replaceWildcardsInString(template.getSubject(), subjectReplace) : template.getSubject();

		String content = contentReplace != null
				? HistoUtil.replaceWildcardsInString(template.getContent(), contentReplace) : template.getContent();

		return sendMailFromSystem(to, subject, content);
	}

	/**
	 * Sends a mail with the standard sender to several addresses
	 * 
	 * @param to
	 * @param subject
	 * @param text
	 * @return
	 */
	public final boolean sendMailFromSystem(String to[], String subject, String text) {
		return sendMail(to, getSystemMail(), getSystemName(), subject, text);
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
	public final boolean sendMailFromSystem(String to, String subject, String text) {
		return sendMail(to, getSystemMail(), getSystemName(), subject, text);
	}

	/**
	 * Sends a pdf mail, using the system's adress and name
	 * 
	 * @param to
	 * @param subject
	 * @param text
	 * @return
	 */
	public final boolean sendMailFromSystem(String to, String subject, String text, PDFContainer container) {
		return sendMail(to, getSystemMail(), getSystemName(), subject, text, container);
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

	public String getSystemMail() {
		return systemMail;
	}

	public void setSystemMail(String systemMail) {
		this.systemMail = systemMail;
	}

	public String getSystemName() {
		return systemName;
	}

	public void setSystemName(String systemName) {
		this.systemName = systemName;
	}
	/********************************************************
	 * Getter/Setter
	 ********************************************************/

}
