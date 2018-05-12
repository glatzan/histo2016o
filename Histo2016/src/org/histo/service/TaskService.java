package org.histo.service;

import org.apache.log4j.Logger;
import org.histo.config.enums.PredefinedFavouriteList;
import org.histo.config.exception.HistoDatabaseInconsistentVersionException;
import org.histo.dao.FavouriteListDAO;
import org.histo.dao.GenericDAO;
import org.histo.model.patient.Diagnosis;
import org.histo.model.patient.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Service
@Getter
@Setter
public class TaskService {

	private static Logger logger = Logger.getLogger("org.histo");

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private FavouriteListDAO favouriteListDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private GenericDAO genericDAO;

	public void copyHistologicalRecord(Diagnosis tmpDiagnosis, boolean overwrite)
			throws HistoDatabaseInconsistentVersionException {
		logger.debug("Setting extended diagnosistext text");
		tmpDiagnosis.getParent()
				.setText(overwrite ? tmpDiagnosis.getDiagnosisPrototype().getExtendedDiagnosisText()
						: tmpDiagnosis.getParent().getText() + "\r\n"
								+ tmpDiagnosis.getDiagnosisPrototype().getExtendedDiagnosisText());

		genericDAO.savePatientData(tmpDiagnosis.getParent(), "log.patient.task.diagnosisRevision.update",
				tmpDiagnosis.getParent().toString());
	}

	public void archiveTask(Task task) {
		// remove from all system lists
		favouriteListDAO.removeReattachedTaskFromList(task, PredefinedFavouriteList.values());

		// finalizing task
		task.setFinalizationDate(System.currentTimeMillis());
		task.setFinalized(true);

		if (task.getStainingCompletionDate() == 0)
			task.setStainingCompletionDate(System.currentTimeMillis());

		if (task.getDiagnosisCompletionDate() == 0)
			task.setDiagnosisCompletionDate(System.currentTimeMillis());

		if (task.getNotificationCompletionDate() == 0)
			task.setNotificationCompletionDate(System.currentTimeMillis());

		genericDAO.savePatientData(task, "log.patient.task.phase.archive", task);

	}

	public void restoreTask(Task task, String commentary) {
		// finalizing task
		task.setFinalizationDate(0);
		task.setFinalized(false);

		genericDAO.savePatientData(task, "log.patient.task.phase.restored", task, commentary);
	}
}
