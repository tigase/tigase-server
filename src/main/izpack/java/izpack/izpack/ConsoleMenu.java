package com.izforge.izpack.installer;

import java.io.IOException;
import java.util.List;


public abstract class ConsoleMenu {
	private final ConsoleHelper console;
	private boolean done = false;
	
	public ConsoleMenu(ConsoleHelper console) {
		this.console = console;
	}

	public void run() 
	throws IOException 
	{
		while (isDone() == false) 
		{
			displayMenu();			
			console.displayEmptyLine();
			console.displayPrompt("Choose action");
			String userChoice = console.systemInAsReader().readLine();
			runAction(userChoice);
		}
	}

	public boolean isDone() {
		return done;
	}

	protected abstract List<IConsoleMenuItem> getMenuItems();
	protected abstract String getHeader();

	private IConsoleMenuItem redisplayMenuItem = new IConsoleMenuItem() {
		public String renderToString() {
			return "Redisplay menu";
		}
		public void runAction() {
			displayMenu();
		}
	};

	private IConsoleMenuItem doneMenuItem = new IConsoleMenuItem() {
		public String renderToString() {
			return "Done";
		}
		public void runAction() {
			done = true;
		}
	};

	private IConsoleMenuItem invalidActionMenuItem = new IConsoleMenuItem() {
		public String renderToString() {
			return "ZONK !!"; // shouldn't be called
		}
		public void runAction() {
			console.displayLabel("Invalid command");
		}
	};


	private void displayItem(IConsoleMenuItem item, String prefix) {
		String itemStr = item.renderToString();
		console.displayLabel(prefix + " => " + itemStr);
	}
	
	private void displayMenu() {
		List<IConsoleMenuItem> items = getMenuItems();
		console.displayLabel("------");
		console.displayLabel(getHeader());
		console.displayEmptyLine();
		
		for (int i = 0 ; i < items.size() ; i++) {
			IConsoleMenuItem item = items.get(i);
			displayItem(item, "" + (i+1));
		}
		displayItem(redisplayMenuItem, "r");
		displayItem(doneMenuItem, "d");
	}

	void runAction(String cmd) {
		
		List<? extends IConsoleMenuItem> menuItems = getMenuItems();

		int nmbOfItems = menuItems.size();
		int nmb = 0;
		try {
			nmb = Integer.parseInt(cmd);
		}
		catch (NumberFormatException nfe) {
			nmb = -1;
		}

		IConsoleMenuItem chosenAction = null;
		if (nmb >= 1 && nmb <= nmbOfItems) 
		{
			chosenAction = menuItems.get(nmb-1);
		} 
		else if ("r".equals(cmd)) 
		{
			chosenAction = redisplayMenuItem;
		} 
		else if ("d".equals(cmd)) 
		{
			chosenAction = doneMenuItem;
		} else 
		{
			chosenAction = invalidActionMenuItem;
		}
		
		chosenAction.runAction();
	}
	
	
}