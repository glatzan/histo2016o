package org.histo.config;


import org.histo.util.FileUtil;

import com.google.gson.Gson;


public class HistoSettings {

	public static final String PDF_TEMPLATE_JSON = "classpath:templates/template.json";
	public static final String TEXT_TEMPLATE_JSON = "classpath:templates/text.json";

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
		HistoSettings result = gson.fromJson(FileUtil.loadTextFile(TEXT_TEMPLATE_JSON), HistoSettings.class);
		return result;
	}
	
	private String emailDefualtTextReportFinished;
	
	private String emailDefaultSubjectReportFinished;

	public String getEmailDefualtTextReportFinished() {
		return emailDefualtTextReportFinished;
	}

	public String getEmailDefaultSubjectReportFinished() {
		return emailDefaultSubjectReportFinished;
	}

	public void setEmailDefualtTextReportFinished(String emailDefualtTextReportFinished) {
		this.emailDefualtTextReportFinished = emailDefualtTextReportFinished;
	}

	public void setEmailDefaultSubjectReportFinished(String emailDefaultSubjectReportFinished) {
		this.emailDefaultSubjectReportFinished = emailDefaultSubjectReportFinished;
	}
}
