package org.histo.config;

import java.io.IOException;
import java.net.URI;
import java.net.URL;

import org.apache.log4j.Logger;
import org.histo.model.transitory.json.ClinicJsonHandler;
import org.histo.model.transitory.json.ClinicPrinterManager;
import org.histo.model.transitory.json.LabelPrinter;
import org.histo.model.transitory.json.LabelPrinterManager;
import org.histo.model.transitory.json.LdapHandler;
import org.histo.model.transitory.json.MailHandler;
import org.histo.util.HistoUtil;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.Resource;

import com.google.gson.Gson;

public class HistoSettings {

	private static Logger logger = Logger.getLogger("org.histo");

	public static final String DEFAULT_SETTINGS_JSON = "classpath:settings/settings.json";
	public static final String TEX_TEMPLATE_JSON = "classpath:settings/printTempaltes.json";
	public static final String LABEL_PRINTER_JSON = "classpath:settings/labelPrinter.json";
	public static final String VERSION_JSON = "classpath:settings/version.json";
	
	
	public static final String PDF_TEMPLATE_JSON = "classpath:templates/template.json";
	
	public static final String HISTO_BASE_URL = "/Histo2016";
	public static final String HISTO_LOGIN_PAGE = "/login.xhtml";

	// http://auginfo/piz?piz=xx
	// http://auginfo/piz?name=xx&vorname=xx&geburtsdatum=2000-01-01

	public static final HistoSettings factory() {
		Gson gson = new Gson();

		logger.debug("Creating Settings Object ");

		HistoSettings result = gson.fromJson(HistoUtil.loadTextFile(DEFAULT_SETTINGS_JSON), HistoSettings.class);
		
		// init printers
		result.getPrinterManager().initPrinters();
		result.getLabelPrinterManager().initPrinters();
		
		return result;
	}

	/**
	 * Directory for creating pdfs
	 */
	private  String workingDirectory;

	/**
	 * The current version of the program
	 */
	private String currentVersion;
	
	/**
	 * Handles clinic printers
	 */
	private ClinicPrinterManager printerManager;

	/**
	 * Handles all labelPrinters
	 */
	private LabelPrinterManager labelPrinterManager;
	
	/**
	 * Object for sending mails via clini backend
	 */
	private MailHandler mail;

	/**
	 * Obejct for ldap communication with clinic backend
	 */
	private LdapHandler ldap;

	/**
	 * Object for handeling clinic backend requsts for patient data
	 */
	private ClinicJsonHandler clinicJsonHandler;

	/**
	 * List of mail to whom unlock requests will be send
	 */
	private String[] adminMails;

	/**
	 * List of mail to whom unlock requests will be send
	 */
	private String[] errorMails;

	/**
	 * Default layout of the slides labels, contains %slideNumber%, slideText%
	 * %slideName% and %date% as wildcards
	 */
	private String defaultSlideLableLayout;


	public static final URI getAbsolutePath(String path) {
		ClassPathXmlApplicationContext appContext = new ClassPathXmlApplicationContext();
		Resource resource = appContext.getResource(path);
		URI result = null;
		try {
			result = resource.getURI();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			appContext.close();
		}

		return result;
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/

	public String getDefaultSlideLableLayout() {
		return defaultSlideLableLayout;
	}

	public void setDefaultSlideLableLayout(String defaultSlideLableLayout) {
		this.defaultSlideLableLayout = defaultSlideLableLayout;
	}

	public MailHandler getMail() {
		return mail;
	}

	public void setMail(MailHandler mail) {
		this.mail = mail;
	}

	public LdapHandler getLdap() {
		return ldap;
	}

	public void setLdap(LdapHandler ldap) {
		this.ldap = ldap;
	}

	public String[] getAdminMails() {
		return adminMails;
	}

	public void setAdminMails(String[] adminMails) {
		this.adminMails = adminMails;
	}

	public ClinicJsonHandler getClinicJsonHandler() {
		return clinicJsonHandler;
	}

	public void setClinicJsonHandler(ClinicJsonHandler clinicJsonHandler) {
		this.clinicJsonHandler = clinicJsonHandler;
	}

	public String[] getErrorMails() {
		return errorMails;
	}

	public void setErrorMails(String[] errorMails) {
		this.errorMails = errorMails;
	}

	public ClinicPrinterManager getPrinterManager() {
		return printerManager;
	}

	public void setPrinterManager(ClinicPrinterManager printerManager) {
		this.printerManager = printerManager;
	}

	public String getWorkingDirectory() {
		return workingDirectory;
	}
	
	public String getCurrentVersion() {
		return currentVersion;
	}

	public LabelPrinterManager getLabelPrinterManager() {
		return labelPrinterManager;
	}

	public void setLabelPrinterManager(LabelPrinterManager labelPrinterManager) {
		this.labelPrinterManager = labelPrinterManager;
	}

	

	/********************************************************
	 * Getter/Setter
	 ********************************************************/

}
