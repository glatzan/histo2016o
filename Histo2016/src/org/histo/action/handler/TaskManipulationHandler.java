package org.histo.action.handler;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.DiagnosisRevisionType;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.GenericDAO;
import org.histo.model.MaterialPreset;
import org.histo.model.StainingPrototype;
import org.histo.model.patient.Block;
import org.histo.model.patient.Diagnosis;
import org.histo.model.patient.DiagnosisContainer;
import org.histo.model.patient.DiagnosisRevision;
import org.histo.model.patient.Sample;
import org.histo.model.patient.Slide;
import org.histo.model.patient.Task;
import org.histo.util.TaskUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Component
@Scope(value = "session")
public class TaskManipulationHandler {

	private static Logger logger = Logger.getLogger("org.histo");

	@Autowired
	private ResourceBundle resourceBundle;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private GenericDAO genericDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private TransactionTemplate transactionTemplate;

	/********************************************************
	 * Diagnosis Info
	 ********************************************************/


	/**
	 * Finalize all diagnosis revisions
	 * 
	 * @param revisions
	 * @param finalize
	 * @throws CustomDatabaseInconsistentVersionException
	 */
	public void finalizeAllDiangosisRevisions(List<DiagnosisRevision> revisions, boolean finalize)
			throws CustomDatabaseInconsistentVersionException {
		for (DiagnosisRevision revision : revisions) {
			revision.setDiagnosisCompleted(finalize);
			revision.setCompleationDate(finalize ? System.currentTimeMillis() : 0);
			genericDAO.savePatientData(revision, finalize ? "log.patient.task.diagnosisContainer.diagnosisRevision.lock"
					: "log.patient.task.diagnosisContainer.diagnosisRevision.unlock", revision.getName());
		}
	}

	/********************************************************
	 * Diagnosis Info
	 ********************************************************/

	/********************************************************
	 * Diagnosis Revision
	 ********************************************************/


	public void copyHistologicalRecord(Diagnosis tmpDiagnosis, boolean overwrite)
			throws CustomDatabaseInconsistentVersionException {
		logger.debug("Setting extended diagnosistext text");
		tmpDiagnosis.getParent()
				.setText(overwrite ? tmpDiagnosis.getDiagnosisPrototype().getExtendedDiagnosisText()
						: tmpDiagnosis.getParent().getText() + "\r\n"
								+ tmpDiagnosis.getDiagnosisPrototype().getExtendedDiagnosisText());

		genericDAO.savePatientData(tmpDiagnosis.getParent(),
				"log.patient.task.diagnosisContainer.diagnosisRevision.update", tmpDiagnosis.getParent().toString());
	}

	/********************************************************
	 * Diagnosis Revision
	 ********************************************************/
}
