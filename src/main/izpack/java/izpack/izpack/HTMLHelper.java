package com.izforge.izpack.installer;


public class HTMLHelper {
	
	public String extractTextFromSimplifiedHTML(String htmlString) {
		// pattern relates to replace, when updating
		// be careful to keep the indexes in order
		// replacements will be performed in the same order
		// as they are in the two arrays
		String[] patterns = {
				"<[^>]*>",
				"&lt;",
				"&gt;",
				"&amp;",
				"&copy;",
				"&ldquo;",
				"&rdquo;"
		};
		String[] replaces = {
				"", // remove tags
				"<", // resolve <
				">", // resolve >
				"&", // resolve &
				"(C)", // turns &copy; to "(C)"
				"\"", // resolve left quotation
				"\"", // resolve right quotation
		};
		
		String result = htmlString;
		for (int i = 0 ; i < patterns.length ; i++) {
			String pattern = patterns[i];
			String replace = replaces[i];
			result = result.replaceAll(pattern, replace);
		}
		
		return result;
	}
	
}