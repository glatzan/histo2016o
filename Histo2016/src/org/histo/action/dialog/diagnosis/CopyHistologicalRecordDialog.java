package org.histo.action.dialog.diagnosis;


import org.histo.action.dialog.AbstractDialog;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.enums.Dialog;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.GenericDAO;
import org.histo.dao.TaskDAO;
import org.histo.model.patient.Diagnosis;
import org.histo.service.TaskService;
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
public class CopyHistologicalRecordDialog extends AbstractDialog {

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private TaskDAO taskDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private WorklistViewHandlerAction worklistViewHandlerAction;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private GenericDAO genericDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private TaskService taskService;

	private Diagnosis diagnosis;

	public void initAndPrepareBean(Diagnosis diagnosis) {
		if (initBean(diagnosis))
			prepareDialog();
	}

	public boolean initBean(Diagnosis diagnosis) {
		try {
			setDiagnosis(genericDAO.reattach(diagnosis));
		} catch (CustomDatabaseInconsistentVersionException e) {
			logger.debug("Version conflict, updating entity");
			task = taskDAO.getTaskAndPatientInitialized(diagnosis.getTask().getId());
			worklistViewHandlerAction.onVersionConflictTask(task, false);
			return false;
		}
		super.initBean(task, Dialog.DIAGNOSIS_RECORD_OVERWRITE);

		return true;
	}

	public void copyHistologicalRecord(boolean overwrite) {
		try {
			taskService.copyHistologicalRecord(getDiagnosis(), overwrite);
		} catch (CustomDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
	}
}
