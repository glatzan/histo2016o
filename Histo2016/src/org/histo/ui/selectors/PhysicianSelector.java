package org.histo.ui.selectors;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.histo.config.enums.ContactRole;
import org.histo.model.AssociatedContact;
import org.histo.model.Physician;
import org.histo.model.patient.Task;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PhysicianSelector implements Serializable {

	private static final long serialVersionUID = -3843101038957964232L;

	private int id;
	private Physician physician;
	private List<ContactRole> associatedRoles;

	private boolean contactOfTask;
	private AssociatedContactSelector contactSelector;

	public PhysicianSelector(Physician physician, int id) {
		this.physician = physician;
		this.id = id;
	}

	public void addAssociatedRole(ContactRole role) {
		if (associatedRoles == null)
			associatedRoles = new ArrayList<ContactRole>();

		associatedRoles.add(role);
	}

	public boolean hasRole(ContactRole[] contactRoles) {
		if (getAssociatedRoles() == null || getAssociatedRoles().size() == 0)
			return false;

		return getAssociatedRoles().stream().anyMatch(p -> {
			for (int i = 0; i < contactRoles.length; i++) {
				if (p == contactRoles[i])
					return true;
			}
			return false;

		});
	}

	public static List<PhysicianSelector> factory(Task task, List<Physician> databasePhysicians) {

		AtomicInteger i = new AtomicInteger(0);

		// loading physicians with associated roles
		List<PhysicianSelector> resultList = databasePhysicians.stream()
				.map(p -> new PhysicianSelector(p, i.getAndIncrement())).collect(Collectors.toList());

		// checking if physician is already added to the task
		loop: for (PhysicianSelector physicianSelector : resultList) {
			// adds the role to the PhysicianSelector to display that the physician is
			// already added
			for (AssociatedContact associatedContact : task.getContacts()) {
				if (associatedContact.getPerson().equals(physicianSelector.getPhysician().getPerson())) {
					physicianSelector.addAssociatedRole(associatedContact.getRole());
					// checking if physician can be removed (removing only possible if no
					// notification was perfomred)
					physicianSelector.setContactSelector(new AssociatedContactSelector(associatedContact));
					physicianSelector.setContactOfTask(true);
					continue loop;
				}
			}
		}

		return resultList;
	}

}
