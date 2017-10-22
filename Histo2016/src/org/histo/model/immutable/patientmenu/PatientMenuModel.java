package org.histo.model.immutable.patientmenu;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Transient;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Type;

import lombok.Getter;
import lombok.Setter;

@Entity
@Immutable
@Getter
@Setter
public class PatientMenuModel {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(updatable = false, nullable = false)
	private Long id;

	@Column(columnDefinition = "VARCHAR")
	private String lastName;

	@Column(columnDefinition = "VARCHAR")
	private String firstName;

	@Column(columnDefinition = "VARCHAR")
	private String title = "";

	@Column
	private String piz = "";

	@Column
	private long creationDate;

	@Column(columnDefinition = "VARCHAR")
	@Type(type = "date")
	private Date birthday;

	@OneToMany(mappedBy = "parent", fetch = FetchType.EAGER)
	@Fetch(value = FetchMode.SUBSELECT)
	@OrderBy("id DESC")
	private List<TaskMenuModel> tasks;

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof PatientMenuModel && ((PatientMenuModel) obj).getId() == getId())
			return true;

		return super.equals(obj);
	}

	@Transient
	public List<TaskMenuModel> getActiveTasks() {
		return getActiveTasks(false);
	}

	/**
	 * Returns a list with all currently active tasks of a Patient
	 * 
	 * @return
	 */
	@Transient
	public List<TaskMenuModel> getActiveTasks(boolean activeOnly) {
		return getTasks().stream().filter(p -> p.isActiveOrActionPending(activeOnly)).collect(Collectors.toList());
	}

	@Transient
	public boolean hasActiveTasks() {
		return hasActiveTasks(false);
	}

	/**
	 * Returns true if at least one task is marked as active
	 * 
	 * @param patient
	 * @return
	 */
	@Transient
	public boolean hasActiveTasks(boolean activeOnly) {
		return getTasks().stream().anyMatch(p -> p.isActiveOrActionPending(activeOnly));
	}

	/**
	 * Returns a list with tasks which are not active
	 * 
	 * @return
	 */
	@Transient
	public List<TaskMenuModel> getNoneActiveTasks() {
		return getTasks().stream().filter(p -> !p.isActiveOrActionPending()).collect(Collectors.toList());
	}

	/**
	 * Returns true if at least one task is not marked as active
	 * 
	 * @param patient
	 * @return
	 */
	@Transient
	public boolean hasNoneActiveTasks() {
		return getTasks().stream().anyMatch(p -> !p.isActiveOrActionPending());
	}
}
