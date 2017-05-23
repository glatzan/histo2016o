package org.histo.model.util;

import org.apache.log4j.Logger;
import org.histo.model.patient.Task;

public class TaskStatusHandler {

	private static Logger logger = Logger.getLogger("org.histo");

	private Task task;

	public TaskStatusHandler(Task task) {
		this.task = task;
	}

//	public boolean updateStainingStatus() {
//		return updateStainingStatus(false);
//	}

//	public boolean updateStainingStatus(boolean stayInPhase) {
//		logger.trace("Method: hasStatingStatusChanged()");
//
//		// staining was performed
//		if (isStainingPerformed() && task.getStainingCompletionDate() == 0) {
//			// setting phase
//			logger.trace("Status has changed: Staining performed and no date set");
//			task.setStainingCompletionDate(System.currentTimeMillis());
//			task.setStainingPhase(stayInPhase);
//			task.setDiagnosisPhase(true);
//		} else if (!isStainingPerformed() && task.getStainingCompletionDate() != 0) {
//			// new stainings created, set completion date to 0
//			logger.trace("Status has changed: Staining was performed, new slides are avalibale");
//			task.setStainingCompletionDate(0);
//			task.setStainingPhase(true);
//		} else {
//			// status has not changed
//			logger.trace("Stainingstatus has not changed (Staining performed: " + isStainingPerformed()
//					+ ") (CompletionDate: " + task.getStainingCompletionDate() + ")");
//			return false;
//		}
//		return true;
//
//	}

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
		return isDiagnosisNeeded() || isReDiagnosisNeeded() || isNotificationStayInPhase() || isStainingNeeded()
				|| isReStainingNeeded() || isStayInStainingPhase() || isNotificationNeeded()
				|| isNotificationStayInPhase();
	}

	/********************************************************
	 * Diagnosis
	 ********************************************************/
	public boolean isDiagnosisPhase() {
		return task.isDiagnosisPhase();
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

	public boolean isReDiagnosisNeeded() {
		return task.isReDiagnosisNeeded();
	}

	public boolean isDiagnosisPhaseAndOtherPhase() {
		return isDiagnosisPhase() && (task.isStainingPhase() || task.isNotificationPhase());
	}

	/********************************************************
	 * Diagnosis
	 ********************************************************/

	/********************************************************
	 * Staining
	 ********************************************************/
	public boolean isStainingPhase() {
		return task.isStainingPhase();
	}

	public boolean isStainingPerformed() {
		return task.isStainingPerformed();
	}

	public boolean isStayInStainingPhase() {
		return task.isStainingPhase() && task.isStainingPerformed();
	}

	public boolean isStainingPerformedAndNotInStainingPhase() {
		return !task.isStainingPhase() && task.isStainingPerformed();
	}

	public boolean isStainingNeeded() {
		return task.isStainingNeeded();
	}

	public boolean isReStainingNeeded() {
		return task.isRestainingNeeded();
	}

	public boolean isStainingPhaseAndOtherPhase() {
		return isStainingPhase() && (task.isDiagnosisPhase() || task.isNotificationPhase());
	}

	/********************************************************
	 * Staining
	 ********************************************************/

	/********************************************************
	 * Notification
	 ********************************************************/
	public boolean isNotificationPhase() {
		return task.isNotificationPhase();
	}

	public boolean isNotificationPerformed() {
		return !task.isNotificationPhase() && task.getNotificationCompletionDate() != 0;
	}

	public boolean isNotificationStayInPhase() {
		return task.isNotificationPhase() && task.getNotificationCompletionDate() != 0;
	}

	public boolean isNotificationNeeded() {
		return task.isNotificationPhase() && task.getNotificationCompletionDate() == 0;
	}
	/********************************************************
	 * Notification
	 ********************************************************/

	public boolean isFinalized(){
		return task.isFinalized();
	}
}
