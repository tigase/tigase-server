/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */

package com.izforge.izpack.panels;

import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import com.izforge.izpack.gui.IzPanelLayout;
import com.izforge.izpack.installer.InstallData;
import com.izforge.izpack.installer.InstallerFrame;
import com.izforge.izpack.installer.IzPanel;
import com.izforge.izpack.panels.TigaseInstallerDBHelper.MsgTarget;
import com.izforge.izpack.panels.TigaseInstallerDBHelper.ResultMessage;
import com.izforge.izpack.panels.TigaseInstallerDBHelper.TigaseDBTask;

/**
 * The Hello panel class.
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class TigaseDBCheckPanel extends IzPanel {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private JTable table = null;
	private Timer delayedTasks = new Timer("DelayedTasks", true);

	private final InstallData idata;

	/**
	 * The constructor.
	 *
	 * @param parent The parent.
	 * @param idata  The installation data.
	 */
	public TigaseDBCheckPanel(InstallerFrame parent, InstallData idata) {
		super(parent, TigaseInstallerCommon.init(idata), new IzPanelLayout());
		this.idata = idata;

		// The config label.
		String msg = parent.langpack.getString("TigaseDBCheckPanel.info");
		add(createMultiLineLabel(msg));
		add(IzPanelLayout.createParagraphGap());

		final String[] columnNames = new String[] {"Action", "Result"};
		
		TigaseDBTask[] tasks = TigaseInstallerDBHelper.Tasks.getTasksInOrder();
		final String[][] data = new String[tasks.length][];
		for (int i = 0 ; i < tasks.length ; i++) {
			TigaseDBTask task = tasks[i];
			data[i] = new String[] { task.getDescription(), "" };
		}
		
		TableModel dataModel = new AbstractTableModel() {
//				private String[] columnNames = names;
//				private Object[][] data = datas;
				public int getColumnCount() { return columnNames.length; }
				public int getRowCount() { return data.length; }
				public String getColumnName(int col) { return columnNames[col]; }
				public Object getValueAt(int row, int col) { return data[row][col]; }
				public Class getColumnClass(int c) { return getValueAt(0, c).getClass(); }
				public boolean isCellEditable(int row, int col) { return false; }
				public void setValueAt(Object value, int row, int col) {
					data[row][col] = value.toString();
					fireTableCellUpdated(row, col);
				}
      };
		// The table area which shows the info.
		table = new JTable(dataModel);
		//		table.setEditable(false);
		//add(table, NEXT_LINE);
		JScrollPane scroller = new JScrollPane(table);
		table.setFillsViewportHeight(true);
		add(scroller, NEXT_LINE);
		// At end of layouting we should call the completeLayout
		// method also they do nothing.
		getLayoutHelper().completeLayout();
	}

	public void panelActivate() {
		super.panelActivate();
		parent.lockNextButton();
		
		final TigaseInstallerDBHelper dbHelper = new TigaseInstallerDBHelper();
		
		delayedTasks.schedule(new TimerTask() {
				
			public void run() {
				TigaseDBTask[] tasks = TigaseInstallerDBHelper.Tasks.getTasksInOrder();
				
				for (int i = 0 ; i < tasks.length ; i++) {
					TigaseDBTask task = tasks[i];
					final int row = i;
					
					MsgTarget msgTarget = new MsgTarget() {
						public ResultMessage addResultMessage() {
							return new ResultMessage() {
								private String fullMsg = "";

								public void append(String msg) {
									fullMsg += msg;
									SwingUtilities.invokeLater(new Runnable() {
										public void run() {
											table.getModel().setValueAt(fullMsg, row, 1);
										}
									});
								}
							};
						}
					};
						
					task.execute(dbHelper, idata.getVariables(), msgTarget);
				}
				parent.unlockNextButton();
			}
		}, 500);
	}

	/**
	 * Indicates wether the panel has been validated or not.
	 *
	 * @return Always true.
	 */
	public boolean isValidated() {
		return true;
	}

}


