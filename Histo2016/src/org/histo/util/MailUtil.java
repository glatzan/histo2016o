package org.histo.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.activation.DataSource;
import javax.mail.util.ByteArrayDataSource;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.MultiPartEmail;
import org.apache.commons.mail.SimpleEmail;
import org.histo.config.HistoSettings;
import org.histo.model.PDFContainer;
import org.histo.ui.NotificationChooser;

public class MailUtil {

	public static final boolean sendMail(String to, String from, String fromName, String subject, String text) {
		SimpleEmail email = new SimpleEmail();

		email.setHostName(HistoSettings.EMAIL_SERVER);
		email.setDebug(HistoSettings.EMAIL_DEBUG);
		email.setSmtpPort(HistoSettings.EMAIL_PORT);
		email.setSSLOnConnect(HistoSettings.EMAIL_SSL);

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

	public static final boolean sendMail(String to, String from, String fromName, String subject, String text,
			PDFContainer container) {
		// Create the email message
		MultiPartEmail email = new MultiPartEmail();

		email.setHostName(HistoSettings.EMAIL_SERVER);
		email.setDebug(HistoSettings.EMAIL_DEBUG);
		email.setSmtpPort(HistoSettings.EMAIL_PORT);
		email.setSSLOnConnect(HistoSettings.EMAIL_SSL);

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

	public static final String replaceWildcardsInString(String text, HashMap<String, String> replace) {
		for (Map.Entry<String, String> entry : replace.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();
			text = text.replace(key, value);
		}
		return text;
	}
}
