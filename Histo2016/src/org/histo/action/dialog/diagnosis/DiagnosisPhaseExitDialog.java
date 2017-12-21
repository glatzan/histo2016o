package org.histo.action.dialog.diagnosis;

import org.histo.action.DialogHandlerAction;
import org.histo.action.UserHandlerAction;
import org.histo.action.dialog.AbstractDialog;
import org.histo.action.handler.TaskManipulationHandler;
import org.histo.action.view.GlobalEditViewHandler;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.DocumentType;
import org.histo.config.enums.PredefinedFavouriteList;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.FavouriteListDAO;
import org.histo.dao.PatientDao;
import org.histo.dao.PdfDAO;
import org.histo.dao.TaskDAO;
import org.histo.model.AssociatedContact;
import org.histo.model.Contact;
import org.histo.model.Person;
import org.histo.model.patient.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Configurable
@Getter
@Setter
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
	private TaskManipulationHandler taskManipulationHandler;

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

	/**
	 * Can be set to true if the task should stay in diagnosis phase.
	 */
	private boolean stayInDiagnosisPhase;

	/**
	 * If true the task will be shifted to the notification phase
	 */
	private boolean goToNotificationPhase;

	public void initAndPrepareBean(Task task) {
		if (initBean(task))
			prepareDialog();
	}

	public boolean initBean(Task task) {
		try {
			taskDAO.initializeTask(task, false);

			this.goToNotificationPhase = true;
			this.stayInDiagnosisPhase = false;

		} catch (CustomDatabaseInconsistentVersionException e) {
			logger.debug("Version conflict, updating entity");
			task = taskDAO.getTaskAndPatientInitialized(task.getId());
			worklistViewHandlerAction.onVersionConflictTask(task, false);
		}

		// small dialog or big with pdf
		if (userHandlerAction.getCurrentUser().getSettings().isPdfPreviewOnDiagnosisApproval()) {
			// inits a template for previewing
			dialogHandlerAction.getPrintDialog().initBeanForExternalDisplay(task,
					new DocumentType[] { DocumentType.DIAGNOSIS_REPORT, DocumentType.DIAGNOSIS_REPORT_EXTERN },
					DocumentType.DIAGNOSIS_REPORT,
					new AssociatedContact(task, new Person(resourceBundle.get("pdf.address.none"), new Contact())));

			super.initBean(task, Dialog.DIAGNOSIS_PHASE_EXIT);
		} else
			super.initBean(task, Dialog.DIAGNOSIS_PHASE_EXIT_SMALL);

		setStayInDiagnosisPhase(false);

		return true;
	}

	public void endDiagnosisPhase() {
		try {
			transactionTemplate.execute(new TransactionCallbackWithoutResult() {

				public void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
					taskManipulationHandler.finalizeAllDiangosisRevisions(
							getTask().getDiagnosisContainer().getDiagnosisRevisions(), true);

					getTask().getDiagnosisContainer().setSignatureDate(System.currentTimeMillis());
					
					// adding to notification phase
					if (goToNotificationPhase)
						favouriteListDAO.addTaskToList(getTask(), PredefinedFavouriteList.NotificationList);

					// removing from diagnosis and rediagnosis list
					favouriteListDAO.removeTaskFromList(getTask(), PredefinedFavouriteList.DiagnosisList,
							PredefinedFavouriteList.ReDiagnosisList);

					// adding to stay in diagnosis phase if selected
					if (isStayInDiagnosisPhase())
						favouriteListDAO.addTaskToList(getTask(), PredefinedFavouriteList.StayInDiagnosisList);
					else
						// removing from stay in diagnosis list
						favouriteListDAO.removeTaskFromList(getTask(), PredefinedFavouriteList.StayInDiagnosisList);

					getTask().setDiagnosisCompletionDate(System.currentTimeMillis());

					genericDAO.savePatientData(getTask(), "log.patient.task.change.diagnosisPhase.end");
				}
			});

		} catch (Exception e) {
			onDatabaseVersionConflict();
		}

		globalEditViewHandler.updateDataOfTask(false);
	}
}
