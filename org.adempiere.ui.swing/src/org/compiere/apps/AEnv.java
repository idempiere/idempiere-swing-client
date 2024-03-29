/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2006 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
package org.compiere.apps;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.RepaintManager;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.adempiere.base.Core;
import org.adempiere.base.IResourceFinder;
import org.compiere.acct.Doc;
import org.compiere.db.CConnection;
import org.compiere.grid.ed.Calculator;
import org.compiere.model.I_AD_WF_Activity;
import org.compiere.model.I_AD_WF_Process;
import org.compiere.model.MMenu;
import org.compiere.model.MQuery;
import org.compiere.model.MRole;
import org.compiere.model.MTable;
import org.compiere.swing.CButton;
import org.compiere.swing.CFrame;
import org.compiere.swing.CMenuItem;
import org.compiere.util.CLogger;
import org.compiere.util.CacheMgt;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Ini;
import org.compiere.util.Msg;
import org.compiere.util.Splash;

/**
 *  Windows Application Environment and utilities
 *
 *  @author 	Jorg Janke
 *  @version 	$Id: AEnv.java,v 1.2 2006/07/30 00:51:27 jjanke Exp $
 *  
 *  @author Colin Rooney (croo) & kstan_79 RFE#1670185
 *  @author victor.perez@e-evolution.com 
 *  @see FR [ 1966328 ] New Window Info to MRP and CRP into View http://sourceforge.net/tracker/index.php?func=detail&aid=1966328&group_id=176962&atid=879335
 *  
 */
public final class AEnv
{
	// Array of active Windows
	private static ArrayList<Container>	s_windows = new ArrayList<Container>(20);
	
	/** Array of hidden Windows				*/
	private static ArrayList<CFrame>	s_hiddenWindows = new ArrayList<CFrame>();
	
	/** Closing Window Indicator			*/
	private static boolean 				s_closingWindows = false;
	
	/**
	 * 	Hide Window
	 *	@param window window
	 *	@return true if window is hidden, otherwise close it
	 */
	static public boolean hideWindow(CFrame window)
	{
		if (!Ini.isCacheWindow() || s_closingWindows)
			return false;
		for (int i = 0; i < s_hiddenWindows.size(); i++)
		{
			CFrame hidden = s_hiddenWindows.get(i);
			if (log.isLoggable(Level.INFO)) log.info(i + ": " + hidden);
			if (hidden.getAD_Window_ID() == window.getAD_Window_ID())
				return false;	//	already there
		}
		if (window.getAD_Window_ID() != 0)	//	workbench
		{
			if (s_hiddenWindows.add(window))
			{
				window.setVisible(false);
				if (log.isLoggable(Level.INFO)) log.info(window.toString());
			//	window.dispatchEvent(new WindowEvent(window, WindowEvent.WINDOW_ICONIFIED));
				if (s_hiddenWindows.size() > 10) {
					CFrame toClose = s_hiddenWindows.remove(0);		//	sort of lru
					try {
						s_closingWindows = true;
						toClose.dispose();
					} finally {
						s_closingWindows = false;
					}
				}
				return true;
			}
		}
		return false;
	}	//	hideWindow

	/**
	 * 	Show Window
	 *	@param AD_Window_ID window
	 *	@return true if window re-displayed
	 */
	static public CFrame showWindow (int AD_Window_ID)
	{
		for (int i = 0; i < s_hiddenWindows.size(); i++)
		{
			CFrame hidden = s_hiddenWindows.get(i);
			if (hidden.getAD_Window_ID() == AD_Window_ID)
			{
				s_hiddenWindows.remove(i);
				if (log.isLoggable(Level.INFO)) log.info(hidden.toString());
				hidden.setVisible(true);
				// De-iconify window - teo_sarca [ 1707221 ]
				int state = hidden.getExtendedState();
				if ((state & CFrame.ICONIFIED) > 0)
					hidden.setExtendedState(state & ~CFrame.ICONIFIED);
				//
				hidden.toFront();
				return hidden;
			}
		}
		return null;
	}	//	showWindow

	/**
	 * 	Clode Windows.
	 */
	static void closeWindows ()
	{
		s_closingWindows = true;
		for (int i = 0; i < s_hiddenWindows.size(); i++)
		{
			CFrame hidden = s_hiddenWindows.get(i);
			hidden.dispose();
		}
		s_hiddenWindows.clear();
		s_closingWindows = false;
	}	//	closeWindows
	
	/**
	 * Show window: de-iconify and bring it to front
	 * @author teo_sarca [ 1707221 ]
	 */
	public static void showWindow(Window window) {
		window.setVisible(true);
		if (window instanceof Frame) {
			Frame f = (Frame)window;
			int state = f.getExtendedState();
			if ((state & Frame.ICONIFIED) > 0)
				f.setExtendedState(state & ~Frame.ICONIFIED);
		}
		window.toFront();
	}

	/**
	 *  Show in the center of the screen.
	 *  (pack, set location and set visibility)
	 * 	@param window Window to position
	 */
	public static void showCenterScreen(Window window)
	{
		positionCenterScreen(window);
		showWindow(window);
	}   //  showCenterScreen
	
	/**
	 * Show frame as maximized.
	 * @param frame
	 */
	public static void showMaximized(Frame frame)
	{
		frame.pack();
		frame.setExtendedState(Frame.MAXIMIZED_BOTH);
		frame.setVisible(true);
		frame.toFront();
	}

	/**
	 *	Position window in center of the screen
	 * 	@param window Window to position
	 */
	public static void positionCenterScreen(Window window)
	{
		positionScreen (window, SwingConstants.CENTER);
	}	//	positionCenterScreen

	/**
	 *  Show in the center of the screen.
	 *  (pack, set location and set visibility)
	 * 	@param window Window to position
	 * 	@param position SwingConstants
	 */
	public static void showScreen(Window window, int position)
	{
		positionScreen(window, position);
		showWindow(window);
	}   //  showScreen


	/**
	 *	Position window in center of the screen
	 * 	@param window Window to position
	 * 	@param position SwingConstants
	 */
	public static void positionScreen (Window window, int position)
	{
		window.pack();
		// take into account task bar and other adornments
		GraphicsConfiguration config = window.getGraphicsConfiguration();
		Rectangle bounds = config.getBounds();
		Dimension sSize = bounds.getSize();
		Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(config);
		sSize.width -= (insets.left + insets.right);
		sSize.height -= (insets.top + insets.bottom);
		
		Dimension wSize = window.getSize();
		//	fit on window
		if (wSize.height > sSize.height)
			wSize.height = sSize.height;
		if (wSize.width > sSize.width)
			wSize.width = sSize.width;
		window.setSize(wSize);
		//	Center
		int x = (sSize.width - wSize.width) / 2;
		int y = (sSize.height - wSize.height) / 2;
		if (position == SwingConstants.CENTER)
			;
		else if (position == SwingConstants.NORTH_WEST)
		{
			x = 0;
			y = 0;
		}
		else if (position == SwingConstants.NORTH)
		{
			y = 0;
		}
		else if (position == SwingConstants.NORTH_EAST)
		{
			x = (sSize.width - wSize.width);
			y = 0;
		}
		else if (position == SwingConstants.WEST)
		{
			x = 0;
		}
		else if (position == SwingConstants.EAST)
		{
			x = (sSize.width - wSize.width);
		}
		else if (position == SwingConstants.SOUTH)
		{
			y = (sSize.height - wSize.height);
		}
		else if (position == SwingConstants.SOUTH_WEST)
		{
			x = 0;
			y = (sSize.height - wSize.height);
		}
		else if (position == SwingConstants.SOUTH_EAST)
		{
			x = (sSize.width - wSize.width);
			y = (sSize.height - wSize.height);
		}
		//
		window.setLocation(bounds.x + x + insets.left, bounds.y + y + insets.top);
	}	//	positionScreen

	/**
	 *	Position in center of the parent window.
	 *  (pack, set location and set visibility)
	 * 	@param parent Parent Window
	 * 	@param window Window to position
	 */
	public static void showCenterWindow(Window parent, Window window)
	{
		positionCenterWindow(parent, window);
		showWindow(window);
	}   //  showCenterWindow

	/**
	 *	Position in center of the parent window
	 *
	 * @param parent Parent Window
	 * @param window Window to position
	 */
	public static void positionCenterWindow(Window parent, Window window)
	{
		if (parent == null)
		{
			positionCenterScreen(window);
			return;
		}
		window.pack();
		//
		Dimension sSize = Toolkit.getDefaultToolkit().getScreenSize();
		// take into account task bar and other adornments
		GraphicsConfiguration config = window.getGraphicsConfiguration();
		Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(config);
		sSize.width -= (insets.left + insets.right);
		sSize.height -= (insets.top + insets.bottom);
		
		Dimension wSize = window.getSize();
		//	fit on window
		if (wSize.height > sSize.height)
			wSize.height = sSize.height;
		if (wSize.width > sSize.width)
			wSize.width = sSize.width;
		window.setSize(wSize);
		//	center in parent
		Rectangle pBounds = parent.getBounds();
		//	Parent is in upper left corner
		if (pBounds.x == pBounds.y && pBounds.x == 0)
		{
			positionCenterScreen(window);
			return;
		}
		//  Find middle
		int x = pBounds.x + ((pBounds.width-wSize.width)/2);
		if (x < 0)
			x = 0;
		int y = pBounds.y + ((pBounds.height-wSize.height)/2);
		if (y < 0)
			y = 0;

		//	Is it on Screen?
		if (x + wSize.width > sSize.width)
			x = sSize.width - wSize.width;
		if (y + wSize.height > sSize.height)
			y = sSize.height - wSize.height;
		//
	//	System.out.println("Position: x=" + x + " y=" + y + " w=" + wSize.getWidth() + " h=" + wSize.getHeight()
	//		+ " - Parent loc x=" + pLoc.x + " y=" + y + " w=" + pSize.getWidth() + " h=" + pSize.getHeight());
		window.setLocation(x + insets.left, y + insets.top);
	}	//	positionCenterScreen
	
	
	/*************************************************************************
	 * 	Get Button
	 *	@param iconName
	 *	@return button
	 */
	public static CButton getButton (String iconName)
	{
		CButton button = new CButton(Env.getImageIcon(iconName + "16.gif"));
		button.setMargin(new Insets (0, 0, 0, 0));
		button.setToolTipText(Msg.getMsg(Env.getCtx(), iconName));
		button.setDefaultCapable(false);
		return button;
	}	//	getButton


	/**
	 *	Create Menu Title (translate it and set Mnemonics).
	 *	Based on MS notation of &Help => H is Mnemonics
	 *
	 *  @param AD_Message message
	 *  @return JMenu
	 */
	public static JMenu getMenu (String AD_Message)
	{
		JMenu menu = new JMenu();
		String text = Msg.getMsg(Env.getCtx(), AD_Message);
		int pos = text.indexOf('&');
		if (pos != -1 && text.length() > pos)	//	We have a nemonic
		{
			char ch = text.toUpperCase().charAt(pos+1);
			if (ch != ' ')
			{
				text = text.substring(0, pos) + text.substring(pos+1);
				menu.setMnemonic(ch);
			}
		}
		menu.setText(text);
		return menu;
	}	//	getMenu

	/**
	 *  Create Menu Item.
	 *  @param actionName   action command
	 *  @param iconName optional name of the icon, defaults to action if null
	 *  @param ks       optional key stroke
	 *  @param menu     menu to add menu item to
	 *  @param al       action listener to register
	 *  @return MenuItem
	 */
	public static JMenuItem addMenuItem (String actionName, String iconName, KeyStroke ks,
		JMenu menu, ActionListener al)
	{
		if (iconName == null)
			iconName = actionName;
		String text = Msg.getMsg(Env.getCtx(), actionName);
		ImageIcon icon = Env.getImageIcon2(iconName + "16");
		CMenuItem mi = new CMenuItem(text, icon);
		mi.setActionCommand(actionName);
		if (ks != null)
			mi.setAccelerator(ks);
		if (menu != null)
			menu.add(mi);
		if (al != null)
			mi.addActionListener(al);
		return mi;
	}   //  addMenuItem

	/**
	 *  Perform action command for common menu items.
	 * 	Created in AMenu.createMenu(), APanel.createMenu(), FormFrame.createMenu()
	 *  @param actionCommand known action command
	 *  @param WindowNo window no
	 *  @param c Container parent
	 *  @return true if actionCommand was found and performed
	 */
	public static boolean actionPerformed (String actionCommand, int WindowNo, Container c)
	{
		MRole role = MRole.getDefault();
		//  File Menu   ------------------------
		if (actionCommand.equals("PrintScreen"))
		{
			PrintScreenPainter.printScreen (getFrame(c));
		}
		else if (actionCommand.equals("ScreenShot"))
		{
			ScreenShot.createJPEG(getFrame(c), null);
		}
	//	else if (actionCommand.equals("Report"))
	//	{
	//		AEnv.showCenterScreen (new ProcessStart());
	//	}
		else if (actionCommand.equals("Exit"))
		{
			if (ADialog.ask(WindowNo, c, "ExitApplication?"))
			{
				AMenu aMenu = (AMenu)getWindow(0);
				aMenu.dispose() ;
			}
		}
		else if (actionCommand.equals("Logout"))
		{
			AMenu aMenu = (AMenu)getWindow(0);
			aMenu.logout();
		}

		//  View Menu   ------------------------
		else if (actionCommand.equals("InfoProduct") && AEnv.canAccessInfo("PRODUCT"))
		{
			org.compiere.apps.search.Info.showProduct (getFrame(c), WindowNo);
		}
		else if (actionCommand.equals("InfoBPartner") && AEnv.canAccessInfo("BPARTNER"))
		{
			org.compiere.apps.search.Info.showBPartner (getFrame(c), WindowNo);
		}
		else if (actionCommand.equals("InfoAsset") && AEnv.canAccessInfo("ASSET"))
		{
			org.compiere.apps.search.Info.showAsset (getFrame(c), WindowNo);
		}
		else if (actionCommand.equals("InfoAccount") && 
				  MRole.getDefault().isShowAcct() &&
				  AEnv.canAccessInfo("ACCOUNT"))
		{
			new org.compiere.acct.AcctViewer();
		}
		else if (actionCommand.equals("InfoSchedule") && AEnv.canAccessInfo("SCHEDULE"))
		{
			new org.compiere.apps.search.InfoSchedule (getFrame(c), null, false);
		}
		//FR [ 1966328 ] 
		else if (actionCommand.equals("InfoMRP") && AEnv.canAccessInfo("MRP"))
		{
			CFrame frame = (CFrame) getFrame(c);
			int	m_menu_id = MMenu.getMenu_ID("MRP Info");
			AMenu menu = AEnv.getAMenu(frame);
			AMenuStartItem form = new AMenuStartItem (m_menu_id, true, Msg.translate(Env.getCtx(), "MRP Info"), menu);		//	async load
			form.start();
		}
		else if (actionCommand.equals("InfoCRP") && AEnv.canAccessInfo("CRP"))
		{
			CFrame frame = (CFrame) getFrame(c);
			int	m_menu_id = MMenu.getMenu_ID("CRP Info");
			AMenu menu = AEnv.getAMenu(frame);
			AMenuStartItem form = new AMenuStartItem (m_menu_id, true, Msg.translate(Env.getCtx(), "CRP Info"), menu);		//	async load
			form.start();			
		}
		else if (actionCommand.equals("InfoOrder") && AEnv.canAccessInfo("ORDER"))
		{
			org.compiere.apps.search.Info.showOrder (getFrame(c), WindowNo, "");
		}
		else if (actionCommand.equals("InfoInvoice") && AEnv.canAccessInfo("INVOICE"))
		{
			org.compiere.apps.search.Info.showInvoice (getFrame(c), WindowNo, "");
		}
		else if (actionCommand.equals("InfoInOut") && AEnv.canAccessInfo("INOUT"))
		{
			org.compiere.apps.search.Info.showInOut (getFrame(c), WindowNo, "");
		}
		else if (actionCommand.equals("InfoPayment") && AEnv.canAccessInfo("PAYMENT"))
		{
			org.compiere.apps.search.Info.showPayment (getFrame(c), WindowNo, "");
		}
		else if (actionCommand.equals("InfoCashLine") && AEnv.canAccessInfo("CASHJOURNAL"))
		{
			org.compiere.apps.search.Info.showCashLine (getFrame(c), WindowNo, "");
		}
		else if (actionCommand.equals("InfoAssignment") && AEnv.canAccessInfo("RESOURCE"))
		{
			org.compiere.apps.search.Info.showAssignment (getFrame(c), WindowNo, "");
		}
		

		//  Go Menu     ------------------------
		else if (actionCommand.equals("WorkFlow"))
		{
			startWorkflowProcess(0,0);
		}
		else if (actionCommand.equals("Home"))
		{
			showWindow(getWindow(0));
		}

		//  Tools Menu  ------------------------
		else if (actionCommand.equals("Calculator"))
		{
			Calculator calc = new org.compiere.grid.ed.Calculator(getFrame(c));
			calc.setDisposeOnEqual(false);
			AEnv.showCenterScreen (calc);
		}
		else if (actionCommand.equals("Calendar"))
		{
			AEnv.showCenterScreen (new org.compiere.grid.ed.Calendar(getFrame(c)));
		}
		else if (actionCommand.equals("Editor"))
		{
			AEnv.showCenterScreen (new org.compiere.grid.ed.Editor(getFrame(c)));
		}
		else if (actionCommand.equals("Script"))
		{
			new BeanShellEditor(getFrame(c));
		}
		else if (actionCommand.equals("Preference"))
		{
			if (role.isShowPreference()) {
				AEnv.showCenterScreen(new Preference (getFrame(c), WindowNo));
			}
		}

		//  Help Menu   ------------------------
		else if (actionCommand.equals("Online"))
		{
			Env.startBrowser(org.compiere.Adempiere.getOnlineHelpURL());
		}
		else if (actionCommand.equals("EMailSupport"))
		{
			ADialog.createSupportEMail(getFrame(c), getFrame(c).getTitle(), "\n\n");
		}
		else if (actionCommand.equals("About"))
		{
			AEnv.showCenterScreen(new AboutBox(getFrame(c)));
		}
		else
			return false;
		//
		return true;
	}   //  actionPerformed

	/**
	 *  Set Text and Mnemonic for Button.
	 *  Create Mnemonics of text containing '&'.
	 *	Based on MS notation of &Help => H is Mnemonics
	 *  @param b The button
	 *  @param text The text with optional Mnemonics
	 */
	public static void setTextMnemonic (JButton b, String text)
	{
		if (text == null || b == null)
			return;
		int pos = text.indexOf('&');
		if (pos != -1)					//	We have a nemonic
		{
			char ch = text.charAt(pos+1);
			b.setMnemonic(ch);
			b.setText(text.substring(0, pos) + text.substring(pos+1));
		}
		b.setText(text);
	}   //  setTextMnemonic

	/**
	 *  Get Mnemonic character from text.
	 *  @param text text with '&'
	 *  @return Mnemonic or 0
	 */
	public static char getMnemonic (String text)
	{
		int pos = text.indexOf('&');
		if (pos != -1)					//	We have a nemonic
			return text.charAt(pos+1);
		return 0;
	}   //  getMnemonic

	
	/*************************************************************************
	 * 	Zoom
	 *	@param AD_Table_ID
	 *	@param Record_ID
	 */
	public static void zoom (int AD_Table_ID, int Record_ID)
	{
		int AD_Window_ID = Env.getZoomWindowID(AD_Table_ID, Record_ID);
		//  Nothing to Zoom to
		if (AD_Window_ID == 0)
			return;
		
		MTable table = MTable.get(Env.getCtx(), AD_Table_ID);
		
		AWindow frame = new AWindow(null);
		if (!frame.initWindow(AD_Window_ID, MQuery.getEqualQuery(table.getTableName() + "_ID", Record_ID)))
			return;
		addToWindowManager(frame);
		if (Ini.isPropertyBool(Ini.P_OPEN_WINDOW_MAXIMIZED))
		{
			AEnv.showMaximized(frame);
		}
		else
		{
			AEnv.showCenterScreen(frame);
		}
		frame = null;
	}	//	zoom

	/**
	 * 	Zoom
	 *	@param query query
	 */
	public static void zoom (MQuery query)
	{
		if (query == null || query.getTableName() == null || query.getTableName().length() == 0)
			return;
		
		int AD_Window_ID = Env.getZoomWindowID(query);
		//  Nothing to Zoom to
		if (AD_Window_ID == 0)
			return;
		
		AWindow frame = new AWindow(null);
		if (!frame.initWindow(AD_Window_ID, query))
			return;
		addToWindowManager(frame);
		if (Ini.isPropertyBool(Ini.P_OPEN_WINDOW_MAXIMIZED))
		{
			AEnv.showMaximized(frame);
		}
		else
		{
			AEnv.showCenterScreen(frame);
		}
		frame = null;
	}	//	zoom
	
	/**
	 * Track open frame in window manager
	 * @param frame
	 */
	public static void addToWindowManager(CFrame frame)
	{
		JFrame top = getWindow(0);
		if (top instanceof AMenu)
		{
			((AMenu)top).getWindowManager().add(frame);
		}
	}
	
	/**
	 * FR [ 1966328 ] 
	 * get AMenu
	 * @param frame
	 */
	public static AMenu getAMenu(CFrame frame)
	{
		JFrame top = getWindow(0);
		if (top instanceof AMenu)
		{
			return (AMenu)top;
		}
		return null;
	}
	/**
	 *	Exit System
	 *  @param status System exit status (usually 0 for no error)
	 */
	public static void exit (int status)
	{
		Env.exitEnv(status);
	}	//	exit

	public static void logout() 
	{
		Env.logout();
		
		Splash.getSplash().setVisible(true);

		//reload
		new AMenu();
	}
	
	/**
	 * 	Is Workflow Process view enabled.
	 *	@return true if enabled
	 */
	public static boolean isWorkflowProcess ()
	{
		if (s_workflow == null)
		{
			s_workflow = Boolean.FALSE;					
			int AD_Table_ID = I_AD_WF_Process.Table_ID;	//	AD_WF_Process	
			if (MRole.getDefault().isTableAccess (AD_Table_ID, true))	//	RO
				s_workflow = Boolean.TRUE;
			else
			{
				AD_Table_ID = I_AD_WF_Activity.Table_ID;	//	AD_WF_Activity	
				if (MRole.getDefault().isTableAccess (AD_Table_ID, true))	//	RO
					s_workflow = Boolean.TRUE;
				else
					if (log.isLoggable(Level.CONFIG)) log.config(s_workflow.toString());
			}
			//	Get Window
			if (s_workflow.booleanValue())
			{
				s_workflow_Window_ID = DB.getSQLValue (null,
					"SELECT AD_Window_ID FROM AD_Table WHERE AD_Table_ID=?", AD_Table_ID);
				if (s_workflow_Window_ID == 0)
					s_workflow_Window_ID = 297;	//	fallback HARDCODED
				//	s_workflow = Boolean.FALSE;
				if (log.isLoggable(Level.CONFIG)) log.config(s_workflow + ", Window=" + s_workflow_Window_ID);
			}
		}
		return s_workflow.booleanValue();
	}	//	isWorkflowProcess

	
	/**
	 * 	Start Workflow Process Window
	 *	@param AD_Table_ID optional table
	 *	@param Record_ID optional record
	 */
	public static void startWorkflowProcess (int AD_Table_ID, int Record_ID)
	{
		if (s_workflow_Window_ID == 0)
			return;
		//
		MQuery query = null;
		if (AD_Table_ID != 0 && Record_ID != 0)
		{
			query = new MQuery("AD_WF_Process");
			query.addRestriction("AD_Table_ID", MQuery.EQUAL, AD_Table_ID);
			query.addRestriction("Record_ID", MQuery.EQUAL, Record_ID);
		}
		//
		AWindow frame = new AWindow(null);
		if (!frame.initWindow(s_workflow_Window_ID, query))
			return;
		addToWindowManager(frame);
		if (Ini.isPropertyBool(Ini.P_OPEN_WINDOW_MAXIMIZED) ) {
			frame.pack();
			frame.setExtendedState(Frame.MAXIMIZED_BOTH);
			frame.setVisible(true);
			frame.toFront();
		} else
			AEnv.showCenterScreen(frame);
		frame = null;
	}	//	startWorkflowProcess
	
	
	/*************************************************************************/

	/** Workflow Menu		*/
	private static Boolean	s_workflow = null;
	/** Workflow Menu		*/
	private static int		s_workflow_Window_ID = 0;
	
	/**	Logger			*/
	private static CLogger log = CLogger.getCLogger(AEnv.class);

	/**
	 *  Get Server Version
	 *  @return Apps Server Version
	 *  @see ALogin#checkVersion
	 */
	public static String getServerVersion ()
	{
		return CConnection.get().getServerVersion();
	}   //  getServerVersion

	/**
	 *  Post Immediate
	 *  @param  WindowNo 		window
	 *  @param  AD_Table_ID     Table ID of Document
	 *  @param  AD_Client_ID    Client ID of Document
	 *  @param  Record_ID       Record ID of this document
	 *  @param  force           force posting
	 *  @return null if success, otherwise error
	 */
	public static String postImmediate (int WindowNo, int AD_Client_ID, 
		int AD_Table_ID, int Record_ID, boolean force)
	{
		if (log.isLoggable(Level.CONFIG)) log.config("Window=" + WindowNo 
			+ ", AD_Table_ID=" + AD_Table_ID + "/" + Record_ID
			+ ", Force=" + force);

		return Doc.manualPosting(WindowNo, AD_Client_ID, AD_Table_ID, Record_ID, force);
	}   //  postImmediate

	/**
	 *  Cache Reset
	 *  @param  tableName	table name
	 *  @param  Record_ID	record id
	 */
	public static void cacheReset (String tableName, int Record_ID)
	{
		if (log.isLoggable(Level.CONFIG)) log.config("TableName=" + tableName + ", Record_ID=" + Record_ID);

		try
		{
			CacheMgt.get().reset(tableName, Record_ID);
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "ex", e);
		}
	}   //  cacheReset
	
	/**
	 * Update all windows after look and feel changes.
	 * @since 2006-11-27 
	 */
	public static void updateUI()
	{
		Set<Window> updated = updateUI0();
		JFrame top = getWindow(0);
		if (top instanceof AMenu)
		{
			CFrame[] frames = ((AMenu)top).getWindowManager().getWindows();
			for (CFrame f : frames)
			{
				if (updated.contains(f)) continue;
				SwingUtilities.updateComponentTreeUI(f);
				f.validate();
				RepaintManager mgr = RepaintManager.currentManager(f);
				Component childs[] = f.getComponents();
				for (Component c : childs) {
					if (c instanceof JComponent)
						mgr.markCompletelyDirty((JComponent)c);
				}
				f.repaint();
				updated.add(f);
			}
		}
	}
	
	/**
	 *  Validate permissions to access Info queries on the view menu 
	*   @author kstan_79
	*   @return true if access is allowed
	*/ 
	
	public static boolean canAccessInfo(String infoWindowName) 
	{
		boolean result=false;
		int roleid= Env.getAD_Role_ID(Env.getCtx());
		String sqlRolePermission="Select COUNT(AD_ROLE_ID) AS ROWCOUNT FROM AD_ROLE WHERE AD_ROLE_ID=" + roleid  
	                              + " AND ALLOW_INFO_" + infoWindowName + "='Y'"; 

		log.config(sqlRolePermission); 
		PreparedStatement prolestmt = null; 
		ResultSet rs = null;
		try 
		{ 
			prolestmt = DB.prepareStatement (sqlRolePermission, null); 
	 
			rs = prolestmt.executeQuery ();  
	 
			rs.next(); 
	 
			if (rs.getInt("ROWCOUNT")>0)
			{
				result=true;
			}
			else 
			{
				return false;
			}
		} 
		catch (Exception e) 
		{
				System.out.println(e); 
				log.log(Level.SEVERE, "(1)", e); 
		} 
		finally
		{
			DB.close(rs, prolestmt);
			rs = null; prolestmt = null;
		}
	
		return result; 
	} // 	canAccessInfo
	
	/**
	 * Update all windows after look and feel changes.
	 * @since 2006-11-27
	 */
	public static Set<Window>updateUI0()
	{
		Set<Window> updated = new HashSet<Window>();
		for (Container c : s_windows)
		{
			Window w = getFrame(c);
			if (w == null) continue;
			if (updated.contains(w)) continue;
			SwingUtilities.updateComponentTreeUI(w);
			w.validate();
			RepaintManager mgr = RepaintManager.currentManager(w);
			Component childs[] = w.getComponents();
			for (Component child : childs) {
				if (child instanceof JComponent)
					mgr.markCompletelyDirty((JComponent)child);
			}
			w.repaint();
			updated.add(w);
		}
		for (Window w : s_hiddenWindows)
		{
			if (updated.contains(w)) continue;
			SwingUtilities.updateComponentTreeUI(w);
			w.validate();
			RepaintManager mgr = RepaintManager.currentManager(w);
			Component childs[] = w.getComponents();
			for (Component child : childs) {
				if (child instanceof JComponent)
					mgr.markCompletelyDirty((JComponent)child);
			}
			w.repaint();
			updated.add(w);
		}
		return updated;
	}
 
	/**
	 *	Add Container and return WindowNo.
	 *  The container is a APanel, AWindow or JFrame/JDialog
	 *  @param win window
	 *  @return WindowNo used for context
	 */
	public static int createWindowNo(Container win)
	{
		int retValue = s_windows.size();
		s_windows.add(win);
		return retValue;
	}	//	createWindowNo

	/**
	 *	Search Window by comparing the Frames
	 *  @param container container
	 *  @return WindowNo of container or 0
	 */
	public static int getWindowNo (Container container)
	{
		if (container == null)
			return 0;
		JFrame winFrame = getFrame(container);
		if (winFrame == null)
			return 0;

		//  loop through windows
		for (int i = 0; i < s_windows.size(); i++)
		{
			Container cmp = (Container)s_windows.get(i);
			if (cmp != null)
			{
				JFrame cmpFrame = getFrame(cmp);
				if (winFrame.equals(cmpFrame))
					return i;
			}
		}
		return 0;
	}	//	getWindowNo

	/**
	 *	Return the JFrame pointer of WindowNo - or null
	 *  @param WindowNo window
	 *  @return JFrame of WindowNo
	 */
	public static JFrame getWindow (int WindowNo)
	{
		JFrame retValue = null;
		try
		{
			retValue = getFrame ((Container)s_windows.get(WindowNo));
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, e.toString());
		}
		return retValue;
	}	//	getWindow

	/**
	 *	Remove window from active list
	 *  @param WindowNo window
	 */
	public static void removeWindow (int WindowNo)
	{
		if (WindowNo>=0 && WindowNo < s_windows.size())
			s_windows.set(WindowNo, null);
	}	//	removeWindow

	/**************************************************************************
	 *	Get Frame of Window
	 *  @param container Container
	 *  @return JFrame of container or null
	 */
	public static JFrame getFrame (Container container)
	{
		Container element = container;
		while (element != null)
		{
			if (element instanceof JFrame)
				return (JFrame)element;
			element = element.getParent();
		}
		return null;
	}	//	getFrame

	public static void reset(boolean finalCall) 
	{
		closeWindows();

		//	Dismantle windows
		/**
		for (int i = 0; i < s_windows.size(); i++)
		{
			Container win = (Container)s_windows.get(i);
			if (win.getClass().getName().endsWith("AMenu")) // Null pointer
				;
			else if (win instanceof Window)
				((Window)win).dispose();
			else
				win.removeAll();
		}
		**/
		//bug [ 1574630 ]
		if (s_windows.size() > 0) {
			if (!finalCall) {
				Container c = s_windows.get(0);
				s_windows.clear();
				createWindowNo(c);
			} else {
				s_windows.clear();
			}
		}
	}
	
	/**
	 *  Return JDialog or JFrame Parent
	 *  @param container Container
	 *  @return JDialog or JFrame of container
	 */
	public static Window getParent (Container container)
	{
		Container element = container;
		while (element != null)
		{
			if (element instanceof JDialog || element instanceof JFrame)
				return (Window)element;
			if (element instanceof Window)
				return (Window)element;
			element = element.getParent();
		}
		return null;
	}   //  getParent
	
	/**
	 *	Get Graphics of container or its parent.
	 *  The element may not have a Graphic if not displayed yet,
	 * 	but the parent might have.
	 *  @param container Container
	 *  @return Graphics of container or null
	 */
	public static Graphics getGraphics (Container container)
	{
		Container element = container;
		while (element != null)
		{
			Graphics g = element.getGraphics();
			if (g != null)
				return g;
			element = element.getParent();
		}
		return null;
	}	//	getGraphics
	
	/**************************************************************************
	 *  Get Image with File name
	 *
	 *  @param fileNameInImageDir full file name in imgaes folder (e.g. Bean16.gif)
	 *  @return image
	 */
	public static Image getImage (String fileNameInImageDir)
	{
		IResourceFinder rf = Core.getResourceFinder();
		URL url =  rf.getResource("images/" + fileNameInImageDir);

//		URL url = Adempiere.class.getResource("images/" + fileNameInImageDir);
		if (url == null)
		{
			log.log(Level.SEVERE, "Not found: " +  fileNameInImageDir);
			return null;
		}
		Toolkit tk = Toolkit.getDefaultToolkit();
		return tk.getImage(url);
	}   //  getImage
}	//	AEnv
