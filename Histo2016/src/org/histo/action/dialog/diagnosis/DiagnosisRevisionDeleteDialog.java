package org.histo.action.dialog.diagnosis;

import javax.faces.event.AbortProcessingException;

import org.histo.action.dialog.AbstractDialog;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.enums.Dialog;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.config.exception.CustomUserNotificationExcepetion;
import org.histo.dao.TaskDAO;
import org.histo.model.patient.DiagnosisRevision;
import org.histo.service.DiagnosisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Component
@Scope(value = "session")
@Setter
@Getter
public class DiagnosisRevisionDeleteDialog extends AbstractDialog {

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
	private DiagnosisService diagnosisService;

	private DiagnosisRevision diagnosisRevision;

	public void initAndPrepareBean(DiagnosisRevision diagnosisRevision) {
		if (initBean(diagnosisRevision))
			prepareDialog();
	}

	public boolean initBean(DiagnosisRevision diagnosisRevision) {

		super.initBean(task, Dialog.DIAGNOSIS_REVISION_DELETE);
		this.diagnosisRevision = diagnosisRevision;

		return true;
	}

	public void deleteDiagnosisRevision() {
		try {
			diagnosisService.removeDiagnosisRevision(diagnosisRevision);
		} catch (CustomUserNotificationExcepetion e) {
			hideDialog();
			// catching if last revision do not delete
			mainHandlerAction.sendGrowlMessages(e);
			throw new AbortProcessingException();
		} catch (CustomDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
	}
}
