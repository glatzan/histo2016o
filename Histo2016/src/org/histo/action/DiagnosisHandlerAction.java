package org.histo.action;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import org.histo.config.HistoSettings;
import org.histo.dao.GenericDAO;
import org.histo.dao.TaskDAO;
import org.histo.model.Diagnosis;
import org.histo.model.Sample;
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
		genericDAO.save(newDiagnosis, resourceBundle.get("log.diagnosis.new"), newDiagnosis.getPatient());
		genericDAO.save(sample);
		genericDAO.refresh(sample.getPatient());
	}

	/**
	 * Checks if the given diagnosis is a normal diagnosis or a followup
	 * diagnosis.
	 * 
	 * @param diagnosis
	 * @return
	 */
	public boolean isDiangosisDiagnosisOrFollowUP(Diagnosis diagnosis) {
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
		helper.showDialog(HistoSettings.DIALOG_DIAGNOSIS_FINALIZE, false, false, true);
	}

	/**
	 * Hides the waring dialog for finalizing diagnoses
	 */
	public void hideFinalizeDiangosisDialog() {
		setTmpDiagnosis(null);
		helper.hideDialog(HistoSettings.DIALOG_DIAGNOSIS_FINALIZE);
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
		if (isDiangosisDiagnosisOrFollowUP(diagnosis)) {

			for (Diagnosis diagnosisIter : sample.getDiagnoses()) {
				diagnosisIter.setFinalized(true);
				diagnosisIter.setFinalizedDate(new Date(System.currentTimeMillis()));
			}

		} else {
			diagnosis.setFinalized(true);
			diagnosis.setFinalizedDate(new Date(System.currentTimeMillis()));
		}

		genericDAO.save(diagnosis, resourceBundle.get("log.diagnosis.finaziled"), diagnosis.getPatient());
		genericDAO.refresh(diagnosis.getPatient());

		hideFinalizeDiangosisDialog();
	}

	/**
	 * Makes a diagnosis editable again.
	 * 
	 * @param diagnosis
	 */
	public void unfinalizeDiagnosis(Diagnosis diagnosis) {
		diagnosis.setFinalized(false);
		diagnosis.setFinalizedDate(null);
		genericDAO.save(diagnosis, resourceBundle.get("log.diagnosis.unfinalize"), diagnosis.getPatient());
	}

	/**
	 * Saves the parent sample of the passed diagnosis, thus saving the
	 * diagnosis as well.
	 * 
	 * @param diagnosis
	 */
	public void saveDiagnosis(Diagnosis diagnosis) {
		taskDAO.getDiagnosisRevisions(diagnosis);
		genericDAO.save(diagnosis.getPatient(), resourceBundle.get("log.diagnosis.changed"), diagnosis.getPatient());
	}

	/**
	 * Shows a dialog for editing the name of the passed diagnosis
	 */
	public void prepareEditDiagnosisNameDialog(Diagnosis diagnosis) {
		setTmpDiagnosis(diagnosis);
		helper.showDialog(HistoSettings.DIALOG_DIAGNOSIS_EDIT_NAME, false, false, true);
	}

	/**
	 * Hides the edit name dialog for the diagnosis
	 */
	public void hideEditDiagnosisNameDialog() {
		setTmpDiagnosis(null);
		helper.hideDialog(HistoSettings.DIALOG_DIAGNOSIS_EDIT_NAME);
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
