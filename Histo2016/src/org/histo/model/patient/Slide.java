package org.histo.model.patient;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SelectBeforeUpdate;
import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.histo.config.enums.Dialog;
import org.histo.model.StainingPrototype;
import org.histo.model.interfaces.ArchivAble;
import org.histo.model.interfaces.CreationDate;
import org.histo.model.interfaces.DeleteAble;
import org.histo.model.interfaces.LogAble;
import org.histo.model.interfaces.Parent;
import org.histo.model.interfaces.SaveAble;
import org.histo.util.TaskUtil;

@Entity
@Audited
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
@SelectBeforeUpdate(true)
@DynamicUpdate(true)
@SequenceGenerator(name = "slide_sequencegenerator", sequenceName = "slide_sequence")
public class Slide implements Parent<Block>, LogAble, CreationDate, DeleteAble, SaveAble {

	private long id;

	private long version;

	private long creationDate;

	private String slideID = "";

	private int uniqueIDinBlock;

	private boolean stainingPerformed;

	private boolean reStaining;

	private String commentary = "";

	private StainingPrototype slidePrototype;

	/**
	 * Eltern Block des Stainings
	 */
	private Block parent;

	@Transient
	public void updateNameOfSlide() {
		// generating block id
		String number = "";
		int stainingsInBlock = TaskUtil.getNumerOfSameStainings(parent, slidePrototype);

		if (stainingsInBlock > 1)
			number = " " + String.valueOf(stainingsInBlock);

		setSlideID(parent.getParent().getSampleID() + parent.getBlockID() + " " + slidePrototype.getName() + number);
	}

	@Id
	@GeneratedValue(generator = "slide_sequencegenerator")
	@Column(unique = true, nullable = false)
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	@Version
	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
	}

	public boolean isStainingPerformed() {
		return stainingPerformed;
	}

	public void setStainingPerformed(boolean stainingPerformed) {
		this.stainingPerformed = stainingPerformed;
	}

	@OneToOne
	@NotAudited
	public StainingPrototype getSlidePrototype() {
		return slidePrototype;
	}

	public void setSlidePrototype(StainingPrototype slidePrototype) {
		this.slidePrototype = slidePrototype;
	}

	public long getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(long creationDate) {
		this.creationDate = creationDate;
	}

	public String getSlideID() {
		return slideID;
	}

	public void setSlideID(String slideID) {
		this.slideID = slideID;
	}

	public boolean isReStaining() {
		return reStaining;
	}

	public void setReStaining(boolean reStaining) {
		this.reStaining = reStaining;
	}

	@Type(type = "text")
	public String getCommentary() {
		return commentary;
	}

	public void setCommentary(String commentary) {
		this.commentary = commentary;
	}

	public int getUniqueIDinBlock() {
		return uniqueIDinBlock;
	}

	public void setUniqueIDinBlock(int uniqueIDinBlock) {
		this.uniqueIDinBlock = uniqueIDinBlock;
	}

	/********************************************************
	 * Interface Parent
	 ********************************************************/
	@ManyToOne
	public Block getParent() {
		return parent;
	}

	public void setParent(Block parent) {
		this.parent = parent;
	}

	/**
	 * Überschreibt Methode aus dem Interface StainingTreeParent
	 */
	@Transient
	@Override
	public Patient getPatient() {
		return getParent().getPatient();
	}
	
	/**
	 * Returns the parent task
	 */
	@Override
	@Transient
	public Task getTask() {
		return getParent().getTask();
	}
	/********************************************************
	 * Interface Parent
	 ********************************************************/

	/********************************************************
	 * Interface ArchiveAble
	 ********************************************************/
	/**
	 * Überschreibt Methode aus dem Interface ArchiveAble <br>
	 * Gibt die ObjektträgerID als identifier zurück
	 */
	@Transient
	@Override
	public String getTextIdentifier() {
		return "Objektträger " + getSlideID();
	}

	/**
	 * Überschreibt Methode aus dem Interface ArchiveAble <br>
	 * Gibt den Dialog zum archivieren zurück
	 */
	@Transient
	@Override
	public Dialog getArchiveDialog() {
		return Dialog.DELETE_TREE_ENTITY;
	}
	/********************************************************
	 * Interface ArchiveAble
	 ********************************************************/
	
	/********************************************************
	 * Interface SaveAble
	 ********************************************************/
	@Override
	@Transient
	public String getLogPath() {
		return getParent().getLogPath() + ", Slide-ID: " + getSlideID() + " (" + getId() + ")";
	}
	/********************************************************
	 * Interface SaveAble
	 ********************************************************/
}
