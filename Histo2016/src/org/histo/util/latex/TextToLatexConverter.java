package org.histo.util.latex;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;

public class TextToLatexConverter {

	public String convertToTex(String string) {
		if (string == null)
			return "";

		for (Map.Entry<String, String> entry : charMap.entrySet()) {
			string = StringUtils.replace(string, entry.getKey(), entry.getValue());
		}
		return string;
	}

	/**
	 * \& \% \$ \# \_ \{ \}
	 * 
	 * @author andi
	 *
	 */
	Map<String, String> charMap = MapUtils.putAll(new HashMap<String, String>(),
			new String[][] { 
				{ "\r\n", "\\\\ \r\n" }, 
				{ "&", "\\&" }, 
				{ "%", "\\%" }, 
				{ "$", "\\$" }, 
				{ "#", "\\#" },
				{ "{", "\\{" }, 
				{ "}", "\\}" }, 
				{ "_", "\\_" } 
			});
}
