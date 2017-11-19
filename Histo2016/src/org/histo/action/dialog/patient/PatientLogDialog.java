package org.histo.action.dialog.patient;

import java.util.List;

import org.histo.action.dialog.AbstractDialog;
import org.histo.config.enums.Dialog;
import org.histo.dao.LogDAO;
import org.histo.model.log.Log;
import org.histo.model.patient.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Configurable
@Getter
@Setter
public class PatientLogDialog extends AbstractDialog {

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private LogDAO logDAO;

	private List<Log> patientLog;

	public void initAndPrepareBean(Patient patient) {
		initBean(patient);
		prepareDialog();
	}

	/**
	 * Initializes the bean
	 * 
	 * @param patient
	 */
	public void initBean(Patient patient) {
		setPatientLog(logDAO.getPatientLog(patient));
		super.initBean(null, Dialog.PATIENT_LOG);
	}
}
