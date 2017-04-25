package org.histo.config.enums;

/**
 * This status determines if a notification event is needed
 * 
 * @author andi
 *
 */
public enum NotificationStatus {
	PERFORMED(1), STAY_IN_PHASE(2), NOTIFICATION_NEEDED(3), NOT_IN_PHASE(10);

	private final int level;

	NotificationStatus(int level) {
		this.level = level;
	}

	public static final NotificationStatus getStainingStatusByLevel(int level) {
		if (level == NotificationStatus.PERFORMED.getLevel())
			return NotificationStatus.PERFORMED;

		if (level == NotificationStatus.NOTIFICATION_NEEDED.getLevel())
			return NotificationStatus.NOTIFICATION_NEEDED;

		if (level == DiagnosisStatus.STAY_IN_PHASE.getLevel())
			return NotificationStatus.STAY_IN_PHASE;

		return null;
	}

	public int getLevel() {
		return level;
	}
}
