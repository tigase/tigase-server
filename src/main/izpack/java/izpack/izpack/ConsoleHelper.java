package com.izforge.izpack.installer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ConsoleHelper 
{
	public void readEnter() 
	throws IOException 
	{
		systemInAsReader().readLine();
	}
	
	public void displayAndBlock(String msg) 
	throws IOException 
	{
		System.out.println(msg);
		readEnter();
	}
	
	
	private BufferedReader reader = null;
	public BufferedReader systemInAsReader() 
	{
		if (reader == null)
		{
			reader = new BufferedReader(
					new InputStreamReader(System.in));
		}
		return reader;
	}

	public void displayLabel(String label) {
		System.out.println(label);
	}
	
	public void displayPrompt(String prompt) {
		
		// cut : or :SPACE suffix
		String[] suffixesToCut = { ":", ": " };
		String newPrompt = prompt;
		for (String suffix : suffixesToCut) {
			if (newPrompt.endsWith(suffix)) 
			{
				int endIndex = newPrompt.length()-suffix.length();
				newPrompt = newPrompt.substring(0, endIndex);
			}
		}
		
		System.out.print(newPrompt + ": ");
	}

	public <T> T readUntilValid(
			String prompt, 
			ValidatingConverter<String, T> converter) 
	throws IOException 
	{
		BufferedReader sysIn = systemInAsReader(); 
		
		T result = null;
		do 
		{
			displayPrompt(prompt);
			String line = sysIn.readLine();
			Option<T> validationResult = 
				converter.convert(line);
			if (validationResult.isDefined) 
			{
				result = validationResult.value;
			} else {
				String errorMsg = validationResult.msg;
				displayLabel(errorMsg);
			}
			
		} while (result == null);
		
		return result;
		
	}

	public int chooseAction(String... actions) 
	throws IOException {
		final int numberOfActions = actions.length;
		if (numberOfActions < 2) {
			throw new IllegalArgumentException("Invalid number of " +
					"actions to choose from");
		}
		
		for (int i = 0 ; i < numberOfActions ; i++) {
			displayLabel( (i+1) + ".  " + actions[i]);
		}
		
		String prompt = "Choose number (1-" + numberOfActions + ")";
		return readUntilValid(prompt, new ValidatingConverter<String, Integer>() {
			public Option<Integer> convert(String from) {
				int number = 0;
				try {
					number = Integer.parseInt(from);
				} catch (NumberFormatException nfe)  {
					return Option.empty("Illegal number");
				}
				
				if (number < 1 || number > numberOfActions) {
					return Option.empty("Choice not in range");
				}
				
				return Option.full(number);
			}
		});
	}

	public void displayEmptyLine() {
		System.out.println();
		
	}

	private interface IPasswordReader {
		String getPassword(String prompt) throws IOException;
	}
	private IPasswordReader passwordReader = null;

	private IPasswordReader getPasswordReader() 
	{
		if (passwordReader == null)  // create only once
		{
// doesn't seem to work for me anyway :/
//			if (System.console() != null) 
//			{
//				// java 6 passwords support
//				passwordReader = new IPasswordReader() 
//				{
//					public String getPassword(String prompt) throws IOException 
//					{
//						displayPrompt(prompt);
//						char[] pwd = System.console().readPassword(); 
//						return pwd == null ? null : pwd.toString();
//					}
//				};
//			} 
//			else 
//			{
				// fall back to display password on console :(
				passwordReader = new IPasswordReader() 
				{
					public String getPassword(String prompt) throws IOException 
					{
						System.out.println("WARNING: password will be visible while entering");
						displayPrompt(prompt);
						return systemInAsReader().readLine();
					}
				};
//			}
		}
		return passwordReader;
	}
	
	public String askForPassword(String prompt) 
	throws IOException 
	{
		return getPasswordReader().getPassword(prompt);
	}

	public void displayMessage(String msg) {
		System.out.println(msg);
	}

	public void displayRaw(String msg) {
		System.out.print(msg);
	}	
	
}