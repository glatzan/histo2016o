package org.histo.model.interfaces;

import org.histo.config.enums.NotificationStatus;

public interface NotificationInfo extends CreationDate {

	public NotificationStatus getNotificationStatus();

}
