package org.histo.action.handler;

import org.apache.log4j.Logger;
import org.histo.config.ResourceBundle;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.GenericDAO;
import org.histo.model.patient.Diagnosis;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
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
				"log.patient.task.diagnosisRevision.update", tmpDiagnosis.getParent().toString());
	}

	/********************************************************
	 * Diagnosis Revision
	 ********************************************************/
}
