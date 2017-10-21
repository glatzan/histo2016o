package org.histo.action.view;

import java.util.List;

import org.apache.log4j.Logger;
import org.histo.action.dialog.diagnosis.CopyHistologicalRecordDialog;
import org.histo.action.handler.TaskManipulationHandler;
import org.histo.config.enums.ContactRole;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.ContactDAO;
import org.histo.dao.GenericDAO;
import org.histo.dao.PhysicianDAO;
import org.histo.dao.UtilDAO;
import org.histo.model.DiagnosisPreset;
import org.histo.model.ListItem;
import org.histo.model.Physician;
import org.histo.model.interfaces.PatientRollbackAble;
import org.histo.model.patient.Block;
import org.histo.model.patient.Diagnosis;
import org.histo.model.patient.Sample;
import org.histo.model.patient.Task;
import org.histo.ui.transformer.DefaultTransformer;
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
	private PhysicianDAO physicianDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private WorklistViewHandlerAction worklistViewHandlerAction;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private TaskManipulationHandler taskManipulationHandler;

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

	/**
	 * List of all diagnosis presets
	 */
	private List<DiagnosisPreset> diagnosisPresets;

	/**
	 * Transfomer for diagnosis prests
	 */
	private DefaultTransformer<DiagnosisPreset> diagnosisPresetsTransformer;

	/**
	 * List of physicians which have the role signature
	 */
	private List<Physician> physiciansToSignList;

	/**
	 * Transfomer for physiciansToSign
	 */
	private DefaultTransformer<Physician> physiciansToSignListTransformer;

	/**
	 * Selected physician to sign the report
	 */
	private Physician signatureOne;

	/**
	 * Selected consultant to sign the report
	 */
	private Physician signatureTwo;

	/**
	 * Contains all available case histories
	 */
	private List<ListItem> caseHistoryList;

	/**
	 * Contains all available wards
	 */
	private List<ListItem> wardList;

	/**
	 * selected List item form caseHistory list
	 */
	private ListItem selectedCaseHistoryItem;

	public void prepareForTask(Task task) {
		logger.debug("Initilize DiagnosisViewHandlerAction for task");

		if (getPhysiciansToSignList() == null)
			setPhysiciansToSignList(physicianDAO.getPhysicians(ContactRole.SIGNATURE, false));
		setPhysiciansToSignListTransformer(new DefaultTransformer<Physician>(getPhysiciansToSignList()));

		if (task.getDiagnosisContainer().getSignatureDate() == 0) {
			task.getDiagnosisContainer().setSignatureDate(TimeUtil.setDayBeginning(System.currentTimeMillis()));
			if (task.getDiagnosisContainer().getSignatureOne().getPhysician() == null
					|| task.getDiagnosisContainer().getSignatureTwo().getPhysician() == null) {
				// TODO set if physician to the left, if consultant to the right
			}
		}

		// loading lists
		if (getCaseHistoryList() == null)
			setCaseHistoryList(utilDAO.getAllStaticListItems(ListItem.StaticList.CASE_HISTORY));
		if (getWardList() == null)
			setWardList(utilDAO.getAllStaticListItems(ListItem.StaticList.WARDS));

		setSignatureOne(task.getDiagnosisContainer().getSignatureOne().getPhysician());
		setSignatureTwo(task.getDiagnosisContainer().getSignatureTwo().getPhysician());

		if (getDiagnosisPresets() == null)
			setDiagnosisPresets(utilDAO.getAllDiagnosisPrototypes());
		setDiagnosisPresetsTransformer(new DefaultTransformer<DiagnosisPreset>(getDiagnosisPresets()));
	}

	/**
	 * Creates a block by using the gui
	 * 
	 * @param sample
	 */
	public void createNewBlock(Sample sample) {
		try {

			taskManipulationHandler.createNewBlock(sample, false);

			// updates the name of all other samples
			for (Block block : sample.getBlocks()) {
				block.updateAllNames(sample.getParent().isUseAutoNomenclature(), false);
			}

			// checking if staining flag of the task object has to be false
			receiptlogViewHandlerAction.checkStainingPhase(sample.getTask(), true);

			// generating gui list
			sample.getParent().generateSlideGuiList();

			// saving patient
			genericDAO.savePatientData(sample, "log.patient.task.sample.update", sample.toString());

		} catch (CustomDatabaseInconsistentVersionException e) {
			// catching database version inconsistencies
			worklistViewHandlerAction.replacePatientTaskInCurrentWorklistAndSetSelected();
		}
	}

	public void onCopyHistologicalRecord(Diagnosis diagnosis) {
		try {
			// setting diagnosistext if no text is set
			if ((diagnosis.getParent().getText() == null || diagnosis.getParent().getText().isEmpty())
					&& diagnosis.getDiagnosisPrototype() != null) {
				taskManipulationHandler.copyHistologicalRecord(diagnosis, true);
				logger.debug("No extended diagnosistext found, text copied");
				return;
			} else if (diagnosis.getDiagnosisPrototype() != null) {
				logger.debug("Extended diagnosistext found, showing confing dialog");
				copyHistologicalRecordDialog.initAndPrepareBean(diagnosis);
			}
		} catch (CustomDatabaseInconsistentVersionException e) {
			// catching database version inconsistencies
			worklistViewHandlerAction.replacePatientTaskInCurrentWorklistAndSetSelected();
		}
	}

	public void onDiagnosisPrototypeChanged(Diagnosis diagnosis) {
		try {
			logger.debug("Updating diagnosis prototype");

			diagnosis.updateDiagnosisWithPrest(diagnosis.getDiagnosisPrototype());

			// updating all contacts on diagnosis change, an determine if the
			// contact should receive a physical case report
			contactDAO.updateNotificationsForPhysicalDiagnosisReport(diagnosis.getTask());

			genericDAO.savePatientData(diagnosis, "log.patient.task.diagnosisContainer.diagnosis.update",
					diagnosis.toString());

			// only setting diagnosis text if one sample and no text has been
			// added
			// jet
			if (diagnosis.getParent().getText() == null || diagnosis.getParent().getText().isEmpty()) {
				diagnosis.getParent().setText(diagnosis.getDiagnosisPrototype().getExtendedDiagnosisText());
				logger.debug("Updating revision extended text");
				genericDAO.savePatientData(diagnosis.getParent(),
						"log.patient.task.diagnosisContainer.diagnosisRevision.update",
						diagnosis.getParent().toString());
			}
		} catch (CustomDatabaseInconsistentVersionException e) {
			// catching database version inconsistencies
			worklistViewHandlerAction.replacePatientTaskInCurrentWorklistAndSetSelected();
		}

	}

	public void onDataChange(PatientRollbackAble toSave, String resourcesKey) {
		onDataChange(toSave, resourcesKey, new Object[0]);
	}

	public void onDataChange(PatientRollbackAble toSave, String resourcesKey, Object... arr) {
		genericDAO.savePatientData(toSave, toSave, resourcesKey, arr);
	}

	public void copyCaseHistory(Task task, ListItem selectedcaseHistoryItem) {
		logger.debug("Copy " + selectedcaseHistoryItem.getValue() + " to " + task);
		task.setCaseHistory(selectedcaseHistoryItem.getValue());
		onDataChange(task, "log.patient.task.change.caseHistory", selectedcaseHistoryItem.getValue());
	}
}
