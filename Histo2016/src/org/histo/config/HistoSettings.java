package org.histo.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class HistoSettings {

	public static final String HISTO_BASE_URL = "/Histo2016";
	public static final String HISTO_LOGIN_PAGE = "/login.xhtml";

	// http://auginfo/piz?piz=xx
	// http://auginfo/piz?name=xx&vorname=xx&geburtsdatum=2000-01-01
	public static final String PATIENT_GET_URL = "http://auginfo/piz";

	public static final String LDAP_HOST = "ldap.ukl.uni-freiburg.de";
	public static final String LDAP_PORT = "389";
	public static final String LDAP_SUFFIX = "dc=ukl,dc=uni-freiburg,dc=de";
	public static final String LDAP_BASE = "ou=people";

	public static final String CENTER_INCLUDE_BLANK = "i_blank.xhtml";
	public static final String CENTER_INCLUDE_PATIENT = "i_patient.xhtml";
	public static final String CENTER_INCLUDE_DIAGNOSIS_INTERN = "i_diagnosisIntern.xhtml";
	public static final String CENTER_INCLUDE_RECEIPTLOG = "i_receiptlog.xhtml";
	public static final String CENTER_INCLUDE_EXTERN_EXTENDED = "i_externextended.xhtml";

	public static final int DIALOG_ARCHIV_STAINING = 0;
	public static final int DIALOG_ARCHIV_SAMPLE = 1;
	public static final int DIALOG_ARCHIV_BLOCK = 2;
	public static final int DIALOG_CREATE_TASK = 3;
	public static final int DIALOG_ADD_SLIDE = 4;
	public static final int DIALOG_STAINING_PERFORMED = 5;
	public static final int DIALOG_ADD_SLIDE_RESTAINING = 6;
	public static final int DIALOG_SETTINGS = 7;
	public static final int DIALOG_LOGOUT = 9;
	public static final int DIALOG_WORKLIST_OPTIONS = 10;
	public static final int DIALOG_PATIENT_EDIT = 11;
	public static final int DIALOG_WORKLIST_ORDER = 13;

	public static final String DIALOG_DIAGNOSIS_FINALIZE = "/pages/dialog/diagnosis/finalizeDiagnosis";
	public static final String DIALOG_DIAGNOSIS_EDIT_NAME = "/pages/dialog/diagnosis/editDiagnosisName";
	public static final String DIALOG_PATIENT_ADD = "/pages/dialog/patient/addPatient";
	public static final String DIALOG_WORKLIST_CONTACTS = "/pages/dialog/contact/addContact";
	
	
	// noch nicht drinnen
	public static final int DIALOG_TEST = 255;
	public static final int DIALOG_ARCHIV_TASK = 2;

	public static final Map<Integer, String> dialogMap;

	static {
		Map<Integer, String> tmpMap = new HashMap<>();
		tmpMap.put(DIALOG_ARCHIV_SAMPLE, "/pages/dialog/task/archiveSample");
		tmpMap.put(DIALOG_ARCHIV_STAINING, "/pages/dialog/task/archiveStaining");
		tmpMap.put(DIALOG_ARCHIV_BLOCK, "/pages/dialog/task/archiveBlock");
		tmpMap.put(DIALOG_CREATE_TASK, "/pages/dialog/task/createTask");
		tmpMap.put(DIALOG_ADD_SLIDE, "/pages/dialog/task/addSlide");
		tmpMap.put(DIALOG_STAINING_PERFORMED, "/pages/dialog/task/staingingPerformed");
		tmpMap.put(DIALOG_ADD_SLIDE_RESTAINING, "/pages/dialog/task/addSlideRestaining");
		tmpMap.put(DIALOG_SETTINGS, "/pages/dialog/settings/settings");
		tmpMap.put(DIALOG_LOGOUT, "/pages/dialog/logout");
		tmpMap.put(DIALOG_WORKLIST_OPTIONS, "/pages/dialog/worklist/worklistOptions");
		tmpMap.put(DIALOG_PATIENT_EDIT, "/pages/dialog/patient/editPatient");
		tmpMap.put(DIALOG_WORKLIST_ORDER, "/pages/dialog/worklist/worklistOrder");

		tmpMap.put(DIALOG_TEST, "/pages/dialog/task/notification");

		dialogMap = Collections.unmodifiableMap(tmpMap);
	}

	public static final String dialog(int dialog) {
		return dialogMap.get(dialog);
	}
}
