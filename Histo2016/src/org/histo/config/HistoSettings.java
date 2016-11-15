package org.histo.config;

import org.histo.util.HistoUtil;

import com.google.gson.Gson;

public class HistoSettings {

	public static final String PDF_TEMPLATE_JSON = "classpath:templates/template.json";
	public static final String DEFAULT_SETTINGS_JSON = "classpath:templates/settings.json";
	public static final String LABEL_PRINTER_JSON = "classpath:templates/labelPrinter.json";

	public static final String HISTO_BASE_URL = "/Histo2016";
	public static final String HISTO_LOGIN_PAGE = "/login.xhtml";

	public static final String EMAIL_SERVER = "smtp.ukl.uni-freiburg.de";
	public static final int EMAIL_PORT = 465;
	public static final boolean EMAIL_SSL = true;
	public static final boolean EMAIL_DEBUG = true;
	public static final String EMAIL_FROM = "augenklinik.histologie@uniklinik-freiburg.de";
	public static final String EMAIL_FROM_NAME = "Histologisches Labor der Augenklinik Freiburg";

	// http://auginfo/piz?piz=xx
	// http://auginfo/piz?name=xx&vorname=xx&geburtsdatum=2000-01-01
	public static final String PATIENT_GET_URL = "http://auginfo/piz";

	public static final String LDAP_HOST = "ldap.ukl.uni-freiburg.de";
	public static final String LDAP_PORT = "389";
	public static final String LDAP_SUFFIX = "dc=ukl,dc=uni-freiburg,dc=de";
	public static final String LDAP_BASE = "ou=people";

	public static final HistoSettings factory() {
		Gson gson = new Gson();
		HistoSettings result = gson.fromJson(HistoUtil.loadTextFile(DEFAULT_SETTINGS_JSON), HistoSettings.class);
		return result;
	}

	/**
	 * Default subject for report emails to physicians
	 */
	private String defaultReportEmailSubject;

	/**
	 * Default text for report emails to physicians
	 */
	private String defaultReportEmailText;

	/**
	 * Default layout of the slides labels, contains %slideNumber%, slideText%
	 * %slideName% and %date% as wildcards
	 */
	private String defaultSlideLableLayout;

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

	public String getDefaultSlideLableLayout() {
		return defaultSlideLableLayout;
	}

	public void setDefaultSlideLableLayout(String defaultSlideLableLayout) {
		this.defaultSlideLableLayout = defaultSlideLableLayout;
	}
}
