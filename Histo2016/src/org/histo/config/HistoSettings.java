package org.histo.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class HistoSettings {

	public static final String PDF_TEMPLATE_JSON = "classpath:templates/template.json";

	public static final String HISTO_BASE_URL = "/Histo2016";
	public static final String HISTO_LOGIN_PAGE = "/login.xhtml";

	public static final String STANDARD_DATEFORMAT_DAY_ONLY = "dd MMM yyyy";
	public static final String STANDARD_DATEFORMAT = "HH:mm:ss dd.MM.yyyy";

	public static final String EMAIL_SERVER = "smtp.ukl.uni-freiburg.de";
	public static final String EMAIL_PORT = "smtp.ukl.uni-freiburg.de";

	// http://auginfo/piz?piz=xx
	// http://auginfo/piz?name=xx&vorname=xx&geburtsdatum=2000-01-01
	public static final String PATIENT_GET_URL = "http://auginfo/piz";

	public static final String LDAP_HOST = "ldap.ukl.uni-freiburg.de";
	public static final String LDAP_PORT = "389";
	public static final String LDAP_SUFFIX = "dc=ukl,dc=uni-freiburg,dc=de";
	public static final String LDAP_BASE = "ou=people";


	public static final int DIALOG_ADD_SLIDE_RESTAINING = 6;
	public static final int DIALOG_LOGOUT = 9;
	
	public static final Map<Integer, String> dialogMap;

	static {
		Map<Integer, String> tmpMap = new HashMap<>();
		tmpMap.put(DIALOG_ADD_SLIDE_RESTAINING, "/pages/dialog/task/addSlideRestaining");
		
		tmpMap.put(DIALOG_LOGOUT, "/pages/dialog/logout");

		dialogMap = Collections.unmodifiableMap(tmpMap);
	}
}
