package org.histo.action.dialog.diagnosis;


import org.histo.action.dialog.AbstractDialog;
import org.histo.action.handler.TaskManipulationHandler;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.enums.Dialog;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.GenericDAO;
import org.histo.dao.PatientDao;
import org.histo.dao.TaskDAO;
import org.histo.model.patient.Diagnosis;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")
public class CopyHistologicalRecordDialog extends AbstractDialog {

	@Autowired
	private TaskDAO taskDAO;

	@Autowired
	private WorklistViewHandlerAction worklistViewHandlerAction;

	@Autowired
	private GenericDAO genericDAO;

	@Autowired
	private PatientDao patientDao;

	@Autowired
	private TaskManipulationHandler taskManipulationHandler;

	private Diagnosis diagnosis;

	public void initAndPrepareBean(Diagnosis diagnosis) {
		if (initBean(diagnosis))
			prepareDialog();
	}

	public boolean initBean(Diagnosis diagnosis) {
		try {
			setDiagnosis(genericDAO.refresh(diagnosis));
		} catch (CustomDatabaseInconsistentVersionException e) {
			logger.debug("Version conflict, updating entity");
			task = taskDAO.getTaskAndPatientInitialized(diagnosis.getTask().getId());
			worklistViewHandlerAction.replacePatientTaskInCurrentWorklistAndSetSelected(task);
			return false;
		}
		super.initBean(task, Dialog.DIAGNOSIS_RECORD_OVERWRITE);

		return true;
	}

	public void copyHistologicalRecord(boolean overwrite) {
		try {
			taskManipulationHandler.copyHistologicalRecord(getDiagnosis(), overwrite);
		} catch (CustomDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
	}

	// ************************ Getter/Setter ************************
	public Diagnosis getDiagnosis() {
		return diagnosis;
	}

	public void setDiagnosis(Diagnosis diagnosis) {
		this.diagnosis = diagnosis;
	}

}
