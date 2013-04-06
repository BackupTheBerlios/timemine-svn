/***********************************************************************\
*
* $Revision$
* $Date$
* $Author$
* Contents: Timemine
* Systems: all
*
\***********************************************************************/

/****************************** Imports ********************************/
// base
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Constructor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Timer;
import java.util.TimerTask;

// graphics
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.events.TreeEvent;
import org.eclipse.swt.events.TreeListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tray;
import org.eclipse.swt.widgets.TrayItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;

/****************************** Classes ********************************/

@interface Warning
{
}

/** time entry comparator
 */
class TimeEntryComparator implements Comparator<Redmine.TimeEntry>
{
  // --------------------------- constants --------------------------------
  enum SortModes
  {
    SPENT_ON,
    HOURS,
    ACTIVITY,
    PROJECT,
    ISSUE,
    COMMENTS
  };

  // --------------------------- variables --------------------------------
  private Redmine   redmine;
  private SortModes sortMode;

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** create time entry comparator
   * @param redmine redmine client
   * @param sortMode sort mode
   */
  TimeEntryComparator(Redmine redmine, SortModes sortMode)
  {
    this.redmine  = redmine;
    this.sortMode = sortMode;
  }

  /** create time entry comparator
   * @param redmine redmine client
   */
  TimeEntryComparator(Redmine redmine)
  {
    this(redmine,SortModes.HOURS);
  }

  /** get sort mode
   * @return sort mode
   */
  public SortModes getSortMode()
  {
    return sortMode;
  }

  /** set sort mode
   * @param sortMode sort mode
   */
  public void setSortMode(SortModes sortMode)
  {
    this.sortMode = sortMode;
  }

  /** compare time entry
   * @param timeEntry0, timeEntry1 file data to compare
   * @return -1 iff timeEntry0 < timeEntry1,
              0 iff timeEntry0 = timeEntry1,
              1 iff timeEntry0 > timeEntry1
   */
  public int compare(Redmine.TimeEntry timeEntry0, Redmine.TimeEntry timeEntry1)
  {
    if      (timeEntry0 == null)
    {
      return  1;
    }
    else if (timeEntry1 == null)
    {
      return -1;
    }
    else
    {
      switch (sortMode)
      {
        case SPENT_ON:
          return timeEntry0.spentOn.compareTo(timeEntry1.spentOn);
        case HOURS:
          if      (timeEntry0.hours < timeEntry1.hours) return -1;
          else if (timeEntry0.hours > timeEntry1.hours) return  1;
          else                                          return  0;
        case ACTIVITY:
          Redmine.Activity activity0 = redmine.getActivity(timeEntry0.activityId);
          Redmine.Activity activity1 = redmine.getActivity(timeEntry1.activityId);
          if ((activity0 != null) && (activity1 != null))
            return activity0.name.compareTo(activity1.name);
          else if (activity0 != null)
            return -1;
          else if (activity1 != null)
            return 1;
          else
            return 0;
        case PROJECT:
          Redmine.Project project0 = redmine.getProject(timeEntry0.projectId);
          Redmine.Project project1 = redmine.getProject(timeEntry1.projectId);
          if ((project0 != null) && (project1 != null))
            return project0.name.compareTo(project1.name);
          else if (project0 != null)
            return -1;
          else if (project1 != null)
            return 1;
          else
            return 0;
        case ISSUE:
          Redmine.Issue issue0 = redmine.getIssue(timeEntry0.issueId);
          Redmine.Issue issue1 = redmine.getIssue(timeEntry1.issueId);
          if ((issue0 != null) && (issue1 != null))
            return issue0.subject.compareTo(issue1.subject);
          else if (issue0 != null)
            return -1;
          else if (issue1 != null)
            return 1;
          else
            return 0;
        case COMMENTS:
          return timeEntry0.comments.compareTo(timeEntry1.comments);
        default:
          return 0;
      }
    }
  }
}

/** Timemine
 */
public class Timemine
{
  /** color data
   */
  class ColorData
  {
    Color foreground;
    Color background;

    ColorData(Settings.Color color)
    {
      this.foreground = (color.foreground != null) ? new Color(display,color.foreground) : null;
      this.background = (color.background != null) ? new Color(display,color.background) : null;
    }

    public void dispose()
    {
      if (foreground != null) foreground.dispose();
      if (background != null) background.dispose();
    }
  }

  /** login data
   */
  class LoginData
  {
    String  serverName;
    int     serverPort;
    boolean serverUseSSL;
    String  loginName;
    String  loginPassword;

    /** create login data
     * @param serverName server name
     * @param serverPort server port
     * @param serverUseSSL truf iff server use SSL
     * @param loginName login name
     * @param loginPassword login password
     */
    LoginData(String serverName, int serverPort, boolean serverUseSSL, String loginName, String loginPassword)
    {
      this.serverName    = !serverName.equals("") ? serverName : Settings.serverName;
      this.serverPort    = (serverPort != 0) ? serverPort : Settings.serverPort;
      this.serverUseSSL  = serverUseSSL;
      this.loginName     = !loginName.equals("") ? loginName : Settings.loginName;
      this.loginPassword = !loginPassword.equals("") ? loginPassword : Settings.loginPassword;
    }
  }

  /** status text
   */
  class StatusText
  {
    final Thread thread;
    final String text;

    /** create status text
     * @param format format
     * @param arguments optional arguments
     */
    StatusText(String format, Object... arguments)
    {
      this.thread = Thread.currentThread();
      this.text   = String.format(format,arguments);
    }

    /** convert to string
     * @return string
     */
    public String toString()
    {
      return text;
    }
  };

  // --------------------------- constants --------------------------------

  // exit codes
  public final int                     EXITCODE_OK             =   0;
  public final int                     EXITCODE_FAIL           =   1;
  public final int                     EXITCODE_RESTART        =  64;
  public final int                     EXITCODE_INTERNAL_ERROR = 127;

  // colors
  public static Color                  COLOR_BLACK;
  public static Color                  COLOR_WHITE;
  public static Color                  COLOR_BACKGROUND;
  public static ColorData              COLOR_TODAY_TIME_ENTRIES;
  public static ColorData              COLOR_TIME_ENTRIES;
  public static ColorData              COLOR_TIME_ENTRIES_UPDATE;
  public static ColorData              COLOR_TIME_ENTRIES_INCOMPLETE;
  public static ColorData              COLOR_TIME_ENTRIES_WEEKEND;
  public static ColorData              COLOR_TIME_ENTRIES_VACATION;

  // images
  public static Image                  IMAGE_PROGRAM_ICON;

  // cursors
  public static Cursor                 CURSOR_WAIT;

  // date/time format
  public static SimpleDateFormat       DATE_FORMAT;

  // user events
  private final int                    USER_EVENT_QUIT = 0xFFFF+0;

  // refresh time
  private final int                    REFRESH_TIME  = 10*60; // [s]

  // command line options
  private static final Option[] options =
  {
    new Option("--host",                       "-h",Options.Types.STRING, "serverName"   ),
    new Option("--port",                       "-p",Options.Types.INTEGER,"serverPort"   ),
    new Option("--ssl",                        "-p",Options.Types.BOOLEAN,"serverUseSSL" ),
    new Option("--name",                       "-n",Options.Types.STRING, "loginName"    ),
    new Option("--password",                   null,Options.Types.STRING, "loginPassword"),
    new Option("--show-login",                 null,Options.Types.BOOLEAN,"showLoginFlag"),

    new Option("--help",                       "-h",Options.Types.BOOLEAN,"helpFlag"     ),

    new Option("--debug",                      null,Options.Types.BOOLEAN,"debugFlag"    ),

    // ignored
    new Option("--swing",                      null, Options.Types.BOOLEAN,null),
  };

  // --------------------------- variables --------------------------------
  private Display                                display;
  private Shell                                  shell;
  private Tray                                   tray;
  private TrayItem                               trayItem;

  private TabFolder                              widgetTabFolder;
  private Composite                              widgetTabToday;
  private Composite                              widgetTabAll;

  private Table                                  widgetTodayTimeEntryTable;
  private Combo                                  widgetProjects;
  private Combo                                  widgetIssueIds;
  private Combo                                  widgetIssues;
  private Label                                  widgetIssueStatus;
  private Spinner                                widgetSpentHourFraction;
  private Spinner                                widgetSpentMinuteFraction;
  private Combo                                  widgetActivities;
  private Text                                   widgetComments;
  private Button                                 widgetAdd;

  private Tree                                   widgetTimeEntryTree;
  private Button                                 widgetNew;
  private Button                                 widgetDelete;

  private Redmine                                redmine;
  private SoftHashMap<Integer,Redmine.TimeEntry> redmineTodayTimeEntryMap;
  private int[]                                  projectIds;
  private int[]                                  issueIds0,issueIds1;
  private int[]                                  activityIds;

  private TimerTask                              refreshTask;
  private TimerTask                              refreshTodayTimeEntriesTask;
  private TimerTask                              refreshTimeEntriesTask;
  private Timer                                  refreshTimer;
  private Timer                                  refreshTodayTimeEntryTimer;
  private Timer                                  refreshTimeEntryTimer;

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** print error to stderr
   * @param format format string
   * @param args optional arguments
   */
  public static void printError(String format, Object... args)
  {
    System.err.println("ERROR: "+String.format(format,args));
  }

  /** print internal error to stderr
   * @param format format string
   * @param args optional arguments
   */
  public static void printInternalError(String format, Object... args)
  {
    System.err.println("INTERNAL ERROR: "+String.format(format,args));
  }

  /** print internal error to stderr
   * @param throwable throwable
   * @param args optional arguments
   */
  public static void printInternalError(Throwable throwable, Object... args)
  {
    printInternalError(throwable.toString());
    printStacktrace(throwable);
  }

  /** print warning to stderr
   * @param format format string
   * @param args optional arguments
   */
  public static void printWarning(String format, Object... args)
  {
    System.err.print("Warning: "+String.format(format,args));
    if (Settings.debugFlag)
    {
      StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
      System.err.print(" at "+stackTrace[2].getFileName()+", "+stackTrace[2].getLineNumber());
    }
    System.err.println();
  }

  /** print exception stack trace to stderr
   * @param exception exception to print
   */
  public static void printStacktrace(Throwable throwable)
  {
    if (Settings.debugFlag)
    {
      for (StackTraceElement stackTraceElement : throwable.getStackTrace())
      {
        System.err.println("  "+stackTraceElement);
      }
    }
  }

  /** renice i/o exception (remove java.io.IOExcpetion text from exception)
   * @param exception i/o exception to renice
   * @return reniced exception
   */
  public static IOException reniceIOException(IOException exception)
  {
    final Pattern PATTERN = Pattern.compile("^(.*?)\\s*java.io.IOException: error=\\d+,\\s*(.*)$",Pattern.CASE_INSENSITIVE);

    Matcher matcher;
    if ((matcher = PATTERN.matcher(exception.getMessage())).matches())
    {
      exception = new IOException(matcher.group(1)+" "+matcher.group(2));
    }

    return exception;
  }

  /** main
   * @param args command line arguments
   */
  public static void main(String[] args)
  {
    new Timemine(args);
  }

  /** timemine main
   * @param args command line arguments
   */
  Timemine(String[] args)
  {
    final char MAIL_AT = '@';

    int exitcode = 255;
    try
    {
      // load settings
      Settings.load();

      // parse arguments
      parseArguments(args);

      // init
      initAll();

      // run
      exitcode = run();

      // done
      doneAll();
    }
    catch (org.eclipse.swt.SWTException exception)
    {
Dprintf.dprintf("exception=%s",exception);
exception.printStackTrace();
      System.err.println("ERROR graphics: "+exception.getCause());
      printStacktrace(exception);
    }
    catch (AssertionError assertionError)
    {
      printInternalError(assertionError);
      System.err.println("Please report this assertion error to torsten.rupp"+MAIL_AT+"gmx.net."); // use MAIL_AT to avoid SPAM
    }
    catch (InternalError error)
    {
      printInternalError(error);
      System.err.println("Please report this internal error to torsten.rupp"+MAIL_AT+"gmx.net."); // use MAIL_AT to avoid SPAM
    }
    catch (Error error)
    {
      printInternalError(error);
      System.err.println("Please report this error to torsten.rupp"+MAIL_AT+"gmx.net."); // use MAIL_AT to avoid SPAM
    }

    System.exit(exitcode);
  }

  /** show error message in dialog
   */
  public void showError(final String message)
  {
    display.syncExec(new Runnable()
    {
      public void run()
      {
        Dialogs.error(shell,message);
      }
    });
  }

  //-----------------------------------------------------------------------

  /** static initializer
   */
  {
    // initialize file associations (Windows only)
    if (isWindowsSystem())
    {
    }
  }

  /** print program usage
   */
  private void printUsage()
  {
    System.out.println("timemine usage: <options> [--]");
    System.out.println("");
    System.out.println("Options: ");
    System.out.println("");
    System.out.println("  -h|--help  - print this help");
    System.out.println("  --debug    - enable debug mode");
  }

  /** parse arguments
   * @param args arguments
   */
  private void parseArguments(String[] args)
  {
    // parse arguments
    int z = 0;
    boolean endOfOptions = false;
    while (z < args.length)
    {
      if      (!endOfOptions && args[z].equals("--"))
      {
        endOfOptions = true;
        z++;
      }
      else if (!endOfOptions && (args[z].startsWith("--") || args[z].startsWith("-")))
      {
        int i = Options.parse(options,args,z,Settings.class);
        if (i < 0)
        {
          throw new Error("Unknown option '"+args[z]+"'!");
        }
        z = i;
      }
      else
      {
        z++;
      }
    }

    // help
    if (Settings.helpFlag)
    {
      printUsage();
      System.exit(EXITCODE_OK);
    }

    // check arguments
  }

  /** check if system is Windows system
   * @return TRUE iff Windows, FALSE otherwise
   */
  private static boolean isWindowsSystem()
  {
    String osName = System.getProperty("os.name").toLowerCase();

    return (osName.indexOf("win") >= 0);
  }

  /** init display variables
   */
  private void initDisplay()
  {
    display = new Display();

    // get colors
    COLOR_BLACK                   = display.getSystemColor(SWT.COLOR_BLACK);
    COLOR_WHITE                   = display.getSystemColor(SWT.COLOR_WHITE);
    COLOR_BACKGROUND              = display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
    COLOR_TODAY_TIME_ENTRIES      = new ColorData(Settings.colorTodayTimeEntries);
    COLOR_TIME_ENTRIES            = new ColorData(Settings.colorTimeEntries);
    COLOR_TIME_ENTRIES_UPDATE     = new ColorData(Settings.colorTimeEntriesUpdate);
    COLOR_TIME_ENTRIES_INCOMPLETE = new ColorData(Settings.colorTimeEntriesIncomplete);
    COLOR_TIME_ENTRIES_WEEKEND    = new ColorData(Settings.colorTimeEntriesWeekend);
    COLOR_TIME_ENTRIES_VACATION   = new ColorData(Settings.colorTimeEntriesVacation);

    // get images
    IMAGE_PROGRAM_ICON            = Widgets.loadImage(display,"program-icon.png");

    // get cursors
    CURSOR_WAIT                   = new Cursor(display,SWT.CURSOR_WAIT);

    // date formats
    DATE_FORMAT                   = new SimpleDateFormat(Settings.dateFormat);
  }

  /** init loaded classes/JARs watchdog
   */
  private void initClassesWatchDog()
  {
    // get timestamp of all classes/JAR files
    final HashMap<File,Long> classModifiedMap = new HashMap<File,Long>();
    LinkedList<File> directoryList = new LinkedList<File>();
    for (String name : System.getProperty("java.class.path").split(File.pathSeparator))
    {
      File file = new File(name);
      if (file.isDirectory())
      {
        directoryList.add(file);
      }
      else
      {
        classModifiedMap.put(file,new Long(file.lastModified()));
      }
    }
    while (directoryList.size() > 0)
    {
      File directory = directoryList.removeFirst();
      File[] files = directory.listFiles();
      if (files != null)
      {
        for (File file : files)
        {
          if (file.isDirectory())
          {
            directoryList.add(file);
          }
          else
          {
            classModifiedMap.put(file,new Long(file.lastModified()));
          }
        }
      }
    }

    // periodically check timestamp of classes/JAR files
    Thread classWatchDogThread = new Thread()
    {
      public void run()
      {
        final long REMINDER_TIME = 5*60*1000;

        long            lastRemindedTimestamp = 0L;
        final boolean[] reminderFlag          = new boolean[]{true};

        for (;;)
        {
          // check timestamps, show warning dialog
          for (final File file : classModifiedMap.keySet())
          {
            if (   reminderFlag[0]
                && (file.lastModified() > classModifiedMap.get(file))
                && (System.currentTimeMillis() > (lastRemindedTimestamp+REMINDER_TIME))
               )
            {
//Dprintf.dprintf("file=%s %d -> %d",file,file.lastModified(),classModifiedMap.get(file));
              display.syncExec(new Runnable()
              {
                public void run()
                {
                  switch (Dialogs.select(shell,"Warning","Class/JAR file '"+file.getName()+"' changed. Is is recommended to restart Timemine now.",new String[]{"Restart","Remind me again in 5min","Ignore"},0))
                  {
                    case 0:
                      // send close event with restart
                      Widgets.notify(shell,USER_EVENT_QUIT,EXITCODE_RESTART);
                      break;
                    case 1:
                      break;
                    case 2:
                      reminderFlag[0] = false;
                      break;
                  }
                }
              });
              lastRemindedTimestamp = System.currentTimeMillis();
            }
          }

          // sleep a short time
          try { Thread.sleep(30*1000); } catch (InterruptedException exception) { /* ignored */ }
        }
      }
    };
    classWatchDogThread.setDaemon(true);
    classWatchDogThread.start();
  }

  /** create main window
   */
  private void createWindow()
  {
    Composite   tab;
    TableColumn tableColumn;
    Group       group;
    Composite   composite,subComposite;
    Button      button;
    Label       label;
    Combo       combo;
    Spinner     spinner;
    Text        text;
    Menu        menu;
    MenuItem    menuItem;

    // create window
    shell = new Shell(display,SWT.SHELL_TRIM);
    shell.setText("Timemine");
    shell.setImage(IMAGE_PROGRAM_ICON);
    shell.setLayout(new TableLayout(new double[]{1.0,0.0,0.0},1.0));
    shell.addShellListener(new ShellListener()
    {
      public void shellClosed(ShellEvent shellEvent)
      {
        shell.setVisible(false);
        shellEvent.doit = false;
      }
      public void shellActivated(ShellEvent e)
      {
      };
      public void shellDeactivated(ShellEvent e)
      {
      };
      public void shellDeiconified(ShellEvent e)
      {
      };
      public void shellIconified(ShellEvent e)
      {
      };
    });

    // create tabs
    widgetTabFolder = Widgets.newTabFolder(shell);
    Widgets.layout(widgetTabFolder,0,0,TableLayoutData.NSWE);

    widgetTabToday = Widgets.addTab(widgetTabFolder,"Today ("+Widgets.acceleratorToText(SWT.F5)+")");
    widgetTabToday.setLayout(new TableLayout(new double[]{1.0,0.0,0.0},1.0,2));
    Widgets.layout(widgetTabToday,0,0,TableLayoutData.NSWE);
    {
      // key accelerators
      final int ACCELERATOR_PROJECT  = SWT.CTRL+'p';
      final int ACCELERATOR_ISSUE    = SWT.CTRL+'i';
      final int ACCELERATOR_SPENT    = SWT.CTRL+'t';
      final int ACCELERATOR_ACTIVITY = SWT.CTRL+'y';
      final int ACCELERATOR_COMMENTS = SWT.CTRL+'c';
      final int ACCELERATOR_SAVE     = SWT.CTRL+'s';

      // today time entry list
      widgetTodayTimeEntryTable = Widgets.newTable(widgetTabToday,SWT.MULTI);
      widgetTodayTimeEntryTable.setLayout(new TableLayout(null,new double[]{1.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0}));
      Widgets.layout(widgetTodayTimeEntryTable,0,0,TableLayoutData.NSWE);
      SelectionListener selectionListener = new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          TableColumn         tableColumn         = (TableColumn)selectionEvent.widget;
          TimeEntryComparator timeEntryComparator = new TimeEntryComparator(redmine);

          if      (tableColumn == widgetTodayTimeEntryTable.getColumn(0)) timeEntryComparator.setSortMode(TimeEntryComparator.SortModes.HOURS   );
          else if (tableColumn == widgetTodayTimeEntryTable.getColumn(1)) timeEntryComparator.setSortMode(TimeEntryComparator.SortModes.ACTIVITY);
          else if (tableColumn == widgetTodayTimeEntryTable.getColumn(2)) timeEntryComparator.setSortMode(TimeEntryComparator.SortModes.PROJECT );
          else if (tableColumn == widgetTodayTimeEntryTable.getColumn(3)) timeEntryComparator.setSortMode(TimeEntryComparator.SortModes.ISSUE   );
          else if (tableColumn == widgetTodayTimeEntryTable.getColumn(4)) timeEntryComparator.setSortMode(TimeEntryComparator.SortModes.COMMENTS);
          Widgets.sortTableColumn(widgetTodayTimeEntryTable,tableColumn,timeEntryComparator);
        }
      };
      tableColumn = Widgets.addTableColumn(widgetTodayTimeEntryTable,0,"Hours",   SWT.RIGHT, 50,false);
      tableColumn.addSelectionListener(selectionListener);
      tableColumn = Widgets.addTableColumn(widgetTodayTimeEntryTable,1,"Activity",SWT.LEFT,  70,true );
      tableColumn.addSelectionListener(selectionListener);
      tableColumn = Widgets.addTableColumn(widgetTodayTimeEntryTable,2,"Project", SWT.LEFT,  80,true );
      tableColumn.addSelectionListener(selectionListener);
      tableColumn = Widgets.addTableColumn(widgetTodayTimeEntryTable,3,"Issue",   SWT.LEFT, 100,true );
      tableColumn.addSelectionListener(selectionListener);
      tableColumn = Widgets.addTableColumn(widgetTodayTimeEntryTable,4,"Comments",SWT.LEFT, 200,true );
      tableColumn.addSelectionListener(selectionListener);
      Widgets.setTableColumnWidth(widgetTodayTimeEntryTable,Settings.geometryTodayTimeEntryColumns.width);

      menu = Widgets.newPopupMenu(shell);
      {
        menuItem = Widgets.addMenuItem(menu,"Add new\u2026",Settings.keyNewTimeEntry);
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            Widgets.invoke(widgetNew);
          }
        });
        menuItem = Widgets.addMenuItem(menu,"Delete\u2026",Settings.keyDeleteTimeEntry);
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            Widgets.invoke(widgetDelete);
          }
        });
      }
      widgetTodayTimeEntryTable.setMenu(menu);

      widgetTodayTimeEntryTable.addMouseListener(new MouseListener()
      {
        public void mouseDoubleClick(MouseEvent mouseEvent)
        {
          Table       widget     = (Table)mouseEvent.widget;
          TableItem[] tableItems = widget.getSelection();

          if (tableItems.length > 0)
          {
            Redmine.TimeEntry timeEntry = (Redmine.TimeEntry)tableItems[0].getData();

            Redmine.SpentOn prevSpentOn = timeEntry.spentOn;
            if (editTimeEntry(timeEntry,"Edit time entry","Save"))
            {
              try
              {
                // update time entry
                redmine.update(timeEntry);

                // refresh/remove today time entry table entry
                if (timeEntry.spentOn.isToday())
                {
                  // refresh
                  refreshTableItem(widgetTodayTimeEntryTable,timeEntry);
                }
                else
                {
                  // remove
                  removeTableItem(widgetTodayTimeEntryTable,timeEntry);
                }

                // refresh tree items
                refreshTreeItem(widgetTimeEntryTree,prevSpentOn);
                refreshTreeItem(widgetTimeEntryTree,timeEntry.spentOn);
              }
              catch (RedmineException exception)
              {
                Dialogs.error(shell,"Cannot update time entry on Redmine server (error: "+exception.getMessage()+")");
                return;
              }
            }
          }
        }
        public void mouseDown(MouseEvent mouseEvent)
        {
        }
        public void mouseUp(MouseEvent mouseEvent)
        {
        }
      });
      widgetTodayTimeEntryTable.addKeyListener(new KeyListener()
      {
        public void keyPressed(KeyEvent keyEvent)
        {
          Table       widget     = (Table)keyEvent.widget;
          TableItem[] tableItems = widget.getSelection();

          if      (Widgets.isAccelerator(keyEvent,SWT.INSERT))
          {
            Widgets.setFocus(widgetProjects);
          }
          else if (Widgets.isAccelerator(keyEvent,SWT.DEL) || Widgets.isAccelerator(keyEvent,SWT.BS))
          {
            if (tableItems.length > 0)
            {
              if (Dialogs.confirm(shell,"Delete "+tableItems.length+" time entries?"))
              {
                try
                {
                  for (TableItem tableItem : tableItems)
                  {
                    Redmine.TimeEntry timeEntry = (Redmine.TimeEntry)tableItem.getData();
                    deleteTimeEntry(timeEntry);
                    removeTableItem(widgetTodayTimeEntryTable,timeEntry);
                  }
                }
                catch (RedmineException exception)
                {
                  Dialogs.error(shell,"Cannot delete data from Redmine server (error: "+exception.getMessage()+")");
                  return;
                }
              }
            }
          }
        }
        public void keyReleased(KeyEvent keyEvent)
        {
        }
      });
      widgetTodayTimeEntryTable.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Table       widget     = (Table)selectionEvent.widget;
          TableItem[] tableItems = widget.getSelection();

          if (tableItems.length > 0)
          {
            int index;

            Redmine.TimeEntry timeEntry = (Redmine.TimeEntry)tableItems[0].getData();
            Redmine.Activity  activity  = redmine.getActivity(timeEntry.activityId);
            Redmine.Project   project   = redmine.getProject(timeEntry.projectId);
            Redmine.Issue     issue     = redmine.getIssue(timeEntry.issueId);

            // select matching project
            int projectIndex = getIndex(projectIds,timeEntry.projectId);
            if ((projectIndex >= 0) && (projectIndex != widgetProjects.getSelectionIndex()))
            {
              widgetProjects.select(projectIndex);

              // get issues for project
              Redmine.Issue[] issues = null;
              try
              {
                issues = redmine.getIssueArray(projectIds[projectIndex]);
              }
              catch (RedmineException exception)
              {
                Dialogs.error(shell,"Cannot get data from Redmine server (error: "+exception.getMessage()+")");
                return;
              }

              // show sorted issues
              widgetIssueIds.removeAll();
              Arrays.sort(issues,new Comparator<Redmine.Issue>()
              {
                public int compare(Redmine.Issue issue0, Redmine.Issue issue1)
                {
                  assert issue0 != null;
                  assert issue1 != null;

                  if      (issue0.id < issue1.id) return -1;
                  else if (issue0.id > issue1.id) return  1;
                  else                            return  0;
                }
              });
              issueIds0 = new int[issues.length];
              for (int i = 0; i < issues.length; i++)
              {
                widgetIssueIds.add(Integer.toString(issues[i].id));
                issueIds0[i] = issues[i].id;
              }

              widgetIssues.removeAll();
              Arrays.sort(issues,new Comparator<Redmine.Issue>()
              {
                public int compare(Redmine.Issue issue0, Redmine.Issue issue1)
                {
                  assert issue0 != null;
                  assert issue1 != null;

                  return issue0.subject.compareTo(issue1.subject);
                }
              });
              issueIds1 = new int[issues.length];
              for (int i = 0; i < issues.length; i++)
              {
                widgetIssues.add(issues[i].subject);
                issueIds1[i] = issues[i].id;
              }
            }

            // select matching issue
            index = getIndex(issueIds0,timeEntry.issueId);
            if (index >= 0) widgetIssueIds.select(index);
            index = getIndex(issueIds1,timeEntry.issueId);
            if (index >= 0) widgetIssues.select(index);
            widgetIssueStatus.setText((issue != null) ? redmine.getStatusName(issue.statusId,"") : "");

            // select matching activity
            index = getIndex(activityIds,timeEntry.activityId);
            if (index >= 0) widgetActivities.select(index);

            // set spent hours, minutes fraction
            widgetSpentHourFraction.setSelection(timeEntry.getHourFraction());
            widgetSpentMinuteFraction.setSelection(timeEntry.getMinuteFraction());

            // set comments
            widgetComments.setText(timeEntry.comments);
          }
        }
      });
      widgetTodayTimeEntryTable.setToolTipText("Time entries of today.\nENTER/RETURN to edit entry.\nDEL/BACKSPACE to delete entries.");

      // add new entry
      group = Widgets.newGroup(widgetTabToday,"New");
      group.setLayout(new TableLayout(0.0,new double[]{0.0,1.0},2));
      Widgets.layout(group,1,0,TableLayoutData.WE);
      {
        label = Widgets.newLabel(group,"Project:",SWT.NONE,ACCELERATOR_PROJECT);
        Widgets.layout(label,0,0,TableLayoutData.W);

        widgetProjects = Widgets.newSelect(group);
        Widgets.layout(widgetProjects,0,1,TableLayoutData.WE,0,2);

        label = Widgets.newLabel(group,"Issue:",SWT.NONE,ACCELERATOR_ISSUE);
        Widgets.layout(label,1,0,TableLayoutData.W);

        composite = Widgets.newComposite(group);
        composite.setLayout(new TableLayout(0.0,new double[]{0.0,1.0}));
        Widgets.layout(composite,1,1,TableLayoutData.WE,0,2);
        {
          widgetIssueIds = Widgets.newSelect(composite,SWT.RIGHT);
          Widgets.layout(widgetIssueIds,0,0,TableLayoutData.W,0,0,0,0,60,SWT.DEFAULT);

          widgetIssues = Widgets.newSelect(composite);
          Widgets.layout(widgetIssues,0,1,TableLayoutData.WE);

          widgetIssueStatus = Widgets.newLabel(composite);
          Widgets.layout(widgetIssueStatus,0,2,TableLayoutData.E,0,0,0,0,60,SWT.DEFAULT);
        }

        label = Widgets.newLabel(group,"Spent:",SWT.NONE,ACCELERATOR_SPENT);
        Widgets.layout(label,2,0,TableLayoutData.W);

        composite = Widgets.newComposite(group);
        composite.setLayout(new TableLayout(0.0,new double[]{0.0,0.0,0.0,0.0,0.0,1.0,0.0}));
        Widgets.layout(composite,2,1,TableLayoutData.WE);
        {
          widgetSpentHourFraction = Widgets.newSpinner(composite,0);
          widgetSpentHourFraction.setTextLimit(2);
          widgetSpentHourFraction.setIncrement(1);
          widgetSpentHourFraction.setSelection(0);
          Widgets.layout(widgetSpentHourFraction,0,0,TableLayoutData.WE);
          widgetSpentHourFraction.setToolTipText("New time entry spent hours.");

          label = Widgets.newLabel(composite,"h");
          Widgets.layout(label,0,1,TableLayoutData.W);

          widgetSpentMinuteFraction = Widgets.newSpinner(composite);
          widgetSpentMinuteFraction.setSelection(Settings.minTimeDelta);
          widgetSpentMinuteFraction.setTextLimit(2);
          widgetSpentMinuteFraction.setIncrement(Settings.minTimeDelta);
          Widgets.layout(widgetSpentMinuteFraction,0,2,TableLayoutData.WE);
          widgetSpentMinuteFraction.setToolTipText("New time entry spent minutes.");

          label = Widgets.newLabel(composite,"min");
          Widgets.layout(label,0,3,TableLayoutData.W);

          combo = Widgets.newOptionMenu(composite);
          Widgets.layout(combo,0,4,TableLayoutData.W,0,0,0,0,64,SWT.DEFAULT);
          String[] values = new String[1+8*60/Settings.minTimeDelta];
          values[0] = "";
          for (int i = 1; i < values.length; i++)
          {
            values[i] = formatHours((double)(i*Settings.minTimeDelta)/60.0);
          }
          combo.setItems(values);
          combo.addSelectionListener(new SelectionListener()
          {
            public void widgetDefaultSelected(SelectionEvent selectionEvent)
            {
            }
            public void widgetSelected(SelectionEvent selectionEvent)
            {
              Combo widget = (Combo)selectionEvent.widget;
              int   index  = widget.getSelectionIndex();

              if (index > 0)
              {
                int hourFraction   = (index*Settings.minTimeDelta)/60;
                int minuteFraction = (index*Settings.minTimeDelta)%60;

                widgetSpentHourFraction.setSelection(hourFraction);
                widgetSpentMinuteFraction.setSelection(minuteFraction);

                widget.select(0);
              }
            }
          });

          widgetActivities = Widgets.newSelect(composite);
          Widgets.layout(widgetActivities,0,5,TableLayoutData.WE);
        }

        label = Widgets.newLabel(group,"Comments:",SWT.NONE,ACCELERATOR_COMMENTS);
        Widgets.layout(label,3,0,TableLayoutData.W);

        widgetComments = Widgets.newText(group);
        Widgets.layout(widgetComments,3,1,TableLayoutData.WE);
        widgetComments.setToolTipText("New time entry comment line.");

        widgetAdd = Widgets.newButton(group,"Add new",ACCELERATOR_SAVE);
        Widgets.layout(widgetAdd,2,2,TableLayoutData.NSE,2,0);
      }

      // add listeners
      widgetProjects.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Combo widget = (Combo)selectionEvent.widget;
          int   index  = widget.getSelectionIndex();

          try
          {
            // get issues for project
            Redmine.Issue[] issues = redmine.getIssueArray(projectIds[index]);

            // sorted issues
            widgetIssueIds.removeAll();
            Arrays.sort(issues,new Comparator<Redmine.Issue>()
            {
              public int compare(Redmine.Issue issue0, Redmine.Issue issue1)
              {
                assert issue0 != null;
                assert issue1 != null;

                if      (issue0.id < issue1.id) return -1;
                else if (issue0.id > issue1.id) return  1;
                else                            return  0;
              }
            });
            issueIds0 = new int[issues.length];
            for (int i = 0; i < issues.length; i++)
            {
              widgetIssueIds.add(Integer.toString(issues[i].id));
              issueIds0[i] = issues[i].id;
            }

            widgetIssues.removeAll();
            Arrays.sort(issues,new Comparator<Redmine.Issue>()
            {
              public int compare(Redmine.Issue issue0, Redmine.Issue issue1)
              {
                assert issue0 != null;
                assert issue1 != null;

                return issue0.subject.compareTo(issue1.subject);
              }
            });
            issueIds1 = new int[issues.length];
            for (int i = 0; i < issues.length; i++)
            {
              widgetIssues.add(issues[i].subject);
              issueIds1[i] = issues[i].id;
            }

            // select first issue
            if (issueIds1.length > 0)
            {
              widgetIssueIds.select(getIndex(issueIds0,issueIds1[0]));
              widgetIssues.select(0);
              widgetIssueStatus.setText(redmine.getStatusName(issues[0].statusId,""));
            }
          }
          catch (RedmineException exception)
          {
            Dialogs.error(shell,"Cannot get data from Redmine server (error: "+exception.getMessage()+")");
            return;
          }
        }
      });
      widgetIssueIds.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Combo widget = (Combo)selectionEvent.widget;

          int index = widget.getSelectionIndex();
          if ((index >= 0) && (index < issueIds0.length))
          {
            index = getIndex(issueIds1,issueIds0[index]);
            if (index >= 0) widgetIssues.select(index);
          }
        }
      });
      widgetIssues.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Combo widget = (Combo)selectionEvent.widget;

          int index = widget.getSelectionIndex();
          if ((index >= 0) && (index < issueIds1.length))
          {
            index = getIndex(issueIds0,issueIds1[index]);
            if (index >= 0) widgetIssueIds.select(index);
          }
        }
      });
      widgetSpentMinuteFraction.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          int hourFraction   = widgetSpentHourFraction.getSelection();
          int minuteFraction = widgetSpentMinuteFraction.getSelection();

          // Note: sometimes the minute value is not a multiple of the increment. Correct this.
          if (minuteFraction >= 0)
          {
            minuteFraction = ((minuteFraction+Settings.minTimeDelta-1)/Settings.minTimeDelta)*Settings.minTimeDelta;
          }
          else
          {
            minuteFraction = -((-minuteFraction+Settings.minTimeDelta-1)/Settings.minTimeDelta)*Settings.minTimeDelta;
          }

          if      (minuteFraction >= 60)
          {
            // increment hours
            hourFraction   += minuteFraction/60;
            minuteFraction %= 60;
          }
          else if (minuteFraction < 0)
          {
            if (hourFraction > 0)
            {
              // decrement hours, reset
              hourFraction   -= (-minuteFraction+60-1)/60;
              minuteFraction = 60-Settings.minTimeDelta;
            }
            else
            {
              // reset
              minuteFraction = 0;
            }
          }

          widgetSpentHourFraction.setSelection(hourFraction);
          widgetSpentMinuteFraction.setSelection(minuteFraction);
        }
      });
      widgetAdd.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          int    projectIndex  = widgetProjects.getSelectionIndex();
          int    issueIndex    = widgetIssues.getSelectionIndex();
          int    activityIndex = widgetActivities.getSelectionIndex();
          double hours         = Redmine.toHours(widgetSpentHourFraction.getSelection(),widgetSpentMinuteFraction.getSelection());
          String comments      = widgetComments.getText().trim();

          if ((projectIndex < 0) || (projectIndex >= projectIds.length ))
          {
            Dialogs.error(shell,"Please select a project for the new time entry.");
            Widgets.setFocus(widgetProjects);
            return;
          }
          if ((issueIndex < 0) || (issueIndex >= issueIds1.length ))
          {
            Dialogs.error(shell,"Please select an issue for the new time entry.");
            Widgets.setFocus(widgetIssues);
            return;
          }
          if (hours <= 0)
          {
            Dialogs.error(shell,"Please select some hours for the new time entry.");
            Widgets.setFocus(widgetSpentMinuteFraction);
            return;
          }
          if ((activityIndex < 0) || (activityIndex >= activityIds.length ))
          {
            Dialogs.error(shell,"Please select an activity for the new time entry.");
            Widgets.setFocus(widgetActivities);
            return;
          }
          if (comments.isEmpty())
          {
            Dialogs.error(shell,"Please enter a comment for the new time entry.");
            Widgets.setFocus(widgetComments);
            return;
          }

          Redmine.TimeEntry timeEntry = redmine.new TimeEntry(projectIds[projectIndex],
                                                              issueIds1[issueIndex],
                                                              activityIds[activityIndex],
                                                              hours,
                                                              comments
                                                             );
          try
          {
            redmine.add(timeEntry);

            // add today time entry table entry
            TableItem tableItem = addTableItem(widgetTodayTimeEntryTable,timeEntry);
            widgetTodayTimeEntryTable.setSelection(tableItem);

            // refresh tree item
            refreshTreeItem(widgetTimeEntryTree,timeEntry.spentOn);
          }
          catch (RedmineException exception)
          {
            Dialogs.error(shell,"Cannot add new time entry (error: "+exception.getMessage()+")");
          }
        }
      });

      // add shortcut listener
      Listener keyListener = new Listener()
      {
        public void handleEvent(Event event)
        {
          if (Widgets.isChildOf(widgetTabToday,event.widget))
          {
            if      (Widgets.isAccelerator(event,ACCELERATOR_PROJECT))
            {
              Widgets.setFocus(widgetProjects);
              event.doit = false;
            }
            else if (Widgets.isAccelerator(event,ACCELERATOR_ISSUE))
            {
              Widgets.setFocus(widgetIssues);
              event.doit = false;
            }
            else if (Widgets.isAccelerator(event,ACCELERATOR_SPENT))
            {
              Widgets.setFocus(widgetSpentHourFraction);
              event.doit = false;
            }
            else if (Widgets.isAccelerator(event,ACCELERATOR_ACTIVITY))
            {
              Widgets.setFocus(widgetActivities);
              event.doit = false;
            }
            else if (Widgets.isAccelerator(event,ACCELERATOR_COMMENTS))
            {
              Widgets.setFocus(widgetComments);
              event.doit = false;
            }
            else if (Widgets.isAccelerator(event,ACCELERATOR_SAVE))
            {
              Widgets.invoke(widgetAdd);
              event.doit = false;
            }
          }
        }
      };
      display.addFilter(SWT.KeyDown,keyListener);

      // set next focus
      Widgets.setNextFocus(widgetProjects,widgetIssueIds,widgetIssues,widgetSpentHourFraction,widgetSpentMinuteFraction,widgetActivities,widgetComments,widgetAdd);
    }
    widgetTabToday.addListener(SWT.Show,new Listener()
    {
      public void handleEvent(Event event)
      {
        Widgets.setFocus(widgetTodayTimeEntryTable);
      }
    });

    widgetTabAll = Widgets.addTab(widgetTabFolder,"All ("+Widgets.acceleratorToText(SWT.F6)+")");
    widgetTabAll.setLayout(new TableLayout(new double[]{1.0,0.0},1.0,2));
    Widgets.layout(widgetTabAll,0,0,TableLayoutData.NSWE);
    {
      // today time entry list
      widgetTimeEntryTree = Widgets.newTree(widgetTabAll,SWT.VIRTUAL|SWT.MULTI);
      widgetTimeEntryTree.setLayout(new TableLayout(1.0,1.0));
      Widgets.layout(widgetTimeEntryTree,0,0,TableLayoutData.NSWE);
      Widgets.addTreeColumn(widgetTimeEntryTree,"Spent on",SWT.LEFT,  80,true);
      Widgets.addTreeColumn(widgetTimeEntryTree,"Hours",   SWT.RIGHT, 50,true);
      Widgets.addTreeColumn(widgetTimeEntryTree,"Activity",SWT.LEFT,  70,true);
      Widgets.addTreeColumn(widgetTimeEntryTree,"Project", SWT.LEFT,  80,true);
      Widgets.addTreeColumn(widgetTimeEntryTree,"Issue",   SWT.LEFT, 100,true);
      Widgets.addTreeColumn(widgetTimeEntryTree,"Comments",SWT.LEFT, 200,true);
      Widgets.setTreeColumnWidth(widgetTimeEntryTree,Settings.geometryTimeEntryColumns.width);

      menu = Widgets.newPopupMenu(shell);
      {
        menuItem = Widgets.addMenuItem(menu,"Add new\u2026",Settings.keyNewTimeEntry);
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            Widgets.invoke(widgetNew);
          }
        });
        menuItem = Widgets.addMenuItem(menu,"Delete\u2026",Settings.keyDeleteTimeEntry);
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            Widgets.invoke(widgetDelete);
          }
        });

        Widgets.addMenuSeparator(menu);

        menuItem = Widgets.addMenuItem(menu,"Mark as vacation days");
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            TreeItem[] treeItems = widgetTimeEntryTree.getSelection();

            if (treeItems.length > 0)
            {
              for (TreeItem treeItem : treeItems)
              {
                if (treeItem.getData() instanceof Redmine.SpentOn)
                {
                  // add vacation day
                  addVacationDate((Redmine.SpentOn)treeItem.getData());

                  // refresh tree item
                  Event treeEvent = new Event();
                  treeEvent.item = treeItem;
                  widgetTimeEntryTree.notifyListeners(SWT.SetData,treeEvent);
                }
              }
            }
          }
        });
        menuItem = Widgets.addMenuItem(menu,"Mark as working days");
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            TreeItem[] treeItems = widgetTimeEntryTree.getSelection();

            if (treeItems.length > 0)
            {
              for (TreeItem treeItem : treeItems)
              {
                if (treeItem.getData() instanceof Redmine.SpentOn)
                {
                  // remove vacation day
                  removeVacationDate((Redmine.SpentOn)treeItem.getData());

                  // refresh tree item
                  Event treeEvent = new Event();
                  treeEvent.item = treeItem;
                  widgetTimeEntryTree.notifyListeners(SWT.SetData,treeEvent);
                }
              }
            }
          }
        });
      }
      widgetTimeEntryTree.setMenu(menu);

      widgetTimeEntryTree.addTreeListener(new TreeListener()
      {
        public void treeCollapsed(TreeEvent treeEvent)
        {
          TreeItem treeItem = (TreeItem)treeEvent.item;

          clearTreeEntries(treeItem);
        }
        public void treeExpanded(TreeEvent treeEvent)
        {
          TreeItem        treeItem = (TreeItem)treeEvent.item;
          Redmine.SpentOn spentOn  = (Redmine.SpentOn)treeItem.getData();

          try
          {
            setTreeEntries(treeItem,redmine.getTimeEntryArray(spentOn));
          }
          catch (RedmineException exception)
          {
            // ignored
            return;
          }
        }
      });
      widgetTimeEntryTree.addListener(SWT.SetData,new Listener()
      {
        public void handleEvent(Event event)
        {
          Tree     tree     = (Tree)event.widget;
          TreeItem treeItem = (TreeItem)event.item;

          try
          {
            int index = tree.indexOf(treeItem);
//Dprintf.dprintf("update treeItem=%s index=%d",treeItem,index);
            if (index >= 0)
            {
              Redmine.SpentOn spentOn        = redmine.new SpentOn(-index);
              int             spentOnWeekday = spentOn.getWeekday();

              double hoursSum = redmine.getTimeEntryHoursSum(spentOn);
//Dprintf.dprintf("update treeItem=%s index=%d spentOn=%s hoursSum=%f",treeItem,index,spentOn,hoursSum);

              treeItem.setData(spentOn);
              treeItem.setText(0,spentOn.toString(DATE_FORMAT));
              treeItem.setText(1,(hoursSum != Redmine.HOURS_UPDATE) ? formatHours(hoursSum) : "");
              if      ((spentOnWeekday == Calendar.SATURDAY) || (spentOnWeekday == Calendar.SUNDAY))
              {
                treeItem.setForeground(COLOR_TIME_ENTRIES_WEEKEND.foreground);
                treeItem.setBackground(COLOR_TIME_ENTRIES_WEEKEND.background);
              }
              else if (isVacationDate(spentOn))
              {
                treeItem.setForeground(COLOR_TIME_ENTRIES_VACATION.foreground);
                treeItem.setBackground(COLOR_TIME_ENTRIES_VACATION.background);
              }
              else if (hoursSum == Redmine.HOURS_UPDATE)
              {
                treeItem.setForeground(COLOR_TIME_ENTRIES_UPDATE.foreground);
                treeItem.setBackground(COLOR_TIME_ENTRIES_UPDATE.background);
              }
              else if (hoursSum < Settings.requiredHoursPerDay)
              {
                treeItem.setForeground(COLOR_TIME_ENTRIES_INCOMPLETE.foreground);
                treeItem.setBackground(COLOR_TIME_ENTRIES_INCOMPLETE.background);
              }
              else
              {
                treeItem.setForeground(COLOR_TIME_ENTRIES.foreground);
                treeItem.setBackground(COLOR_TIME_ENTRIES.background);
              }
              if (treeItem.getExpanded())
              {
                setTreeEntries(treeItem,redmine.getTimeEntryArray(spentOn));
              }
              else
              {
                clearTreeEntries(treeItem);
              }
            }
          }
          catch (RedmineException exception)
          {
            // ignored
            return;
          }
        }
      });
      widgetTimeEntryTree.addMouseListener(new MouseListener()
      {
        public void mouseDoubleClick(MouseEvent mouseEvent)
        {
          Tree widget = (Tree)mouseEvent.widget;

          // get tree item at mouse position (Note: use first column which is required on Windows)
          TreeItem treeItem = widget.getItem(new Point(widget.getColumns()[0].getWidth()/2,mouseEvent.y));

          if (treeItem != null)
          {
            if      (treeItem.getData() instanceof Redmine.SpentOn)
            {
              /* On Linux a double-click does not open the entry by default. Send a
                 event to initiate this behavior on Linux.
              */
              if (System.getProperty("os.name").toLowerCase().matches("linux"))
              {
                Event treeEvent = new Event();
                treeEvent.item = treeItem;
                if (treeItem.getExpanded())
                {
                  widget.notifyListeners(SWT.Collapse,treeEvent);
                }
                else
                {
                  widget.notifyListeners(SWT.Expand,treeEvent);
                }
              }
            }
            else if (treeItem.getData() instanceof Redmine.TimeEntry)
            {
              Redmine.TimeEntry timeEntry = (Redmine.TimeEntry)treeItem.getData();

              Redmine.SpentOn prevSpentOn = timeEntry.spentOn;
              if (editTimeEntry(timeEntry,"Edit time entry","Save"))
              {
                try
                {
                  // update time entry
                  redmine.update(timeEntry);

                  // refresh tree items
                  refreshTreeItem(widget,prevSpentOn);
                  refreshTreeItem(widget,timeEntry.spentOn);

                  // refresh/add/remove today time entry table entry
                  if (timeEntry.spentOn.isToday())
                  {
                    if (prevSpentOn.isToday())
                    {
                      // refresh
                      refreshTableItem(widgetTodayTimeEntryTable,timeEntry);
                    }
                    else
                    {
                      // add
                      addTableItem(widgetTodayTimeEntryTable,timeEntry);
                    }
                  }
                  else
                  {
                    // remove
                    removeTableItem(widgetTodayTimeEntryTable,timeEntry);
                  }
                }
                catch (RedmineException exception)
                {
                  Dialogs.error(shell,"Cannot update time entry on Redmine server (error: "+exception.getMessage()+")");
                  return;
                }
              }
            }
          }
        }
        public void mouseDown(MouseEvent mouseEvent)
        {
        }
        public void mouseUp(MouseEvent mouseEvent)
        {
        }
      });
      widgetTimeEntryTree.addKeyListener(new KeyListener()
      {
        public void keyPressed(KeyEvent keyEvent)
        {
          Tree widget = (Tree)keyEvent.widget;

          if      (Widgets.isAccelerator(keyEvent,SWT.SPACE))
          {
            TreeItem[] treeItems = widget.getSelection();
            if (treeItems.length > 0)
            {
              Event treeEvent = new Event();
              treeEvent.item = treeItems[0];
              if (treeItems[0].getExpanded())
              {
                widget.notifyListeners(SWT.Collapse,treeEvent);
              }
              else
              {
                widget.notifyListeners(SWT.Expand,treeEvent);
              }
            }
          }
          else if (   Widgets.isAccelerator(keyEvent,Settings.keyEditTimeEntry)
                   || Widgets.isAccelerator(keyEvent,SWT.CR)
                   || Widgets.isAccelerator(keyEvent,SWT.KEYPAD_CR)
                  )
          {
            TreeItem[] treeItems = widget.getSelection();
            if (treeItems.length > 0)
            {
              if (treeItems[0].getData() instanceof Redmine.TimeEntry)
              {
                Redmine.TimeEntry timeEntry = (Redmine.TimeEntry)treeItems[0].getData();

                if (editTimeEntry(timeEntry,"Edit time entry","Save"))
                {
                  try
                  {
                    // update time entry
                    redmine.update(timeEntry);

                    Redmine.Activity activity = redmine.getActivity(timeEntry.activityId);
                    Redmine.Project  project  = redmine.getProject(timeEntry.projectId);
                    Redmine.Issue    issue    = redmine.getIssue(timeEntry.issueId);

                    // update tree item
                    treeItems[0].setText(1,formatHours(timeEntry.hours));
                    treeItems[0].setText(2,(activity != null) ? activity.name : "");
                    treeItems[0].setText(3,(project != null) ? project.name : "");
                    treeItems[0].setText(4,(issue != null) ? issue.subject : "");
                    treeItems[0].setText(5,timeEntry.comments);

                    // refresh parent tree item
                    Event treeEvent = new Event();
                    treeEvent.item = treeItems[0].getParentItem();
                    widget.notifyListeners(SWT.SetData,treeEvent);

                    // update today time entry table entry
                    refreshTableItem(widgetTodayTimeEntryTable,timeEntry);
                  }
                  catch (RedmineException exception)
                  {
                    Dialogs.error(shell,"Cannot update time entry on Redmine server (error: "+exception.getMessage()+")");
                    return;
                  }
                }
              }
            }
          }
          else if (   Widgets.isAccelerator(keyEvent,Settings.keyNewTimeEntry)
                   || Widgets.isAccelerator(keyEvent,SWT.INSERT)
                  )
          {
            // get date of selected day or date of today
            Redmine.SpentOn spentOn;

            TreeItem[] treeItems = widgetTimeEntryTree.getSelection();
            if (treeItems.length > 0)
            {
              TreeItem treeItem = treeItems[0];
              while (!(treeItem.getData() instanceof Redmine.SpentOn))
              {
                treeItem = treeItem.getParentItem();
              }
              spentOn = (Redmine.SpentOn)(((Redmine.SpentOn)treeItem.getData()).clone());
            }
            else
            {
              spentOn = redmine.today();
            }

            try
            {
              // add new time entry
              addTimeEntry(spentOn);
            }
            catch (RedmineException exception)
            {
              Dialogs.error(shell,"Cannot add new time entry (error: "+exception.getMessage()+")");
              return;
            }
          }
          else if (   Widgets.isAccelerator(keyEvent,Settings.keyDeleteTimeEntry)
                   || Widgets.isAccelerator(keyEvent,SWT.DEL)
                   || Widgets.isAccelerator(keyEvent,SWT.BS)
                  )
          {
            TreeItem[] treeItems = widget.getSelection();
            if (treeItems.length > 0)
            {
              if (Dialogs.confirm(shell,"Delete "+treeItems.length+" time entries?"))
              {
                try
                {
                  for (TreeItem treeItem : treeItems)
                  {
                    Redmine.TimeEntry timeEntry = (Redmine.TimeEntry)treeItem.getData();
                    deleteTimeEntry(timeEntry);
                    removeTreeItem(widgetTimeEntryTree,timeEntry);
                  }
                }
                catch (RedmineException exception)
                {
                  Dialogs.error(shell,"Cannot delete time entries (error: "+exception.getMessage()+")");
                  return;
                }
              }
            }
          }
        }
        public void keyReleased(KeyEvent keyEvent)
        {
        }
      });
      widgetTimeEntryTree.setToolTipText("All time entries of user.\nENTER/RETURN to open/edit entry.\nDEL/BACKSPACE to delete entries.");

      composite = Widgets.newComposite(widgetTabAll);
      composite.setLayout(new TableLayout(0.0,0.0));
      Widgets.layout(composite,1,0,TableLayoutData.E);
      {
        widgetNew = Widgets.newButton(composite,"Add\u2026",Settings.keyNewTimeEntry);
        Widgets.layout(widgetNew,0,0,TableLayoutData.E,0,0,0,0,80,SWT.DEFAULT);
        widgetNew.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            // get spent-on of selected day or date of today
            Redmine.SpentOn spentOn;

            TreeItem[] treeItems = widgetTimeEntryTree.getSelection();
            if (treeItems.length > 0)
            {
              TreeItem treeItem = treeItems[0];
              while (!(treeItem.getData() instanceof Redmine.SpentOn))
              {
                treeItem = treeItem.getParentItem();
              }
              spentOn = (Redmine.SpentOn)(((Redmine.SpentOn)treeItem.getData()).clone());
            }
            else
            {
              spentOn = redmine.today();
            }

            try
            {
              // add new time entry
              addTimeEntry(spentOn);
            }
            catch (RedmineException exception)
            {
              Dialogs.error(shell,"Cannot add new time entry (error: "+exception.getMessage()+")");
              return;
            }
          }
        });

        widgetDelete = Widgets.newButton(composite,"Delete\u2026",Settings.keyDeleteTimeEntry);
        Widgets.layout(widgetDelete,0,1,TableLayoutData.E,0,0,0,0,80,SWT.DEFAULT);
        widgetDelete.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
Dprintf.dprintf("");
            TreeItem[] treeItems = widgetTimeEntryTree.getSelection();
            if (treeItems.length > 0)
            {
              // get time entries to delete
              ArrayList<Redmine.TimeEntry> timeEntryList = new ArrayList<Redmine.TimeEntry>();
              try
              {
                for (TreeItem treeItem : treeItems)
                {
                  if       (treeItem.getData() instanceof Redmine.SpentOn)
                  {
                    for (Redmine.TimeEntry timeEntry : redmine.getTimeEntryArray((Redmine.SpentOn)treeItem.getData()))
                    {
                      timeEntryList.add(timeEntry);
                    }
                  }
                  else if  (treeItem.getData() instanceof Redmine.TimeEntry)
                  {
                    timeEntryList.add((Redmine.TimeEntry)treeItem.getData());
                  }
                }
              }
              catch (RedmineException exception)
              {
                Dialogs.error(shell,"Cannot get time entries to delete (error: "+exception.getMessage()+")");
                return;
              }

              if (   (timeEntryList.size() > 0)
                  && Dialogs.confirm(shell,"Delete "+timeEntryList.size()+" time entries?")
                 )
              {
                try
                {
                  for (Redmine.TimeEntry timeEntry : timeEntryList)
                  {
                    deleteTimeEntry(timeEntry);
                    removeTableItem(widgetTodayTimeEntryTable,timeEntry);
                    removeTreeItem(widgetTimeEntryTree,timeEntry);
                  }
                }
                catch (RedmineException exception)
                {
                  Dialogs.error(shell,"Cannot delete time entries (error: "+exception.getMessage()+")");
                  return;
                }
              }
            }
          }
        });
      }
    }
    widgetTabAll.addListener(SWT.Show,new Listener()
    {
      public void handleEvent(Event event)
      {
        Widgets.setFocus(widgetTimeEntryTree);
      }
    });

    Widgets.showTab(widgetTabFolder,widgetTabToday);
    Widgets.setFocus(widgetTodayTimeEntryTable);
  }

  /** create menu
   */
  private void createMenu()
  {
    Menu     menuBar;
    Menu     menu,subMenu;
    MenuItem menuItem;

    // create menu
    menuBar = Widgets.newMenuBar(shell);

    menu = Widgets.addMenu(menuBar,"Program");
    {
      menuItem = Widgets.addMenuItem(menu,"Refresh");
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          refreshTask.run();
          refreshTimeEntriesTask.run();
        }
      });

      menuItem = Widgets.addMenuItem(menu,"Preferences\u2026",Settings.keyPreferences);
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          editPreferences();
        }
      });

      Widgets.addMenuSeparator(menu);

      menuItem = Widgets.addMenuItem(menu,"Restart");
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          // send close event with restart
          Widgets.notify(shell,USER_EVENT_QUIT,EXITCODE_RESTART);
        }
      });

      menuItem = Widgets.addMenuItem(menu,"Quit",Settings.keyQuit);
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          // send close-event to shell
          Widgets.notify(shell,USER_EVENT_QUIT,EXITCODE_OK);
        }
      });
    }

    menu = Widgets.addMenu(menuBar,"Help");
    {
      menuItem = Widgets.addMenuItem(menu,"About");
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Dialogs.info(shell,"About","Timemine "+Config.VERSION_MAJOR+"."+Config.VERSION_MINOR+" (revision "+Config.VERSION_REVISION+").\n\nWritten by Torsten Rupp.");
        }
      });
    }
  }

  /** create tray item
   */
  private void createTrayItem()
  {
    final Menu menu;
    MenuItem   menuItem;

    tray = display.getSystemTray();
    if (tray != null)
    {
      trayItem = new TrayItem (tray, SWT.NONE);
      trayItem.setImage(IMAGE_PROGRAM_ICON);
      trayItem.addListener(SWT.Show, new Listener()
      {
        public void handleEvent (Event event)
        {
          System.out.println("show");
        }
      });
      trayItem.setToolTipText("Timemine - Redmine time utility");
      trayItem.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          shell.setVisible(!shell.isVisible());
        }
      });

      menu = new Menu(shell,SWT.POP_UP);
      menuItem = Widgets.addMenuItem(menu,"Restart");
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          // send close-event to shell
          Widgets.notify(shell,USER_EVENT_QUIT,EXITCODE_RESTART);
        }
      });
      menuItem = Widgets.addMenuItem(menu,"Quit");
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          // send close-event to shell
          Widgets.notify(shell,USER_EVENT_QUIT,EXITCODE_OK);
        }
      });
      trayItem.addListener(SWT.MenuDetect,new Listener()
      {
        public void handleEvent(Event event)
        {
          menu.setVisible(true);
        }
      });
    }
    else
    {
      trayItem = null;
    }
  }

  /** create event handlers
   */
  private void createEventHandlers()
  {
  }

  /** init all
   */
  private void initAll()
  {
    // init display
    initDisplay();

    // connect to Redmine server
    LoginData loginData = new LoginData(Settings.serverName,Settings.serverPort,Settings.serverUseSSL,Settings.loginName,Settings.loginPassword);
    boolean connectOkFlag = false;
    if (!Settings.showLoginFlag && !connectOkFlag)
    {
      // try to connect to Redmine server with given credentials
      try
      {
        // init Redmine client
//Dprintf.dprintf("%s %d name=%s pass=%s",loginData.serverName,loginData.serverPort,loginData.loginName,loginData.loginPassword);
        redmine = new Redmine(loginData.serverName,loginData.serverPort,loginData.serverUseSSL,loginData.loginName,loginData.loginPassword);
        connectOkFlag = true;
      }
      catch (RedmineException exception)
      {
        // ignored
      }
    }
    while (!connectOkFlag)
    {
      // get login data
      if (!getLoginData(loginData))
      {
        System.exit(EXITCODE_OK);
      }

      // try to connect to Redmine server
      try
      {
        // init Redmine client
        redmine = new Redmine(loginData.serverName,
                              loginData.serverPort,
                              loginData.serverUseSSL,
                              loginData.loginName,
                              loginData.loginPassword
                             );
        connectOkFlag = true;
      }
      catch (RedmineException exception)
      {
        if (!Dialogs.confirmError(new Shell(),"Connection fail","Error: "+exception.getMessage()+((exception.getCause() != null) ? ":\n\n"+exception.getCause().getMessage() : ""),"Try again","Cancel"))
        {
          System.exit(EXITCODE_FAIL);
        }
      }
    }
    Settings.serverName   = loginData.serverName;
    Settings.serverPort   = loginData.serverPort;
    Settings.serverUseSSL = loginData.serverUseSSL;
    Settings.loginName    = loginData.loginName;

    // add watchdog for loaded classes/JARs
    initClassesWatchDog();

    // create main window and menu
    createWindow();
    createMenu();
    createTrayItem();
    createEventHandlers();

    // start timer to refresh data
    refreshTask = new TimerTask()
    {
      public void run()
      {
        if (Settings.debugFlag)
        {
          System.err.println("DEBUG: Refresh data");
        }
        try
        {
          // clear caches
          redmine.clearCaches();

          // get projects, activities
          final Redmine.Project[]  projects   = redmine.getProjectArray();
          final Redmine.Activity[] activities = redmine.getActivityArray();

          // sort projects, activities
          Arrays.sort(projects,new Comparator<Redmine.Project>()
          {
            public int compare(Redmine.Project project0, Redmine.Project project1)
            {
              assert project0 != null;
              assert project1 != null;

              return project0.name.compareTo(project1.name);
            }
          });
          Arrays.sort(activities,new Comparator<Redmine.Activity>()
          {
            public int compare(Redmine.Activity activity0, Redmine.Activity activity1)
            {
              assert activity0 != null;
              assert activity1 != null;

              return activity0.name.compareTo(activity1.name);
            }
          });

          // refresh project list, activity list, time entry table
          display.syncExec(new Runnable()
          {
            public void run()
            {
              // show projects
              widgetProjects.removeAll();
              projectIds = new int[projects.length];
              for (int i = 0; i < projects.length; i++)
              {
                widgetProjects.add(projects[i].name);
                projectIds[i] = projects[i].id;
              }

              // show activities
              widgetActivities.removeAll();
              activityIds = new int[activities.length];
              for (int i = 0; i < activities.length; i++)
              {
                widgetActivities.add(activities[i].name);
                if (activities[i].isDefault) widgetActivities.select(i);
                activityIds[i] = activities[i].id;
              }
            }
          });
        }
        catch (RedmineException exception)
        {
          // ignored
        }
      }
    };
    refreshTimer = new Timer();
    refreshTimer.schedule(refreshTask,0L,REFRESH_TIME*1000L);

    refreshTodayTimeEntriesTask = new TimerTask()
    {
      public void run()
      {
        if (Settings.debugFlag)
        {
          System.err.println("DEBUG: Refresh data");
        }
        try
        {
          // get today time entries
          final SoftHashMap<Integer,Redmine.TimeEntry> timeEntryMap = redmine.getTimeEntries(redmine.today());
//Dprintf.dprintf("timeEntryMap.size()=%d",timeEntryMap.size());

          // refresh today time entry table
          display.syncExec(new Runnable()
          {
            public void run()
            {
              // update entries, remove not existing entries
              TableItem[] tableItems = widgetTodayTimeEntryTable.getItems();
              for (TableItem tableItem : tableItems)
              {
                Redmine.TimeEntry timeEntry = (Redmine.TimeEntry)tableItem.getData();

                if (timeEntryMap.containsKey(timeEntry.id))
                {
                  refreshTableItem(widgetTodayTimeEntryTable,timeEntry);
                  timeEntryMap.remove(timeEntry.id);
                }
                else
                {
                  removeTableItem(widgetTodayTimeEntryTable,timeEntry);
                }
              }

              // update existing entries

              // add new entries
              for (Redmine.TimeEntry timeEntry : timeEntryMap.values())
              {
                addTableItem(widgetTodayTimeEntryTable,timeEntry);
              }
            }
          });
        }
        catch (RedmineException exception)
        {
          // ignored
        }
      }
    };
    refreshTodayTimeEntryTimer = new Timer();
    refreshTodayTimeEntryTimer.schedule(refreshTodayTimeEntriesTask,0L,10*1000L);

    refreshTimeEntriesTask = new TimerTask()
    {
      public void run()
      {
        if (Settings.debugFlag)
        {
          System.err.println("DEBUG: Refresh time entry data");
        }
        try
        {
          // get number ot time entry days
          final int timeEntryDays = (int)((System.currentTimeMillis()-redmine.getTimeEntryStartDate().getTime())/(24*60*60*1000));

          // refreh today time entry table
          display.syncExec(new Runnable()
          {
            public void run()
            {
              // update all tree items
              for (TreeItem treeItem : widgetTimeEntryTree.getItems())
              {
                if (treeItem.getData() instanceof Redmine.SpentOn)
                {
                  Event treeEvent = new Event();
                  treeEvent.item = treeItem;
                  widgetTimeEntryTree.notifyListeners(SWT.SetData,treeEvent);
                }
              }

              // set total number of time entries in all-table
              widgetTimeEntryTree.setItemCount(timeEntryDays);
            }
          });
        }
        catch (RedmineException exception)
        {
          // ignored
        }
      }
    };
    refreshTimeEntryTimer = new Timer();
    refreshTimeEntryTimer.schedule(refreshTimeEntriesTask,0L,10*1000L);
  }

  /** done all
   */
  private void doneAll()
  {
    // shutdown running background tasks
    refreshTimeEntryTimer.cancel();
    refreshTodayTimeEntryTimer.cancel();
  }

  /** run application
   * @return exit code
   */
  private int run()
  {
    final int[] result = new int[1];

    // set window size, manage window (Note: for some reason the layout may change after the Windows is open)
    shell.setSize(Settings.geometryMain.x,Settings.geometryMain.y);
    shell.open();
    shell.setSize(Settings.geometryMain.x,Settings.geometryMain.y);

    // listener
    shell.addListener(USER_EVENT_QUIT,new Listener()
    {
      public void handleEvent(Event event)
      {
        // store geometry
        Settings.geometryMain                  = shell.getSize();
        Settings.geometryTodayTimeEntryColumns = new Settings.ColumnSizes(Widgets.getTableColumnWidth(widgetTodayTimeEntryTable));
        Settings.geometryTimeEntryColumns      = new Settings.ColumnSizes(Widgets.getTreeColumnWidth(widgetTimeEntryTree));

        // save settings
        boolean saveSettings = true;
        if (Settings.isFileModified())
        {
          switch (Dialogs.select(shell,"Confirmation","Settings were modified externally.",new String[]{"Overwrite","Just quit","Cancel"},0))
          {
            case 0:
              break;
            case 1:
              saveSettings = false;
              break;
            case 2:
              event.doit = false;
              return;
          }
        }
        if (saveSettings)
        {
          Settings.save();
        }

        // store exitcode
        result[0] = event.index;

        // close
        shell.dispose();
      }
    });

    display.addFilter(SWT.KeyDown,new Listener()
    {
      public void handleEvent(Event event)
      {
        if      (Widgets.isAccelerator(event,Settings.keyTabTodayTimeEntries))
        {
          Widgets.showTab(widgetTabFolder,widgetTabToday);
          Widgets.setFocus(widgetTodayTimeEntryTable);
          event.doit = false;
        }
        else if (Widgets.isAccelerator(event,Settings.keyTabTimeEntries))
        {
          Widgets.showTab(widgetTabFolder,widgetTabAll);
          Widgets.setFocus(widgetTimeEntryTree);
          event.doit = false;
        }
/*
        else if (Widgets.isAccelerator(event,Settings.keyNewTimeEntry))
        {
Dprintf.dprintf("");
          Redmine.TimeEntry timeEntry = redmine.new TimeEntry(Redmine.ID_NONE,
                                                              Redmine.ID_NONE,
                                                              redmine.getDefaultActivityId(),
                                                              (double)Settings.minTimeDelta/60.0,
                                                              ""
                                                             );

          if (editTimeEntry(timeEntry))
          {
Dprintf.dprintf("");
          }
        }
*/
      }
    });

    // SWT event loop
    while (!shell.isDisposed())
    {
//System.err.print(".");
      if (!display.readAndDispatch())
      {
        display.sleep();
      }
    }

    return result[0];
  }

  /** server/password dialog
   * @param loginData server login data
   * @return true iff login data ok, false otherwise
   */
  private boolean getLoginData(final LoginData loginData)
  {
    TableLayout     tableLayout;
    TableLayoutData tableLayoutData;
    Composite       composite,subComposite;
    Label           label;
    Button          button;

    final Shell dialog = Dialogs.openModal(new Shell(),"Login Redmine server",300,SWT.DEFAULT);

    final Text    widgetServerName;
    final Spinner widgetServerPort;
    final Button  widgetServerUseSSL;
    final Text    widgetLoginName;
    final Text    widgetLoginPassword;
    final Button  widgetLoginButton;

    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(0.0,new double[]{0.0,1.0},2));
    Widgets.layout(composite,0,0,TableLayoutData.NSWE);
    {
      label = Widgets.newLabel(composite,"Server:");
      Widgets.layout(label,0,0,TableLayoutData.W);

      subComposite = Widgets.newComposite(composite);
      subComposite.setLayout(new TableLayout(0.0,new double[]{1.0,0.0,0.0}));
      Widgets.layout(subComposite,0,1,TableLayoutData.WE);
      {
        widgetServerName = new Text(subComposite,SWT.LEFT|SWT.BORDER);
        if (loginData.serverName != null) widgetServerName.setText(loginData.serverName);
        widgetServerName.setLayoutData(new TableLayoutData(0,0,TableLayoutData.WE));

        label = Widgets.newLabel(subComposite,"Port:");
        Widgets.layout(label,0,1,TableLayoutData.W);

        widgetServerPort = Widgets.newSpinner(subComposite,1,65535);
        widgetServerPort.setTextLimit(5);
        widgetServerPort.setSelection(loginData.serverPort);
        Widgets.layout(widgetServerPort,0,2,TableLayoutData.WE);

        widgetServerUseSSL = Widgets.newCheckbox(subComposite,"SSL");
        widgetServerUseSSL.setSelection(loginData.serverUseSSL);
        Widgets.layout(widgetServerUseSSL,0,3,TableLayoutData.E);
      }

      label = Widgets.newLabel(composite,"Name:");
      Widgets.layout(label,1,0,TableLayoutData.W);

      widgetLoginName = Widgets.newText(composite);
      if (loginData.loginName != null) widgetLoginName.setText(loginData.loginName);
      Widgets.layout(widgetLoginName,1,1,TableLayoutData.WE);

      label = Widgets.newLabel(composite,"Password:");
      Widgets.layout(label,2,0,TableLayoutData.W);

      widgetLoginPassword = Widgets.newPassword(composite);
      Widgets.layout(widgetLoginPassword,2,1,TableLayoutData.WE);
    }

    // buttons
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(0.0,1.0,2));
    Widgets.layout(composite,1,0,TableLayoutData.WE);
    {
      widgetLoginButton = Widgets.newButton(composite,"Login");
      Widgets.layout(widgetLoginButton,0,0,TableLayoutData.W,0,0,0,0,60,SWT.DEFAULT);
      widgetLoginButton.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          loginData.serverName    = widgetServerName.getText();
          loginData.serverPort    = widgetServerPort.getSelection();
          loginData.serverUseSSL  = widgetServerUseSSL.getSelection();
          loginData.loginName     = widgetLoginName.getText();
          loginData.loginPassword = widgetLoginPassword.getText();
          Dialogs.close(dialog,true);
        }
      });

      button = Widgets.newButton(composite,"Cancel");
      Widgets.layout(button,0,1,TableLayoutData.E,0,0,0,0,60,SWT.DEFAULT);
      button.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Dialogs.close(dialog,false);
        }
      });
    }

    // add listeners

    // set next focus
    Widgets.setNextFocus(widgetServerName,widgetServerPort,widgetServerUseSSL,widgetLoginName,widgetLoginPassword,widgetLoginButton);

    // run
    if ((loginData.serverName != null) && (loginData.serverName.length() != 0)) Widgets.setFocus(widgetLoginPassword);
    Boolean result = (Boolean)Dialogs.run(dialog);

    return (result != null) ? result : false;
  }

  /** get index in integer array
   * @param array integer array
   * @param value value to find
   * @return index or -1
   */
  private int getIndex(int[] array, int value)
  {
    for (int i = 0; i < array.length; i++)
    {
      if (array[i] == value) return i;
    }
    return -1;
  }

  /** format hours/minutes into string
   * @param time time
   * @return string
   */
  private String formatHours(double hours)
  {
    return String.format("%02d:%02d",(int)Math.floor(hours),(((int)Math.floor(hours*100.0)%100)*60)/100);
  }

  /** check if days are equal
   * @param date0,date1 dates to check
   * @return true iff days are equal
   */
  private boolean isSameDay(Date date0, Date date1)
  {
    Calendar calendar0 = Calendar.getInstance(); calendar0.setTime(date0);
    Calendar calendar1 = Calendar.getInstance(); calendar1.setTime(date1);

    return    (calendar0.get(Calendar.YEAR ) == calendar1.get(Calendar.YEAR ))
           && (calendar0.get(Calendar.MONTH) == calendar1.get(Calendar.MONTH))
           && (calendar0.get(Calendar.DATE ) == calendar1.get(Calendar.DATE ));
  }

  /** add time entry to table
   * @param table table widget
   * @param timeEntry time entry to add
   */
  private TableItem addTableItem(Table table, Redmine.TimeEntry timeEntry)
  {
    Redmine.Activity activity = redmine.getActivity(timeEntry.activityId);
    Redmine.Project  project  = redmine.getProject(timeEntry.projectId);
    Redmine.Issue    issue    = redmine.getIssue(timeEntry.issueId);

    return Widgets.addTableEntry(widgetTodayTimeEntryTable,
                                 timeEntry,
                                 formatHours(timeEntry.hours),
                                 (activity != null) ? activity.name : "",
                                 (project != null) ? project.name : "",
                                 (issue != null) ? issue.subject : "",
                                 timeEntry.comments
                                );
  }

  /** remove time entry from table
   * @param table table widget
   * @param timeEntry time entry to remove
   */
  private void removeTableItem(Table table, Redmine.TimeEntry timeEntry)
  {
    Widgets.removeTableEntry(widgetTodayTimeEntryTable,timeEntry);
  }

  /** refresh time entry in table
   * @param table table widget
   * @param timeEntry time entry to refresh
   */
  private void refreshTableItem(Table table, Redmine.TimeEntry timeEntry)
  {
    Redmine.Activity activity = redmine.getActivity(timeEntry.activityId);
    Redmine.Project  project  = redmine.getProject(timeEntry.projectId);
    Redmine.Issue    issue    = redmine.getIssue(timeEntry.issueId);

    Widgets.updateTableEntry(widgetTodayTimeEntryTable,
                             timeEntry,
                             formatHours(timeEntry.hours),
                             (activity != null) ? activity.name : "",
                             (project != null) ? project.name : "",
                             (issue != null) ? issue.subject : "",
                             timeEntry.comments
                            );
  }

  /** set sub-tree time entries
   * @param treeItem parent tree item
   * @param timeEntries time entries to set
   */
  private void setTreeEntries(TreeItem treeItem, Redmine.TimeEntry[] timeEntries)
    throws RedmineException
  {
    final String EMPTY = StringUtils.repeat(' ',255);

    treeItem.removeAll();
    for (Redmine.TimeEntry timeEntry : timeEntries)
    {
      Redmine.Activity activity = redmine.getActivity(timeEntry.activityId);
      Redmine.Project  project  = redmine.getProject(timeEntry.projectId);
      Redmine.Issue    issue    = redmine.getIssue(timeEntry.issueId);

      TreeItem subTreeItem = new TreeItem(treeItem,SWT.NONE);
      subTreeItem.setData(timeEntry);
      subTreeItem.setText(0,EMPTY);
      subTreeItem.setText(1,formatHours(timeEntry.hours));
      subTreeItem.setText(2,(activity != null) ? activity.name : "");
      subTreeItem.setText(3,(project != null) ? project.name : "");
      subTreeItem.setText(4,(issue != null) ? issue.subject : "");
      subTreeItem.setText(5,timeEntry.comments);
      subTreeItem.setForeground(COLOR_TIME_ENTRIES.foreground);
      subTreeItem.setBackground(COLOR_TIME_ENTRIES.background);
    }
    treeItem.setExpanded(true);
  }

  /** clear sub-tree time entries
   * @param treeItem parent tree item
   */
  private void clearTreeEntries(TreeItem treeItem)
  {
    treeItem.removeAll();
    new TreeItem(treeItem,SWT.NONE);
    treeItem.setExpanded(false);
  }

  /** remove time entry and collaps sub-tree if needed
   * @param tree tree
   * @param timeEntry time entry to remove
   * @return
   */
  private void removeTreeItem(Tree tree, Redmine.TimeEntry timeEntry)
  {
    Widgets.removeTreeEntry(tree,timeEntry);
  }

  /** refresh time entry in tree
   * @param tree tree widget
   * @param spentOn spent-on date
   */
  private void refreshTreeItem(Tree tree, Redmine.SpentOn spentOn)
  {
    for (TreeItem refreshTreeItem : tree.getItems())
    {
      if      (refreshTreeItem.getData() instanceof Redmine.SpentOn)
      {
        if (spentOn.equals((Redmine.SpentOn)refreshTreeItem.getData()))
        {
          Event treeEvent = new Event();
          treeEvent.item = refreshTreeItem;
//Dprintf.dprintf("found refreshTreeItem=%s",refreshTreeItem);
          tree.notifyListeners(SWT.SetData,treeEvent);
          break;
        }
      }
    }
  }

  /** edit time entry
   * @param timeEntry time entry to edit
   * @param title window title text
   * @param okText ok-button text
   * @return true if time entry edited, false otherwise
   */
  private boolean editTimeEntry(final Redmine.TimeEntry timeEntry, String title, String okText)
  {
    /** dialog data
     */
    class Data
    {
      int[] projectIds;
      int[] issueIds0;
      int[] issueIds1;
      int[] activityIds;

      Data()
      {
        projectIds  = null;
        issueIds0   = null;
        issueIds1   = null;
        activityIds = null;
      }
    };

    // key accelerators
    final int ACCELERATOR_PROJECT  = SWT.CTRL+'p';
    final int ACCELERATOR_ISSUE    = SWT.CTRL+'i';
    final int ACCELERATOR_SPENT    = SWT.CTRL+'t';
    final int ACCELERATOR_ACTIVITY = SWT.CTRL+'y';
    final int ACCELERATOR_COMMENTS = SWT.CTRL+'c';
    final int ACCELERATOR_SAVE     = SWT.CTRL+'s';

    final Data  data = new Data();
    final Shell dialog;
    Composite   composite,subComposite,subSubComposite;
    Label       label;
    Combo       combo;
    Button      button;

    // repository edit dialog
    dialog = Dialogs.openModal(shell,title,new double[]{1.0,0.0},1.0);

    final Combo    widgetProjects;
    final Combo    widgetIssueIds;
    final Combo    widgetIssues;
    final DateTime widgetSpentOn;
    final Spinner  widgetSpentHourFraction;
    final Spinner  widgetSpentMinuteFraction;
    final Combo    widgetActivities;
    final Text     widgetComments;
    final Button   widgetSave;
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(0.0,new double[]{0.0,1.0,0.0},4));
    Widgets.layout(composite,0,0,TableLayoutData.NSWE,0,0,4);
    {
      label = Widgets.newLabel(composite,"Project:",SWT.NONE,ACCELERATOR_PROJECT);
      Widgets.layout(label,0,0,TableLayoutData.W);

      widgetProjects = Widgets.newSelect(composite);
      Widgets.layout(widgetProjects,0,1,TableLayoutData.WE);

      label = Widgets.newLabel(composite,"Issue:",SWT.NONE,ACCELERATOR_ISSUE);
      Widgets.layout(label,1,0,TableLayoutData.W);

      subComposite = Widgets.newComposite(composite);
      subComposite.setLayout(new TableLayout(0.0,new double[]{0.0,1.0}));
      Widgets.layout(subComposite,1,1,TableLayoutData.WE);
      {
        widgetIssueIds = Widgets.newSelect(subComposite);
        Widgets.layout(widgetIssueIds,0,0,TableLayoutData.W,0,0,0,0,60,SWT.DEFAULT);

        widgetIssues = Widgets.newSelect(subComposite);
        Widgets.layout(widgetIssues,0,1,TableLayoutData.WE);

        widgetIssueStatus = Widgets.newLabel(subComposite);
        Widgets.layout(widgetIssueStatus,0,2,TableLayoutData.E,0,0,0,0,60,SWT.DEFAULT);
      }

      label = Widgets.newLabel(composite,"Spent:",SWT.NONE,ACCELERATOR_SPENT);
      Widgets.layout(label,2,0,TableLayoutData.W);

      subComposite = Widgets.newComposite(composite);
      subComposite.setLayout(new TableLayout(0.0,1.0));
      Widgets.layout(subComposite,2,1,TableLayoutData.WE);
      {
        subSubComposite = Widgets.newComposite(subComposite);
        subSubComposite.setLayout(new TableLayout(0.0,0.0));
        Widgets.layout(subSubComposite,0,0,TableLayoutData.WE);
        {
          widgetSpentOn = Widgets.newDate(subSubComposite);
          widgetSpentOn.setDay  (timeEntry.spentOn.getDay());
          widgetSpentOn.setMonth(timeEntry.spentOn.getMonth());
          widgetSpentOn.setYear (timeEntry.spentOn.getYear());
          Widgets.layout(widgetSpentOn,0,0,TableLayoutData.W);
          widgetSpentOn.setToolTipText("Spent-on date.");

          widgetSpentHourFraction = Widgets.newSpinner(subSubComposite,0);
          widgetSpentHourFraction.setTextLimit(2);
          widgetSpentHourFraction.setIncrement(1);
          widgetSpentHourFraction.setSelection(timeEntry.getHourFraction());
          Widgets.layout(widgetSpentHourFraction,0,1,TableLayoutData.WE,0,0,0,0,60,SWT.DEFAULT);
          widgetSpentHourFraction.setToolTipText("Spent time hours.");

          label = Widgets.newLabel(subSubComposite,"h");
          Widgets.layout(label,0,2,TableLayoutData.W);

          widgetSpentMinuteFraction = Widgets.newSpinner(subSubComposite);
          widgetSpentMinuteFraction.setTextLimit(2);
          widgetSpentMinuteFraction.setIncrement(Settings.minTimeDelta);
          widgetSpentMinuteFraction.setSelection(timeEntry.getMinuteFraction());
          Widgets.layout(widgetSpentMinuteFraction,0,3,TableLayoutData.WE,0,0,0,0,60,SWT.DEFAULT);
          widgetSpentMinuteFraction.setToolTipText("Spent time minutes.");

          label = Widgets.newLabel(subSubComposite,"min");
          Widgets.layout(label,0,4,TableLayoutData.W);

          combo = Widgets.newOptionMenu(subSubComposite);
          Widgets.layout(combo,0,5,TableLayoutData.W);
          String[] values = new String[1+8*60/Settings.minTimeDelta];
          values[0] = "";
          for (int i = 1; i < values.length; i++)
          {
            values[i] = formatHours((double)(i*Settings.minTimeDelta)/60.0);
          }
          combo.setItems(values);
          combo.addSelectionListener(new SelectionListener()
          {
            public void widgetDefaultSelected(SelectionEvent selectionEvent)
            {
            }
            public void widgetSelected(SelectionEvent selectionEvent)
            {
              Combo widget = (Combo)selectionEvent.widget;
              int   index  = widget.getSelectionIndex();

              if (index > 0)
              {
                int hourFraction   = (index*Settings.minTimeDelta)/60;
                int minuteFraction = (index*Settings.minTimeDelta)%60;

                widgetSpentHourFraction.setSelection(hourFraction);
                widgetSpentMinuteFraction.setSelection(minuteFraction);

                widget.select(0);
              }
            }
          });
        }
      }

      label = Widgets.newLabel(composite,"Activity:",SWT.NONE,ACCELERATOR_ACTIVITY);
      Widgets.layout(label,3,0,TableLayoutData.W);

      widgetActivities = Widgets.newSelect(composite);
      Widgets.layout(widgetActivities,3,1,TableLayoutData.WE);

      label = Widgets.newLabel(composite,"Comments:",SWT.NONE,ACCELERATOR_COMMENTS);
      Widgets.layout(label,4,0,TableLayoutData.W);

      widgetComments = Widgets.newText(composite);
      widgetComments.setText(timeEntry.comments);
      Widgets.layout(widgetComments,4,1,TableLayoutData.WE);
      widgetComments.setToolTipText("Time entry comment line.");
    }

    // buttons
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(0.0,1.0));
    Widgets.layout(composite,1,0,TableLayoutData.WE,0,0,4);
    {
      widgetSave = Widgets.newButton(composite,okText,ACCELERATOR_SAVE);
      Widgets.layout(widgetSave,0,0,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      widgetSave.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          int    projectIndex  = widgetProjects.getSelectionIndex();
          int    issueIndex    = widgetIssues.getSelectionIndex();
          int    activityIndex = widgetActivities.getSelectionIndex();
          double hours         = Redmine.toHours(widgetSpentHourFraction.getSelection(),widgetSpentMinuteFraction.getSelection());
          String comments      = widgetComments.getText().trim();

          if ((projectIndex < 0) || (projectIndex >= data.projectIds.length ))
          {
            Dialogs.error(shell,"Please select a project for the new time entry.");
            Widgets.setFocus(widgetProjects);
            return;
          }
          if ((issueIndex < 0) || (issueIndex >= data.issueIds1.length ))
          {
            Dialogs.error(shell,"Please select an issue for the new time entry.");
            Widgets.setFocus(widgetIssues);
            return;
          }
          if (hours <= 0)
          {
            Dialogs.error(shell,"Please select some hours for the new time entry.");
            Widgets.setFocus(widgetSpentMinuteFraction);
            return;
          }
          if ((activityIndex < 0) || (activityIndex >= data.activityIds.length ))
          {
            Dialogs.error(shell,"Please select an activity for the new time entry.");
            Widgets.setFocus(widgetActivities);
            return;
          }
          if (comments.isEmpty())
          {
            Dialogs.error(shell,"Please enter a comment for the new time entry.");
            Widgets.setFocus(widgetComments);
            return;
          }

          timeEntry.projectId  = data.projectIds[projectIndex];
          timeEntry.issueId    = data.issueIds1[issueIndex];
          timeEntry.spentOn    = redmine.new SpentOn(Widgets.getDate(widgetSpentOn));
          timeEntry.hours      = hours;
          timeEntry.activityId = data.activityIds[activityIndex];
          timeEntry.comments   = comments;

          Dialogs.close(dialog,true);
        }
      });

      button = Widgets.newButton(composite,"Cancel");
      Widgets.layout(button,0,1,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      button.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Dialogs.close(dialog,false);
        }
      });
    }

    // get projects, issues, activities
    Redmine.Project[]  projects   = null;
    Redmine.Issue[]    issues     = null;
    Redmine.Activity[] activities = null;
    try
    {
      projects   = redmine.getProjectArray();
      issues     = redmine.getIssueArray(timeEntry.projectId);
      activities = redmine.getActivityArray();
    }
    catch (RedmineException exception)
    {
      Dialogs.error(shell,"Cannot get data from Redmine server (error: "+exception.getMessage()+")");
      return false;
    }

    // show sorted projects, issues, activities
    Arrays.sort(projects,new Comparator<Redmine.Project>()
    {
      public int compare(Redmine.Project project0, Redmine.Project project1)
      {
        assert project0 != null;
        assert project1 != null;

        return project0.name.compareTo(project1.name);
      }
    });
    data.projectIds = new int[projects.length];
    for (int i = 0; i < projects.length; i++)
    {
      widgetProjects.add(projects[i].name);
      if (timeEntry.projectId == projects[i].id) widgetProjects.select(i);
      data.projectIds[i] = projects[i].id;
    }

    Arrays.sort(issues,new Comparator<Redmine.Issue>()
    {
      public int compare(Redmine.Issue issue0, Redmine.Issue issue1)
      {
        assert issue0 != null;
        assert issue1 != null;

        if      (issue0.id < issue1.id) return -1;
        else if (issue0.id > issue1.id) return  1;
        else                            return  0;
      }
    });
    data.issueIds0 = new int[issues.length];
    for (int i = 0; i < issues.length; i++)
    {
      widgetIssueIds.add(Integer.toString(issues[i].id));
      if (timeEntry.issueId == issues[i].id) widgetIssueIds.select(i);
      data.issueIds0[i] = issues[i].id;
    }
    Arrays.sort(issues,new Comparator<Redmine.Issue>()
    {
      public int compare(Redmine.Issue issue0, Redmine.Issue issue1)
      {
        assert issue0 != null;
        assert issue1 != null;

        return issue0.subject.compareTo(issue1.subject);
      }
    });
    data.issueIds1 = new int[issues.length];
    for (int i = 0; i < issues.length; i++)
    {
      widgetIssues.add(issues[i].subject);
      if (timeEntry.issueId == issues[i].id) widgetIssues.select(i);
      data.issueIds1[i] = issues[i].id;
    }

    Arrays.sort(activities,new Comparator<Redmine.Activity>()
    {
      public int compare(Redmine.Activity activity0, Redmine.Activity activity1)
      {
        assert activity0 != null;
        assert activity1 != null;

        return activity0.name.compareTo(activity1.name);
      }
    });
    data.activityIds = new int[activities.length];
    for (int i = 0; i < activities.length; i++)
    {
      widgetActivities.add(activities[i].name);
      if (timeEntry.activityId == activities[i].id) widgetActivities.select(i);
      data.activityIds[i] = activities[i].id;
    }

    // add listeners
    widgetProjects.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
        Combo widget = (Combo)selectionEvent.widget;
        int   index  = widget.getSelectionIndex();

        // get issues for project
        Redmine.Issue[] issues = null;
        try
        {
          issues = redmine.getIssueArray(data.projectIds[index]);
        }
        catch (RedmineException exception)
        {
          Dialogs.error(shell,"Cannot get data from Redmine server (error: "+exception.getMessage()+")");
          return;
        }

        // show sorted issues
        widgetIssueIds.removeAll();
        Arrays.sort(issues,new Comparator<Redmine.Issue>()
        {
          public int compare(Redmine.Issue issue0, Redmine.Issue issue1)
          {
            assert issue0 != null;
            assert issue1 != null;

            if      (issue0.id < issue1.id) return -1;
            else if (issue0.id > issue1.id) return  1;
            else                            return  0;
          }
        });
        data.issueIds0 = new int[issues.length];
        for (int i = 0; i < issues.length; i++)
        {
          widgetIssueIds.add(Integer.toString(issues[i].id));
          data.issueIds0[i] = issues[i].id;
        }

        Arrays.sort(issues,new Comparator<Redmine.Issue>()
        {
          public int compare(Redmine.Issue issue0, Redmine.Issue issue1)
          {
            assert issue0 != null;
            assert issue1 != null;

            return issue0.subject.compareTo(issue1.subject);
          }
        });
        widgetIssues.removeAll();
        data.issueIds1 = new int[issues.length];
        for (int i = 0; i < issues.length; i++)
        {
          widgetIssues.add(issues[i].subject);
          data.issueIds1[i] = issues[i].id;
        }

        // select first issue
        if (data.issueIds1.length > 0)
        {
          widgetIssueIds.select(getIndex(data.issueIds0,data.issueIds1[0]));
          widgetIssues.select(0);
          widgetIssueStatus.setText(redmine.getStatusName(issues[0].statusId,""));
        }
      }
    });
    widgetIssueIds.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
        Combo widget = (Combo)selectionEvent.widget;

        int index = widget.getSelectionIndex();
        if ((index >= 0) && (index < data.issueIds0.length))
        {
          index = getIndex(data.issueIds1,data.issueIds0[index]);
          if (index >= 0) widgetIssues.select(index);
        }
      }
    });
    widgetIssues.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
        Combo widget = (Combo)selectionEvent.widget;

        int index = widget.getSelectionIndex();
        if ((index >= 0) && (index < data.issueIds1.length))
        {
          index = getIndex(data.issueIds0,data.issueIds1[index]);
          if (index >= 0) widgetIssueIds.select(index);
        }
      }
    });
    widgetSpentMinuteFraction.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
        int hourFraction   = widgetSpentHourFraction.getSelection();
        int minuteFraction = widgetSpentMinuteFraction.getSelection();

        // Note: sometimes the minute value is not a multiple of the increment. Correct this.
        if (minuteFraction >= 0)
        {
          minuteFraction = ((minuteFraction+Settings.minTimeDelta-1)/Settings.minTimeDelta)*Settings.minTimeDelta;
        }
        else
        {
          minuteFraction = -((-minuteFraction+Settings.minTimeDelta-1)/Settings.minTimeDelta)*Settings.minTimeDelta;
        }

        if      (minuteFraction >= 60)
        {
          // increment hours
          hourFraction   += minuteFraction/60;
          minuteFraction %= 60;
        }
        else if (minuteFraction < 0)
        {
          if (hourFraction > 0)
          {
            // decrement hours, reset
            hourFraction   -= (-minuteFraction+60-1)/60;
            minuteFraction = 60-Settings.minTimeDelta;
          }
          else
          {
            // reset
            minuteFraction = 0;
          }
        }

        widgetSpentHourFraction.setSelection(hourFraction);
        widgetSpentMinuteFraction.setSelection(minuteFraction);
      }
    });

    // add shortcut listener
    Listener keyListener = new Listener()
    {
      public void handleEvent(Event event)
      {
        if (Widgets.isChildOf(dialog,event.widget))
        {
          if      (Widgets.isAccelerator(event,ACCELERATOR_PROJECT))
          {
            Widgets.setFocus(widgetProjects);
            event.doit = false;
          }
          else if (Widgets.isAccelerator(event,ACCELERATOR_ISSUE))
          {
            Widgets.setFocus(widgetIssues);
            event.doit = false;
          }
          else if (Widgets.isAccelerator(event,ACCELERATOR_SPENT))
          {
            Widgets.setFocus(widgetSpentOn);
            event.doit = false;
          }
          else if (Widgets.isAccelerator(event,ACCELERATOR_ACTIVITY))
          {
            Widgets.setFocus(widgetActivities);
            event.doit = false;
          }
          else if (Widgets.isAccelerator(event,ACCELERATOR_COMMENTS))
          {
            Widgets.setFocus(widgetComments);
            event.doit = false;
          }
          else if (Widgets.isAccelerator(event,ACCELERATOR_SAVE))
          {
            Widgets.invoke(widgetSave);
            event.doit = false;
          }
        }
      }
    };
    display.addFilter(SWT.KeyDown,keyListener);

    // set next focus
    Widgets.setNextFocus(widgetProjects,
                         widgetIssues,
                         widgetSpentOn,
                         widgetSpentHourFraction,
                         widgetSpentMinuteFraction,
                         widgetActivities,
                         widgetComments,
                         widgetSave
                        );

    // show dialog
    Dialogs.show(dialog);

    // run
    if      (timeEntry.projectId == Redmine.ID_NONE) Widgets.setFocus(widgetProjects);
    else if (timeEntry.issueId   == Redmine.ID_NONE) Widgets.setFocus(widgetIssues);
    else                                             Widgets.setFocus(widgetSpentMinuteFraction);
    boolean result = (Boolean)Dialogs.run(dialog,false);

    // remove listeners, free resources
    display.removeFilter(SWT.KeyDown,keyListener);

    return result;
  }

  /** add new time entry
   * @param spentOn spent-on
   */
  private void addTimeEntry(Redmine.SpentOn spentOn)
    throws RedmineException
  {
    // create new time entry
    Redmine.TimeEntry timeEntry = redmine.new TimeEntry(Redmine.ID_NONE,
                                                        Redmine.ID_NONE,
                                                        redmine.getDefaultActivityId(),
                                                        (double)Settings.minTimeDelta/60.0,
                                                        "",
                                                        spentOn
                                                       );

    if (editTimeEntry(timeEntry,"Add time entry","Add"))
    {
      redmine.add(timeEntry);

      // refresh tree item
      refreshTreeItem(widgetTimeEntryTree,timeEntry.spentOn);

      // add today time entry table entry
      if (timeEntry.spentOn.isToday())
      {
        TableItem tableItem = addTableItem(widgetTodayTimeEntryTable,timeEntry);
        widgetTodayTimeEntryTable.setSelection(tableItem);
      }
    }
  }

  /** add new time entry
   * @param date date
   */
  private void deleteTimeEntry(Redmine.TimeEntry timeEntry)
    throws RedmineException
  {
    redmine.delete(timeEntry);
  }

  /** add vacation date
   * @param date date
   */
  private void addVacationDate(Redmine.SpentOn spentOn)
  {
    Settings.VacationDate vacationDate = new Settings.VacationDate(spentOn.getDate());

    if (!Settings.vacationDateSet.contains(vacationDate))
    {
      Settings.vacationDateSet.add(vacationDate);
    }
  }

  /** remove vacation date
   * @param date date
   */
  private void removeVacationDate(Redmine.SpentOn spentOn)
  {
    Settings.VacationDate vacationDate = new Settings.VacationDate(spentOn.getDate());

    if (Settings.vacationDateSet.contains(vacationDate))
    {
      Settings.vacationDateSet.remove(vacationDate);
    }
  }

  /** check if date is vacation date
   * @param date date
   */
  private boolean isVacationDate(Redmine.SpentOn spentOn)
  {
    return Settings.vacationDateSet.contains(new Settings.VacationDate(spentOn.getDate()));
  }

  /** edit preferences
   */
  private void editPreferences()
  {
    /** dialog data
     */
    class Data
    {
      Data()
      {
      }
    };

    final Data  data = new Data();
    final Shell dialog;
    TabFolder   tabFolder;
    Composite   composite,subComposite,subSubComposite;
    Label       label;
    Combo       combo;
    Button      button;

    // repository edit dialog
    dialog = Dialogs.openModal(shell,"Preferences",new double[]{1.0,0.0},1.0);

    final Text    widgetServerName;
    final Spinner widgetServerPort;
    final Button  widgetServerUseSSL;
    final Text    widgetLoginName;

    final Table   widgetColors;

    final Text    widgetDateFormat;
    final Spinner widgetRequiredHoursPerDay;
    final Table   widgetShowFlags;

    final Button  widgetButtonSave;

    // create tab
    tabFolder = Widgets.newTabFolder(dialog);
    Widgets.layout(tabFolder,0,0,TableLayoutData.NSWE);
    {
      composite = Widgets.addTab(tabFolder,"Server");
      composite.setLayout(new TableLayout(0.0,new double[]{0.0,1.0},2));
      Widgets.layout(composite,0,0,TableLayoutData.NSWE);
      {
        label = Widgets.newLabel(composite,"Server:");
        Widgets.layout(label,0,0,TableLayoutData.W);

        subComposite = Widgets.newComposite(composite);
        subComposite.setLayout(new TableLayout(0.0,new double[]{1.0,0.0,0.0}));
        Widgets.layout(subComposite,0,1,TableLayoutData.WE);
        {
          widgetServerName = new Text(subComposite,SWT.LEFT|SWT.BORDER);
          if (Settings.serverName != null) widgetServerName.setText(Settings.serverName);
          widgetServerName.setLayoutData(new TableLayoutData(0,0,TableLayoutData.WE));

          label = Widgets.newLabel(subComposite,"Port:");
          Widgets.layout(label,0,1,TableLayoutData.W);

          widgetServerPort = Widgets.newSpinner(subComposite,1,65535);
          widgetServerPort.setTextLimit(5);
          widgetServerPort.setSelection(Settings.serverPort);
          Widgets.layout(widgetServerPort,0,2,TableLayoutData.WE);

          widgetServerUseSSL = Widgets.newCheckbox(subComposite,"SSL");
          widgetServerUseSSL.setSelection(Settings.serverUseSSL);
          Widgets.layout(widgetServerUseSSL,0,3,TableLayoutData.E);
        }

        label = Widgets.newLabel(composite,"Login name:");
        Widgets.layout(label,1,0,TableLayoutData.W);

        widgetLoginName = Widgets.newText(composite);
        if (Settings.loginName != null) widgetLoginName.setText(Settings.loginName);
        Widgets.layout(widgetLoginName,1,1,TableLayoutData.WE);
      }
      Widgets.setNextFocus(widgetServerName,widgetServerPort,widgetServerUseSSL,widgetLoginName);

      composite = Widgets.addTab(tabFolder,"Colors");
      composite.setLayout(new TableLayout(1.0,1.0,2));
      Widgets.layout(composite,0,1,TableLayoutData.NSWE);
      {
        widgetColors = Widgets.newTable(composite);
        Widgets.layout(widgetColors,0,0,TableLayoutData.NSWE);
        Widgets.addTableColumn(widgetColors,0,"Name", SWT.LEFT,250,true);
        Widgets.addTableColumn(widgetColors,1,"Foreground",SWT.LEFT,100,true);
        Widgets.addTableColumn(widgetColors,2,"Background",SWT.LEFT,100,true);
        widgetColors.addMouseListener(new MouseListener()
        {
          public void mouseDoubleClick(MouseEvent mouseEvent)
          {
            Table widget = (Table)mouseEvent.widget;

            int index = widget.getSelectionIndex();
            if (index >= 0)
            {
              TableItem      tableItem = widget.getItem(index);
              String         name      = tableItem.getText(0);
              Settings.Color color     = (Settings.Color)tableItem.getData();

              if (editColor(name,color))
              {
                Widgets.setTableEntryColor(widgetColors,color,1,(color.foreground != null) ? new Color(null,color.foreground) : null);
                Widgets.setTableEntryColor(widgetColors,color,2,(color.background != null) ? new Color(null,color.background) : null);
                widgetColors.deselectAll();
              }
            }
          }
          public void mouseDown(MouseEvent mouseEvent)
          {
          }
          public void mouseUp(MouseEvent mouseEvent)
          {
          }
        });
        widgetColors.setToolTipText("Colors list.");
        addColors(widgetColors);
      }

      composite = Widgets.addTab(tabFolder,"Misc");
      composite.setLayout(new TableLayout(new double[]{0.0,0.0,1.0,0.0},new double[]{0.0,1.0},2));
      Widgets.layout(composite,0,7,TableLayoutData.NSWE);
      {
        label = Widgets.newLabel(composite,"Date format:");
        Widgets.layout(label,0,0,TableLayoutData.W);
        widgetDateFormat = Widgets.newText(composite);
        widgetDateFormat.setText(Settings.dateFormat);
        Widgets.layout(widgetDateFormat,0,1,TableLayoutData.WE);
        widgetDateFormat.setToolTipText("Date format.\nPatterns:\n  y - year digit\n  M - month digit\n  d - day digit\n  E - week day name");

        label = Widgets.newLabel(composite,"Required hours per day:");
        Widgets.layout(label,1,0,TableLayoutData.W);
        widgetRequiredHoursPerDay = Widgets.newSpinner(composite,0);
        widgetRequiredHoursPerDay.setTextLimit(2);
        widgetRequiredHoursPerDay.setIncrement(1);
        widgetRequiredHoursPerDay.setSelection((int)Settings.requiredHoursPerDay);
        Widgets.layout(widgetRequiredHoursPerDay,1,1,TableLayoutData.W,0,0,0,0,60,SWT.DEFAULT);
        widgetRequiredHoursPerDay.setToolTipText("Required working hours per day.");

        label = Widgets.newLabel(composite,"Show flags:");
        Widgets.layout(label,2,0,TableLayoutData.NW);
        subComposite = Widgets.newComposite(composite);
        subComposite.setLayout(new TableLayout(1.0,1.0));
        Widgets.layout(subComposite,2,1,TableLayoutData.NSWE);
        {
          widgetShowFlags = Widgets.newTable(subComposite,SWT.CHECK);
          Widgets.layout(widgetShowFlags,0,0,TableLayoutData.NSWE);
          Widgets.addTableColumn(widgetShowFlags,0,"",SWT.LEFT,500,true);
          widgetShowFlags.setLinesVisible(false);
          widgetShowFlags.setHeaderVisible(false);
          widgetShowFlags.addListener(SWT.Resize, new Listener()
          {
            public void handleEvent(Event event)
            {
              Table       table = (Table)event.widget;
              TableColumn tableColumn = table.getColumn(0);

              tableColumn.setWidth(table.getClientArea().width);
            }
          });
          widgetShowFlags.setToolTipText("Show dialogs flags.");

          Widgets.addTableEntry(widgetShowFlags,Settings.showUpdateStatusErrors,"update status errors").setChecked(Settings.showUpdateStatusErrors);
        }

        label = Widgets.newLabel(composite,"Miscellaneous:");
        Widgets.layout(label,3,0,TableLayoutData.W);
        subComposite = Widgets.newComposite(composite);
        subComposite.setLayout(new TableLayout(0.0,0.0));
        Widgets.layout(subComposite,3,1,TableLayoutData.WE);
        {
//TODO cache expire time
//TODO requiredHoursPerDay
        }
      }
    }

    // buttons
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(0.0,1.0));
    Widgets.layout(composite,1,0,TableLayoutData.WE,0,0,4);
    {
      widgetButtonSave = Widgets.newButton(composite,"Save");
      Widgets.layout(widgetButtonSave,0,0,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      widgetButtonSave.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Settings.serverName          = widgetServerName.getText();
          Settings.serverPort          = widgetServerPort.getSelection();
          Settings.serverUseSSL        = widgetServerUseSSL.getSelection();
          Settings.loginName           = widgetLoginName.getText();
          saveColors(widgetColors);
          Settings.dateFormat          = widgetDateFormat.getText().trim();
          Settings.requiredHoursPerDay = (double)widgetRequiredHoursPerDay.getSelection();
          for (TableItem tableItem : widgetShowFlags.getItems())
          {
            Object data = tableItem.getData();
            if (data == Settings.showUpdateStatusErrors) Settings.showUpdateStatusErrors = tableItem.getChecked();
          }

          Settings.geometryPreferences = dialog.getSize();

          Dialogs.close(dialog,true);
        }
      });

      button = Widgets.newButton(composite,"Cancel");
      Widgets.layout(button,0,1,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      button.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Dialogs.close(dialog,false);
        }
      });
    }

    // listeners

    // show dialog
    Dialogs.show(dialog,Settings.geometryPreferences);

    // run dialog
    if ((Boolean)Dialogs.run(dialog,false))
    {
      boolean saveSettings = true;
      if (Settings.isFileModified())
      {
        saveSettings = Dialogs.confirm(shell,
                                       "Confirmation",
                                       "Settings were modified externally.\nOverwrite settings?",
                                       "Overwrite",
                                       "Cancel",
                                       false
                                      );
      }
      if (saveSettings)
      {
        Settings.save();
      }

      synchronized(Settings.showRestartAfterConfigChanged)
      {
        if (Settings.showRestartAfterConfigChanged)
        {
// NYI ??? return showRestartAfterConfigChanged flag?
          if (Dialogs.confirm(shell,
                              "Confirmation",
                              "Some settings may become active only after restarting Timemine.\nRestart now?",
                              "Now",
                              "Later",
                              true
                             )
             )
          {
            Widgets.notify(shell,USER_EVENT_QUIT,EXITCODE_RESTART);
          }
        }
      }
    }
  }

  /** add colors to colors list
   */
  private void addColors(Table widgetColors)
  {
    try
    {
      // instantiate config adapter class
      Constructor         constructor         = Settings.SettingValueAdapterColor.class.getDeclaredConstructor(Settings.class);
      SettingValueAdapter settingValueAdapter = (SettingValueAdapter)constructor.newInstance(new Settings());

      // get setting classes
      Class[] settingClasses = Settings.getSettingClasses();
      for (Class clazz : settingClasses)
      {
        for (Field field : clazz.getDeclaredFields())
        {
          for (Annotation annotation : field.getDeclaredAnnotations())
          {
            if      (annotation instanceof SettingValue)
            {
              SettingValue configValue = (SettingValue)annotation;
              if (Settings.SettingValueAdapterColor.class.isAssignableFrom(configValue.type()))
              {
                // get color
                Settings.Color color = (Settings.Color)field.get(null);

                // get name
                String name = (!configValue.name().isEmpty()) ? configValue.name() : field.getName();

                // add entry
                color = color.clone();
                Widgets.addTableEntry(widgetColors,color,name.substring(5));   // Note: remove prefix "Color"
                if (color.foreground != null) Widgets.setTableEntryColor(widgetColors,color,1,new Color(null,color.foreground));
                if (color.background != null) Widgets.setTableEntryColor(widgetColors,color,2,new Color(null,color.background));
              }
            }
          }
        }
      }
    }
    catch (Exception exception)
    {
      // cannot happen
      Timemine.printInternalError(exception);
    }
  }

  /** save colors
   * @param widgetColors color widget
   * @return
   */
  private void saveColors(Table widgetColors)
  {
    try
    {
      // instantiate config adapter class
      Constructor         constructor         = Settings.SettingValueAdapterColor.class.getDeclaredConstructor(Settings.class);
      SettingValueAdapter settingValueAdapter = (SettingValueAdapter)constructor.newInstance(new Settings());

      // get setting classes
      Class[] settingClasses = Settings.getSettingClasses();
      for (Class clazz : settingClasses)
      {
        for (Field field : clazz.getDeclaredFields())
        {
          for (Annotation annotation : field.getDeclaredAnnotations())
          {
            if      (annotation instanceof SettingValue)
            {
              SettingValue configValue = (SettingValue)annotation;
              if (Settings.SettingValueAdapterColor.class.isAssignableFrom(configValue.type()))
              {
                // get color
                Settings.Color color = (Settings.Color)field.get(null);

                // get name
                String name = (!configValue.name().isEmpty()) ? configValue.name() : field.getName();

                // set color
                name = name.substring(5);
                boolean found = false;
                for (TableItem tableItem : widgetColors.getItems())
                {
                  if (name.equals(tableItem.getText(0)))
                  {
                    field.set(null,tableItem.getData());
                    found = true;
                    break;
                  }
                }
                if (!found)
                {
                  Timemine.printInternalError("Color %s not found in table!",name);
                }
              }
            }
          }
        }
      }
    }
    catch (Exception exception)
    {
      // cannot happen
      Timemine.printInternalError(exception);
    }
  }

  /** edit color
   * @param name name
   * @param color color
   * @return true if edit OK, false on cancel
   */
  private boolean editColor(String name, final Settings.Color color)
  {
    Composite composite,subComposite;
    Label     label;
    Text      text;
    Button    button;

    // add editor dialog
    final Shell dialog = Dialogs.openModal(shell,"Edit color",100,SWT.DEFAULT,new double[]{1.0,0.0},1.0);

    final Canvas widgetColorForeground;
    final Canvas widgetColorBackground;
    final Label  widgetValueForeground;
    final Label  widgetValueBackground;

    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(null,new double[]{0.0,1.0},4));
    Widgets.layout(composite,0,0,TableLayoutData.WE,0,0,4);
    {
      label = Widgets.newLabel(composite,"Name:");
      Widgets.layout(label,0,0,TableLayoutData.W);
      text = Widgets.newStringView(composite);
      text.setText(name);
      Widgets.layout(text,0,1,TableLayoutData.WE);

      label = Widgets.newLabel(composite,"Foreground:");
      Widgets.layout(label,1,0,TableLayoutData.W);
      subComposite = Widgets.newComposite(composite);
      subComposite.setLayout(new TableLayout(null,new double[]{1.0,0.0}));
      Widgets.layout(subComposite,1,1,TableLayoutData.WE);
      {
        widgetColorForeground = Widgets.newCanvas(subComposite,SWT.BORDER);
        widgetColorForeground.setForeground((color.foreground != null) ? new Color(null,color.foreground) : null);
        widgetColorForeground.setBackground((color.foreground != null) ? new Color(null,color.foreground) : null);
        Widgets.layout(widgetColorForeground,0,0,TableLayoutData.WE,0,0,0,0,60,20);
        String colorName = (color.foreground != null)
                             ? String.format("#%02x%02x%02x",color.foreground.red,color.foreground.green,color.foreground.blue)
                             : "";
        widgetValueForeground = Widgets.newLabel(subComposite,colorName);
        Widgets.layout(widgetValueForeground,0,1,TableLayoutData.W,0,0,0,0,60,SWT.DEFAULT);
        widgetColorForeground.addMouseListener(new MouseListener()
        {
          public void mouseDoubleClick(MouseEvent mouseEvent)
          {
          }
          public void mouseDown(MouseEvent mouseEvent)
          {
          }
          public void mouseUp(MouseEvent mouseEvent)
          {
            ColorDialog colorDialog = new ColorDialog(dialog);
            colorDialog.setRGB(color.foreground);
            RGB rgb = colorDialog.open();
            if (rgb != null)
            {
              color.foreground = rgb;
              widgetColorForeground.setForeground(new Color(null,rgb));
              widgetColorForeground.setBackground(new Color(null,rgb));
              widgetValueForeground.setText(String.format("#%02x%02x%02x",color.foreground.red,color.foreground.green,color.foreground.blue));
            }
          }
        });
      }

      label = Widgets.newLabel(composite,"Background:");
      Widgets.layout(label,2,0,TableLayoutData.W);
      subComposite = Widgets.newComposite(composite);
      subComposite.setLayout(new TableLayout(null,new double[]{1.0,0.0}));
      Widgets.layout(subComposite,2,1,TableLayoutData.WE);
      {
        widgetColorBackground = Widgets.newCanvas(subComposite,SWT.BORDER);
        widgetColorBackground.setForeground((color.background != null) ? new Color(null,color.background) : null);
        widgetColorBackground.setBackground((color.background != null) ? new Color(null,color.background) : null);
        Widgets.layout(widgetColorBackground,0,0,TableLayoutData.WE,0,0,0,0,60,20);
        String colorName = (color.background != null)
                             ? String.format("#%02x%02x%02x",color.background.red,color.background.green,color.background.blue)
                             : "";
        widgetValueBackground = Widgets.newLabel(subComposite,colorName);
        Widgets.layout(widgetValueBackground,0,1,TableLayoutData.W,0,0,0,0,60,SWT.DEFAULT);
        widgetColorBackground.addMouseListener(new MouseListener()
        {
          public void mouseDoubleClick(MouseEvent mouseEvent)
          {
          }
          public void mouseDown(MouseEvent mouseEvent)
          {
          }
          public void mouseUp(MouseEvent mouseEvent)
          {
            ColorDialog colorDialog = new ColorDialog(dialog);
            colorDialog.setRGB(color.background);
            RGB rgb = colorDialog.open();
            if (rgb != null)
            {
              color.background = rgb;
              widgetColorBackground.setForeground(new Color(null,rgb));
              widgetColorBackground.setBackground(new Color(null,rgb));
              widgetValueBackground.setText(String.format("#%02x%02x%02x",color.background.red,color.background.green,color.background.blue));
            }
          }
        });
      }
    }

    // buttons
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(0.0,1.0));
    Widgets.layout(composite,1,0,TableLayoutData.WE,0,0,4);
    {
      button = Widgets.newButton(composite,"Save");
      Widgets.layout(button,0,0,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      button.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Dialogs.close(dialog,true);
        }
      });

      button = Widgets.newButton(composite,"Default");
      Widgets.layout(button,0,1,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      button.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          color.foreground = color.DEFAULT_FOREGROUND;
          color.background = color.DEFAULT_BACKGROUND;

          widgetColorForeground.setForeground((color.foreground != null) ? new Color(null,color.foreground) : null);
          widgetColorForeground.setBackground((color.foreground != null) ? new Color(null,color.foreground) : null);
          widgetColorBackground.setForeground((color.background != null) ? new Color(null,color.background) : null);
          widgetColorBackground.setBackground((color.background != null) ? new Color(null,color.background) : null);

          String foregroundColorName = (color.foreground != null) ? String.format("#%02x%02x%02x",color.foreground.red,color.foreground.green,color.foreground.blue) : "";
          String backgroundColorName = (color.background != null) ? String.format("#%02x%02x%02x",color.background.red,color.background.green,color.background.blue) : "";
          widgetValueForeground.setText(foregroundColorName);
          widgetValueBackground.setText(backgroundColorName);
        }
      });

      button = Widgets.newButton(composite,"Cancel");
      Widgets.layout(button,0,2,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      button.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Dialogs.close(dialog,false);
        }
      });
    }

    // add listeners

    return (Boolean)Dialogs.run(dialog,false);
  }
}

/* end of file */
