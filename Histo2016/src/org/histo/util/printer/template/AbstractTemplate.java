package org.histo.util.printer.template;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.histo.config.HistoSettings;
import org.histo.config.enums.DocumentType;
import org.histo.model.PDFContainer;
import org.histo.model.interfaces.HasID;
import org.histo.util.interfaces.FileHandlerUtil;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.reflect.TypeToken;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AbstractTemplate implements HasID, FileHandlerUtil {

	@Expose
	protected long id;

	@Expose
	protected String name;

	@Expose
	protected String file;

	@Expose
	protected String file2;

	@Expose
	protected DocumentType documentType;

	@Expose
	protected boolean defaultDocument;

	@Expose
	protected boolean doNotSave;

	public PDFContainer generatePDF(PDFGenerator generator) {
		return null;
	}

	/**
	 * Creates an array of texTample objects
	 * 
	 * @param jsonFile
	 * @return
	 */
	public static final AbstractTemplate[] factroy(String jsonFile) {
		return factroy(jsonFile, null);
	}

	/**
	 * Returns a filtered template list
	 * 
	 * @param jsonFile
	 * @param types
	 * @return
	 */
	public static final AbstractTemplate[] factroy(String jsonFile, DocumentType[] types) {

		Type type = new TypeToken<AbstractTemplate[]>() {
		}.getType();

		GsonBuilder gb = new GsonBuilder();
		gb.registerTypeAdapter(type, new TemplateDeserializer());
		
		Gson gson = gb.create();
		
		ArrayList<AbstractTemplate> result1 = gson.fromJson(FileHandlerUtil.getContentOfFile(jsonFile), type);

		AbstractTemplate[] result = result1.toArray(new AbstractTemplate[result1.size()]);
		
		if (types != null)
			result = AbstractTemplate.getTemplatesByTypes(result, types);

		return result;
	}

	/**
	 * Loads the default template list an returns a subselection containing the
	 * given type
	 * 
	 * @param types
	 * @return
	 */
	public static final AbstractTemplate[] getTemplatesByType(DocumentType types) {
		return getTemplatesByTypes(new DocumentType[] { types });
	}

	/**
	 * Loads the default list an returns a subselection containing the given
	 * types
	 * 
	 * @param types
	 * @return
	 */
	public static final AbstractTemplate[] getTemplatesByTypes(DocumentType[] types) {
		AbstractTemplate[] templates = AbstractTemplate.factroy(HistoSettings.TEX_TEMPLATE_JSON);
		return getTemplatesByTypes(templates, types);
	}

	/**
	 * Returns templates matching the given types
	 * 
	 * @param tempaltes
	 * @param type
	 * @return
	 */
	public static final AbstractTemplate getTemplateByType(AbstractTemplate[] tempaltes, DocumentType type) {
		AbstractTemplate[] result = getTemplatesByType(tempaltes, type);
		if (result.length > 0)
			return result[0];
		return null;
	}

	/**
	 * Returns templates matching the given types
	 * 
	 * @param array
	 * @param type
	 * @return
	 */
	public static final AbstractTemplate[] getTemplatesByType(AbstractTemplate[] tempaltes, DocumentType type) {
		return getTemplatesByTypes(tempaltes, new DocumentType[] { type });
	}

	/**
	 * Returns templates matching the given types
	 * 
	 * @param tempaltes
	 * @param type
	 * @return
	 */
	public static final AbstractTemplate[] getTemplatesByTypes(AbstractTemplate[] tempaltes, DocumentType[] type) {
		List<AbstractTemplate> result = new ArrayList<AbstractTemplate>();

		logger.debug("Getting templates out of " + tempaltes.length);

		for (int i = 0; i < tempaltes.length; i++) {
			for (int y = 0; y < type.length; y++) {
				logger.debug("Template type one: " + tempaltes[i].getDocumentType() + ", template type two " + type[y]);
				if (tempaltes[i].getDocumentType() == type[y]) {
					logger.debug("Found Template type " + type + " name: " + tempaltes[i].getName());
					result.add(tempaltes[i]);
					break;
				}
			}
		}

		logger.debug("Found " + result.size() + " templates");

		AbstractTemplate[] resultArr = new AbstractTemplate[result.size()];

		return result.toArray(resultArr);
	}

	/**
	 * Returns the defaultTempalte of an list;
	 * 
	 * @param array
	 * @return
	 */
	public static final AbstractTemplate getDefaultTemplate(AbstractTemplate[] array) {
		for (int i = 0; i < array.length; i++) {
			if (array[i].isDefaultDocument())
				return array[i];
		}
		return null;
	}

	/**
	 * Returns the defaultTempalte of an list;
	 * 
	 * @param array
	 * @return
	 */
	public static final AbstractTemplate getDefaultTemplate(AbstractTemplate[] array, DocumentType ofType) {
		for (int i = 0; i < array.length; i++) {
			if (array[i].isDefaultDocument() && array[i].getDocumentType() == ofType)
				return array[i];
		}
		return null;
	}

	public String getContentOfFile() {
		return FileHandlerUtil.getContentOfFile(getFile());
	}

	public String getContentOfFile2() {
		return FileHandlerUtil.getContentOfFile(getFile2());
	}

}
