package org.histo.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Transient;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SelectBeforeUpdate;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RevisionNumber;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.StaticList;
import org.histo.model.interfaces.ArchivAble;
import org.histo.model.interfaces.HasID;
import org.histo.model.interfaces.ListOrder;

@Entity
@Audited
@SelectBeforeUpdate(true)
@DynamicUpdate(true)
@SequenceGenerator(name = "listItem_sequencegenerator", sequenceName = "listItem_sequence")
public class ListItem implements ListOrder<ListItem>, ArchivAble, HasID {

	private long id;

	private StaticList listType;

	private String value;

	private int indexInList;
	
	private boolean archived;

	@Id
	@GeneratedValue(generator = "listItem_sequencegenerator")
	@Column(unique = true, nullable = false)
	@RevisionNumber
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public StaticList getListType() {
		return listType;
	}

	@Enumerated(EnumType.STRING)
	public void setListType(StaticList listType) {
		this.listType = listType;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public boolean isArchived() {
		return archived;
	}

	public void setArchived(boolean archived) {
		this.archived = archived;
	}
	/********************************************************
	 * Interface ListOrder
	 ********************************************************/
	@Column
	public int getIndexInList() {
		return indexInList;
	}

	public void setIndexInList(int indexInList) {
		this.indexInList = indexInList;
	}
	/********************************************************
	 * Interface ListOrder
	 ********************************************************/

	/********************************************************
	 * Interface ArchiveAble
	 ********************************************************/
	@Override
	@Transient
	public String getTextIdentifier() {
		return null;
	}

	@Override
	@Transient
	public Dialog getArchiveDialog() {
		return null;
	}
	/********************************************************
	 * Interface ArchiveAble
	 ********************************************************/
}
