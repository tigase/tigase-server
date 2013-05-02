package com.izforge.izpack.installer;

import java.io.IOException;
import java.util.List;

public class ConsolePager 
{
	private static TextHelper textHelper = new TextHelper();
	
	private static int DEFAULT_NO_OF_LINES = 20;
	private static String DEFAULT_PROMPT = "--- Press ENTER to continue ---";
	
	public void displayLongText(String text) 
	throws IOException 
	{
		displayLongText(
				text, 
				DEFAULT_NO_OF_LINES,
				DEFAULT_PROMPT);
	}
	
	public void displayLongText(
			String text, 
			int noOfLinesEach, 
			String continuePrompt) 
	throws IOException 
	{
		List<String> lines = textHelper.convertToLines(text);
		
		ConsoleHelper helper = new ConsoleHelper();
		for (int i = 0 ; i < lines.size() ; i++) 
		{
			System.out.println(lines.get(i));
			if (i != 0 && i % noOfLinesEach == 0) 
			{
				helper.displayAndBlock(continuePrompt);
			}
		}
	}
	
}