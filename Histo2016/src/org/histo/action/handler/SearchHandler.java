package org.histo.action.handler;

import java.util.ArrayList;
import java.util.Date;
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
import org.histo.util.TimeUtil;
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

	public List<PatientList> searhcForPatientNameAndBirthday(String name, String surname, Date birthday) throws CustomExceptionToManyEntries, CustomNullPatientExcepetion {

		ArrayList<PatientList> result = new ArrayList<>();

		// list for excluding results for the histo database search
		ArrayList<String> foundPiz = new ArrayList<String>();

		List<Patient> clinicPatients = settingsHandler.getClinicJsonHandler()
				.getPatientsFromClinicJson("?name=" + name + (surname != null ? ("&vorname=" + surname) : "")
						+ (birthday != null ? "&geburtsdatum=" + TimeUtil.formatDate(birthday, "yyyy-MM-dd") : ""));

		// list of pizes to serach in the histo database
		ArrayList<String> toSearchPizes = new ArrayList<String>(clinicPatients.size());

		int id = 0;

		if (!clinicPatients.isEmpty()) {

			logger.trace("Patients in clinic backend found");

			// getting all pizes in one Array
			clinicPatients.forEach(p -> toSearchPizes.add(p.getPiz()));

			List<Patient> histoMatchList = new ArrayList<Patient>(0);

			// creating a list of patient from the histo backend pizes
			// which where obtained from the clinic backend
			histoMatchList = patientDao.searchForPatientPizList(toSearchPizes);

			for (Patient cPatient : clinicPatients) {

				PatientList patientList = null;

				// search if already added to the histo backend
				for (Patient hPatient : histoMatchList) {
					if (cPatient.getPiz().equals(hPatient.getPatient().getPiz())) {
						patientList = new PatientList(id++, hPatient);
						histoMatchList.remove(hPatient);
						foundPiz.add(hPatient.getPiz());
						// TODO update the patient in histo database
						break;
					}
				}

				// was not added add to normal list
				if (patientList == null) {
					patientList = new PatientList(id++, cPatient);
					patientList.setNotHistoDatabase(true);
				}
				result.add(patientList);
			}
		}

		// search for external patient in histo database, excluding the
		// already found patients via piz
		List<Patient> histoPatients = patientDao.getPatientsByNameSurnameDateExcludePiz(name, surname, birthday,
				foundPiz);

		for (Patient patient : histoPatients) {
			result.add(new PatientList(id++, patient));
		}

		return result;
	}
}
