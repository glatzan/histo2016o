package org.histo.config.enums;

public enum PredefinedFavouriteList {

	StainingList(1), DiagnosisList(2), NotificationList(3), ReStainingList(4), ReDiagnosisList(5), StayInStainingList(
			6), StayInDiagnosisList(7), StayInNotificationList(8), CouncilLending(9), CouncilPending(10), CouncilCompleted(11);

	private final int id;

	PredefinedFavouriteList(final int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}
}
