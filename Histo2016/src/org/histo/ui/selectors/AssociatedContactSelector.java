package org.histo.ui.selectors;

import java.util.List;
import java.util.stream.Collectors;

import org.histo.model.AssociatedContact;
import org.histo.model.patient.Task;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssociatedContactSelector extends AbstractSelector {

	private AssociatedContact contact;
	private boolean deleteAble;

	protected AssociatedContactSelector(AssociatedContact contact) {
		this.contact = contact;
		// deletion only possible if no notification was performed
		setDeleteAble(
				contact.getNotifications() != null ? !contact.getNotifications().stream().anyMatch(p -> p.isPerformed())
						: true);
	}

	public static List<AssociatedContactSelector> factory(Task task) {
		return task.getContacts().stream().map(p -> new AssociatedContactSelector(p)).collect(Collectors.toList());
	}
}
