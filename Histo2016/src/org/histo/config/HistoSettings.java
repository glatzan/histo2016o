package org.histo.config;

import java.io.IOException;
import java.net.URI;

import org.apache.log4j.Logger;
import org.histo.action.MainHandlerAction;
import org.histo.model.transitory.mail.MailHandler;
import org.histo.util.interfaces.FileHandlerUtil;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.Resource;

import com.google.gson.Gson;

public class HistoSettings {

	private static Logger logger = Logger.getLogger("org.histo");

	public static final String DEFAULT_SETTINGS_JSON = "classpath:settings/settings.json";
	public static final String TEX_TEMPLATE_JSON = "classpath:settings/printTempaltes.json";
	public static final String MAIL_TEMPLATE_JSON = "classpath:settings/mailTemplates.json";
	public static final String LABEL_PRINTER_JSON = "classpath:settings/labelPrinter.json";

	public static final String PDF_TEMPLATE_JSON = "classpath:templates/template.json";

	public static final String HISTO_BASE_URL = "/Histo2016";
	public static final String HISTO_LOGIN_PAGE = "/login.xhtml";

	// http://auginfo/piz?piz=xx
	// http://auginfo/piz?name=xx&vorname=xx&geburtsdatum=2000-01-01

	public static final HistoSettings factory() {
		return factory(null);
	}
	
	public static final HistoSettings factory(MainHandlerAction mainHandlerAction) {
		Gson gson = new Gson();

		logger.debug("Creating Settings Object ");

		HistoSettings result = gson.fromJson(FileHandlerUtil.getContentOfFile(DEFAULT_SETTINGS_JSON), HistoSettings.class);

		return result;
	}

	/**
	 * Object for sending mails via clini backend
	 */
	private MailHandler mail;

	/**
	 * List of mail to whom unlock requests will be send
	 */
	private String[] adminMails;

	/**
	 * List of mail to whom unlock requests will be send
	 */
	private String[] errorMails;

	public static final URI getAbsolutePath(String path) {
		ClassPathXmlApplicationContext appContext = new ClassPathXmlApplicationContext();
		Resource resource = appContext.getResource(path);
		System.out.println(resource);
		URI result = null;
		try {
			result = resource.getURI();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			appContext.close();
		}

		return result;
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/

	public MailHandler getMail() {
		return mail;
	}

	public void setMail(MailHandler mail) {
		this.mail = mail;
	}

	public String[] getAdminMails() {
		return adminMails;
	}

	public void setAdminMails(String[] adminMails) {
		this.adminMails = adminMails;
	}

	public String[] getErrorMails() {
		return errorMails;
	}

	public void setErrorMails(String[] errorMails) {
		this.errorMails = errorMails;
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/

}
