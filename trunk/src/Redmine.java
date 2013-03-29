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
import java.lang.ref.SoftReference;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.UnknownHostException;
import java.net.URL;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

// XML
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


// Apache commons
import org.apache.commons.codec.binary.Base64;

/****************************** Classes ********************************/

/** Redmine exception
 */
class RedmineException extends Exception
{
  /**
   * @param
   * @return
   */
  RedmineException(String message, Exception cause)
  {
    super(message,cause);
  }

  /**
   * @param
   * @return
   */
  RedmineException(String message, Object... arguments)
  {
    this(String.format(message,arguments),(Exception)null);
  }

  /**
   * @param
   * @return
   */
  RedmineException(Exception cause)
  {
    this((String)null,cause);
  }
}

/** Redmine client functions
 */
public class Redmine
{
  /** base entity
   */
  class Entity
  {
    public int id;

    Entity(int id)
    {
      this.id = id;
    }
  }

  /** user
   */
  class User extends Entity
  {
    public final String firstName;
    public final String lastName;
    public final String login;
    public final String mail;
    public final Date   createdOn;
    public final Date   lastLoginOn;

    User(int id, String firstName, String lastName, String login, String mail, Date createdOn, Date lastLoginOn)
    {
      super(id);

      this.firstName   = firstName;
      this.lastName    = lastName;
      this.login       = login;
      this.mail        = mail;
      this.createdOn   = createdOn;
      this.lastLoginOn = lastLoginOn;
    }

    /** convert to string
     * @return string
     */
    public String toString()
    {
      return "User { "+id+", "+firstName+", "+lastName+", "+mail+", "+login+" }";
    }
  }

  /** tracker
   */
  class Tracker extends Entity
  {
    public final String name;

    Tracker(int id, String name)
    {
      super(id);

      this.name = name;
    }

    /** convert to string
     * @return string
     */
    public String toString()
    {
      return "Tracker { "+id+", "+name+" }";
    }
  }

  /** status
   */
  class Status extends Entity
  {
    public final String name;

    Status(int id, String name)
    {
      super(id);

      this.name = name;
    }

    /** convert to string
     * @return string
     */
    public String toString()
    {
      return "Status { "+id+", "+name+" }";
    }
  }

  /** priority
   */
  class Priority extends Entity
  {
    public final String  name;
    public final boolean isDefault;

    Priority(int id, String name, boolean isDefault)
    {
      super(id);

      this.name      = name;
      this.isDefault = isDefault;
    }

    /** convert to string
     * @return string
     */
    public String toString()
    {
      return "Priorities { "+id+", "+name+" }";
    }
  }

  /** activity
   */
  class Activity extends Entity
  {
    public final String  name;
    public final boolean isDefault;

    Activity(int id, String name, boolean isDefault)
    {
      super(id);

      this.name      = name;
      this.isDefault = isDefault;
    }

    /** convert to string
     * @return string
     */
    public String toString()
    {
      return "Activity { "+id+", "+name+" }";
    }
  }

  /** project
   */
  class Project extends Entity
  {
    public final String name;

    Project(int id, String name)
    {
      super(id);
      this.name = name;
    }

    /** convert to string
     * @return string
     */
    public String toString()
    {
      return "Project { "+id+", "+name+" }";
    }
  }

  /** issue
   */
  class Issue extends Entity
  {
    public int        projectId;
    public int        trackerId;
    public int        statusId;
    public int        priorityId;
    public int        authorId;
    public String     subject;
    public String     description;
    public Date       startDate;
    public Date       dueDate;
    public int        doneRatio;
    public double     estimatedHours;
    public final Date createdOn;
    public Date       updateOn;
    public Date       closedOn;

    Issue(int    id,
          int    projectId,
          int    trackerId,
          int    statusId,
          int    priorityId,
          int    authorId,
          String subject,
          String description,
          Date   startDate,
          Date   dueDate,
          int    doneRatio,
          double estimatedHours,
          Date   createdOn,
          Date   updateOn,
          Date   closedOn
         )
    {
      super(id);
      this.projectId      = projectId;
      this.trackerId      = trackerId;
      this.statusId       = statusId;
      this.priorityId     = priorityId;
      this.authorId       = authorId;
      this.subject        = subject;
      this.description    = description;
      this.startDate      = startDate;
      this.dueDate        = dueDate;
      this.doneRatio      = doneRatio;
      this.estimatedHours = estimatedHours;
      this.createdOn      = createdOn;
      this.updateOn       = updateOn;
      this.closedOn       = closedOn;
    }

    /** convert to string
     * @return string
     */
    public String toString()
    {
      return "Issue { "+id+", "+projectId+", "+trackerId+", "+statusId+", "+subject+", "+description+" }";
    }
  }

  /** time entry
   */
  class TimeEntry extends Entity
  {
    public int        projectId;
    public int        issueId;
    public int        userId;
    public int        activityId;
    public double     hours;
    public String     comments;
    public Date       spentOn;
    public final Date createdOn;
    public Date       updateOn;

    /** create time entry
     * @param id time entry id
     * @param projectId project id
     * @param issueId issue id
     * @param userId user id
     * @param activityId activity id
     * @param hourse spent hours
     * @param comments comments
     * @param spentOn spent-on date
     * @param createdOn created-on date
     * @param updateOn update-on date
     */
    TimeEntry(int    id,
              int    projectId,
              int    issueId,
              int    userId,
              int    activityId,
              double hours,
              String comments,
              Date   spentOn,
              Date   createdOn,
              Date   updateOn
             )
    {
      super(id);
      this.projectId  = projectId;
      this.issueId    = issueId;
      this.userId     = userId;
      this.activityId = activityId;
      this.hours      = hours;
      this.comments   = comments;
      this.spentOn    = spentOn;
      this.createdOn  = createdOn;
      this.updateOn   = updateOn;
    }

    /** create time entry
     * @param projectId project id
     * @param issueId issue id
     * @param activityId activity id
     * @param hourse spent hours
     * @param comments comments
     * @param spentOn spent-on date
     */
    TimeEntry(int    projectId,
              int    issueId,
              int    activityId,
              double hours,
              String comments,
              Date   spentOn
             )
    {
      this(ID_NONE,
           projectId,
           issueId,
           ownUserId,
           activityId,
           hours,
           comments,
           spentOn,
           new Date(),
           new Date()
          );
    }

    /** create time entry
     * @param projectId project id
     * @param issueId issue id
     * @param activityId activity id
     * @param hourse spent hours
     * @param comments comments
     */
    TimeEntry(int    projectId,
              int    issueId,
              int    activityId,
              double hours,
              String comments
             )
    {
      this(projectId,
           issueId,
           activityId,
           hours,
           comments,
           new Date()
          );
    }

    /** get spent hours fraction
     * @return hours fraction
     */
    public int getHourFraction()
    {
      return (int)Math.floor(hours);
    }

    /** get spent minutes fraction
     * @return minutes fraction
     */
    public int getMinuteFraction()
    {
      return (((int)(hours*100.0)%100)*60)/100;
    }

    /** set spent time
     * @param hours spent hours
     * @param minutes spent minutes
     */
    public void setHours(int hours, int minutes)
    {
      this.hours = toHours(hours,minutes);
    }

    /** convert to string
     * @return string
     */
    public String toString()
    {
      return "TimeEntry { "+id+", "+projectId+", "+issueId+", "+activityId+", "+hours+", "+spentOn+", "+comments+" }";
    }
  }

  /** parse element handler
   */
  private abstract class ParseElementHandler<T>
  {
    // --------------------------- constants --------------------------------

    // --------------------------- variables --------------------------------
    private int     index;
    private Object  result;
    private boolean done;

    // ------------------------ native functions ----------------------------

    // ---------------------------- methods ---------------------------------

    /** create element handler
     */
    ParseElementHandler()
    {
      this.index = 0;
      this.done  = false;
    }

    /** process root element handler
     * @param element header element
     */
    public void root(Element element)
    {
    }

    /** process element handler
     * @param element data element
     */
    abstract public void data(Element element);

    /** store entity handler
     * @param index index [0..n-1]
     * @param entity entity to store
     */
    public void store(int index, T entity)
    {
    }

    /** done iteration
     * @param result result value
     */
    public void done(Object result)
    {
      this.result = result;
      this.done   = true;
    }

    /** done iteration
     */
    public void done()
    {
      done(null);
    }

    protected void setIndex(int index)
    {
      this.index = index;
    }

    /** store entity
     * @param entity entity to store
     */
    protected void store(T entity)
    {
      store(index,entity);
    }

    // ----------------------------------------------------------------------

    /** check if done
     * @return true iff done
     */
    private boolean isDone()
    {
      return done;
    }

    /** get result
     * @return result or null
     */
    private Object getResult()
    {
      return this.result;
    }
  }

  /** parse element handler
   */
  private abstract class CreateHandler
  {
    // --------------------------- constants --------------------------------

    // --------------------------- variables --------------------------------

    // ------------------------ native functions ----------------------------

    // ---------------------------- methods ---------------------------------

    /** create XML document handler
     */
    CreateHandler()
    {
    }

    /** create XML data elements handler
     * @param document XML document
     * @param rootElement root element
     */
    abstract public void data(Document document, Element rootElement);

    // ----------------------------------------------------------------------
  }

  // --------------------------- constants --------------------------------
  public  final static int ID_ANY  = -1;
  private final static int ID_NONE =  0;

  private final static int              ENTRY_LIMIT     = 100; // max. 100
  private final static SimpleDateFormat DATE_FORMAT     = new SimpleDateFormat("yyyy-MM-dd");
  private final static SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

  private final SoftReference<TimeEntry> TIME_ENTRY_NULL = new SoftReference<TimeEntry>(null);

  // --------------------------- variables --------------------------------

  private String                              server;
  private int                                 port;
  private String                              authorization;

  private int                                 ownUserId;

  private SoftHashMap<Integer,User>           userMap                          = new SoftHashMap<Integer,User>();
  private SoftHashMap<Integer,Tracker>        trackerMap                       = new SoftHashMap<Integer,Tracker>();
  private SoftHashMap<Integer,Status>         statusMap                        = new SoftHashMap<Integer,Status>();
  private SoftHashMap<Integer,Priority>       priorityMap                      = new SoftHashMap<Integer,Priority>();
  private SoftHashMap<Integer,Activity>       activityMap                      = new SoftHashMap<Integer,Activity>();
  private SoftHashMap<Integer,Project>        projectMap                       = new SoftHashMap<Integer,Project>();
  private int                                 projectsTotal                    = 0;
  private SoftHashMap<Integer,Issue>          issueMap                         = new SoftHashMap<Integer,Issue>();
  private int                                 issuesTotal                      = 0;
  private HashMap<Integer,Integer>            timeEntryIdMap                   = new HashMap<Integer,Integer>();   // map id -> index
  private HashMap<Date,Integer>               timeEntryDateMap                 = new HashMap<Date,Integer>();   // map date -> index
  private ArrayList<SoftReference<TimeEntry>> timeEntries                      = new ArrayList<SoftReference<TimeEntry>>();
  private Date                                timeEntryStartDate;
  private long                                lastTimeEntryDataUpdateTimeStamp = 0L;

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** initialize Redmine client
   * @param server server name
   * @param port server port
   * @param loginName login name
   * @param loginPasssword login password
   */
  public Redmine(String server, int port, final String loginName, String loginPassword)
    throws RedmineException
  {
    this.server        = server;
    this.port          = port;
    this.authorization = "Basic "+new String(Base64.encodeBase64((loginName+":"+loginPassword).getBytes()));

    // get login user id
    this.ownUserId = (Integer)iterateData("/users/current","user",new ParseElementHandler<User>()
    {
      public void data(Element element)
      {
        String value = getValue(element,"login");

        if ((value != null) && value.equals(loginName))
        {
          done(getIntValue(element,"id"));
        }
      }
    });
  }

  /** get own user id
   * @return own user id
   */
  public int getOwnUserId()
  {
    return this.ownUserId;
  }

  /** get Redmine users
   * @return user hash map <id,user>
   */
  public synchronized SoftHashMap<Integer,User> getUsers()
    throws RedmineException
  {
    userMap.clear();
    getData("/users","user",new ParseElementHandler<User>()
    {
      public void data(Element element)
      {
        User user = new User(getIntValue(element,"id"),
                             getValue(element,"firstname"),
                             getValue(element,"lastname"),
                             getValue(element,"login"),
                             getValue(element,"mail"),
                             getDateValue(element,"created_on"),
                             getDateValue(element,"last_login_in")
                            );

        store(user);
      }
      public void store(int index, User user)
      {
        userMap.put(user.id,user);
      }
    });

    return userMap;
  }

  /** get user
   * @param userId user id
   * @return user
   */
  public User getUser(int userId)
    throws RedmineException
  {
    User user = userMap.get(userId);
    if (user == null)
    {
      getUsers();
      user = userMap.get(userId);
    }

    return user;
  }

  /** get Redmine trackers
   * @return tracker hash map <id,tracker>
   */
  public synchronized SoftHashMap<Integer,Tracker> getTrackers()
    throws RedmineException
  {
    trackerMap.clear();
    getData("/trackers","tracker",new ParseElementHandler<Tracker>()
    {
      public void data(Element element)
      {
        Tracker tracker = new Tracker(getIntValue(element,"id"),
                                      getValue(element,"name")
                                     );

        store(tracker);
      }
      public void store(int index, Tracker tracker)
      {
        trackerMap.put(tracker.id,tracker);
      }
    });

    return trackerMap;
  }

  /** get Redmine status
   * @return status hash map <id,status>
   */
  public SoftHashMap<Integer,Status> getStatus()
    throws RedmineException
  {
    statusMap.clear();
    getData("/issue_statuses","issue_status",new ParseElementHandler<Status>()
    {
      public void data(Element element)
      {
        Status status = new Status(getIntValue(element,"id"),
                                   getValue(element,"name")
                                  );

        store(status);
      }
      public void store(int index, Status status)
      {
        statusMap.put(status.id,status);
      }
    });

    return statusMap;
  }

  /** get Redmine priorities
   * @return priority hash map <id,priority>
   */
  public SoftHashMap<Integer,Priority> getPriorities()
    throws RedmineException
  {
    priorityMap.clear();
    getData("/enumerations/issue_priorities","issue_priority",new ParseElementHandler<Priority>()
    {
      public void data(Element element)
      {
        Priority priority = new Priority(getIntValue(element,"id"),
                                         getValue(element,"name"),
                                         getBooleanValue(element,"is_default")
                                        );

        store(priority);
      }
      public void store(int index, Priority priority)
      {
        priorityMap.put(priority.id,priority);
      }
    });

    return priorityMap;
  }

  /** get Redmine activities
   * @return activity hash map <id,activity>
   */
  public synchronized SoftHashMap<Integer,Activity> getActivities()
    throws RedmineException
  {
    activityMap.clear();
    getData("/enumerations/time_entry_activities","time_entry_activity",new ParseElementHandler<Activity>()
    {
      public void data(Element element)
      {
        Activity activity = new Activity(getIntValue(element,"id"),
                                         getValue(element,"name"),
                                         getBooleanValue(element,"is_default")
                                        );

        store(activity);
      }
      public void store(int index, Activity activity)
      {
        activityMap.put(activity.id,activity);
      }
    });

    return activityMap;
  }

  /** get Redmine activities as an array
   * @return project array
   */
  public Activity[] getActivityArray()
    throws RedmineException
  {
    getActivities();
    return activityMap.values().toArray(new Redmine.Activity[activityMap.size()]);
  }

  /** get Redmine activity
   * @param activityId activity id
   * @return activity or null
   */
  public Activity getActivity(int activityId)
  {
    try
    {
      Activity activity = activityMap.get(activityId);
      if (activity == null)
      {
        getActivities();
        activity = activityMap.get(activityId);
      }

      return activity;
    }
    catch (RedmineException exception)
    {
      return null;
    }
  }

  /** get Redmine projects
   * @param clearFlag true to clear map before refresh
   * @return project hash map <id,project>
   */
  public synchronized SoftHashMap<Integer,Project> getProjects(boolean clearFlag)
    throws RedmineException
  {
    if (clearFlag) projectMap.clear();
    getData("/projects","project",new ParseElementHandler<Project>()
    {
      public void root(Element element)
      {
        projectsTotal = getIntAttribute(element,"total_count");
      }
      public void data(Element element)
      {
        Project project = new Project(getIntValue(element,"id"),
                                      getValue(element,"name")
                                     );

        store(project);
      }
      public void store(int index, Project project)
      {
        projectMap.put(project.id,project);
      }
    });

    return projectMap;
  }

  /** get Redmine projects
   * @return project hash map <id,project>
   */
  public SoftHashMap<Integer,Project> getProjects()
    throws RedmineException
  {
    return getProjects(false);
  }

  /** get Redmine projects as an array
   * @param refreshFlag true to refresh data
   * @return project array
   */
  public Project[] getProjectArray(boolean refreshFlag)
    throws RedmineException
  {
    getProjects(refreshFlag);
    return projectMap.values().toArray(new Redmine.Project[projectMap.size()]);
  }

  /** get Redmine projects as an array
   * @return project array
   */
  public Project[] getProjectArray()
    throws RedmineException
  {
    return getProjectArray(false);
  }

  /** get Redmine project
   * @param projectId project id
   * @return project or null
   */
  public Project getProject(int projectId)
  {
    try
    {
      Project project = projectMap.get(projectId);
      if (project == null)
      {
        getProjects();
        project = projectMap.get(projectId);
      }

      return project;
    }
    catch (RedmineException exception)
    {
      return null;
    }
  }

  /** get Redmine issues
   * @param projectId project id or ID_ANY
   * @param clearFlag true to clear map before refresh
   * @return issue hash map <id,issue>
   */
  public synchronized SoftHashMap<Integer,Issue> getIssues(final int projectId, boolean clearFlag)
    throws RedmineException
  {
    if (clearFlag) issueMap.clear();
    getData("/issues","issue",new ParseElementHandler<Issue>()
    {
      public void root(Element element)
      {
        issuesTotal = getIntAttribute(element,"total_count");
      }
      public void data(Element element)
      {
        if (   (projectId == ID_ANY)
            || (getIntAttribute(element,"project","id") == projectId)
           )
        {
          Issue issue = new Issue(getIntValue(element,"id"),
                                  getIntAttribute(element,"project","id"),
                                  getIntAttribute(element,"tracker","id"),
                                  getIntAttribute(element,"status","id"),
                                  getIntAttribute(element,"priority","id"),
                                  getIntAttribute(element,"author","id"),
                                  getValue(element,"subject" ),
                                  getValue(element,"description"),
                                  getDateValue(element,"start_date"),
                                  getDateValue(element,"due_date"),
                                  getIntValue(element,"done_ratio"),
                                  getDoubleValue(element,"estimated_hours" ),
                                  getDateValue(element,"created_on"),
                                  getDateValue(element,"updated_on"),
                                  getDateValue(element,"closed_on")
                                 );
          store(issue);
        }
      }
      public void store(int index, Issue issue)
      {
        issueMap.put(issue.id,issue);
      }
    });

    return issueMap;
  }

  /** get Redmine issues
   * @param projectId project id
   * @return issue hash map <id,issue>
   */
  public SoftHashMap<Integer,Issue> getIssues(int projectId)
    throws RedmineException
  {
    return getIssues(projectId,false);
  }

  /** get Redmine issues as an array
   * @param projectId project id
   * @param refreshFlag true to refresh data
   * @return issue array
   */
  public Issue[] getIssueArray(int projectId, boolean refreshFlag)
    throws RedmineException
  {
    getIssues(projectId,refreshFlag);

    ArrayList<Issue> issueList = new ArrayList<Issue>();
    for (Issue issue : issueMap.values())
    {
      if (issue.projectId == projectId) issueList.add(issue);
    }

    return issueList.toArray(new Redmine.Issue[issueList.size()]);
  }

  /** get Redmine issues as an array
   * @param projectId project id
   * @return issue array
   */
  public Issue[] getIssueArray(int projectId)
    throws RedmineException
  {
    return getIssueArray(projectId,false);
  }

  /** get Redmine issue
   * @param issueId issue id
   * @return issue or null
   */
  public Issue getIssue(int issueId)
  {
    try
    {
      Issue issue = issueMap.get(issueId);
      if (issue == null)
      {
        getIssues(ID_ANY,false);
        issue = issueMap.get(issueId);
      }

      return issue;
    }
    catch (RedmineException exception)
    {
      return null;
    }
  }

  /** get Redmine time entires
   * @param projectId project id or ID_ANY
   * @param issueId issue id or ID_ANY
   * @param userId user id or ID_ANY
   * @param spentOn spent-on date or null
   * @param refreshFlag true to refresh data
   * @return issue hash map <id,time entry>
   */
  public synchronized SoftHashMap<Integer,TimeEntry> getTimeEntries(final int projectId, final int issueId, final int userId, final Date spentOn, boolean refreshFlag)
    throws RedmineException
  {
    SoftHashMap<Integer,TimeEntry> timeEntryMap = new SoftHashMap<Integer,TimeEntry>();

    // clear data if refresh is requested
    if (refreshFlag)
    {
      for (int i = 0; i < timeEntries.size(); i++)
      {
        timeEntries.set(i,TIME_ENTRY_NULL);
      }
      lastTimeEntryDataUpdateTimeStamp = 0L;
    }

    // update number of time entries, start date
    updateTimeEntryData();

    // get data
    for (int i = 0; i < timeEntries.size(); i++)
    {
      // get soft-reference for entry
      TimeEntry timeEntry = timeEntries.get(i).get();
      if (timeEntry == null)
      {
//Dprintf.dprintf("fill %d length %d",i,ENTRY_LIMIT);
        // fill time entry array (get as much data a possible with single request)
        fillTimeEntry(i,ENTRY_LIMIT);

        // get soft-reference for entry
        timeEntry = timeEntries.get(i).get();
      }

      // get time entry
      if (timeEntry != null)
      {
        if (matchTimeEntry(timeEntry,projectId,issueId,userId,spentOn))
        {
//Dprintf.dprintf("calendar0=%s calendar1=%s",calendar0,calendar1);
          timeEntryMap.put(timeEntry.id,timeEntry);
        }
      }
    }

    return timeEntryMap;
  }

  /** get Redmine time entries
   * @param projectId project id or ID_ANY
   * @param issueId issue id or ID_ANY
   * @param userId user id or ID_ANY
   * @param spentOn spent-on date or null
   * @return time entry hash map <id,time entry>
   */
  public SoftHashMap<Integer,TimeEntry> getTimeEntries(int projectId, int issueId, int userId, Date spentOn)
    throws RedmineException
  {
    return getTimeEntries(projectId,issueId,userId,spentOn,false);
  }

  /** get Redmine time entires
   * @param projectId project id or ID_ANY
   * @param issueId issue id or ID_ANY
   * @param userId user id or ID_ANY
   * @return issue hash map <id,time entry>
   */
  public SoftHashMap<Integer,TimeEntry> getTimeEntries(int projectId, int issueId, int userId)
    throws RedmineException
  {
    return getTimeEntries(projectId,issueId,userId,null);
  }

  /** get total number of own time entries
   * @param projectId project id or ID_ANY
   * @param issueId issue id or ID_ANY
   * @param userId user id or ID_ANY
   * @param spentOn spent-on date or null
   * @return total number of time entries
   */
  public synchronized int getTimeEntryCount(int projectId, int issueId, int userId, Date spentOn)
    throws RedmineException
  {
    int n = 0;

    // update number of time entries, start date
    updateTimeEntryData();

    // get data
    for (int i = 0; i < timeEntries.size(); i++)
    {
      // get soft-reference for entry
      SoftReference<TimeEntry> softReference = timeEntries.get(i);
      if (softReference == null)
      {
        // fill time entry array (get as much data a possible with single request)
        fillTimeEntry(i,ENTRY_LIMIT);

        // get soft-reference for entry
        softReference = timeEntries.get(i);
      }

      // get time entry
      if (softReference != null)
      {
        TimeEntry timeEntry = softReference.get();

        if (matchTimeEntry(timeEntry,projectId,issueId,userId,spentOn))
        {
          n++;
        }
      }
    }

    return n;
  }

  /** get total number of time entries
   * @param projectId project id or ID_ANY
   * @param issueId issue id or ID_ANY
   * @param userId user id or ID_ANY
   * @return total number of time entries
   */
  public int getTimeEntryCount(int projectId, int issueId, int userId)
    throws RedmineException
  {
    return getTimeEntryCount(projectId,issueId,userId,null);
  }

  /** get total number of own time entries
   * @param projectId project id or ID_ANY
   * @param issueId issue id or ID_ANY
   * @return total number of time entries
   */
  public int getTimeEntryCount(int projectId, int issueId)
    throws RedmineException
  {
    return getTimeEntryCount(projectId,issueId,ownUserId);
  }

  /** get total number of own time entries
   * @return total number of time entries
   */
  public int getTimeEntryCount()
    throws RedmineException
  {
    return getTimeEntryCount(ID_ANY,ID_ANY);
  }

  /** get total number of own time entries
   * @return total number of time entries
   */
  public Date getTimeEntryStartDate()
    throws RedmineException
  {
    // update number of time entries, start date
    updateTimeEntryData();

    return timeEntryStartDate;
  }

  /** get hours sum of time entries
   * @param projectId project id or ID_ANY
   * @param issueId issue id or ID_ANY
   * @param userId user id or ID_ANY
   * @param spentOn spent-on date or null
   * @return hours sum of time entries
   */
  public synchronized double getTimeEntryHoursSum(int projectId, int issueId, int userId, Date spentOn)
    throws RedmineException
  {
    double hoursSum = 0.0;

    // update number of time entries, start date
    updateTimeEntryData();

    // get data
    for (int i = 0; i < timeEntries.size(); i++)
    {
      // get soft-reference for entry
      SoftReference<TimeEntry> softReference = timeEntries.get(i);
      if (softReference == null)
      {
        // fill time entry array (get as much data a possible with single request)
        fillTimeEntry(i,ENTRY_LIMIT);

        // get soft-reference for entry
        softReference = timeEntries.get(i);
      }

      // get time entry
      if (softReference != null)
      {
        TimeEntry timeEntry = softReference.get();

        if (matchTimeEntry(timeEntry,projectId,issueId,userId,spentOn))
        {
          hoursSum += timeEntry.hours;
        }
      }
    }

    return hoursSum;
  }

  /** get hours sum of own time entries for today
   * @param projectId project id or ID_ANY
   * @param issueId issue id or ID_ANY
   * @param userId user id or ID_ANY
   * @return hours sum of time entries
   */
  public double getTimeEntryHoursSum(int projectId, int issueId, int userId)
    throws RedmineException
  {
    return getTimeEntryHoursSum(projectId,issueId,userId,new Date());
  }

  /** get hours sum of own time entries
   * @param projectId project id or ID_ANY
   * @param issueId issue id or ID_ANY
   * @param spentOn spent-on date or null
   * @return hours sum of time entries
   */
  public double getTimeEntryHoursSum(int projectId, int issueId, Date spentOn)
    throws RedmineException
  {
    return getTimeEntryHoursSum(projectId,issueId,ownUserId,spentOn);
  }

  /** get hours sum of own time entries
   * @param projectId project id or ID_ANY
   * @param issueId issue id or ID_ANY
   * @return hours sum of time entries
   */
  public double getTimeEntryHoursSum(int projectId, int issueId)
    throws RedmineException
  {
    return getTimeEntryHoursSum(projectId,issueId,ownUserId);
  }

  /** get Redmine time entries as an array
   * @param projectId project id or ID_ANY
   * @param issueId issue id or ID_ANY
   * @param userId user id or ID_ANY
   * @param spentOn spent-on date or null
   * @param refreshFlag true to refresh data
   * @return time entry array
   */
  public synchronized TimeEntry[] getTimeEntryArray(int projectId, int issueId, int userId, Date spentOn, boolean refreshFlag)
    throws RedmineException
  {
    // fill specific time entries
    getTimeEntries(projectId,issueId,userId,spentOn,refreshFlag);

    // create array
    ArrayList<TimeEntry> filteredTimeEntries = new ArrayList<TimeEntry>();
    for (int i = 0; i < timeEntries.size(); i++)
    {
      SoftReference<TimeEntry> softReference = timeEntries.get(i);
      if (softReference != null)
      {
        TimeEntry timeEntry = softReference.get();

        Calendar calendar0 = null;
        Calendar calendar1 = null;
        if (spentOn != null)
        {
          calendar0 = Calendar.getInstance(); calendar0.setTime(spentOn);
          calendar1 = Calendar.getInstance(); calendar1.setTime(timeEntry.spentOn);
        }
        if (   (   (projectId == ID_ANY)
                || (timeEntry.projectId == projectId)
               )
            && (   (issueId == ID_ANY)
                || (timeEntry.issueId == issueId)
               )
            && (   (userId == ID_ANY)
                || (timeEntry.userId == userId)
               )
            && (   (spentOn == null)
                || (   (calendar0.get(Calendar.YEAR ) == calendar1.get(Calendar.YEAR ))
                    && (calendar0.get(Calendar.MONTH) == calendar1.get(Calendar.MONTH))
                    && (calendar0.get(Calendar.DATE ) == calendar1.get(Calendar.DATE ))
                   )
               )
           )
        {
//Dprintf.dprintf("calendar0=%s calendar1=%s",calendar0,calendar1);
          filteredTimeEntries.add(timeEntry);
        }
      }
    }

    return filteredTimeEntries.toArray(new TimeEntry[filteredTimeEntries.size()]);
  }

  /** get own Redmine time entries as an array
   * @param projectId project id or ID_ANY
   * @param issueId issue id or ID_ANY
   * @param spentOn spent-on date or null
   * @param refreshFlag true to refresh data
   * @return time entry array
   */
  public TimeEntry[] getTimeEntryArray(int projectId, int issueId, Date spentOn, boolean refreshFlag)
    throws RedmineException
  {
    return getTimeEntryArray(projectId,issueId,ownUserId,spentOn,refreshFlag);
  }

  /** get own Redmine time entries as an array
   * @param projectId project id or ID_ANY
   * @param issueId issue id or ID_ANY
   * @param spentOn spent-on date or null
   * @return time entry array
   */
  public TimeEntry[] getTimeEntryArray(int projectId, int issueId, Date spentOn)
    throws RedmineException
  {
    return getTimeEntryArray(projectId,issueId,ownUserId,spentOn,false);
  }

  /** get own Redmine time entries of today as an array
   * @param projectId project id or ID_ANY
   * @param issueId issue id or ID_ANY
   * @param refreshFlag true to refresh data
   * @return time entry array
   */
  public TimeEntry[] getTimeEntryArray(int projectId, int issueId, boolean refreshFlag)
    throws RedmineException
  {
    return getTimeEntryArray(projectId,issueId,ownUserId,new Date(),refreshFlag);
  }

  /** get own Redmine time entries of today as an array
   * @param projectId project id or ID_ANY
   * @param issueId issue id or ID_ANY
   * @return time entry array
   */
  public TimeEntry[] getTimeEntryArray(int projectId, int issueId)
    throws RedmineException
  {
    return getTimeEntryArray(projectId,issueId,false);
  }

  /** get Redmine time entry
   * @param index index of time entry [0..n-1]
   * @return time entry or null
   */
  public synchronized TimeEntry getTimeEntryAt(int index)
    throws RedmineException
  {
    SoftReference<TimeEntry> softReference = timeEntries.get(index);
    if (softReference == null)
    {
Dprintf.dprintf("todo: get at index");
      getTimeEntries(ID_ANY,ID_ANY,ID_ANY,null);

      softReference = timeEntries.get(index);
      if (softReference == null)
      {
Dprintf.dprintf("todo: get at index");
        getTimeEntries(ID_ANY,ID_ANY,ID_ANY,null);

        softReference = timeEntries.get(index);
      }
    }

    return (softReference != null) ? softReference.get() : null;
  }

  /** get Redmine time entry
   * @param timeEntryId id of time entry
   * @return time entry or null
   */
  public TimeEntry getTimeEntry(int timeEntryId)
    throws RedmineException
  {
    TimeEntry timeEntry = null;

    Integer index = timeEntryIdMap.get(timeEntryId);
    if (index == null)
    {
      getTimeEntries(ID_ANY,ID_ANY,ID_ANY,null);
      index = timeEntryIdMap.get(timeEntryId);
    }
Dprintf.dprintf("index=%s",index);

    if (index != null)
    {
      timeEntry = getTimeEntryAt(index);
    }

    return timeEntry;
  }

  /** get Redmine time entry at specific date (newest entry)
   * @param index index of time entry [0..n-1]
   * @return time entry or null
   */
  public TimeEntry getTimeEntry(Date date)
    throws RedmineException
  {
    TimeEntry timeEntry = null;
Dprintf.dprintf("required?");

    Integer index = timeEntryDateMap.get(date);
    if (index == null)
    {
      getTimeEntries(ID_ANY,ID_ANY,ID_ANY,null);
      index = timeEntryDateMap.get(date);
    }
Dprintf.dprintf("index=%s",index);

    if (index != null)
    {
      timeEntry = getTimeEntryAt(index);
    }

    return timeEntry;
  }

  /** add time entry
   * @param timeEntry time entry to add
   */
  public void add(final TimeEntry timeEntry)
    throws RedmineException
  {
    postData("/time_entries","time_entry",timeEntry,new CreateHandler()
    {
      public void data(Document document, Element rootElement)
      {
        Element element;

        element = document.createElement("issue_id");
        element.appendChild(document.createTextNode(Integer.toString(timeEntry.issueId)));
        rootElement.appendChild(element);

        element = document.createElement("spent_on");
        element.appendChild(document.createTextNode(DATE_FORMAT.format(timeEntry.spentOn)));
        rootElement.appendChild(element);

        element = document.createElement("hours");
        element.appendChild(document.createTextNode(Double.toString(timeEntry.hours)));
        rootElement.appendChild(element);

        element = document.createElement("activity_id");
        element.appendChild(document.createTextNode(Integer.toString(timeEntry.activityId)));
        rootElement.appendChild(element);

        element = document.createElement("comments");
        element.appendChild(document.createTextNode(timeEntry.comments));
        rootElement.appendChild(element);
      }
    });
  }

  /** update time entry
   * @param timeEntry time entry to update
   */
  public void update(final TimeEntry timeEntry)
    throws RedmineException
  {
    putData("/time_entries/"+timeEntry.id,"time_entry",timeEntry,new CreateHandler()
    {
      public void data(Document document, Element rootElement)
      {
        Element element;

        element = document.createElement("issue_id");
        element.appendChild(document.createTextNode(Integer.toString(timeEntry.issueId)));
        rootElement.appendChild(element);

        element = document.createElement("spent_on");
        element.appendChild(document.createTextNode(DATE_FORMAT.format(timeEntry.spentOn)));
        rootElement.appendChild(element);

        element = document.createElement("hours");
        element.appendChild(document.createTextNode(Double.toString(timeEntry.hours)));
        rootElement.appendChild(element);

        element = document.createElement("activity_id");
        element.appendChild(document.createTextNode(Integer.toString(timeEntry.activityId)));
        rootElement.appendChild(element);

        element = document.createElement("comments");
        element.appendChild(document.createTextNode(timeEntry.comments));
        rootElement.appendChild(element);
      }
    });
  }

  /** delete time entry
   * @param timeEntry time entry to delete
   */
  public void delete(TimeEntry timeEntry)
    throws RedmineException
  {
    deleteData("/time_entries/"+timeEntry.id);
  }

  /** convert house/minutes fraction into duration
   * @param hourFraction spent hours fraction
   * @param minuteFraction spent minutes fraction
   * @return hours
   */
  public static double toHours(int hourFraction, int minuteFraction)
  {
    return (double)hourFraction+(double)minuteFraction/60.0;
  }

  //-----------------------------------------------------------------------

  /** get data
   * @param urlString URL string
   * @param name XML entry name
   * @param offset start offset
   * @param length max. number of elements to get
   * @param parseElementHandler element handler
   */
  private void getData(String urlString, String name, int offset, int length, ParseElementHandler parseElementHandler)
    throws RedmineException
  {
    boolean done  = false;
    int     index = offset;

    while (!done)
    {
      HttpURLConnection connection = null;
      try
      {
        Document document;
        NodeList nodeList;
        Node     node;

        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder        documentBuilder        = documentBuilderFactory.newDocumentBuilder();

        // get data from Redmine server
        URL url = new URL("http://"+server+":"+port+urlString+".xml?offset="+offset+"&limit="+Math.min(length,ENTRY_LIMIT));
        connection = (HttpURLConnection)url.openConnection();
        connection.setRequestMethod("GET");
        connection.setDoOutput(false);
        connection.setRequestProperty("Authorization",authorization);
        if (Settings.debugFlag)
        {
          System.err.println("DEBUG: Get '"+url+"'");
        }

        // parse XML
        document = documentBuilder.parse(connection.getInputStream());
        document.getDocumentElement().normalize();

        // get number of total entries
        int totalCount = getIntAttribute(document.getDocumentElement(),"total_count");

        // process root element
        parseElementHandler.root((Element)document.getDocumentElement());

        // process data elements
        nodeList = document.getElementsByTagName(name);
        for (int i = 0; i < nodeList.getLength(); i++)
        {
          node = nodeList.item(i);

          if (node.getNodeType() == Node.ELEMENT_NODE)
          {
            parseElementHandler.setIndex(index);
            parseElementHandler.data((Element)node);
          }
          index++;
          length--;

          if (parseElementHandler.isDone()) break;
        }

        // check if done
        done =    parseElementHandler.isDone()
               || (length <= 0)
               || (index >= totalCount)
               || (offset >= totalCount);

        // close connectin
        connection.disconnect(); connection = null;

        // next segment
        offset += ENTRY_LIMIT;
      }
      catch (ParserConfigurationException exception)
      {
Dprintf.dprintf("");
        throw new RedmineException(exception);
      }
      catch (SAXException exception)
      {
Dprintf.dprintf("");
        throw new RedmineException(exception);
      }
      catch (ProtocolException exception)
      {
Dprintf.dprintf("");
        throw new RedmineException(exception);
      }
      catch (UnknownHostException exception)
      {
        throw new RedmineException("Unknown host '"+server+"'");
      }
      catch (IOException exception)
      {
        throw new RedmineException("Receive data fail",exception);
      }
      finally
      {
        if (connection != null) connection.disconnect();
      }
    }
  }

  /** get data
   * @param urlString URL string
   * @param name XML entry name
   * @param entityMap entity map for storage
   * @param parseElementHandler element handler
   */
  private void getData(String urlString, String name, ParseElementHandler parseElementHandler)
    throws RedmineException
  {
    getData(urlString,name,0,ENTRY_LIMIT,parseElementHandler);
  }

  /** iterate over data
   * @param urlString URL string
   * @param name XML entry name
   * @param parseElementHandler element handler
   * @return result
   */
  private Object iterateData(String urlString, String name, ParseElementHandler parseElementHandler)
    throws RedmineException
  {
    getData(urlString,name,parseElementHandler);

    return parseElementHandler.getResult();
  }

  /** get length of data
   * @param urlString URL string
   * @param name XML entry name
   * @return length of data
   */
  private int getDataLength(String urlString, String name)
    throws RedmineException
  {
    final int[] result = new int[1];

    getData(urlString,name,0,1,new ParseElementHandler()
    {
      public void root(Element element)
      {
        result[0] = getIntAttribute(element,"total_count");
      }
      public void data(Element element)
      {
      }
    });
    if (Settings.debugFlag)
    {
      System.err.println("DEBUG: Get length '"+urlString+"': "+result[0]);
    }

    return result[0];
  }

  /** add data
   * @param urlString URL string
   * @param name XML entry name
   * @param entityMap entity map for storage
   * @param CreateHandler document create handler
   * @param ParseElementHandler element handler
   */
  private void postData(String urlString, String name, Entity entity, CreateHandler createHandler)
    throws RedmineException
  {
    HttpURLConnection connection = null;
    try
    {
      Document document;

      DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder        documentBuilder        = documentBuilderFactory.newDocumentBuilder();

      // create XML
      document = documentBuilder.newDocument();
      document.setXmlStandalone(true);
      Element rootElement = document.createElement(name);
      document.appendChild(rootElement);
      createHandler.data(document,rootElement);

      // add data on Redmine server
      URL url = new URL("http://"+server+":"+port+urlString+".xml");
      connection = (HttpURLConnection)url.openConnection();
      connection.setRequestMethod("POST");
      connection.setDoOutput(true);
      connection.setRequestProperty("Content-Type","application/xml; charset=utf-8");
      connection.setRequestProperty("Authorization",authorization);
      if (Settings.debugFlag)
      {
        System.err.println("DEBUG: Post '"+url+"'");
      }

      // output data
      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      Transformer transformer = transformerFactory.newTransformer();
      DOMSource domSource = new DOMSource(document);
      if (Settings.debugFlag)
      {
        System.err.println("DEBUG: xml data ");
        StreamResult streamResult = new StreamResult(System.err);
        transformer.transform(domSource,streamResult);
      }
      StreamResult streamResult = new StreamResult(connection.getOutputStream());
      transformer.transform(domSource,streamResult);

      // check result
      if (connection.getResponseCode() != 201)
      {
        throw new RedmineException("server error return error "+connection.getResponseCode()+": "+connection.getResponseMessage());
      }

      // get entity id
      document = documentBuilder.parse(connection.getInputStream());
      document.getDocumentElement().normalize();

      NodeList nodeList = document.getElementsByTagName(name);
      for (int i = 0; i < nodeList.getLength(); i++)
      {
        Node node = nodeList.item(i);

        if (node.getNodeType() == Node.ELEMENT_NODE)
        {
          entity.id = getIntValue((Element)node,"id");
          break;
        }
      }
      if (entity.id == ID_NONE)
      {
        throw new RedmineException("no id for created entity returned");
      }

      // close connectin
      connection.disconnect(); connection = null;
    }
    catch (ParserConfigurationException exception)
    {
      new RedmineException(exception);
    }
    catch (SAXException exception)
    {
      new RedmineException(exception);
    }
    catch (TransformerException exception)
    {
      throw new RedmineException(exception);
    }
    catch (UnknownHostException exception)
    {
      throw new RedmineException("Unknown host '"+server+"'");
    }
    catch (IOException exception)
    {
      throw new RedmineException("Send data fail",exception);
    }
    finally
    {
      if (connection != null) connection.disconnect();
    }
  }

  /** update data
   * @param urlString URL string
   * @param name XML entry name
   * @param entityMap entity map for storage
   * @param CreateHandler document create handler
   * @param ParseElementHandler element handler
   */
  private void putData(String urlString, String name, Entity entity, CreateHandler createHandler)
    throws RedmineException
  {
    HttpURLConnection connection = null;
    try
    {
      Document document;

      DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder        documentBuilder        = documentBuilderFactory.newDocumentBuilder();

      // create XML
      document = documentBuilder.newDocument();
      document.setXmlStandalone(true);
      Element rootElement = document.createElement(name);
      document.appendChild(rootElement);
      createHandler.data(document,rootElement);

      // update data on Redmine server
      URL url = new URL("http://"+server+":"+port+urlString+".xml");
      connection = (HttpURLConnection)url.openConnection();
      connection.setRequestMethod("PUT");
      connection.setDoOutput(true);
      connection.setRequestProperty("Content-Type","application/xml; charset=utf-8");
      connection.setRequestProperty("Authorization",authorization);
      if (Settings.debugFlag)
      {
        System.err.println("DEBUG: Put '"+url+"'");
      }

      // output data
      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      Transformer transformer = transformerFactory.newTransformer();
      DOMSource domSource = new DOMSource(document);
      if (Settings.debugFlag)
      {
        System.err.println("DEBUG: xml data ");
        StreamResult streamResult = new StreamResult(System.err);
        transformer.transform(domSource,streamResult);
      }
      StreamResult streamResult = new StreamResult(connection.getOutputStream());
      transformer.transform(domSource,streamResult);

      // check result
      if (connection.getResponseCode() != 200)
      {
        throw new RedmineException("server error return error "+connection.getResponseCode()+": "+connection.getResponseMessage());
      }

      // close connectin
      connection.disconnect(); connection = null;
    }
    catch (ParserConfigurationException exception)
    {
      new RedmineException(exception);
    }
    catch (TransformerException exception)
    {
      throw new RedmineException(exception);
    }
    catch (UnknownHostException exception)
    {
      throw new RedmineException("Unknown host '"+server+"'");
    }
    catch (IOException exception)
    {
      throw new RedmineException("Send data fail",exception);
    }
    finally
    {
      if (connection != null) connection.disconnect();
    }
  }

  /** delete data
   * @param urlString URL string
   * @param name XML entry name
   */
  private void deleteData(String urlString)
    throws RedmineException
  {
    HttpURLConnection connection = null;
    try
    {
      Document document;

      DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder        documentBuilder        = documentBuilderFactory.newDocumentBuilder();

      // delete data from Redmine server
      URL url = new URL("http://"+server+":"+port+urlString+".xml");
      connection = (HttpURLConnection)url.openConnection();
      connection.setRequestMethod("DELETE");
      connection.setDoOutput(true);
      connection.setRequestProperty("Content-Type","application/xml; charset=utf-8");
      connection.setRequestProperty("Authorization",authorization);
      if (Settings.debugFlag)
      {
        System.err.println("DEBUG: Delete '"+url+"'");
      }

      // check result
      if (connection.getResponseCode() != 200)
      {
        throw new RedmineException("server error return error "+connection.getResponseCode()+": "+connection.getResponseMessage());
      }

      // close connectin
      connection.disconnect(); connection = null;
    }
    catch (ParserConfigurationException exception)
    {
      new RedmineException(exception);
    }
    catch (UnknownHostException exception)
    {
      throw new RedmineException("Unknown host '"+server+"'");
    }
    catch (IOException exception)
    {
      throw new RedmineException("Send data fail",exception);
    }
    finally
    {
      if (connection != null) connection.disconnect();
    }
  }

  /** get value from XML element
   * @param element XML element
   * @param tagName tag name
   * @return value
   */
  private String getValue(Element element, String tagName)
  {
    String value = "";

    NodeList nodeList = element.getElementsByTagName(tagName);
    if ((nodeList != null) && (nodeList.getLength() > 0))
    {
      NodeList subNodeList = nodeList.item(0).getChildNodes();
      if (subNodeList != null)
      {
        Node node = (Node)subNodeList.item(0);
        if (node != null)
        {
          value = node.getNodeValue();
        }
      }
    }

    return value;
  }

  /** get boolean value from XML element
   * @param element XML element
   * @param tagName tag name
   * @return boolean value
   */
  private boolean getBooleanValue(Element element, String tagName)
  {
    String value = getValue(element,tagName).trim();
    try
    {
      return Integer.parseInt(value) != 0;
    }
    catch (NumberFormatException exception)
    {
      return    value.equalsIgnoreCase("true")
             || value.equalsIgnoreCase("on")
             || value.equalsIgnoreCase("yes");
    }
  }

  /** get integer value from XML element
   * @param element XML element
   * @param tagName tag name
   * @return integer value
   */
  private int getIntValue(Element element, String tagName)
  {
    try
    {
      return Integer.parseInt(getValue(element,tagName));
    }
    catch (NumberFormatException exception)
    {
      return 0;
    }
  }

  /** get long value from XML element
   * @param element XML element
   * @param tagName tag name
   * @return long value
   */
  private long getLongValue(Element element, String tagName)
  {
    try
    {
      return Long.parseLong(getValue(element,tagName));
    }
    catch (NumberFormatException exception)
    {
      return 0L;
    }
  }

  /** get double from XML element
   * @param element XML element
   * @param tagName tag name
   * @return double value
   */
  private double getDoubleValue(Element element, String tagName)
  {
    try
    {
      return Double.parseDouble(getValue(element,tagName));
    }
    catch (NumberFormatException exception)
    {
      return 0.0;
    }
  }

  /** get date value from XML element
   * @param element XML element
   * @param tagName tag name
   * @return date value
   */
  private Date getDateValue(Element element, String tagName)
  {
    return parseDate(getValue(element,tagName));
  }

  /** get attribute value from XML element
   * @param element XML element
   * @param attributeName attribute name
   * @return value
   */
  private String getAttribute(Element element, String attributeName)
  {
    String value = "";

    Attr attribute = element.getAttributeNode(attributeName);
    if (attribute != null)
    {
      value = attribute.getValue();
    }

    return value;
  }

  /** get boolean attribute value from XML element
   * @param element XML element
   * @param attributeName attribute name
   * @return boolean value
   */
  private boolean getBooleanAttribute(Element element, String attributeName)
  {
    String value = getAttribute(element,attributeName).trim();
    try
    {
      return Integer.parseInt(value) != 0;
    }
    catch (NumberFormatException exception)
    {
      return    value.equalsIgnoreCase("true")
             || value.equalsIgnoreCase("on")
             || value.equalsIgnoreCase("yes");
    }
  }

  /** get integer attribute value from XML element
   * @param element XML element
   * @param attributeName attribute name
   * @return integer value
   */
  private int getIntAttribute(Element element, String attributeName)
  {
    try
    {
      return Integer.parseInt(getAttribute(element,attributeName));
    }
    catch (NumberFormatException exception)
    {
      return 0;
    }
  }

  /** get long attribute value from XML element
   * @param element XML element
   * @param attributeName tag name
   * @return long value
   */
  private long getLongAttribute(Element element, String attributeName)
  {
    try
    {
      return Long.parseLong(getAttribute(element,attributeName));
    }
    catch (NumberFormatException exception)
    {
      return 0L;
    }
  }

  /** get double attribute value from XML element
   * @param element XML element
   * @param attributeName attribute name
   * @return double value
   */
  private double getDoubleAttribute(Element element, String attributeName)
  {
    try
    {
      return Double.parseDouble(getAttribute(element,attributeName));
    }
    catch (NumberFormatException exception)
    {
      return 0.0;
    }
  }

  /** get date attribute value from XML element
   * @param element XML element
   * @param attributeName attribute name
   * @return date value
   */
  private Date getDateAttribute(Element element, String attributeName)
  {
    return parseDate(getAttribute(element,attributeName));
  }

  /** get attribute value from XML element
   * @param element XML element
   * @param tagName tag name
   * @param attributeName attribute name
   * @return value
   */
  private String getAttribute(Element element, String tagName, String attributeName)
  {
    String value = "";

    Node node = element.getElementsByTagName(tagName).item(0);
    if (node != null)
    {
      if (node.getNodeType() == Node.ELEMENT_NODE)
      {
        value = getAttribute((Element)node,attributeName);
      }
    }

    return value;
  }

  /** get boolean attribute value from XML element
   * @param element XML element
   * @param tagName tag name
   * @param attributeName attribute name
   * @return boolean value
   */
  private boolean getBooleanValue(Element element, String tagName, String attributeName)
  {
    String value = getAttribute(element,tagName,attributeName).trim();
    try
    {
      return Integer.parseInt(value) != 0;
    }
    catch (NumberFormatException exception)
    {
      return    value.equalsIgnoreCase("true")
             || value.equalsIgnoreCase("on")
             || value.equalsIgnoreCase("yes");
    }
  }

  /** get integer attribute value from XML element
   * @param element XML element
   * @param tagName tag name
   * @param attributeName attribute name
   * @return integer value
   */
  private int getIntAttribute(Element element, String tagName, String attributeName)
  {
    try
    {
      return Integer.parseInt(getAttribute(element,tagName,attributeName));
    }
    catch (NumberFormatException exception)
    {
      return 0;
    }
  }

  /** get long attribute value from XML element
   * @param element XML element
   * @param tagName tag name
   * @param attributeName attribute name
   * @return long value
   */
  private long getLongAttribute(Element element, String tagName, String attributeName)
  {
    try
    {
      return Long.parseLong(getAttribute(element,tagName,attributeName));
    }
    catch (NumberFormatException exception)
    {
      return 0L;
    }
  }

  /** get double attribute value from XML element
   * @param element XML element
   * @param tagName tag name
   * @param attributeName attribute name
   * @return double value
   */
  private double getDoubleAttribute(Element element, String tagName, String attributeName)
  {
    try
    {
      return Double.parseDouble(getAttribute(element,tagName,attributeName));
    }
    catch (NumberFormatException exception)
    {
      return 0.0;
    }
  }

  /** get date attribute value from XML element
   * @param element XML element
   * @param tagName tag name
   * @param attributeName attribute name
   * @return date value
   */
  private Date getDateAttribute(Element element, String tagName, String attributeName)
  {
    return parseDate(getAttribute(element,tagName,attributeName));
  }

  /** parse date
   * @param string date/time string
   * @return date/time or current date/time
   */
  private Date parseDate(String string)
  {
    final String[] FORMATS = new String[]
    {
      "yyyy-MM-dd HH:mm:ss Z",       // 2011-01-25 15:19:06 +0900
      "yyyy-MM-dd HH:mm Z",          // 2011-01-25 15:19 +0900
      "yyyy/MM/dd HH:mm:ss",         // 2010/05/13 07:09:36
      "EEE MMM dd HH:mm:ss yyyy Z",  // Wed Dec 17 15:41:19 2008 +0100
      "yyyy-MM-dd",                  // 2011-01-25
      "dd-MMM-yy",                   // 25-Jan-11
    };

    final Locale[] LOCALES = new Locale[]
    {
      Locale.US,
      Locale.GERMAN,
      Locale.GERMANY,

      Locale.CANADA,
      Locale.CANADA_FRENCH,
      Locale.CHINA,
      Locale.CHINESE,
      Locale.ENGLISH,
      Locale.FRANCE,
      Locale.FRENCH,
      Locale.ITALIAN,
      Locale.ITALY,
      Locale.JAPAN,
      Locale.JAPANESE,
      Locale.KOREA,
      Locale.KOREAN,
      Locale.PRC,
      Locale.ROOT,
      Locale.SIMPLIFIED_CHINESE,
      Locale.TAIWAN,
      Locale.TRADITIONAL_CHINESE,
      Locale.UK,
    };

    Date date;

    for (String format : FORMATS)
    {
      for (Locale locale : LOCALES)
      {
        try
        {
          date = new SimpleDateFormat(format,locale).parse(string);
          return date;
        }
        catch (ParseException exception)
        {
          // ignored, try next format
        }
      }
    }

    return new Date();
  }

  /** update time entry counter, start date
   */
  private void updateTimeEntryData()
    throws RedmineException
  {
    if (System.currentTimeMillis() > (lastTimeEntryDataUpdateTimeStamp+Settings.cacheExpireTime*1000))
    {
      // get total number of time entries
      int timeEntryCount = getDataLength("/time_entries","time_entry");
      timeEntries.ensureCapacity(timeEntryCount);
      for (int i = timeEntries.size(); i < timeEntryCount; i++)
      {
        timeEntries.add(TIME_ENTRY_NULL);
      }

      // get date of first time entry
      getData("/time_entries","time_entry",timeEntryCount-1,1,new ParseElementHandler<TimeEntry>()
      {
        public void data(Element element)
        {
          timeEntryStartDate = getDateValue(element,"spent_on");
//Dprintf.dprintf("timeEntryStartDate=%s",timeEntryStartDate);
        }
      });

      lastTimeEntryDataUpdateTimeStamp = System.currentTimeMillis();
    }
  }

  /** fill time entry array
   * @param offset start offset
   * @param length length to fill
   */
  private void fillTimeEntry(int offset, int length)
    throws RedmineException
  {
    getData("/time_entries","time_entry",offset,length,new ParseElementHandler<TimeEntry>()
    {
      public void data(Element element)
      {
//Dprintf.dprintf("element=%s projectId=%d issueId=%d userId=%d spentOn=%s",element,getIntAttribute(element,"project","id"),getIntAttribute(element,"issue","id"),getIntAttribute(element,"user","id"),getDateValue(element,"spent_on"),getDoubleValue(element,"hours"));
        TimeEntry timeEntry = new TimeEntry(getIntValue(element,"id"),
                                            getIntAttribute(element,"project","id"),
                                            getIntAttribute(element,"issue","id"),
                                            getIntAttribute(element,"user","id"),
                                            getIntAttribute(element,"activity","id"),
                                            getDoubleValue(element,"hours"),
                                            getValue(element,"comments"),
                                            getDateValue(element,"spent_on"),
                                            getDateValue(element,"created_on"),
                                            getDateValue(element,"updated_on")
                                           );
        store(timeEntry);
      }
      public void store(int index, TimeEntry timeEntry)
      {
//Dprintf.dprintf("index=%d id=%d: %s",index,timeEntry.id,timeEntry);
        timeEntryIdMap.put(timeEntry.id,index);
        if (!timeEntryDateMap.containsKey(timeEntry.spentOn)) timeEntryDateMap.put(timeEntry.spentOn,index);
        timeEntries.set(index,new SoftReference<TimeEntry>(timeEntry));
      }
    });
  }

  /** match time entry
   * @param timeEntry time entry
   * @param projectId project id or ID_ANY
   * @param issueId issue id or ID_ANY
   * @param userId user id or ID_ANY
   * @param spentOn spent-on date or null
   * @return true if time entry match, false otherwise
   */
  private boolean matchTimeEntry(TimeEntry timeEntry, int projectId, int issueId, int userId, Date spentOn)
  {
    Calendar calendar0 = null;
    Calendar calendar1 = null;
    if (spentOn != null)
    {
      calendar0 = Calendar.getInstance(); calendar0.setTime(spentOn);
      calendar1 = Calendar.getInstance(); calendar1.setTime(timeEntry.spentOn);
    }
    return    (   (projectId == ID_ANY)
               || (timeEntry.projectId == projectId)
              )
           && (   (issueId == ID_ANY)
               || (timeEntry.issueId == issueId)
              )
           && (   (userId == ID_ANY)
               || (timeEntry.userId == userId)
              )
           && (   (spentOn == null)
               || (   (calendar0.get(Calendar.YEAR ) == calendar1.get(Calendar.YEAR ))
                   && (calendar0.get(Calendar.MONTH) == calendar1.get(Calendar.MONTH))
                   && (calendar0.get(Calendar.DATE ) == calendar1.get(Calendar.DATE ))
                  )
              );
  }
}

/* end of file */
