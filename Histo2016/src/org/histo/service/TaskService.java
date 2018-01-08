package org.histo.service;

import org.histo.config.enums.PredefinedFavouriteList;
import org.histo.dao.FavouriteListDAO;
import org.histo.dao.GenericDAO;
import org.histo.model.patient.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Service
@Getter
@Setter
public class TaskService {

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private FavouriteListDAO favouriteListDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private GenericDAO genericDAO;

	public void archiveTask(Task task) {
		// remove from all system lists
		favouriteListDAO.removeTaskFromList(task, PredefinedFavouriteList.values());

		// finalizing task
		task.setFinalizationDate(System.currentTimeMillis());
		task.setFinalized(true);

		if(task.getStainingCompletionDate() == 0)
			task.setStainingCompletionDate(System.currentTimeMillis());
		
		if(task.getDiagnosisCompletionDate() == 0)
			task.setDiagnosisCompletionDate(System.currentTimeMillis());
		
		if(task.getNotificationCompletionDate() == 0)
			task.setNotificationCompletionDate(System.currentTimeMillis());

		genericDAO.savePatientData(task, "log.patient.task.change.diagnosisPhase.archive", new Object[] { task });

	}

	public void restoreTask(Task task, String commentary) {
		// finalizing task
		task.setFinalizationDate(0);
		task.setFinalized(false);

		genericDAO.savePatientData(task, "log.patient.task.change.diagnosisPhase.dearchive",
				new Object[] { task, commentary });
	}
}
