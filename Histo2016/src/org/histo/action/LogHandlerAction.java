package org.histo.action;

import java.io.Serializable;
import java.util.List;

import org.histo.config.enums.Dialog;
import org.histo.dao.LogDAO;
import org.histo.model.Log;
import org.histo.model.patient.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")
public class LogHandlerAction implements Serializable {

	private static final long serialVersionUID = 914245140737663306L;
	
	@Autowired
	LogDAO logDAO;
	
	@Autowired
	HelperHandlerAction helper;
	
	@Autowired
	MainHandlerAction mainHandlerAction;
	
	private List<Log> patientLog;

	public void preparePatientLogDialog(Patient patient){
		setPatientLog(logDAO.getPatientLog(patient));
		mainHandlerAction.showDialog(Dialog.PATIENT_LOG);
	}
	
	public List<Log> getPatientLog() {
		return patientLog;
	}

	public void setPatientLog(List<Log> patientLog) {
		this.patientLog = patientLog;
	}
	
	
}
