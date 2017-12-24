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

import lombok.Getter;
import lombok.Setter;

@Entity
@Audited
@SelectBeforeUpdate(true)
@DynamicUpdate(true)
@SequenceGenerator(name = "block_sequencegenerator", sequenceName = "block_sequence")
@Getter
@Setter
public class Block
		implements LogAble, DeleteAble, Parent<Sample>, PatientRollbackAble<Sample>, IdManuallyAltered, HasID {

	@Id
	@GeneratedValue(generator = "block_sequencegenerator")
	@Column(unique = true, nullable = false)
	private long id;

	@Version
	private long version;

	/**
	 * Parent of this block
	 */
	@ManyToOne
	private Sample parent;

	/**
	 * ID in block
	 */
	@Column
	private String blockID = "";

	/**
	 * True if the user has manually altered the sample ID
	 */
	@Column
	private boolean idManuallyAltered;

	/**
	 * staining array
	 */
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "parent")
	@Fetch(value = FetchMode.SUBSELECT)
	@OrderBy("creationDate ASC, id ASC")
	private List<Slide> slides = new ArrayList<Slide>();

	/**
	 * Date of sample creation
	 */
	@Column
	private long creationDate;

	@Transient
	public void removeStaining(Slide staining) {
		getSlides().remove(staining);
	}

	@Transient
	public boolean updateNameOfBlock(boolean useAutoNomenclature, boolean ignoreManuallyNamedItems) {
		if (!isIdManuallyAltered() || (ignoreManuallyNamedItems && isIdManuallyAltered())) {
			if (useAutoNomenclature) {
				String name;

				if (parent.getBlocks().size() > 1) {
					name = TaskUtil.getCharNumber(getParent().getBlocks().indexOf(this));
				} else {
					// no block name
					name = "";
				}

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
	public void updateAllNames() {
		updateAllNames(getTask().isUseAutoNomenclature(), false);
	}

	@Transient
	public void updateAllNames(boolean useAutoNomenclature, boolean ignoreManuallyNamedItems) {
		updateNameOfBlock(useAutoNomenclature, ignoreManuallyNamedItems);
		getSlides().stream().forEach(p -> p.updateNameOfSlide(useAutoNomenclature, ignoreManuallyNamedItems));
	}

	@Override
	public String toString() {
		return "Block: " + getBlockID() + (getId() != 0 ? ", ID: " + getId() : "");
	}

	/********************************************************
	 * Interface Parent
	 ********************************************************/
	/**
	 * �berschreibt Methode aus dem Interface StainingTreeParent
	 */
	@Transient
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
}
