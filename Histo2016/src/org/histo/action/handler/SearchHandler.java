package org.histo.action.handler;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.histo.action.MainHandlerAction;
import org.histo.action.PatientHandlerAction;
import org.histo.action.WorklistHandlerAction;
import org.histo.action.dialog.task.CreateTaskDialog;
import org.histo.config.ResourceBundle;
import org.histo.config.exception.CustomExceptionToManyEntries;
import org.histo.config.exception.CustomNullPatientExcepetion;
import org.histo.dao.PatientDao;
import org.histo.model.patient.Patient;
import org.histo.ui.PatientList;
import org.primefaces.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")
public class SearchHandler {

	private static Logger logger = Logger.getLogger("org.histo");

	@Autowired
	private PatientDao patientDao;

	@Autowired
	private SettingsHandler settingsHandler;

	public Patient serachForPiz(String piz) {
		// only search if 8 digit are provides
		if (piz != null && piz.matches("^[0-9]{8}$")) {
			Patient patient = patientDao.searchForPatientByPiz(piz);
			Patient clinicPatient;
			try {
				clinicPatient = settingsHandler.getClinicJsonHandler().getPatientFromClinicJson("/" + piz);
				if (patient != null) {
					patient.copyIntoObject(clinicPatient);
					return patient;
				} else {
					return clinicPatient;
				}
			} catch (JSONException | CustomExceptionToManyEntries | CustomNullPatientExcepetion e) {
				return null;
			}
		}
		return null;
	}
}
