package org.histo.model.transitory.settings;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.histo.model.interfaces.GsonAble;
import org.histo.util.interfaces.FileHandlerUtil;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.reflect.TypeToken;

import lombok.Getter;
import lombok.Setter;

/**
 * <b>Change:</b> <b>Fix:</b> <b>Added:</b> Version 1.2.3 1 = Significant change
 * 2 = feature added 3 = bugfix
 * 
 * @author glatza
 *
 */
@Getter
@Setter
public class Version {

	private static final String PREFIX_VERSION = "+v ";
	private static final String PREFIX_INFO = "+++";
	private static final String SUFFIX_FIX = "!";
	private static final String SUFFIX_CHANGE = "*";
	private static final String SUFFIX_ADDED = "+";
	private static final String SUFFIX_REMOVED = "-";

	private static final String FIX_STR = "<b>Fix: </b>";
	private static final String CHANGE_STR = "<b>Change: </b>";
	private static final String ADDED_STR = "<b>Added: </b>";
	private static final String REMOVED_STR = "<b>Remove: </b>";

	private String version;

	private List<String> changes;

	/**
	 * Factory loads a list of Version Objects from a json file
	 * 
	 * @param jsonFile
	 * @return
	 */
	public static final List<Version> factroy(String file) {

		List<String> fileContent = FileHandlerUtil.getContentOfFileAsArray(file);
		ArrayList<Version> resultArray = new ArrayList<Version>();

		Version currentVersion = null;

		for (String content : fileContent) {
			// if string starts with version prefix, start new version
			if (content.startsWith(PREFIX_VERSION)) {
				currentVersion = new Version();
				currentVersion.setVersion(content.substring(3));
				currentVersion.setChanges(new ArrayList<String>());
				resultArray.add(currentVersion);
			} else {
				// if version info
				if (content.startsWith(PREFIX_INFO) && currentVersion != null) {
					// check which version info is present

					if (content.regionMatches(PREFIX_INFO.length(), SUFFIX_FIX, 0, SUFFIX_FIX.length())) {
						currentVersion.getChanges()
								.add(FIX_STR + content.substring(PREFIX_INFO.length() + SUFFIX_FIX.length() +1));
					} else if (content.regionMatches(PREFIX_INFO.length(), SUFFIX_CHANGE, 0, SUFFIX_CHANGE.length())) {
						currentVersion.getChanges()
								.add(CHANGE_STR + content.substring(PREFIX_INFO.length() + SUFFIX_FIX.length()+1));
					} else if (content.regionMatches(PREFIX_INFO.length(), SUFFIX_ADDED, 0, SUFFIX_ADDED.length())) {
						currentVersion.getChanges()
								.add(ADDED_STR + content.substring(PREFIX_INFO.length() + SUFFIX_FIX.length()+1));
					} else if (content.regionMatches(PREFIX_INFO.length(), SUFFIX_REMOVED, 0,
							SUFFIX_REMOVED.length())) {
						currentVersion.getChanges()
								.add(REMOVED_STR + content.substring(PREFIX_INFO.length() + SUFFIX_FIX.length()+1));
					} else {
						currentVersion.getChanges().add(content.substring(PREFIX_INFO.length()+1));
					}
				}
			}
		}

		return resultArray;
	}

}
