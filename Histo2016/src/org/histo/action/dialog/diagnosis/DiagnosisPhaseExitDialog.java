package org.histo.action.dialog.diagnosis;

import java.util.List;

import org.histo.action.DialogHandlerAction;
import org.histo.action.UserHandlerAction;
import org.histo.action.dialog.AbstractDialog;
import org.histo.action.view.GlobalEditViewHandler;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.PredefinedFavouriteList;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.FavouriteListDAO;
import org.histo.dao.PatientDao;
import org.histo.dao.PdfDAO;
import org.histo.dao.TaskDAO;
import org.histo.model.patient.DiagnosisRevision;
import org.histo.model.patient.Task;
import org.histo.service.DiagnosisService;
import org.histo.ui.transformer.DefaultTransformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Component
@Scope(value = "session")
@Setter
@Getter
public class DiagnosisPhaseExitDialog extends AbstractDialog {

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
	private DialogHandlerAction dialogHandlerAction;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private FavouriteListDAO favouriteListDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private PatientDao patientDao;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private PdfDAO pdfDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private TransactionTemplate transactionTemplate;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private GlobalEditViewHandler globalEditViewHandler;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private UserHandlerAction userHandlerAction;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private DiagnosisService diagnosisService;

	/**
	 * If true the task will be removed from worklist
	 */
	private boolean removeFromWorklist;

	/**
	 * If true the task will be removed from diagnosis list
	 */
	private boolean removeFromDiangosisList;

	/**
	 * If true the diagnosis phase of the task will be finished
	 */
	private boolean endDiangosisPhase;

	/**
	 * If true the task will be shifted to the notification phase
	 */
	private boolean goToNotificationPhase;

	/**
	 * List of diagnosis revisions of the task
	 */
	private List<DiagnosisRevision> diagnosisRevisions;

	/**
	 * Transformer for diagnosis revisions
	 */
	private DefaultTransformer<DiagnosisRevision> diagnosisRevisionTransformer;

	/**
	 * Diagnosis revision to notify about
	 */
	private DiagnosisRevision selectedRevision;

	public void initAndPrepareBean(Task task) {
		if (initBean(task, (DiagnosisRevision) null))
			prepareDialog();
	}

	public void initAndPrepareBean(Task task, DiagnosisRevision selectedRevision) {
		if (initBean(task, selectedRevision))
			prepareDialog();
	}

	public boolean initBean(Task task, DiagnosisRevision selectedRevision) {
		try {
			taskDAO.initializeTask(task, false);

		} catch (CustomDatabaseInconsistentVersionException e) {
			logger.debug("Version conflict, updating entity");
			task = taskDAO.getTaskAndPatientInitialized(task.getId());
			worklistViewHandlerAction.replaceTaskInCurrentWorklist(task, false);
		}

		super.initBean(task, Dialog.DIAGNOSIS_PHASE_EXIT);

		setDiagnosisRevisions(task.getDiagnosisRevisions());
		setDiagnosisRevisionTransformer(new DefaultTransformer<DiagnosisRevision>(getDiagnosisRevisions()));
		setSelectedRevision(selectedRevision);

		// if last diangosis in task
		boolean lastDiagnosis = task.getDiagnosisRevisions()
				.indexOf(selectedRevision) == task.getDiagnosisRevisions().size() - 1;

		setRemoveFromWorklist(lastDiagnosis);

		setEndDiangosisPhase(lastDiagnosis);
		setRemoveFromDiangosisList(lastDiagnosis);

		setGoToNotificationPhase(true);

		return true;
	}

	public void approveDiangosis() {
		try {

			// end diagnosis phase
			if (endDiangosisPhase && removeFromDiangosisList) {
				diagnosisService.endDiagnosisPhase(getTask());
				mainHandlerAction.sendGrowlMessages(resourceBundle.get("growl.diagnosis.endAll"),
						resourceBundle.get(goToNotificationPhase ? "growl.diagnosis.endAll.text.true"
								: "growl.diagnosis.endAll.text.false"));
			} else {
				// otherwise approve diangosis
				diagnosisService.approveDiangosis(getSelectedRevision(), goToNotificationPhase);

				mainHandlerAction.sendGrowlMessages(
						resourceBundle.get("growl.diagnosis.approved", getSelectedRevision().getName()),
						resourceBundle.get(goToNotificationPhase ? "growl.diagnosis.endAll.text.true"
								: "growl.diagnosis.endAll.text.false"));
				if (removeFromDiangosisList)
					
					favouriteListDAO.removeReattachedTaskFromList(task, PredefinedFavouriteList.DiagnosisList,
							PredefinedFavouriteList.ReDiagnosisList);
			}

			// adding to notification phase
			if (goToNotificationPhase)
				favouriteListDAO.addReattachedTaskToList(getTask(), PredefinedFavouriteList.NotificationList);

			if (removeFromWorklist) {
				// only remove from worklist if patient has one active task
				if (task.getPatient().getTasks().stream().filter(p -> !p.isFinalized()).count() > 1) {
					mainHandlerAction.sendGrowlMessagesAsResource("growl.error",
							"growl.error.worklist.remove.moreActive");
				} else {
					worklistViewHandlerAction.removePatientFromCurrentWorklist(task.getPatient());
					worklistViewHandlerAction.onDeselectPatient(true);
				}
			}

		} catch (CustomDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
	}
}
