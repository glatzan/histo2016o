package org.histo.action;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import org.histo.config.HistoSettings;
import org.histo.dao.GenericDAO;
import org.histo.dao.TaskDAO;
import org.histo.model.Diagnosis;
import org.histo.model.History;
import org.histo.model.Sample;
import org.histo.model.Task;
import org.histo.util.Log;
import org.histo.util.ResourceBundle;
import org.histo.util.TaskUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")
public class DiagnosisHandlerAction implements Serializable {

	@Autowired
	private GenericDAO genericDAO;

	@Autowired
	private TaskDAO taskDAO;
	
	@Autowired
	private Log log;

	@Autowired
	private HelperHandlerAction helper;

	@Autowired
	private ResourceBundle resourceBundle;
	
	private static final long serialVersionUID = -1214161114824263589L;

	private Diagnosis tmpDiagnosis;

	/**
	 * Checks if a follow up diagnosis (Nachbefundung) can be created. This is
	 * only possible if one normal diagnosis is present.
	 * 
	 * @param sample
	 * @return
	 */
	public boolean isDiagonsisFollowUPCreationPossible(Sample sample) {
		List<Diagnosis> diagnoses = sample.getDiagnoses();

		if (diagnoses.size() == 1)
			return true;
		else
			return false;
	}

	/**
	 * Checks if a diagnosis revision can be created. This in only possible if
	 * all other diagnoses are finalized.
	 * 
	 * @param sample
	 * @return
	 */
	public boolean isDiagonsisRevisionCreationPossible(Sample sample) {
		List<Diagnosis> diagnoses = sample.getDiagnoses();

		if (diagnoses.size() < 1)
			return false;

		for (Diagnosis diagnosis : diagnoses) {
			if (!diagnosis.isFinalized()) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Creates an new Diagnosis with the given type. Adds it to the passed
	 * sample and saves the sample in the database.
	 * 
	 * @param sample
	 * @param type
	 */
	public void createNewDiagnosis(Sample sample, int type) {
		Diagnosis newDiagnosis = TaskUtil.createNewDiagnosis(sample, type);
		genericDAO.save(newDiagnosis, resourceBundle.get("log.diagnosis.new"));
		genericDAO.merge(sample);

		log.info(Log.LOG_DIAGNOSIS_NEW, sample.getPatient(), newDiagnosis.getId(), newDiagnosis.getName(),
				sample.getSampleID(), sample.getParent().getTaskID());
	}

	/**
	 * Checks if the given diagnosis is a normal diagnosis or a followup
	 * diagnosis.
	 * 
	 * @param diagnosis
	 * @return
	 */
	public boolean isDiangosisToFinalizeDiagnosisOrFollowUP(Diagnosis diagnosis) {
		if (diagnosis.getType() == Diagnosis.TYPE_DIAGNOSIS
				|| diagnosis.getType() == Diagnosis.TYPE_FOLLOW_UP_DIAGNOSIS) {
			return true;
		}
		return false;
	}

	/**
	 * Shows a waring dialog before finalizing a diagnosis.
	 */
	public void prepareFinalizeDiagnosisDialog(Diagnosis diagnosis) {
		setTmpDiagnosis(diagnosis);
		System.out.println(HistoSettings.dialog(HistoSettings.DIALOG_DIAGNOSIS_FINALIZE));
		helper.showDialog(HistoSettings.dialog(HistoSettings.DIALOG_DIAGNOSIS_FINALIZE));
	}

	/**
	 * Hides the waring dialog for finalizing diagnoses
	 */
	public void hideFinalizeDiangosisDialog() {
		setTmpDiagnosis(null);
		helper.hideDialog(HistoSettings.dialog(HistoSettings.DIALOG_DIAGNOSIS_FINALIZE));
	}

	/**
	 * Finalizes the passed diagnosis. If the diagnosis is a normal diagnosis or
	 * an follow up diagnosis, all other normal or follow up (there can only be
	 * one other) will be finalized as well. If the diagnosis is a revision
	 * diagnosis, only the revision diagnosis will be finalized.
	 * 
	 * @param diagnosis
	 */
	public void finalizeDiagnosis(Diagnosis diagnosis) {

		Sample sample = diagnosis.getParent();
		if (isDiangosisToFinalizeDiagnosisOrFollowUP(diagnosis)) {

			for (Diagnosis diagnosisIter : sample.getDiagnoses()) {
				diagnosisIter.setFinalized(true);
				diagnosisIter.setFinalizedDate(new Date(System.currentTimeMillis()));

				log.info(Log.LOG_DIAGNOSIS_NEW, sample.getPatient(), diagnosisIter.getId(), diagnosisIter.getName(),
						sample.getSampleID(), sample.getParent().getTaskID(), diagnosisIter.asGson());
			}

		} else {
			diagnosis.setFinalized(true);
			diagnosis.setFinalizedDate(new Date(System.currentTimeMillis()));

			log.info(Log.LOG_DIAGNOSIS_NEW, sample.getPatient(), diagnosis.getId(), diagnosis.getName(),
					sample.getSampleID(), sample.getParent().getTaskID(), diagnosis.asGson());
		}

		genericDAO.merge(diagnosis.getParent(), resourceBundle.get("log.diagnosis.finaziled"));

		hideFinalizeDiangosisDialog();
	}

	/**
	 * Saves the parent sample of the passed diagnosis, thus saving the
	 * diagnosis as well.
	 * 
	 * @param diagnosis
	 */
	public void saveDiagnosis(Diagnosis diagnosis) {
		taskDAO.getDiagnosisRevisions(diagnosis);
		
		genericDAO.merge(diagnosis.getParent(), resourceBundle.get("log.diagnosis.changed"));
	}
	

	/********************************************************
	 * Getter/Setter
	 ********************************************************/

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
