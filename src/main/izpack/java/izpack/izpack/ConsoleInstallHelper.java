package com.izforge.izpack.installer;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;


public class ConsoleInstallHelper {
	// helpers
	public final ConsoleHelper console = new ConsoleHelper();
	public final IOHelper io = new IOHelper();
	public final ResourceHelper resource = new ResourceHelper();
	public final ConsolePager pager = new ConsolePager();
	public final HTMLHelper html = new HTMLHelper();
	public final VariablesHelper variables = new VariablesHelper();
	
	// singleton, use getInstance
	public static ConsoleInstallHelper getInstance() { return instance; }
	private ConsoleInstallHelper() {}
	private static final ConsoleInstallHelper instance = new ConsoleInstallHelper();
		
}


class IOHelper {
	
	public String readAll(BufferedReader reader) 
	throws IOException 
	{
		StringBuilder builder = new StringBuilder();
		
		String line = "";
		do 
		{
			line = reader.readLine();
			if (line != null) 
			{
				builder.append(line);
				builder.append("\n");
			}
		} while (line != null);
		
		return builder.toString();
	}
	
}


class TextHelper {
	List<String> convertToLines(String stringWithNewlines) {
		String[] lines = stringWithNewlines.split("\n");
		return Arrays.asList(lines);
	}
}
