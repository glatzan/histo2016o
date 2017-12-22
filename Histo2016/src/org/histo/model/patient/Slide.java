package org.histo.model.patient;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SelectBeforeUpdate;
import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.histo.config.enums.Dialog;
import org.histo.model.StainingPrototype;
import org.histo.model.interfaces.DeleteAble;
import org.histo.model.interfaces.HasID;
import org.histo.model.interfaces.IdManuallyAltered;
import org.histo.model.interfaces.LogAble;
import org.histo.model.interfaces.Parent;
import org.histo.model.interfaces.PatientRollbackAble;
import org.histo.util.TaskUtil;

@Entity
@Audited
@SelectBeforeUpdate(true)
@DynamicUpdate(true)
@SequenceGenerator(name = "slide_sequencegenerator", sequenceName = "slide_sequence")
public class Slide implements Parent<Block>, LogAble, DeleteAble, PatientRollbackAble, IdManuallyAltered, HasID {

	private long id;

	private long version;

	private int uniqueIDinTask;

	private String slideID = "";

	private boolean idManuallyAltered;

	private long creationDate;

	private long completionDate;

	private boolean stainingCompleted;

	private boolean reStaining;

	private String commentary = "";

	private StainingPrototype slidePrototype;

	/**
	 * Eltern Block des Stainings
	 */
	private Block parent;

	@Transient
	public void updateAllNames(boolean useAutoNomenclature, boolean ignoreManuallyNamedItems) {
		updateNameOfSlide(useAutoNomenclature,ignoreManuallyNamedItems);
	}
	
	@Transient
	public boolean updateNameOfSlide(boolean useAutoNomenclature, boolean ignoreManuallyNamedItems) {
		if (!isIdManuallyAltered() || (isIdManuallyAltered() && ignoreManuallyNamedItems)) {
			StringBuilder name = new StringBuilder();

			if (useAutoNomenclature) {
				
				System.out.println(parent.getParent().getSampleID() + " ");
				
				// generating block id
				name.append(parent.getParent().getSampleID());
				name.append(parent.getBlockID());
				if (name.length() > 0)
					name.append(" ");

				name.append(slidePrototype.getName());

				int stainingsInBlock = TaskUtil.getNumerOfSameStainings(this);

				if (stainingsInBlock > 1)
					name.append(String.valueOf(stainingsInBlock));

				if (getSlideID() == null || !getSlideID().equals(name)) {
					setSlideID(name.toString());
					setIdManuallyAltered(false);
					return true;
				}
			} else if (getSlideID() == null || getSlideID().isEmpty()) {
				name.append(slidePrototype.getName());

				int stainingsInBlock = TaskUtil.getNumerOfSameStainings(this);

				if (stainingsInBlock > 1)
					name.append(String.valueOf(stainingsInBlock));

				setSlideID(name.toString());
				setIdManuallyAltered(false);
				return true;
			}
		}

		return false;
	}

	@Override
	@Transient
	public String toString() {
		return "ID: " + getId() + ", Slide ID: " + getSlideID();
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

	public boolean isStainingCompleted() {
		return stainingCompleted;
	}

	public void setStainingCompleted(boolean stainingCompleted) {
		this.stainingCompleted = stainingCompleted;
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

	public long getCompletionDate() {
		return completionDate;
	}

	public void setCompletionDate(long completionDate) {
		this.completionDate = completionDate;
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

	public int getUniqueIDinTask() {
		return uniqueIDinTask;
	}

	public void setUniqueIDinTask(int uniqueIDinTask) {
		this.uniqueIDinTask = uniqueIDinTask;
	}

	public boolean isIdManuallyAltered() {
		return idManuallyAltered;
	}

	public void setIdManuallyAltered(boolean idManuallyAltered) {
		this.idManuallyAltered = idManuallyAltered;
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
	 * �berschreibt Methode aus dem Interface StainingTreeParent
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
	 * Interface Delete Able
	 ********************************************************/
	/**
	 * �berschreibt Methode aus dem Interface ArchiveAble <br>
	 * Gibt die Objekttr�gerID als identifier zur�ck
	 */
	@Transient
	@Override
	public String getTextIdentifier() {
		return "Objekttr�ger " + getSlideID();
	}

	/**
	 * �berschreibt Methode aus dem Interface ArchiveAble <br>
	 * Gibt den Dialog zum archivieren zur�ck
	 */
	@Transient
	@Override
	public Dialog getArchiveDialog() {
		return Dialog.DELETE_TREE_ENTITY;
	}

	/********************************************************
	 * Interface Delete Able
	 ********************************************************/

	/********************************************************
	 * Interface PatientRollbackAble
	 ********************************************************/
	@Override
	@Transient
	public String getLogPath() {
		return getParent().getLogPath() + ", Slide-ID: " + getSlideID() + " (" + getId() + ")";
	}

	/********************************************************
	 * Interface PatientRollbackAble
	 ********************************************************/
}
