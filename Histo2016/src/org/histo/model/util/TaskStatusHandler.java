package org.histo.model.util;

import javax.persistence.Transient;

import org.apache.log4j.Logger;
import org.histo.config.enums.StainingStatus;
import org.histo.model.patient.Task;

public class TaskStatusHandler {

	private static Logger logger = Logger.getLogger("org.histo");

	private Task task;

	public TaskStatusHandler(Task task) {
		this.task = task;
	}

	public boolean updateStainingStatus(boolean stayInPhase) {
		logger.trace("Method: hasStatingStatusChanged()");
		// staining is performed and date = 0, so staining was performed
		// recently
		if (isStainingPerformed() && task.getStainingCompletionDate() == 0) {
			task.setStainingCompletionDate(System.currentTimeMillis());
			task.setStainingPhase(stayInPhase);
			return true;
			// the staining process was performed (date != 0), but there are new
			// slides to stain
		} else if (!isStainingPerformed() && task.getStainingCompletionDate() != 0) {
			task.setStainingCompletionDate(0);
			task.setStainingPhase(true);
			return true;
		}

		// status has not changed
		return false;
	}

	public boolean updateDiagnosisStatus(boolean performed, boolean stayInPhase) {
		logger.trace("Method: updateDiagnosisStatus(boolean performed, boolean stayInPhase)");
		if (performed) {
			if (task.getDiagnosisCompletionDate() == 0) {
				task.setDiagnosisCompletionDate(System.currentTimeMillis());
				task.setDiagnosisPhase(stayInPhase);
				return true;
			} else {
				task.setDiagnosisPhase(stayInPhase);
				return false;
			}
		} else {
			if (task.getDiagnosisCompletionDate() != 0) {
				task.setDiagnosisCompletionDate(0);
				task.setDiagnosisPhase(true);
				return true;
			} else
				return true;
		}

	}

	public boolean updateNotificationStatus(boolean performed, boolean stayInPhase) {
		logger.trace("Method: updateDiagnosisStatus(boolean performed, boolean stayInPhase)");
		if (performed) {
			if (task.getNotificationCompletionDate() == 0) {
				task.setNotificationCompletionDate(System.currentTimeMillis());
				task.setNotificationPhase(stayInPhase);
				return true;
			} else {
				task.setNotificationPhase(stayInPhase);
				return false;
			}
		} else {
			if (task.getNotificationCompletionDate() != 0) {
				task.setNotificationCompletionDate(0);
				task.setNotificationPhase(true);
				return true;
			} else
				return true;
		}

	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/
	public Task getTask() {
		return task;
	}

	public void setTask(Task task) {
		this.task = task;
	}

	public boolean isActive() {
		return isDiagnosisNeeded() || isReDiangosisNeeded() || isNotificationStayInPhase() || isStainingNeeded()
				|| isReStainingNeeded() || isStayInStainingPhase() || isNotificationNeeded()
				|| isNotificationStayInPhase();
	}

	public boolean isDiagnosisPerformed() {
		return task.isDiagnosisPerformed();
	}

	public boolean isDiagnosisStayInPhase() {
		return task.isDiagnosisPhase() && task.isDiagnosisPerformed();
	}

	public boolean isDiagnosisNeeded() {
		return task.isDiagnosisNeeded();
	}

	public boolean isReDiangosisNeeded() {
		return task.isReDiagnosisNeeded();
	}

	public boolean isStainingPerformed() {
		return task.isStaningPerformed();
	}

	public boolean isStayInStainingPhase() {
		return task.isStainingPhase() && task.isStainingPhase();
	}

	public boolean isStainingNeeded() {
		return task.isStainingNeeded();
	}

	public boolean isReStainingNeeded() {
		return task.isRestainingNeeded();
	}

	public boolean isNotificationPerformed() {
		return !task.isNotificationPhase() && task.getNotificationCompletionDate() != 0;
	}

	public boolean isNotificationStayInPhase() {
		return task.isNotificationPhase() && isNotificationPerformed();
	}

	public boolean isNotificationNeeded() {
		return task.isNotificationPhase() && task.getNotificationCompletionDate() == 0;
	}
	/********************************************************
	 * Getter/Setter
	 ********************************************************/

}
