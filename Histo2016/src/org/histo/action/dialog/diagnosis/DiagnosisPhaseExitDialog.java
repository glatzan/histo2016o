package org.histo.action.dialog.diagnosis;

import org.histo.action.DialogHandlerAction;
import org.histo.action.dialog.AbstractDialog;
import org.histo.action.handler.TaskManipulationHandler;
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

	/**
	 * Can be set to true if the task should stay in diagnosis phase.
	 */
	private boolean stayInDiagnosisPhase;

	public void initAndPrepareBean(Task task) {
		if (initBean(task))
			prepareDialog();
	}

	public boolean initBean(Task task) {
		try {
			taskDAO.initializeTask(task, false);
		} catch (CustomDatabaseInconsistentVersionException e) {
			logger.debug("Version conflict, updating entity");
			task = taskDAO.getTaskAndPatientInitialized(task.getId());
			worklistViewHandlerAction.replacePatientTaskInCurrentWorklistAndSetSelected(task);
		}
		super.initBean(task, Dialog.DIAGNOSIS_PHASE_EXIT);

		setStayInDiagnosisPhase(false);

		// inits a template for previewing
		dialogHandlerAction.getPrintDialog().initBeanForExternalDisplay(task,
				new DocumentType[] { DocumentType.DIAGNOSIS_REPORT, DocumentType.DIAGNOSIS_REPORT_EXTERN },
				DocumentType.DIAGNOSIS_REPORT,
				new AssociatedContact(task, new Person(resourceBundle.get("pdf.address.none"), new Contact())));

		return true;
	}

	public void endDiagnosisPhase() {
		try {
			transactionTemplate.execute(new TransactionCallbackWithoutResult() {

				public void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
					taskManipulationHandler.finalizeAllDiangosisRevisions(
							getTask().getDiagnosisContainer().getDiagnosisRevisions(), true);

					// adding to notification phase
					favouriteListDAO.addTaskToList(getTask(), PredefinedFavouriteList.NotificationList);

					// removing from diagnosis list
					if (getTask().isListedInFavouriteList(PredefinedFavouriteList.DiagnosisList))
						favouriteListDAO.removeTaskFromList(getTask(), PredefinedFavouriteList.DiagnosisList);

					// removing from REdiagnosis list
					if (getTask().isListedInFavouriteList(PredefinedFavouriteList.ReDiagnosisList))
						favouriteListDAO.removeTaskFromList(getTask(), PredefinedFavouriteList.ReDiagnosisList);

					// adding to stay in diagnosis phase if selected
					if (isStayInDiagnosisPhase()
							&& !getTask().isListedInFavouriteList(PredefinedFavouriteList.StayInDiagnosisList))
						favouriteListDAO.addTaskToList(getTask(), PredefinedFavouriteList.StayInDiagnosisList);
					else if (getTask().isListedInFavouriteList(PredefinedFavouriteList.StayInDiagnosisList))
						// removing from stay in diagnosis list
						favouriteListDAO.removeTaskFromList(getTask(), PredefinedFavouriteList.StayInDiagnosisList);

					getTask().setDiagnosisCompletionDate(System.currentTimeMillis());

					// finalizing task
					getTask().setFinalizationDate(System.currentTimeMillis());
					getTask().setFinalized(true);

					// generating final diagnosis report
					if (dialogHandlerAction.getPrintDialog().getPdfContainer()
							.getType() != DocumentType.DIAGNOSIS_REPORT) {
						dialogHandlerAction.getPrintDialog().setDefaultTemplateOfType(DocumentType.DIAGNOSIS_REPORT);
					}

					dialogHandlerAction.getPrintDialog().getPdfContainer()
							.setType(DocumentType.DIAGNOSIS_REPORT_COMPLETED);

					pdfDAO.attachPDF(getTask().getPatient(), getTask(),
							dialogHandlerAction.getPrintDialog().getPdfContainer());

					genericDAO.savePatientData(getTask(), "log.patient.task.change.diagnosisPhase.end");
				}
			});

		} catch (Exception e) {
			onDatabaseVersionConflict();
		}

	}

	// ************************ Getter/Setter ************************
	public boolean isStayInDiagnosisPhase() {
		return stayInDiagnosisPhase;
	}

	public void setStayInDiagnosisPhase(boolean stayInDiagnosisPhase) {
		this.stayInDiagnosisPhase = stayInDiagnosisPhase;
	}
}
