package org.histo.ui.task;

import org.histo.config.enums.PredefinedFavouriteList;
import org.histo.model.patient.Task;

import lombok.Getter;
import lombok.Setter;

/**
 * Task status class, is used for gui request because the isInLists requests
 * will slow the gui down a lot.
 * 
 * @author andi
 *
 */
@Getter
@Setter
public class TaskStatus {

	private Task task;

	private boolean stainingNeeded;
	private boolean reStainingNeeded;
	private boolean stayInStainingList;

	private boolean diagnosisNeeded;
	private boolean reDiagnosisNeeded;
	private boolean stayInDiagnosisList;

	private boolean notificationNeeded;
	private boolean stayInNotificationList;

	private boolean councilLendingMTA;
	private boolean councilLendingSecretary;
	private boolean councilPending;
	private boolean councilCompleted;

	private boolean finalized;

	public TaskStatus(Task task) {
		this.task = task;

		this.stainingNeeded = task.isListedInFavouriteList(PredefinedFavouriteList.StainingList);
		this.reStainingNeeded = task.isListedInFavouriteList(PredefinedFavouriteList.ReStainingList);
		this.stayInStainingList = task.isListedInFavouriteList(PredefinedFavouriteList.StayInStainingList);

		this.diagnosisNeeded = task.isListedInFavouriteList(PredefinedFavouriteList.DiagnosisList);
		this.reDiagnosisNeeded = task.isListedInFavouriteList(PredefinedFavouriteList.ReDiagnosisList);
		this.stayInDiagnosisList = task.isListedInFavouriteList(PredefinedFavouriteList.StayInDiagnosisList);

		this.notificationNeeded = task.isListedInFavouriteList(PredefinedFavouriteList.NotificationList);
		this.stayInNotificationList = task.isListedInFavouriteList(PredefinedFavouriteList.StayInNotificationList);

		this.councilLendingMTA = task.isListedInFavouriteList(PredefinedFavouriteList.CouncilLendingMTA);
		this.councilLendingSecretary = task.isListedInFavouriteList(PredefinedFavouriteList.CouncilLendingSecretary);
		this.councilPending = task.isListedInFavouriteList(PredefinedFavouriteList.CouncilPending);
		this.councilCompleted = task.isListedInFavouriteList(PredefinedFavouriteList.CouncilCompleted);

		this.finalized = task.isFinalized();
	}

}
