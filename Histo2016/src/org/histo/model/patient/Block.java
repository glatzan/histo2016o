package org.histo.model.patient;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.SequenceGenerator;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.SelectBeforeUpdate;
import org.hibernate.envers.Audited;
import org.histo.config.enums.Dialog;
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
@SequenceGenerator(name = "block_sequencegenerator", sequenceName = "block_sequence")
public class Block implements Parent<Sample>, LogAble, DeleteAble, PatientRollbackAble, IdManuallyAltered, HasID {

	private long id;

	private long version;

	/**
	 * Parent of this block
	 */
	private Sample parent;

	/**
	 * ID in block
	 */
	private String blockID = "";

	/**
	 * True if the user has manually altered the sample ID
	 */
	private boolean idManuallyAltered;

	/**
	 * staining array
	 */
	private List<Slide> slides;

	/**
	 * Date of sample creation
	 */
	private long creationDate;

	public void removeStaining(Slide staining) {
		getSlides().remove(staining);
	}

	@Transient
	public boolean updateNameOfBlock(boolean useAutoNomenclature, boolean ignoreManuallyNamedItems) {
		if (!isIdManuallyAltered() || (ignoreManuallyNamedItems && isIdManuallyAltered())) {
			if (useAutoNomenclature && parent.getBlocks().size() > 1) {
				String name = TaskUtil.getCharNumber(getParent().getBlocks().indexOf(this));
				if (getBlockID() == null || !getBlockID().equals(name)) {
					setBlockID(name);
					setIdManuallyAltered(false);
					return true;
				}
			}
		}

		return false;
	}

	@Transient
	public void updateAllNames(boolean useAutoNomenclature, boolean ignoreManuallyNamedItems) {
		updateNameOfBlock(useAutoNomenclature, ignoreManuallyNamedItems);
		getSlides().stream().forEach(p -> p.updateNameOfSlide(useAutoNomenclature, ignoreManuallyNamedItems));
	}

	@Override
	public String toString() {
		return "ID: " + getId() + " BlockID: " + getBlockID();
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/
	@Id
	@GeneratedValue(generator = "block_sequencegenerator")
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

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "parent")
	@Fetch(value = FetchMode.SUBSELECT)
	@OrderBy("creationDate ASC, id ASC")
	public List<Slide> getSlides() {
		if (slides == null)
			slides = new ArrayList<>();
		return slides;
	}

	public void setSlides(List<Slide> slides) {
		this.slides = slides;
	}

	public String getBlockID() {
		return blockID;
	}

	public void setBlockID(String blockID) {
		this.blockID = blockID;
	}

	public long getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(long creationDate) {
		this.creationDate = creationDate;
	}

	public boolean isIdManuallyAltered() {
		return idManuallyAltered;
	}

	public void setIdManuallyAltered(boolean idManuallyAltered) {
		this.idManuallyAltered = idManuallyAltered;
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/

	/********************************************************
	 * Interface Parent
	 ********************************************************/
	@ManyToOne
	public Sample getParent() {
		return parent;
	}

	public void setParent(Sample parent) {
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
	 * Gibt die BlockID als identifier zur�ck
	 */
	@Transient
	@Override
	public String getTextIdentifier() {
		return "Block " + getBlockID();
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
		return getParent().getLogPath() + ", Block-ID: " + getBlockID() + " (" + getId() + ")";
	}
	/********************************************************
	 * Interface PatientRollbackAble
	 ********************************************************/
}
