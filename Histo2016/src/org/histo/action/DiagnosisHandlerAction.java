package org.histo.action;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Transient;

import org.apache.log4j.Logger;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.DiagnosisRevisionType;
import org.histo.config.enums.Dialog;
import org.histo.dao.GenericDAO;
import org.histo.model.DiagnosisPreset;
import org.histo.model.patient.Diagnosis;
import org.histo.model.patient.DiagnosisInfo;
import org.histo.model.patient.DiagnosisRevision;
import org.histo.model.patient.Sample;
import org.histo.model.patient.Task;
import org.histo.util.TaskUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")
public class DiagnosisHandlerAction implements Serializable {

	private static final long serialVersionUID = -1214161114824263589L;

	private static Logger logger = Logger.getLogger(DiagnosisHandlerAction.class);

	@Autowired
	private GenericDAO genericDAO;

	@Autowired
	private HelperHandlerAction helper;

	@Autowired
	private ResourceBundle resourceBundle;

	@Autowired
	private MainHandlerAction mainHandlerAction;

	@Autowired
	@Lazy
	private TaskHandlerAction taskHandlerAction;

	private Task tmpTask;

	private Diagnosis tmpDiagnosis;

	/**
	 * Creates a new diagnosis and adds it to the given diagnosisRevision
	 * 
	 * @param revision
	 * @param sample
	 * @return
	 */
	public Diagnosis createDiagnosis(DiagnosisRevision revision, Sample sample) {
		logger.info("Creating new diagnosis");

		Diagnosis diagnosis = new Diagnosis();
		diagnosis.setSample(sample);
		diagnosis.setGenerationDate(System.currentTimeMillis());
		diagnosis.setParent(revision);
		// diagnosis.setName(getDiagnosisName(sample, diagnosis,
		// resourceBundle));

		revision.getDiagnoses().add(diagnosis);

		// saving to database
		genericDAO.save(
				diagnosis, resourceBundle.get("log.patient.task.diagnosisInfo.diagnosis.new",
						sample.getParent().getTaskID(), sample.getSampleID(), revision.getId(), diagnosis.getName()),
				diagnosis.getPatient());

		return diagnosis;
	}

	/**
	 * Removes a diagnosis from the parent and deletes it.
	 * 
	 * @param diagnosis
	 * @return
	 */
	public Diagnosis removeDiagnosis(Diagnosis diagnosis) {
		logger.info("Removing diagnosis " + diagnosis.getName());

		Sample tmp = diagnosis.getSample();

		diagnosis.setSample(null);

		diagnosis.getParent().getDiagnoses().remove(diagnosis);

		genericDAO.delete(diagnosis,
				resourceBundle.get("log.patient.task.diagnosisInfo.diagnosis.remove",
						diagnosis.getParent().getParent().getParent().getTaskID(), tmp.getSampleID(),
						diagnosis.getParent().getId(), diagnosis.getName()),
				diagnosis.getPatient());

		return diagnosis;
	}

	/**
	 * Creates a diagnosisRevision, adds it to the given DiagnosisInfo and
	 * creates also all needed diagnoses
	 * 
	 * @param parent
	 * @param type
	 * @return
	 */
	public DiagnosisRevision createDiagnosisRevision(DiagnosisInfo parent, DiagnosisRevisionType type) {
		logger.info("Creating new diagnosisRevision");

		DiagnosisRevision diagnosisRevision = new DiagnosisRevision();
		diagnosisRevision.setSequenceNumber(parent.getDiagnosisRevisions().size());
		diagnosisRevision.setType(type);
		diagnosisRevision.setParent(parent);
		diagnosisRevision
				.setName(TaskUtil.getDiagnosisName(parent.getDiagnosisRevisions(), diagnosisRevision, resourceBundle));

		// saving to database
		genericDAO.save(diagnosisRevision,
				resourceBundle.get("log.patient.task.diagnosisInfo.diagnosisRevision.new",
						diagnosisRevision.getParent().getParent().getTaskID(), diagnosisRevision.getName()),
				diagnosisRevision.getPatient());

		parent.getDiagnosisRevisions().add(diagnosisRevision);

		// creating a diagnosis for every sample
		for (Sample sample : parent.getParent().getSamples()) {
			createDiagnosis(diagnosisRevision, sample);
		}

		// saving to database
		genericDAO.save(diagnosisRevision,
				resourceBundle.get("log.patient.task.diagnosisInfo.diagnosisRevision.update",
						diagnosisRevision.getParent().getParent().getTaskID(), diagnosisRevision.getName()),
				diagnosisRevision.getPatient());

		return diagnosisRevision;
	}

	/**
	 * Deleting a DiagnosisRevision and all included diagnoese
	 * 
	 * @param revision
	 * @return
	 */
	public DiagnosisRevision removeDiagnosisRevision(DiagnosisRevision revision) {
		logger.info("Removing diagnosisRevision " + revision.getName());

		revision.getParent().getDiagnosisRevisions().remove(revision);

		genericDAO
				.delete(revision,
						resourceBundle.get("log.patient.task.diagnosisInfo.diagnosisRevision.delete",
								revision.getParent().getParent().getTaskID(), revision.getName()),
						revision.getPatient());

		return revision;
	}

	/**
	 * Updates a diagnosisRevision with a sample list. Used for adding and
	 * removing samples after initial revision creation.
	 * 
	 * @param diagnosisRevision
	 * @param samples
	 */
	public void updateDiagnosisRevisionToSampleCount(DiagnosisRevision diagnosisRevision, List<Sample> samples) {
		logger.info("Updating diagnosis list with new sample list");

		List<Diagnosis> diagnosesInRevision = diagnosisRevision.getDiagnoses();

		List<Sample> samplesToAddDiagnosis = new ArrayList<Sample>(samples);

		List<Diagnosis> toRemoveDiagnosis = new ArrayList<Diagnosis>();

		outerLoop: for (Diagnosis diagnosis : diagnosesInRevision) {
			// sample already in diagnosisList, removing from to add array
			for (Sample sample : samplesToAddDiagnosis) {
				if (sample.getId() == diagnosis.getSample().getId()) {
					samplesToAddDiagnosis.remove(sample);
					logger.trace("Sample found, Removing sample " + sample.getId() + " from list.");
					continue outerLoop;
				}
			}
			logger.trace("Diagnosis has no sample, removing diagnosis " + diagnosis.getId());
			// not found within samples, so sample was deleted, deleting
			// diagnosis as well.
			toRemoveDiagnosis.add(diagnosis);
		}

		// removing diagnose if necessary
		for (Diagnosis diagnosis : toRemoveDiagnosis) {
			removeDiagnosis(diagnosis);
		}

		// adding new diagnoses if there are new samples
		for (Sample sample : samplesToAddDiagnosis) {
			logger.trace("Adding new diagnosis for sample " + sample.getId());
			createDiagnosis(diagnosisRevision, sample);
		}

		genericDAO.save(diagnosisRevision,
				resourceBundle.get("log.patient.task.diagnosisInfo.diagnosisRevision.new",
						diagnosisRevision.getParent().getParent().getTaskID(), diagnosisRevision.getName()),
				diagnosisRevision.getPatient());
	}

	/**
	 * Updates all diagnosisRevision of the given diagnosisInfo
	 * 
	 * @param diagnosisInfo
	 * @param samples
	 */
	public void updateDiagnosisInfoToSampleCount(DiagnosisInfo diagnosisInfo, List<Sample> samples) {
		logger.info("Updating diagnosis info to new sample list");
		for (DiagnosisRevision revision : diagnosisInfo.getDiagnosisRevisions()) {
			updateDiagnosisRevisionToSampleCount(revision, samples);
		}

		genericDAO.save(diagnosisInfo,
				resourceBundle.get("log.patient.task.diagnosisInfo.update", diagnosisInfo.getParent().getTaskID()),
				diagnosisInfo.getPatient());
	}

	/**
	 * Checks if a diagnosis revision can be created. This in only possible if
	 * all other diagnoses are finalized.
	 * 
	 * @param sample
	 * @return
	 */
	public boolean isDiagonsisRevisionCreationPossible(Sample sample) {
		// List<Diagnosis> diagnoses = sample.getDiagnoses();
		//
		// if (diagnoses.size() < 1)
		// return false;
		//
		// for (Diagnosis diagnosis : diagnoses) {
		// if (!diagnosis.isFinalized()) {
		// return false;
		// }
		// }
		//
		// TODO: rework
		return true;
	}

	/**
	 * Creates an new Diagnosis with the given type. Adds it to the passed
	 * sample and saves the sample in the database.
	 * 
	 * @param sample
	 * @param type
	 */
	public void createDiagnosisFromGui(Sample sample, DiagnosisRevisionType type) {
		// createDiagnosis(sample, type);
		// genericDAO.save(sample.getPatient(),
		// resourceBundle.get("log.patient.save"), sample.getPatient());
	}

	public void createDiagnosisForSample() {

	}

	/**
	 * Shows a waring dialog before finalizing a diagnosis.
	 */
	public void prepareFinalizeDiagnosisDialog(Task task) {
		setTmpTask(task);
		mainHandlerAction.showDialog(Dialog.DIAGNOSIS_FINALIZE);
	}

	/**
	 * Hides the waring dialog for finalizing diagnoses
	 */
	public void hideFinalizeDiangosisDialog() {
		setTmpTask(null);
		mainHandlerAction.hideDialog(Dialog.DIAGNOSIS_FINALIZE);
	}

	/**
	 * Finalizes all diagnoses of the task.
	 */
	public void finalizeDiagnoses(Task task) {
		// for (Sample sample : task.getSamples()) {
		// for (Diagnosis diagnosis : sample.getDiagnoses()) {
		// finalizeDiagnosis(diagnosis);
		// }
		// }
		// task.setDiagnosisCompleted(true);
		// task.setDiagnosisCompletionDate(System.currentTimeMillis());
		// TODO: rework
	}

	/**
	 * Finalizes the passed diagnosis.
	 * 
	 * @param diagnosis
	 */
	public void finalizeDiagnosis(Diagnosis diagnosis) {
		diagnosis.setFinalized(true);
		diagnosis.setFinalizedDate(System.currentTimeMillis());

		genericDAO.save(diagnosis, resourceBundle.get("log.patient.diagnosis.finaziled"), diagnosis.getPatient());
		genericDAO.refresh(diagnosis.getPatient());
	}

	/**
	 * Shows a waring dialog before unfinalizing a diagnosis.
	 */
	public void prepareUnfinalizeDiagnosisDialog(Diagnosis diagnosis) {
		// setTmpDiagnosis(diagnosis);
		mainHandlerAction.showDialog(Dialog.DIAGNOSIS_UNFINALIZE);
	}

	/**
	 * Hides the waring dialog for unfinalizing diagnoses
	 */
	public void hideUnfinalizeDiangosisDialog() {
		// setTmpDiagnosis(null);
		mainHandlerAction.hideDialog(Dialog.DIAGNOSIS_UNFINALIZE);
	}

	/**
	 * Makes a diagnosis editable again.
	 * 
	 * @param diagnosis
	 */
	public void unfinalizeDiagnosis(Diagnosis diagnosis) {
		diagnosis.setFinalized(false);
		diagnosis.setFinalizedDate(0);
		genericDAO.save(diagnosis, resourceBundle.get("log.patient.diagnosis.unfinalize"), diagnosis.getPatient());

		hideUnfinalizeDiangosisDialog();
	}

	public void onDiagnosisPrototypeChanged(Diagnosis diagnosis) {
		logger.debug("Updating diagnosis prototype");
		Task task = diagnosis.getParent().getParent().getParent();

		diagnosis.updateDiagnosisWithPrest(diagnosis.getDiagnosisPrototype());

		genericDAO.save(diagnosis,
				resourceBundle.get("log.patient.task.diagnosisInfo.diagnosis.update", diagnosis.getLogPath()),
				diagnosis.getPatient());

		// only setting diagnosis text if one sample and no text has been added
		// jet
		if (diagnosis.getParent().getText() == null || diagnosis.getParent().getText().isEmpty()) {
			diagnosis.getParent().setText(diagnosis.getDiagnosisPrototype().getExtendedDiagnosisText());
			logger.debug("Updating revision extended text");
			genericDAO.save(diagnosis.getParent(),
					resourceBundle.get("log.patient.task.diagnosisInfo.diagnosisRevision.update", task.getId(),
							diagnosis.getParent().getName()),
					diagnosis.getPatient());
		}

	}

	/**
	 * Overwrites the extended text of the diagnosisRevison if no text is
	 * present. If text was entered, a dialog will be shown.
	 * 
	 * @param tmpDiagnosis
	 */
	public void prepareCopyHistologicalRecordDialog(Diagnosis tmpDiagnosis) {
		setTmpDiagnosis(tmpDiagnosis);

		// setting diagnosistext if no text is set
		if ((tmpDiagnosis.getParent().getText() == null || tmpDiagnosis.getParent().getText().isEmpty())
				&& tmpDiagnosis.getDiagnosisPrototype() != null) {
			copyHistologicalRecord(tmpDiagnosis, true);
			logger.debug("No extended diagnosistext found, text copied");
			return;
		}

		if (tmpDiagnosis.getDiagnosisPrototype() != null) {
			logger.debug("Extended diagnosistext found, showing confing dialog");
			mainHandlerAction.showDialog(Dialog.DIAGNOSIS_RECORD_OVERWRITE);
		}

	}

	/**
	 * Will overwrite the diangosisrevision text if overwrite ist true,
	 * ostherwise the text of the diagnosis will be added
	 * 
	 * @param tmpDiagnosis
	 * @param overwrite
	 */
	public void copyHistologicalRecord(Diagnosis tmpDiagnosis, boolean overwrite) {
		logger.debug("Setting extended diagnosistext text");
		tmpDiagnosis.getParent()
				.setText(overwrite ? tmpDiagnosis.getDiagnosisPrototype().getExtendedDiagnosisText()
						: tmpDiagnosis.getParent().getText() + "\r\n"
								+ tmpDiagnosis.getDiagnosisPrototype().getExtendedDiagnosisText());

		genericDAO.save(tmpDiagnosis.getParent(),
				resourceBundle.get("log.patient.task.diagnosisInfo.diagnosisRevision.update",
						tmpDiagnosis.getParent().getParent().getParent().getId(), tmpDiagnosis.getParent().getName()),
				tmpDiagnosis.getPatient());

		hideCopyHistologicalRecordDialog();
	}

	/**
	 * Hides the overwrite warning dialog
	 */
	public void hideCopyHistologicalRecordDialog() {
		setTmpDiagnosis(null);
		mainHandlerAction.hideDialog(Dialog.DIAGNOSIS_RECORD_OVERWRITE);
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/
	public Task getTmpTask() {
		return tmpTask;
	}

	public void setTmpTask(Task tmpTask) {
		this.tmpTask = tmpTask;
	}

	public Diagnosis getTmpDiagnosis() {
		return tmpDiagnosis;
	}

	public void setTmpDiagnosis(Diagnosis tmpDiagnosis) {
		this.tmpDiagnosis = tmpDiagnosis;
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/

}
