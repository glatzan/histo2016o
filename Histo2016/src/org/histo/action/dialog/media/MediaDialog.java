package org.histo.action.dialog.media;

import java.util.List;

import org.apache.log4j.Logger;
import org.histo.action.dialog.AbstractDialog;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.DocumentType;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.UtilDAO;
import org.histo.model.PDFContainer;
import org.histo.model.interfaces.HasDataList;
import org.histo.model.patient.Patient;
import org.histo.ui.interfaces.PdfGuiProvider;
import org.histo.ui.transformer.DefaultTransformer;
import org.histo.util.StreamUtils;
import org.primefaces.model.StreamedContent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Configurable
@Getter
@Setter
public class MediaDialog extends AbstractDialog implements PdfGuiProvider {

	private static Logger logger = Logger.getLogger("org.histo");

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private UtilDAO utilDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private UploadDialog uploadDialog;

	/**
	 * Description for the user what he or she is about to do
	 */
	private String actionDescription;

	/**
	 * True if in select mode, false if in media display mode
	 */
	private boolean selectMode;

	/**
	 * Patient of the dataLists
	 */
	private Patient patient;

	/**
	 * Temporary task
	 */
	private HasDataList[] dataLists;

	/**
	 * Transformer for datalists
	 */
	private DefaultTransformer<HasDataList> dataListsTransformer;

	/**
	 * Currently selected datalist
	 */
	private HasDataList selectedDatalist;

	/**
	 * Used for showing pdf in a dialog
	 */
	private PDFContainer pDFContainerToRender;

	/**
	 * Selected container, will not be set to null after dialog was closed
	 */
	private PDFContainer selectedPdfContainer;

	/**
	 * True if upload button should be shown
	 */
	private boolean uploadEndabled;

	/**
	 * Type of uploaded file, used if upload dialog should be shown
	 */
	private DocumentType uploadFileType;

	/**
	 * Type of uploaded file, used if upload dialog shoud be shown
	 */
	private DocumentType[] uploadAvailableFileType;

	/**
	 * Lists for that the data will be uploaded
	 */
	private HasDataList[] uploadDataLists;

	/**
	 * If true the selected pdf will be automatically copied into the given
	 * DataLists
	 */
	private boolean autoCopy;

	/**
	 * If true the selected file will be moved not copied to the new lists
	 */
	private boolean autoMove;

	/**
	 * If true the user can toggle the moveSelection option
	 */
	private boolean showAutoMoveOption;

	/**
	 * If advance copy mode is enabled, the selected file will be copied to these
	 * lists
	 */
	private HasDataList[] autoCopyModeTargetLists;

	/**
	 * If true pdf containers will be deleted on remove from datalist
	 */
	private boolean deleteOnRemove = false;

	public void initBeanInSelectMode(Patient patient, HasDataList toSelectFrom, PDFContainer mediaToDisplay) {
		initBeanInSelectMode(patient, new HasDataList[] { toSelectFrom }, toSelectFrom, mediaToDisplay);
	}

	public void initBeanInSelectMode(Patient patient, HasDataList[] toSelectFrom, HasDataList selectedDataList,
			PDFContainer mediaToDisplay) {
		initBean(patient, toSelectFrom, selectedDataList, mediaToDisplay, true);
	}

	public void initBeanInDisyplayMode(Patient patient, HasDataList toSelectFrom, PDFContainer mediaToDisplay) {
		initBeanInDisyplayMode(patient, new HasDataList[] { toSelectFrom }, toSelectFrom, mediaToDisplay);
	}

	public void initBeanInDisyplayMode(Patient patient, HasDataList[] toSelectFrom, HasDataList selectedDataList,
			PDFContainer mediaToDisplay) {
		initBean(patient, toSelectFrom, selectedDataList, mediaToDisplay, false);
	}

	public boolean initBean(Patient patient, HasDataList dataList, boolean selectMode) {
		return initBean(patient, new HasDataList[] { dataList }, dataList, null, selectMode);
	}

	public boolean initBean(Patient patient, HasDataList dataList, PDFContainer mediaToDisplay, boolean selectMode) {
		return initBean(patient, new HasDataList[] { dataList }, dataList, mediaToDisplay, selectMode);
	}

	public boolean initBean(Patient patient, HasDataList[] dataLists, boolean selectMode) {
		return initBean(patient, dataLists, dataLists[0], null, selectMode);
	}

	public boolean initBean(Patient patient, HasDataList[] dataLists, PDFContainer mediaToDisplay, boolean selectMode) {
		return initBean(patient, dataLists, dataLists[0], mediaToDisplay, selectMode);
	}

	public boolean initBean(Patient patient, HasDataList[] dataLists, HasDataList selectedDataList,
			PDFContainer mediaToDisplay, boolean selectMode) {
		return initBean(patient, dataLists, selectedDataList, mediaToDisplay, selectMode, true);
	}

	public boolean initBean(Patient patient, HasDataList[] dataLists, HasDataList selectedDataList,
			PDFContainer mediaToDisplay, boolean selectMode, boolean initDatalist) {

		if (initDatalist) {
			try {
				for (HasDataList hasDataList : dataLists) {
					utilDAO.initializeDataList(hasDataList);
				}
			} catch (CustomDatabaseInconsistentVersionException e) {
				onDatabaseVersionConflict();
				return false;
			}
		}

		setPatient(patient);
		setDataLists(dataLists);
		setDataListsTransformer(new DefaultTransformer<HasDataList>(dataLists, true));
		setSelectMode(selectMode);
		setSelectedDatalist(selectedDataList);

		if (mediaToDisplay == null && selectedDataList.getAttachedPdfs().size() > 0)
			mediaToDisplay = selectedDataList.getAttachedPdfs().get(0);

		setPDFContainerToRender(mediaToDisplay);

		super.initBean(null, Dialog.MEDIA);

		disableUpload();

		diableAutoCopyMode();

		return true;
	}

	public boolean initiBeanForExternalView(List<PDFContainer> pdfList, PDFContainer selectedPDf) {
		setDataLists(new HasDataList[] { new HasDataList() {

			private List<PDFContainer> list = pdfList;

			@Override
			public long getId() {
				return 1;
			}

			@Override
			public List<PDFContainer> getAttachedPdfs() {
				return list;
			}

			@Override
			public void setAttachedPdfs(List<PDFContainer> attachedPdfs) {
				list = attachedPdfs;
			}

			@Override
			public String getDatalistIdentifier() {
				return "";
			}
		} });

		setSelectedDatalist(getDataLists()[0]);

		setPDFContainerToRender(selectedPDf);
		return true;
	}

	public void enableUpload(HasDataList[] dataListsToUpload, DocumentType[] availableFileType) {
		enableUpload(dataListsToUpload, availableFileType, availableFileType[0]);
	}

	public void enableUpload(HasDataList[] dataListsToIpload, DocumentType[] availableFileType,
			DocumentType selectedFileType) {
		logger.trace("Uploadmode activated");
		setUploadEndabled(true);
		setUploadAvailableFileType(availableFileType);
		setUploadFileType(selectedFileType);
		setUploadDataLists(dataListsToIpload);
	}

	public void disableUpload() {
		setUploadEndabled(false);
		setUploadAvailableFileType(null);
		setUploadDataLists(null);
		setUploadFileType(null);
	}

	public void showUploadDialog() {
		if (isUploadEndabled()) {
			uploadDialog.initAndPrepareBean(getUploadDataLists(), getPatient(), getUploadAvailableFileType(),
					getUploadFileType());
		}
	}

	public void enableAutoCopyMode(HasDataList[] dataListsToCopy, boolean movePdfs, boolean showMovePdfsOption)
			throws CustomDatabaseInconsistentVersionException {
		logger.trace("Advance copy mode activated");
		for (HasDataList hasDataList : dataListsToCopy) {
			utilDAO.initializeDataList(hasDataList);
		}

		setAutoCopy(true);
		setAutoCopyModeTargetLists(dataListsToCopy);
		setAutoMove(movePdfs);
		setShowAutoMoveOption(showMovePdfsOption);
	}

	public void diableAutoCopyMode() {
		setAutoCopy(false);
		setAutoCopyModeTargetLists(null);
		setAutoMove(false);
	}

	public boolean hasMoreDatalists() {
		if (getDataLists().length > 1)
			return true;
		return false;
	}

	public void onSelectData() {
		setSelectedPdfContainer(getPDFContainerToRender());

		try {
			if (isAutoCopy() && getAutoCopyModeTargetLists() != null && getAutoCopyModeTargetLists().length > 0) {

				logger.debug("Auto copy is enabled");
				// true if the to copy pdf was found in a target copy list, if
				// move
				// pdf (if enabled) will no be performed
				boolean foundInToCopyLists = false;

				for (HasDataList copyToList : getAutoCopyModeTargetLists()) {
					if (copyToList.getAttachedPdfs().stream()
							.anyMatch(p -> p.getId() == getSelectedPdfContainer().getId())) {
						foundInToCopyLists = true;
						logger.debug("Found file in target list, do not remove");
						continue;
					}

					copyToList.getAttachedPdfs().add(getSelectedPdfContainer());

					logger.debug("Adding file to targetlist " + copyToList.getDatalistIdentifier());
					genericDAO.savePatientData(copyToList, getPatient(), "log.patient.pdf.attached",
							getSelectedPdfContainer().getName());

				}

				// removing pdf form selected datalist if autoMove is enabled
				// and the pdf was not found in the target copy list
				if (isAutoMove() && !foundInToCopyLists) {
					logger.debug("Removing file from " + getSelectedDatalist().getDatalistIdentifier());
					removeFromDataList(getSelectedDatalist(), getSelectedPdfContainer());
				}
			}

		} catch (

		CustomDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
	}

	public void onUploadReturn() {
		if (getSelectedPdfContainer() == null && getSelectedDatalist().getAttachedPdfs().size() > 0) {
			setSelectedPdfContainer(getSelectedDatalist().getAttachedPdfs().get(0));
		}
	}

	public void abortDialog() {
		super.hideDialog();
		if (isAutoCopy() && isSelectMode())
			setSelectedPdfContainer(null);
	}

	public void removeFromDataList(HasDataList dataList, PDFContainer container)
			throws CustomDatabaseInconsistentVersionException {
		removeFileFormDataList(getPatient(), dataList, container, isDeleteOnRemove());
	}

	/**
	 * Removing file form datalist
	 * 
	 * @param dataList
	 * @param container
	 * @throws CustomDatabaseInconsistentVersionException
	 */
	public void removeFileFormDataList(Patient patient, HasDataList dataList, PDFContainer container, boolean delete)
			throws CustomDatabaseInconsistentVersionException {
		try {

			PDFContainer toRemove = dataList.getAttachedPdfs().stream().filter(p -> p.getId() == container.getId())
					.collect(StreamUtils.singletonCollector());

			dataList.getAttachedPdfs().remove(toRemove);

			genericDAO.savePatientData(dataList, patient, "log.patient.pdf.removed", container.getName());

			logger.debug("Removed PDF (" + container.getName() + ") from Datalist");
			if (delete)
				genericDAO.delete(container, "log.patient.pdft.deleted", new Object[] { container.getName() });

		} catch (IllegalStateException e) {
		}
	}

	public StreamedContent getPdfContent() {
		return PdfGuiProvider.super.getPdfContent();
	}
}
