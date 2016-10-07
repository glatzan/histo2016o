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

	public static final String CENTER_INCLUDE_BLANK = "i_blank.xhtml";
	public static final String CENTER_INCLUDE_PATIENT = "i_patient.xhtml";
	public static final String CENTER_INCLUDE_DIAGNOSIS_INTERN = "i_diagnosis.xhtml";
	public static final String CENTER_INCLUDE_RECEIPTLOG = "i_receiptlog.xhtml";

	public static final String DIALOG_DIAGNOSIS_FINALIZE = "/pages/dialog/diagnosis/finalizeDiagnosis";
	public static final String DIALOG_DIAGNOSIS_UNFINALIZE = "/pages/dialog/diagnosis/unfinalizeDiagnosis";
	public static final String DIALOG_DIAGNOSIS_EDIT_NAME = "/pages/dialog/diagnosis/editDiagnosisName";
	public static final String DIALOG_WORKLIST_CONTACTS_ADD = "/pages/dialog/contact/addContact";
	public static final String DIALOG_WORKLIST_CONTACTS_PERFORM = "/pages/dialog/contact/performContact";
	public static final String DIALOG_PRINT = "/pages/dialog/print/print";
	public static final String DIALOG_CREATE_TASK = "/pages/dialog/task/createTask";
	public static final String DIALOG_CREATE_SAMPLE = "/pages/dialog/task/createSample";
	public static final String DIALOG_ADD_SLIDE_TO_BLOCK = "/pages/dialog/task/addSlide";
	public static final String DIALOG_ARCHIV_STAINING = "/pages/dialog/task/archiveStaining";
	public static final String DIALOG_ARCHIV_SAMPLE = "/pages/dialog/task/archiveSample";
	public static final String DIALOG_ARCHIV_BLOCK = "/pages/dialog/task/archiveBlock";
	public static final String DIALOG_ARCHIV_TASK = "/pages/dialog/task/archiveTask";
	public static final String DIALOG_PATIENT_LOG = "/pages/dialog/history/patientLog";
	public static final String DIALOG_ALL_STAINING_PERFORMED = "/pages/dialog/task/staingingPerformed";
	

	public static final int DIALOG_ADD_SLIDE_RESTAINING = 6;
	public static final int DIALOG_SETTINGS = 7;
	public static final int DIALOG_LOGOUT = 9;
	public static final int DIALOG_PATIENT_EDIT = 11;
	public static final int DIALOG_WORKLIST_ORDER = 13;
	
	public static final Map<Integer, String> dialogMap;

	static {
		Map<Integer, String> tmpMap = new HashMap<>();
		tmpMap.put(DIALOG_ADD_SLIDE_RESTAINING, "/pages/dialog/task/addSlideRestaining");
		tmpMap.put(DIALOG_SETTINGS, "/pages/dialog/settings/settings");
		tmpMap.put(DIALOG_LOGOUT, "/pages/dialog/logout");
		
		tmpMap.put(DIALOG_PATIENT_EDIT, "/pages/dialog/patient/editPatient");
		tmpMap.put(DIALOG_WORKLIST_ORDER, "/pages/dialog/worklist/worklistOrder");

		dialogMap = Collections.unmodifiableMap(tmpMap);
	}

	public static final String dialog(int dialog) {
		return dialogMap.get(dialog);
	}
}
