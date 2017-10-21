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

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Immutable;
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
	private long dueDate = 0;

	@ManyToOne(targetEntity = PatientMenuModel.class)
	private PatientMenuModel parent;

	@ManyToMany(fetch = FetchType.EAGER)
	@Fetch(value = FetchMode.SUBSELECT)
	@JoinTable(name = "task_favouritelist", joinColumns = {
			@JoinColumn(table = "taskmenumodel", name = "task_id", referencedColumnName = "id") }, inverseJoinColumns = {
					@JoinColumn(table = "favouritelist", name = "favouritelists_id", referencedColumnName = "id", nullable = true) })
	private List<FavouriteList> favouriteLists;
}
