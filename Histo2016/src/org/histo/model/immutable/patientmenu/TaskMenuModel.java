package org.histo.model.immutable.patientmenu;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Immutable;
import org.histo.config.enums.PredefinedFavouriteList;
import org.histo.config.enums.TaskPriority;
import org.histo.model.FavouriteList;

import lombok.Getter;
import lombok.Setter;

@Entity
@Immutable
@Getter
@Setter
public class TaskMenuModel {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(updatable = false, nullable = false)
	private Long id;

	@Column(length = 10)
	private String taskID = "";

	@Enumerated(EnumType.ORDINAL)
	private TaskPriority taskPriority;

	@Column
	private long dueDate;

	@Column
	private long creationDate;

	@Column
	private long diagnosisCompletionDate;

	@Column
	private long stainingCompletionDate;

	@ManyToOne(targetEntity = PatientMenuModel.class)
	private PatientMenuModel parent;

	@ManyToMany(fetch = FetchType.EAGER)
	@Fetch(value = FetchMode.SUBSELECT)
	@JoinTable(name = "task_favouritelist", joinColumns = {
			@JoinColumn(table = "taskmenumodel", name = "task_id", referencedColumnName = "id") }, inverseJoinColumns = {
					@JoinColumn(table = "favouritelist", name = "favouritelists_id", referencedColumnName = "id", nullable = true) })
	private List<FavouriteList> favouriteLists;

	@Transient
	private boolean active;

	@Transient
	public boolean isActiveOrActionPending() {
		return isActiveOrActionPending(false);
	}

	/**
	 * Returns true if the task is marked as active or an action is pending. If
	 * activeOnly is true only the active attribute of the task will be
	 * evaluated.
	 * 
	 * @param task
	 * @return
	 */
	@Transient
	public boolean isActiveOrActionPending(boolean activeOnly) {
		if (activeOnly)
			return isActive();

		if (isActive())
			return true;

		if (isListedInFavouriteList(PredefinedFavouriteList.StainingList, PredefinedFavouriteList.ReStainingList,
				PredefinedFavouriteList.StayInStainingList, PredefinedFavouriteList.DiagnosisList,
				PredefinedFavouriteList.ReDiagnosisList, PredefinedFavouriteList.StayInDiagnosisList,
				PredefinedFavouriteList.NotificationList, PredefinedFavouriteList.StayInNotificationList))
			return true;

		return false;
	}

	@Transient
	public boolean isListedInFavouriteList(PredefinedFavouriteList... predefinedFavouriteLists) {
		long[] arr = new long[predefinedFavouriteLists.length];
		int i = 0;
		for (PredefinedFavouriteList predefinedFavouriteList : predefinedFavouriteLists) {
			arr[i] = predefinedFavouriteList.getId();
			i++;
		}

		return isListedInFavouriteList(arr);
	}

	@Transient
	public boolean isListedInFavouriteList(long... idArr) {
		for (long id : idArr) {
			if (getFavouriteLists().stream().anyMatch(p -> p.getId() == id))
				return true;
		}
		return false;
	}

	@Transient
	public boolean isListedInFavouriteList(FavouriteList... favouriteLists) {
		long[] arr = new long[favouriteLists.length];
		int i = 0;
		for (FavouriteList favouriteList : favouriteLists) {
			arr[i] = favouriteList.getId();
			i++;
		}

		return isListedInFavouriteList(arr);
	}
}
