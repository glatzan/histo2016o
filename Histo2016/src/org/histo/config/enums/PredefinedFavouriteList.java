package org.histo.config.enums;

import java.util.ArrayList;
import java.util.List;

public enum PredefinedFavouriteList {

	StainingList(1), DiagnosisList(2), NotificationList(3), ReStainingList(4), ReDiagnosisList(5), StayInStainingList(
			6), StayInDiagnosisList(7), StayInNotificationList(
					8), CouncilLendingMTA(9), CouncilLendingSecretary(10), CouncilPending(11), CouncilCompleted(12);

	private final int id;

	PredefinedFavouriteList(final int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public static List<Integer> getIdArr() {
		PredefinedFavouriteList[] entry = PredefinedFavouriteList.values();
		List<Integer> ids = new ArrayList<Integer>(entry.length);

		for (int i = 0; i < entry.length; i++) {
			ids.add(entry[i].getId());
		}

		return ids;
	}
}
