package org.histo.model.patient;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Basic;
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

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.SelectBeforeUpdate;
import org.hibernate.envers.Audited;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.StainingStatus;
import org.histo.model.interfaces.ArchivAble;
import org.histo.model.interfaces.CreationDate;
import org.histo.model.interfaces.DeleteAble;
import org.histo.model.interfaces.LogAble;
import org.histo.model.interfaces.Parent;
import org.histo.model.interfaces.StainingInfo;
import org.histo.util.TaskUtil;

@Entity
@Audited
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
@SelectBeforeUpdate(true)
@DynamicUpdate(true)
@SequenceGenerator(name = "block_sequencegenerator", sequenceName = "block_sequence")
public class Block implements Parent<Sample>, StainingInfo, CreationDate, LogAble, DeleteAble {

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
	 * staining array
	 */
	private List<Slide> slides;

	/**
	 * Date of sample creation
	 */
	private long creationDate;

	/**
	 * Unique slide counter is increased for every added slide;
	 */
	private int uniqueSlideCounter = 0;

	public void removeStaining(Slide staining) {
		getSlides().remove(staining);
	}

	@Transient
	public int getNextSlideNumber() {
		return ++uniqueSlideCounter;
	}

	@Transient
	public void updateNameOfBlock(boolean useAutoNomenclature) {
		if (useAutoNomenclature && parent.getBlocks().size() > 1) {
			setBlockID(TaskUtil.getCharNumber(getParent().getBlocks().indexOf(this)));
		} else
			setBlockID(" ");
	}

	@Transient
	public void updateNamesOfSlides(boolean useAutoNomenclature){
		for (Slide slide : slides) {
			slide.updateNameOfSlide();
		}
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

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
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

	public int getUniqueSlideCounter() {
		return uniqueSlideCounter;
	}

	public void setUniqueSlideCounter(int uniqueSlideCounter) {
		this.uniqueSlideCounter = uniqueSlideCounter;
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/
	/********************************************************
	 * Interface StainingStauts
	 ********************************************************/
	/**
	 * Overwrites the {@link StainingInfo} interfaces new method. Returns true
	 * if the creation date was on the same as the current day.
	 */
	@Override
	@Transient
	public boolean isNew() {
		return isNew(getCreationDate());
	}

	/**
	 * Returns the status of the staining process. Either it can return staining
	 * performed, staining needed, restaining needed (restaining is returned if
	 * at least one staining is marked as restaining).
	 */
	@Override
	@Transient
	public StainingStatus getStainingStatus() {
		// if empty return staining needed
		if (slides.isEmpty())
			return StainingStatus.STAINING_NEEDED;

		boolean stainingNeeded = false;

		for (Slide staining : slides) {

			// continue if no staining is needed
			if (staining.isStainingPerformed())
				continue;
			else {
				// check if restaining is needed (restaining > staining) so
				// return that it is needed
				if (staining.isReStaining())
					return StainingStatus.RE_STAINING_NEEDED;
				else
					stainingNeeded = true;
			}
		}
		return stainingNeeded ? StainingStatus.STAINING_NEEDED : StainingStatus.PERFORMED;
	}

	/********************************************************
	 * Interface StainingStauts
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
	 * Überschreibt Methode aus dem Interface StainingTreeParent
	 */
	@Transient
	@Override
	public Patient getPatient() {
		return getParent().getPatient();
	}

	/********************************************************
	 * Interface Parent
	 ********************************************************/

	/********************************************************
	 * Interface ArchiveAble
	 ********************************************************/
	/**
	 * Überschreibt Methode aus dem Interface ArchiveAble <br>
	 * Gibt die BlockID als identifier zurück
	 */
	@Transient
	@Override
	public String getTextIdentifier() {
		return "Block " + getBlockID();
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

}
