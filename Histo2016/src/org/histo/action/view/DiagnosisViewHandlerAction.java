package org.histo.action.view;

import org.apache.log4j.Logger;
import org.histo.action.dialog.diagnosis.CopyHistologicalRecordDialog;
import org.histo.config.exception.HistoDatabaseInconsistentVersionException;
import org.histo.dao.ContactDAO;
import org.histo.dao.FavouriteListDAO;
import org.histo.dao.GenericDAO;
import org.histo.dao.UtilDAO;
import org.histo.model.ListItem;
import org.histo.model.Signature;
import org.histo.model.interfaces.PatientRollbackAble;
import org.histo.model.patient.Diagnosis;
import org.histo.model.patient.DiagnosisRevision;
import org.histo.model.patient.Task;
import org.histo.service.SampleService;
import org.histo.service.TaskService;
import org.histo.util.TimeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Controller
@Scope("session")
@Getter
@Setter
public class DiagnosisViewHandlerAction {

	private static Logger logger = Logger.getLogger("org.histo");
	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private WorklistViewHandlerAction worklistViewHandlerAction;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private TaskService taskService;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private ReceiptlogViewHandlerAction receiptlogViewHandlerAction;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private CopyHistologicalRecordDialog copyHistologicalRecordDialog;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private UtilDAO utilDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private GenericDAO genericDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private ContactDAO contactDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private SampleService sampleService;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private FavouriteListDAO favouriteListDAO;

	/**
	 * selected List item form caseHistory list
	 */
	private ListItem selectedCaseHistoryItem;

	public void prepareForTask(Task task) {
		logger.debug("Initilize DiagnosisViewHandlerAction for task");

		for (DiagnosisRevision revision : task.getDiagnosisRevisions()) {
			if (revision.getCompletionDate() == 0) {
				revision.setSignatureDate(TimeUtil.setDayBeginning(System.currentTimeMillis()));

				if(revision.getSignatureOne() == null)
					revision.setSignatureOne(new Signature());
				
				if(revision.getSignatureTwo() == null)
					revision.setSignatureTwo(new Signature());
				
				if (revision.getSignatureOne().getPhysician() == null
						|| revision.getSignatureTwo().getPhysician() == null) {
					// TODO set if physician to the left, if consultant to the right
				}
			}

		}
	}

	public void onCopyHistologicalRecord(Diagnosis diagnosis) {
		try {
			// setting diagnosistext if no text is set
			if ((diagnosis.getParent().getText() == null || diagnosis.getParent().getText().isEmpty())
					&& diagnosis.getDiagnosisPrototype() != null) {
				taskService.copyHistologicalRecord(diagnosis, true);
				logger.debug("No extended diagnosistext found, text copied");
				return;
			} else if (diagnosis.getDiagnosisPrototype() != null) {
				logger.debug("Extended diagnosistext found, showing confing dialog");
				copyHistologicalRecordDialog.initAndPrepareBean(diagnosis);
			}
		} catch (HistoDatabaseInconsistentVersionException e) {
			// catching database version inconsistencies
			worklistViewHandlerAction.replaceSelectedTask();
		}
	}

	public void onDiagnosisPrototypeChanged(Diagnosis diagnosis) {
		try {
			logger.debug("Updating diagnosis prototype");

			diagnosis.updateDiagnosisWithPrest(diagnosis.getDiagnosisPrototype());

			// updating all contacts on diagnosis change, an determine if the
			// contact should receive a physical case report
			contactDAO.updateNotificationsForPhysicalDiagnosisReport(diagnosis.getTask());

			genericDAO.savePatientData(diagnosis, "log.patient.task.diagnosisRevision.diagnosis.update",
					diagnosis.toString());

			// only setting diagnosis text if one sample and no text has been
			// added
			// jet
			if (diagnosis.getParent().getText() == null || diagnosis.getParent().getText().isEmpty()) {
				diagnosis.getParent().setText(diagnosis.getDiagnosisPrototype().getExtendedDiagnosisText());
				logger.debug("Updating revision extended text");
				genericDAO.savePatientData(diagnosis.getParent(), "log.patient.task.diagnosisRevision.update",
						diagnosis.getParent().toString());
			}
		} catch (HistoDatabaseInconsistentVersionException e) {
			// catching database version inconsistencies
			worklistViewHandlerAction.replaceSelectedTask();
		}

	}

	/**
	 * Updates the signatures role
	 * 
	 * @param physician
	 */
	public void onPhysiciansSignatureChange(Signature signature) {
		String role = signature.getPhysician() != null ? signature.getPhysician().getClinicRole() : "";
		signature.setRole(role != null ? role : "");
	}

	public void onDataChange(PatientRollbackAble<?> toSave, String resourcesKey) {
		onDataChange(toSave, resourcesKey, new Object[0]);
	}

	/**
	 * Saves dynamically changed data of the views. Error-handling is done via
	 * global error Handler.
	 * 
	 * @param toSave
	 * @param resourcesKey
	 * @param arr
	 */
	public void onDataChange(PatientRollbackAble<?> toSave, String resourcesKey, Object... arr) {
		genericDAO.savePatientData(toSave, toSave, resourcesKey, arr);
	}

	public void copyCaseHistory(Task task, ListItem selectedcaseHistoryItem) {
		logger.debug("Copy " + selectedcaseHistoryItem.getValue() + " to " + task);
		task.setCaseHistory(selectedcaseHistoryItem.getValue());
		onDataChange(task, "log.patient.task.change.caseHistory", selectedcaseHistoryItem.getValue());
	}

}
