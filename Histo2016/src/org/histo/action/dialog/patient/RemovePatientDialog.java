package org.histo.action.dialog.patient;

import org.histo.action.dialog.AbstractDialog;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.enums.Dialog;
import org.histo.config.exception.HistoDatabaseInconsistentVersionException;
import org.histo.dao.LogDAO;
import org.histo.model.patient.Patient;
import org.histo.service.PatientService;
import org.histo.service.dao.PatientDao;
import org.histo.service.dao.impl.PatientDaoImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Component
@Scope(value = "session")
@Getter
@Setter
public class RemovePatientDialog extends AbstractDialog {

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private WorklistViewHandlerAction worklistViewHandlerAction;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private PatientDao patientDao;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private PatientService patientService;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private LogDAO logDAO;

	private Patient patient;

	public void initAndPrepareBean(Patient patient) {
		initBean(patient);
		prepareDialog();
	}

	public void initBean(Patient patient) {
		try {
			setPatient(genericDAO.reattach(patient));
		} catch (HistoDatabaseInconsistentVersionException e) {
			logger.debug("Version conflict, updating entity");
			setPatient(patientDao.find(patient.getId(), true, false));
			worklistViewHandlerAction.replacePatientInCurrentWorklist(getPatient(), false);
		}
		super.initBean(null, Dialog.PATIENT_REMOVE);

		setPatient(patient);
	}

	public void removePatient() {
		try {
			logDAO.deletePatientLogs(patient);
			patientService.removePatient(patient);
		} catch (Exception e) {
			onDatabaseVersionConflict();
		}
	}
}
