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

// SSL
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

// XML
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.OutputKeys;
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
      return "Priority { "+id+", "+name+", "+isDefault+" }";
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
      return "Activity { "+id+", "+name+", "+isDefault+" }";
    }
  }

  /** project
   */
  class Project extends Entity
  {
    public String     name;
    public String     identifier;
    public String     description;
    public final Date createdOn;
    public Date       updateOn;

    /** create project
     * @param id time entry id
     * @param name name of project
     * @param identifier name of identifier
     * @param description name of description
     * @param createdOn created-on date
     * @param updateOn update-on date
     */
    Project(int    id,
            String name,
            String identifier,
            String description,
            Date   createdOn,
            Date   updateOn
           )
    {
      super(id);
      this.name        = name;
      this.identifier  = identifier;
      this.description = description;
      this.createdOn   = createdOn;
      this.updateOn    = updateOn;
    }

    /** create project
     * @param id time entry id
     * @param name name of project
     * @param identifier name of identifier
     * @param description name of description
     * @param createdOn created-on date
     * @param updateOn update-on date
     */
    Project(int    id,
            String name,
            String identifier,
            String description
           )
    {
      this(id,name,identifier,description,new Date(),new Date());
    }

    /** convert to string
     * @return string
     */
    public String toString()
    {
      return "Project { "+((id != ID_NONE) ? id : "none")+", "+name+" }";
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

    /** create time entry
     * @param id time entry id
     * @param projectId project id
     * @param issueId issue id
     * @param userId user id
     * @param activityId activity id
     * @param hourse spent hours
     * @param comments comments
     * @param createdOn created-on date
     * @param updateOn update-on date
     */
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
      return "Issue { "+((id != ID_NONE) ? id : "none")+", "+((projectId != ID_NONE) ? projectId : "none")+", "+((trackerId != ID_NONE) ? trackerId : "none")+", "+((statusId != ID_NONE) ? statusId : "none")+", "+subject+", "+description+" }";
    }
  }

  /** spent-on date
   */
  class SpentOn extends Object implements Cloneable
  {
    private Calendar calendar;

    /** create spent-on date
     * @param year year
     * @param month month
     * @param day day
     */
    SpentOn(int year, int month, int day)
    {
      this();
      calendar.set(Calendar.YEAR,        year );
      calendar.set(Calendar.MONTH,       month);
      calendar.set(Calendar.DAY_OF_MONTH,day  );
    }

    /** create spent-on date
     * @param date date
     */
    SpentOn(Date date)
    {
      this();
      Calendar fromCalendar = Calendar.getInstance(); fromCalendar.setTime(date);
      calendar.set(Calendar.YEAR,        fromCalendar.get(Calendar.YEAR        ));
      calendar.set(Calendar.MONTH,       fromCalendar.get(Calendar.MONTH       ));
      calendar.set(Calendar.DAY_OF_MONTH,fromCalendar.get(Calendar.DAY_OF_MONTH));
    }

    /** create spent-on date
     * @param dayOffset day offset from today
     */
    SpentOn(int dayOffset)
    {
      this();
      Calendar fromCalendar = Calendar.getInstance();
      calendar.add(Calendar.DAY_OF_MONTH,dayOffset);
    }

    /** create spent-on date
     */
    SpentOn()
    {
      this.calendar = Calendar.getInstance();
    }

    /** clone object
     * @return cloned object
     */
    public SpentOn clone()
    {
      return new SpentOn(calendar.getTime());
    }

    /** get year
     * @return year
     */
    int getYear()
    {
      return calendar.get(Calendar.YEAR);
    }

    /** get month
     * @return month
     */
    int getMonth()
    {
      return calendar.get(Calendar.MONTH);
    }

    /** get day
     * @return day
     */
    int getDay()
    {
      return calendar.get(Calendar.DAY_OF_MONTH);
    }

    /** get weekday
     * @return weekday
     */
    int getWeekday()
    {
      return calendar.get(Calendar.DAY_OF_WEEK);
    }

    /** get date
     * @return date
     */
    Date getDate()
    {
      return calendar.getTime();
    }

    /** check if objects are equal
     * @param object object
     * @return true iff equal
     */
    public boolean equals(Object object)
    {
      return hashCode() == object.hashCode();
    }

    /** check if spent-on date is today
     * @return true iff today
     */
    public boolean isToday()
    {
      Calendar todayCalendar = Calendar.getInstance();

      return    (calendar.get(Calendar.YEAR        ) == todayCalendar.get(Calendar.YEAR        ))
             && (calendar.get(Calendar.MONTH       ) == todayCalendar.get(Calendar.MONTH       ))
             && (calendar.get(Calendar.DAY_OF_MONTH) == todayCalendar.get(Calendar.DAY_OF_MONTH));
    }

    /** check if spent-in dates are equal
     * @param spentOn spent-on date
     * @return true iff equal
     */
    public boolean equals(SpentOn spentOn)
    {
      return    (calendar.get(Calendar.YEAR) == spentOn.calendar.get(Calendar.YEAR))
             && (calendar.get(Calendar.MONTH) == spentOn.calendar.get(Calendar.MONTH))
             && (calendar.get(Calendar.DAY_OF_MONTH) == spentOn.calendar.get(Calendar.DAY_OF_MONTH));
    }

    /** check if spent-on is before date
     * @param spentOn spent-on date
     * @return true iff before date
     */
    public boolean isBefore(SpentOn spentOn)
    {
      return    (   (calendar.get(Calendar.YEAR) < spentOn.calendar.get(Calendar.YEAR))
                )
             || (   (calendar.get(Calendar.YEAR) == spentOn.calendar.get(Calendar.YEAR))
                 && (calendar.get(Calendar.MONTH) < spentOn.calendar.get(Calendar.MONTH))
                )
             || (   (calendar.get(Calendar.YEAR) == spentOn.calendar.get(Calendar.YEAR))
                 && (calendar.get(Calendar.MONTH) == spentOn.calendar.get(Calendar.MONTH))
                 && (calendar.get(Calendar.DAY_OF_MONTH) < spentOn.calendar.get(Calendar.DAY_OF_MONTH))
                );
    }

    /** check if spent-on is before date
     * @param spentOn spent-on date
     * @return true iff before date
     */
    public boolean isBefore(Date date)
    {
      return isBefore(new SpentOn(date));
    }

    /** check if spent-on is after date
     * @param spentOn spent-on date
     * @return true iff after date
     */
    public boolean isAfter(SpentOn spentOn)
    {
      return    (   (calendar.get(Calendar.YEAR) > spentOn.calendar.get(Calendar.YEAR))
                )
             || (   (calendar.get(Calendar.YEAR) == spentOn.calendar.get(Calendar.YEAR))
                 && (calendar.get(Calendar.MONTH) > spentOn.calendar.get(Calendar.MONTH))
                )
             || (   (calendar.get(Calendar.YEAR) == spentOn.calendar.get(Calendar.YEAR))
                 && (calendar.get(Calendar.MONTH) == spentOn.calendar.get(Calendar.MONTH))
                 && (calendar.get(Calendar.DAY_OF_MONTH) > spentOn.calendar.get(Calendar.DAY_OF_MONTH))
                );
    }

    /** check if spent-on is after date
     * @param spentOn spent-on date
     * @return true iff after date
     */
    public boolean isAfter(Date date)
    {
      return isAfter(new SpentOn(date));
    }

    /** compare spent-on dates
     * @param spentOn spent-on date
     * @return -1,0,1 if before/equal/after
     */
    public int compareTo(SpentOn spentOn)
    {
      return calendar.compareTo(spentOn.calendar);
    }

    /** get hash code: day+month+year only
     * @return hash code
     */
    public int hashCode()
    {
      return   ((calendar.get(Calendar.DAY_OF_MONTH) & 0x0000001F) << 0)
             | ((calendar.get(Calendar.MONTH       ) & 0x0000000F) << 5)
             | ((calendar.get(Calendar.YEAR        ) & 0x007FFFFF) << 9);
    }

    /** convert to string
     * @return string
     */
    public String toString(SimpleDateFormat simpleDateFormat)
    {
      return simpleDateFormat.format(calendar.getTime());
    }

    /** convert to string
     * @return string
     */
    public String toString()
    {
      final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

      return toString(DATE_FORMAT);
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
    public SpentOn    spentOn;
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
    TimeEntry(int     id,
              int     projectId,
              int     issueId,
              int     userId,
              int     activityId,
              double  hours,
              String  comments,
              SpentOn spentOn,
              Date    createdOn,
              Date    updateOn
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
    TimeEntry(int     id,
              int     projectId,
              int     issueId,
              int     userId,
              int     activityId,
              double  hours,
              String  comments,
              Date    spentOn,
              Date    createdOn,
              Date    updateOn
             )
    {
      this(id,
           projectId,
           issueId,
           userId,
           activityId,
           hours,
           comments,
           new SpentOn(spentOn),
           createdOn,
           updateOn
          );
    }

    /** create time entry
     * @param projectId project id
     * @param issueId issue id
     * @param activityId activity id
     * @param hourse spent hours
     * @param comments comments
     * @param spentOn spent-on date
     */
    TimeEntry(int     projectId,
              int     issueId,
              int     activityId,
              double  hours,
              String  comments,
              SpentOn spentOn
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
           today()
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
      return "TimeEntry { "+((id != ID_NONE) ? id : "none")+", "+((projectId != ID_NONE) ? projectId : "none")+", "+((issueId != ID_NONE) ? issueId : "none")+", "+((activityId != ID_NONE) ? activityId : "none")+", "+hours+", "+spentOn+", "+comments+" }";
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
     * @param defaultResult default result value
     */
    ParseElementHandler(Object defaultResult)
    {
      this.index  = 0;
      this.result = defaultResult;
      this.done   = false;
    }

    /** create element handler
     */
    ParseElementHandler()
    {
      this(null);
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

  /** background update thread
   */
  private class UpdateThread extends Thread
  {
    private final int TIMEOUT = 60*1000;

    private boolean quitFlag   = false;
    private Object  trigger    = new Object();
    private boolean updateFlag = false;

    UpdateThread()
    {
      setDaemon(true);
    }

    public void run()
    {
      while (!quitFlag)
      {
        // wait for update requested or timeout
        synchronized(trigger)
        {
          if (!updateFlag)
          {
            try { trigger.wait(TIMEOUT); } catch (InterruptedException exception) { Dprintf.dprintf("");
            /* ignored */ }
          }
          updateFlag = false;
        }

        // update projects, activities

        // update issues

        // update hours sums
        ArrayList<SpentOn> spentOnList = new ArrayList<SpentOn>();
        synchronized(timeEntryHoursSumDateMap)
        {
          for (SpentOn spentOn : timeEntryHoursSumDateMap.keySet())
          {
            if (timeEntryHoursSumDateMap.get(spentOn) < 0) spentOnList.add(spentOn);
          }
        }
        for (SpentOn spentOn : spentOnList)
        {
          try
          {
            // get hours sum
            double timeEntryHoursSum = getTimeEntryHoursSum(ID_ANY,ID_ANY,ownUserId,spentOn);
//Dprintf.dprintf("spentOn=%s timeEntryHoursSum=%f",spentOn,timeEntryHoursSum);

            // store
            synchronized(timeEntryHoursSumDateMap)
            {
              timeEntryHoursSumDateMap.put(spentOn,timeEntryHoursSum);
            }
          }
          catch (RedmineException exception)
          {
            // ignored
          }
        }
      }
          }

    public void quit()
    {
      quitFlag = true;
    }

    public void triggerUpdate()
    {
      synchronized(trigger)
      {
        updateFlag = true;
        trigger.notifyAll();
      }
    }

    public void updateTimeEntryHoursSum(SpentOn spentOn)
    {
      synchronized(timeEntryHoursSumDateMap)
      {
        timeEntryHoursSumDateMap.put(spentOn,HOURS_UPDATE);
        triggerUpdate();
      }
    }
  }

  // --------------------------- constants --------------------------------
  public  final static int               ID_ANY          = -1;
  public  final static int               ID_NONE         =  0;
  public  final static double            HOURS_UPDATE    = -1;

  private final static int               ENTRY_LIMIT     = 100; // max. 100
  private final static SimpleDateFormat  DATE_FORMAT     = new SimpleDateFormat("yyyy-MM-dd");
  private final static SimpleDateFormat  DATETIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

  private final SoftReference<TimeEntry> TIME_ENTRY_NULL = new SoftReference<TimeEntry>(null);

  // --------------------------- variables --------------------------------

  private String                              serverName;
  private int                                 serverPort;
  private boolean                             serverUseSSL;
  private String                              authorization;
  private HostnameVerifier                    anyHostnameVerifier;

  private UpdateThread                        updateThread;

  private int                                 ownUserId;

  private SoftHashMap<Integer,User>           userMap                    = new SoftHashMap<Integer,User>();
  private SoftHashMap<Integer,Tracker>        trackerMap                 = new SoftHashMap<Integer,Tracker>();
  private SoftHashMap<Integer,Status>         statusMap                  = new SoftHashMap<Integer,Status>();
  private SoftHashMap<Integer,Priority>       priorityMap                = new SoftHashMap<Integer,Priority>();
  private SoftHashMap<Integer,Activity>       activityMap                = new SoftHashMap<Integer,Activity>();

  private SoftHashMap<Integer,Project>        projectMap                 = new SoftHashMap<Integer,Project>();
  private int                                 projectsCount              = 0;

  private SoftHashMap<Integer,Issue>          issueMap                   = new SoftHashMap<Integer,Issue>();
  private int                                 issuesCount                = 0;

  private HashMap<Integer,Integer>            timeEntryIdMap             = new HashMap<Integer,Integer>();      // map id -> index
  private HashMap<SpentOn,Integer>            timeEntrySpentOnMap        = new HashMap<SpentOn,Integer>();      // map spent-on date -> index
  private ArrayList<SoftReference<TimeEntry>> timeEntries                = new ArrayList<SoftReference<TimeEntry>>();
  private long                                timeEntriesUpdateTimeStamp = 0L;
  private Date                                timeEntryStartDate;
  private SoftHashMap<SpentOn,Double>         timeEntryHoursSumDateMap   = new SoftHashMap<SpentOn,Double>();   // map spent-on date -> hours sum

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** initialize Redmine client
   * @param serverName server name
   * @param serverPort server port
   * @param serverUseSSL true iff server use SSL
   * @param loginName login name
   * @param loginPasssword login password
   */
  public Redmine(String serverName, int serverPort, boolean serverUseSSL, final String loginName, String loginPassword)
    throws RedmineException
  {
    // initialize variables
    this.serverName    = serverName;
    this.serverPort    = serverPort;
    this.serverUseSSL  = serverUseSSL;
    this.authorization = "Basic "+new String(Base64.encodeBase64((loginName+":"+loginPassword).getBytes()));

    // host name verifier which accept all host names
    this.anyHostnameVerifier = new HostnameVerifier()
    {
      public boolean verify(String hostname, SSLSession session)
      {
        return true;
      }
    };

    // install trust-manager which accept all certificates
    try
    {
      SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(new KeyManager[0],new TrustManager[]{new AnyTrustManager()},new SecureRandom());
      SSLContext.setDefault(sslContext);
    }
    catch (KeyManagementException exception)
    {
      throw new RedmineException("SSL fail",exception);
    }
    catch (NoSuchAlgorithmException exception)
    {
      throw new RedmineException("SSL fail",exception);
    }

    // get login user id
    this.ownUserId = (Integer)iterateData("/users/current","user",new ParseElementHandler(new Integer(ID_NONE))
    {
      public void data(Element element)
      {
//Dprintf.dprintf("getValue(element,login)=%s id=%d",getValue(element,"login"),getIntValue(element,"id"));
        done(getIntValue(element,"id"));
      }
    });

    // start update worker thread
    updateThread = new UpdateThread();
    updateThread.start();
    updateThread.triggerUpdate();
  }

  /** get own user id
   * @return own user id
   */
  public int getOwnUserId()
  {
    return this.ownUserId;
  }

  public SpentOn today()
  {
    return new SpentOn();
  }

  /** get Redmine users
   * @return user hash map <id,user>
   */
  public synchronized SoftHashMap<Integer,User> getUsers()
    throws RedmineException
  {
    if (userMap.size() <= 0)
    {
      // get user data
      final ArrayList<User> userList = new ArrayList<User>();
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
        public void store(User user)
        {
          userList.add(user);
        }
      });

      // store into map
      synchronized(userMap)
      {
        userMap.clear();
        for (User user : userList)
        {
          userMap.put(user.id,user);
        }
      }
    }

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
   * @param forceRefresh true to force refresh of data
   * @return tracker hash map <id,tracker>
   */
  public synchronized SoftHashMap<Integer,Tracker> getTrackers(boolean forceRefresh)
    throws RedmineException
  {
    if ((trackerMap.size() <= 0) || forceRefresh)
    {
      // get tracker data
      final ArrayList<Tracker> trackerList = new ArrayList<Tracker>();
      getData("/trackers","tracker",new ParseElementHandler<Tracker>()
      {
        public void data(Element element)
        {
          Tracker tracker = new Tracker(getIntValue(element,"id"),
                                        getValue(element,"name")
                                       );

          store(tracker);
        }
        public void store(Tracker tracker)
        {
          trackerList.add(tracker);
        }
      });

      // store into map
      synchronized(trackerMap)
      {
        trackerMap.clear();
        for (Tracker tracker : trackerList)
        {
          trackerMap.put(tracker.id,tracker);
        }
      }
    }

    return trackerMap;
  }

  /** get Redmine trackers as an array
   * @return tracker array
   */
  public Tracker[] getTrackerArray()
    throws RedmineException
  {
    getTrackers(false);
    return trackerMap.values().toArray(new Redmine.Tracker[trackerMap.size()]);
  }

  /** get tracker
   * @param trackerId tracker id
   * @return tracker
   */
  public Tracker getTracker(int trackerId)
    throws RedmineException
  {
    Tracker tracker = trackerMap.get(trackerId);
    if (tracker == null)
    {
      getTrackers(false);
      tracker = trackerMap.get(trackerId);
    }

    return tracker;
  }

  /** get tracker name
   * @param trackerId tracker id
   * @param defaultName default name
   * @return tracker name
   */
  public String getTrackerName(int trackerId, String defaultName)
  {
    try
    {
      Tracker tracker = getTracker(trackerId);

      return (tracker != null) ? tracker.name : defaultName;
    }
    catch (RedmineException exception)
    {
      return defaultName;
    }
  }

  /** get Redmine status
   * @param forceRefresh true to force refresh of data
   * @return status hash map <id,status>
   */
  public SoftHashMap<Integer,Status> getStatus(boolean forceRefresh)
    throws RedmineException
  {
    if ((statusMap.size() <= 0) || forceRefresh)
    {
      // get status data
      final ArrayList<Status> statusList = new ArrayList<Status>();
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
          statusList.add(status);
        }
      });

      // store into map
      synchronized(statusMap)
      {
        statusMap.clear();
        for (Status status : statusList)
        {
          statusMap.put(status.id,status);
        }
      }
    }

    return statusMap;
  }

  /** get Redmine status as an array
   * @return status array
   */
  public Status[] getStatusArray()
    throws RedmineException
  {
    getStatus(false);
    return statusMap.values().toArray(new Redmine.Status[statusMap.size()]);
  }

  /** get status
   * @param statusId status id
   * @return status
   */
  public Status getStatus(int statusId)
    throws RedmineException
  {
    Status status = statusMap.get(statusId);
    if (status == null)
    {
      getStatus(false);
      status = statusMap.get(statusId);
    }

    return status;
  }

  /** get status name
   * @param statusId status id
   * @param defaultName default name
   * @return status name
   */
  public String getStatusName(int statusId, String defaultName)
  {
    try
    {
      Status status = getStatus(statusId);

      return (status != null) ? status.name : defaultName;
    }
    catch (RedmineException exception)
    {
      return defaultName;
    }
  }

  /** get Redmine priorities
   * @return priority hash map <id,priority>
   */
  public SoftHashMap<Integer,Priority> getPriorities()
    throws RedmineException
  {
    if (priorityMap.size() <= 0)
    {
      // get priority data
      final ArrayList<Priority> priorityList = new ArrayList<Priority>();
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
          priorityList.add(priority);
        }
      });

      // store into map
      synchronized(priorityMap)
      {
        priorityMap.clear();
        for (Priority priority : priorityList)
        {
          priorityMap.put(priority.id,priority);
        }
      }
    }

    return priorityMap;
  }

  /** get priority
   * @param priorityId priority id
   * @return priority
   */
  public Priority getPriority(int priorityId)
    throws RedmineException
  {
    Priority priority = priorityMap.get(priorityId);
    if (priority == null)
    {
      getPriorities();
      priority = priorityMap.get(priorityId);
    }

    return priority;
  }

  /** get default priority id
   * @return id or ID_NONE
   */
  public int getDefaultPriorityId()
  {
    int priorityId = ID_NONE;
    for (Priority priority : priorityMap.values())
    {
      if (priority.isDefault)
      {
        priorityId = priority.id;
        break;
      }
    }

    return priorityId;
  }

  /** get Redmine activities
   * @param forceRefresh true to force refresh of data
   * @return activity hash map <id,activity>
   */
  public synchronized SoftHashMap<Integer,Activity> getActivities(boolean forceRefresh)
    throws RedmineException
  {
    if ((activityMap.size() <= 0) || forceRefresh)
    {
      // get activity data
      final ArrayList<Activity> activityList = new ArrayList<Activity>();
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
        public void store(Activity activity)
        {
          activityList.add(activity);
        }
      });

      // store into map
      synchronized(activityMap)
      {
        activityMap.clear();
        for (Activity activity : activityList)
        {
          activityMap.put(activity.id,activity);
        }
      }
    }

    return activityMap;
  }

  /** get Redmine activities as an array
   * @return project array
   */
  public Activity[] getActivityArray()
    throws RedmineException
  {
    getActivities(false);
    return activityMap.values().toArray(new Redmine.Activity[activityMap.size()]);
  }

  /** get default activity id
   * @return id or ID_NONE
   */
  public int getDefaultActivityId()
  {
    int activityId = ID_NONE;
    for (Activity activity : activityMap.values())
    {
      if (activity.isDefault)
      {
        activityId = activity.id;
        break;
      }
    }

    return activityId;
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
        getActivities(false);
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
   * @param forceRefresh true to force refresh of data
   * @return project hash map <id,project>
   */
  public synchronized SoftHashMap<Integer,Project> getProjects(boolean forceRefresh)
    throws RedmineException
  {
    if ((projectMap.size() <= 0) || forceRefresh)
    {
      // get projects data
      final ArrayList<Project> projectList = new ArrayList<Project>();
      getData("/projects","project",new ParseElementHandler<Project>()
      {
        public void root(Element element)
        {
          projectsCount = getIntAttribute(element,"total_count");
        }
        public void data(Element element)
        {
          Project project = new Project(getIntValue(element,"id"),
                                        getValue(element,"name"),
                                        getValue(element,"identifier"),
                                        getValue(element,"description"),
                                        getDateValue(element,"created_on"),
                                        getDateValue(element,"updated_on")
                                       );

          store(project);
        }
        public void store(Project project)
        {
          projectList.add(project);
        }
      });

      // store into map
      synchronized(projectMap)
      {
        projectMap.clear();
        for (Project project : projectList)
        {
          projectMap.put(project.id,project);
        }
      }
    }

    return projectMap;
  }

  /** get Redmine projects as an array
   * @return project array
   */
  public Project[] getProjectArray()
    throws RedmineException
  {
    getProjects(false);
    return projectMap.values().toArray(new Redmine.Project[projectMap.size()]);
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
        getProjects(false);
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
   * @return issue hash map <id,issue>
   */
  public synchronized SoftHashMap<Integer,Issue> getIssues(boolean forceRefresh)
    throws RedmineException
  {
    if ((issueMap.size() <= 0) || forceRefresh)
    {
new Throwable().printStackTrace();
    // get issues
      final ArrayList<Issue> issueList = new ArrayList<Issue>();
      getData("/issues","issue",new ParseElementHandler<Issue>()
      {
        public void root(Element element)
        {
          issuesCount = getIntAttribute(element,"total_count");
        }
        public void data(Element element)
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
        public void store(Issue issue)
        {
          issueList.add(issue);
        }
      });

      // store into map
      synchronized(issueMap)
      {
        issueMap.clear();
        for (Issue issue : issueList)
        {
          issueMap.put(issue.id,issue);
        }
      }
    }

    return issueMap;
  }

  /** get Redmine issues as an array
   * @param projectId project id
   * @return issue array
   */
  public Issue[] getIssueArray(int projectId)
    throws RedmineException
  {
    getIssues(false);

    ArrayList<Issue> issueList = new ArrayList<Issue>();
    synchronized(issueMap)
    {
      for (Issue issue : issueMap.values())
      {
        if (issue.projectId == projectId) issueList.add(issue);
      }
    }

    return issueList.toArray(new Redmine.Issue[issueList.size()]);
  }

  /** get Redmine issue
   * @param issueId issue id
   * @return issue or null
   */
  public Issue getIssue(int issueId)
  {
    try
    {
      Issue issue;
      synchronized(issueMap)
      {
        issue = issueMap.get(issueId);
        if (issue == null)
        {
          getIssues(false);
          issue = issueMap.get(issueId);
        }
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
   * @return time entry hash map <id,time entry>
   */
  public synchronized SoftHashMap<Integer,TimeEntry> getTimeEntries(final int projectId, final int issueId, final int userId, final SpentOn spentOn)
    throws RedmineException
  {
    SoftHashMap<Integer,TimeEntry> timeEntryMap = new SoftHashMap<Integer,TimeEntry>();

    // update number of time entries, start date
    updateTimeEntryData();

    // get data
    synchronized(timeEntries)
    {
      for (int i = 0; i < timeEntries.size(); i++)
      {
        // get soft-reference for entry
        SoftReference<TimeEntry> softReference = timeEntries.get(i);
        if ((softReference == null) || (softReference.get() == null))
        {
  //Dprintf.dprintf("fill %d length %d",i,ENTRY_LIMIT);
          // fill time entry array (get as much data a possible with single request)
          fillTimeEntry(i,ENTRY_LIMIT);

          // get soft-reference for entry
          softReference = timeEntries.get(i);
        }

        // get time entry
        if ((softReference != null) && (softReference.get() != null))
        {
          TimeEntry timeEntry = softReference.get();

          if (matchTimeEntry(timeEntry,projectId,issueId,userId,spentOn))
          {
  //Dprintf.dprintf("calendar0=%s calendar1=%s",calendar0,calendar1);
            timeEntryMap.put(timeEntry.id,timeEntry);
          }

          // stop when date is found before given spent date (Note: assume time entries are sorted descended)
          if (timeEntry.spentOn.isBefore(spentOn))
          {
  //Dprintf.dprintf("stop %s",timeEntry);
            break;
          }
        }
      }
    }

    return timeEntryMap;
  }

  /** get Redmine time entries
   * @param userId user id or ID_ANY
   * @param spentOn spent-on date or null
   * @return time entry hash map <id,time entry>
   */
  public SoftHashMap<Integer,TimeEntry> getTimeEntries(SpentOn spentOn)
    throws RedmineException
  {
    return getTimeEntries(ID_ANY,ID_ANY,ownUserId,spentOn);
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

  /** get total number time entries
   * @param projectId project id or ID_ANY
   * @param issueId issue id or ID_ANY
   * @param userId user id or ID_ANY
   * @param spentOn spent-on date or null
   * @return total number of time entries
   */
  public synchronized int getTimeEntryCount(int projectId, int issueId, int userId, SpentOn spentOn)
    throws RedmineException
  {
    int n = 0;

    // update number of time entries, start date
    updateTimeEntryData();

    // get data
    synchronized(timeEntries)
    {
      for (int i = 0; i < timeEntries.size(); i++)
      {
        // get soft-reference for entry
        SoftReference<TimeEntry> softReference = timeEntries.get(i);
        if ((softReference == null) || (softReference.get() == null))
        {
          // fill time entry array (get as much data a possible with single request)
          fillTimeEntry(i,ENTRY_LIMIT);

          // get soft-reference for entry
          softReference = timeEntries.get(i);
        }

        // get time entry
        if ((softReference != null) && (softReference.get() != null))
        {
          TimeEntry timeEntry = softReference.get();

          if (matchTimeEntry(timeEntry,projectId,issueId,userId,spentOn))
          {
            n++;
          }

          // stop when date is found before given spent date (Note: assume time entries are sorted descended)
          if (timeEntry.spentOn.isBefore(spentOn))
          {
  //Dprintf.dprintf("stop %s",timeEntry);
            break;
          }
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
   * @param spentOn spent-on date or null
   * @return total number of time entries
   */
  public int getTimeEntryCount(int projectId, int issueId, SpentOn spentOn)
    throws RedmineException
  {
    return getTimeEntryCount(projectId,issueId,ownUserId,spentOn);
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
   * @param spentOn spent-on date or null
   * @return total number of time entries
   */
  public int getTimeEntryCount(SpentOn spentOn)
    throws RedmineException
  {
    return getTimeEntryCount(ID_ANY,ID_ANY,spentOn);
  }

  /** get total number of own time entries
   * @return total number of time entries
   */
  public int getTimeEntryCount()
    throws RedmineException
  {
    return getTimeEntryCount(null);
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
   * @return hours sum of time entries or UPDATE if data still not available
   */
  public synchronized double getTimeEntryHoursSum(int projectId, int issueId, int userId, SpentOn spentOn)
    throws RedmineException
  {
    double hoursSum = 0.0;

    // update number of time entries, start date
    updateTimeEntryData();

    // get data
    synchronized(timeEntries)
    {
      for (int i = 0; i < timeEntries.size(); i++)
      {
        // get soft-reference for entry
        SoftReference<TimeEntry> softReference = timeEntries.get(i);
        if ((softReference == null) || (softReference.get() == null))
        {
          // fill time entry array (get as much data a possible with single request)
          fillTimeEntry(i,ENTRY_LIMIT);

          // get soft-reference for entry
          softReference = timeEntries.get(i);
        }

        // get time entry
        if ((softReference != null) && (softReference.get() != null))
        {
          TimeEntry timeEntry = softReference.get();

          // match entry
          if (matchTimeEntry(timeEntry,projectId,issueId,userId,spentOn))
          {
  //Dprintf.dprintf("add %s: %f",timeEntry.spentOn,timeEntry.hours);
            hoursSum += timeEntry.hours;
          }

          // stop when date is found before given spent date (Note: assume time entries are sorted descended)
          if (timeEntry.spentOn.isBefore(spentOn))
          {
  //Dprintf.dprintf("stop %s",timeEntry);
            break;
          }
        }
      }
    }

    return hoursSum;
  }

  /** get hours sum of own time entries
   * @param projectId project id or ID_ANY
   * @param issueId issue id or ID_ANY
   * @param spentOn spent-on date or null
   * @return hours sum of time entries
   */
  public double getTimeEntryHoursSum(int projectId, int issueId, SpentOn spentOn)
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
    return getTimeEntryHoursSum(projectId,issueId,ownUserId,null);
  }

  /** get hours sum of own time entries
   * @param spentOn spent-on date or null
   * @return hours sum of time entries
   */
  public double getTimeEntryHoursSum(SpentOn spentOn)
    throws RedmineException
  {
    double hoursSum = HOURS_UPDATE;

    synchronized(timeEntryHoursSumDateMap)
    {
      if (timeEntryHoursSumDateMap.containsKey(spentOn))
      {
        hoursSum = timeEntryHoursSumDateMap.get(spentOn);
//Dprintf.dprintf("found for %s %f",spentOn,hoursSum);
      }
      else
      {
        updateThread.updateTimeEntryHoursSum(spentOn);
      }
    }

    return hoursSum;
//    return getTimeEntryHoursSum(ID_ANY,ID_ANY,ownUserId,spentOn);
  }

  /** get Redmine time entries as an array
   * @param projectId project id or ID_ANY
   * @param issueId issue id or ID_ANY
   * @param userId user id or ID_ANY
   * @param spentOn spent-on date or null
   * @return time entry array
   */
  public synchronized TimeEntry[] getTimeEntryArray(int projectId, int issueId, int userId, SpentOn spentOn)
    throws RedmineException
  {
    // update number of time entries, start date
    updateTimeEntryData();

    // create array
    ArrayList<TimeEntry> filteredTimeEntries = new ArrayList<TimeEntry>();
    synchronized(timeEntries)
    {
      for (int i = 0; i < timeEntries.size(); i++)
      {
        // get soft-reference for entry
        SoftReference<TimeEntry> softReference = timeEntries.get(i);
        if ((softReference == null) || (softReference.get() == null))
        {
          // fill time entry array (get as much data a possible with single request)
          fillTimeEntry(i,ENTRY_LIMIT);

          // get soft-reference for entry
          softReference = timeEntries.get(i);
        }

        // get time entry
        if ((softReference != null) && (softReference.get() != null))
        {
          TimeEntry timeEntry = softReference.get();
  //Dprintf.dprintf("timeEntry=%s",timeEntry);

          if (matchTimeEntry(timeEntry,projectId,issueId,userId,spentOn))
          {
  //Dprintf.dprintf("add timeEntry=%s",timeEntry);
            filteredTimeEntries.add(timeEntry);
          }

          // stop when date is found before given spent date (Note: assume time entries are sorted descended)
          if ((spentOn != null) && timeEntry.spentOn.isBefore(spentOn))
          {
            break;
          }
        }
      }
    }

    return filteredTimeEntries.toArray(new TimeEntry[filteredTimeEntries.size()]);
  }

  /** get own Redmine time entries as an array
   * @param projectId project id or ID_ANY
   * @param issueId issue id or ID_ANY
   * @param spentOn spent-on date or null
   * @return time entry array
   */
  public TimeEntry[] getTimeEntryArray(int projectId, int issueId, SpentOn spentOn)
    throws RedmineException
  {
    return getTimeEntryArray(projectId,issueId,ownUserId,spentOn);
  }

  /** get own Redmine time entries as an array
   * @param spentOn spent-on date or null
   * @return time entry array
   */
  public TimeEntry[] getTimeEntryArray(SpentOn spentOn)
    throws RedmineException
  {
    return getTimeEntryArray(ID_ANY,ID_ANY,spentOn);
  }

  /** get own Redmine time entries of today as an array
   * @param projectId project id or ID_ANY
   * @param issueId issue id or ID_ANY
   * @return time entry array
   */
  public TimeEntry[] getTimeEntryArray(int projectId, int issueId)
    throws RedmineException
  {
    return getTimeEntryArray(projectId,issueId,today());
  }

  /** get Redmine time entry
   * @param index index of time entry [0..n-1]
   * @return time entry or null
   */
  public synchronized TimeEntry getTimeEntryAt(int index)
    throws RedmineException
  {
    SoftReference<TimeEntry> softReference;
    synchronized(timeEntries)
    {
      softReference = timeEntries.get(index);
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
  public TimeEntry getTimeEntry(SpentOn spentOn)
    throws RedmineException
  {
    TimeEntry timeEntry = null;
Dprintf.dprintf("required?");

    Integer index = timeEntrySpentOnMap.get(spentOn);
    if (index == null)
    {
      getTimeEntries(ID_ANY,ID_ANY,ID_ANY,null);
      index = timeEntrySpentOnMap.get(spentOn);
    }
    if (index != null)
    {
      timeEntry = getTimeEntryAt(index);
    }

    return timeEntry;
  }

  /** clear user cache
   */
  public void clearUserCache()
  {
    synchronized(userMap)
    {
      userMap.clear();
    }
  }

  /** clear tracker cache
   */
  public void clearTrackerCache()
  {
    synchronized(userMap)
    {
      trackerMap.clear();
    }
  }

  /** clear status cache
   */
  public void clearStatusCache()
  {
    synchronized(userMap)
    {
      statusMap.clear();
    }
  }

  /** clear priority cache
   */
  public void clearPriorityCache()
  {
    synchronized(userMap)
    {
      priorityMap.clear();
    }
  }

  /** clear activity cache
   */
  public void clearActivityCache()
  {
    synchronized(userMap)
    {
      activityMap.clear();
    }
  }

  /** clear project cache
   */
  public void clearProjectCache()
  {
    synchronized(userMap)
    {
      projectMap.clear();
    }
  }

  /** clear issue cache
   */
  public void clearIssueCache()
  {
    synchronized(issueMap)
    {
      issueMap.clear();
    }
  }

  /** clear time entry cache
   * @param timeEntry time entry to clear from cache
   */
  public void clearTimeEntryCache(TimeEntry timeEntry)
  {
    synchronized(timeEntries)
    {
      for (int i = 0; i < timeEntries.size(); i++)
      {
        timeEntries.set(i,TIME_ENTRY_NULL);
      }
      timeEntriesUpdateTimeStamp = 0L;
    }
  }

  /** clear time entry cache
   */
  public void clearTimeEntryCache()
  {
    synchronized(timeEntries)
    {
      for (int i = 0; i < timeEntries.size(); i++)
      {
        timeEntries.set(i,TIME_ENTRY_NULL);
      }
      timeEntriesUpdateTimeStamp = 0L;
    }
  }

  /** clear time entry hours sum cache
   * @param timeEntry time entry to clear from cache
   */
  public void clearTimeEntryHoursSumCache(TimeEntry timeEntry)
  {
    synchronized(timeEntryHoursSumDateMap)
    {
      timeEntryHoursSumDateMap.put(timeEntry.spentOn,HOURS_UPDATE);
    }
  }

  /** clear time entry hours sum cache
   */
  public void clearTimeEntryHoursSumCache()
  {
    synchronized(timeEntryHoursSumDateMap)
    {
      timeEntryHoursSumDateMap.clear();
    }
  }

  /** add time entry
   * @param timeEntry time entry to add
   */
  public void add(final TimeEntry timeEntry)
    throws RedmineException
  {
    // add time entry on Redmine server
    postData("/time_entries","time_entry",timeEntry,new CreateHandler()
    {
      public void data(Document document, Element rootElement)
      {
        Element element;

        element = document.createElement("issue_id");
        element.appendChild(document.createTextNode(Integer.toString(timeEntry.issueId)));
        rootElement.appendChild(element);

        element = document.createElement("spent_on");
        element.appendChild(document.createTextNode(timeEntry.spentOn.toString(DATE_FORMAT)));
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

    // clear caches
    clearTimeEntryCache(timeEntry);
    clearTimeEntryHoursSumCache(timeEntry);
  }

  /** update time entry
   * @param timeEntry time entry to update
   */
  public void update(final TimeEntry timeEntry)
    throws RedmineException
  {
    // update time entry on Redmine server
    putData("/time_entries/"+timeEntry.id,"time_entry",timeEntry,new CreateHandler()
    {
      public void data(Document document, Element rootElement)
      {
        Element element;

        element = document.createElement("issue_id");
        element.appendChild(document.createTextNode(Integer.toString(timeEntry.issueId)));
        rootElement.appendChild(element);

        element = document.createElement("spent_on");
        element.appendChild(document.createTextNode(timeEntry.spentOn.toString(DATE_FORMAT)));
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

    // clear caches
    clearTimeEntryCache(timeEntry);
    clearTimeEntryHoursSumCache(timeEntry);
  }

  /** delete time entry
   * @param timeEntry time entry to delete
   */
  public void delete(TimeEntry timeEntry)
    throws RedmineException
  {
    // delete time entry from Redmine server
    deleteData("/time_entries/"+timeEntry.id);

    // clear caches
    clearTimeEntryCache(timeEntry);
    clearTimeEntryHoursSumCache(timeEntry);
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

  /** trust manager which accept any certificate
   */
  private static class AnyTrustManager implements X509TrustManager
  {
    @Override
    public void checkClientTrusted(X509Certificate[] arg0, String arg1)
      throws CertificateException
    {
    }

    @Override
    public void checkServerTrusted(X509Certificate[] arg0, String arg1)
      throws CertificateException
    {
    }

    @Override
    public X509Certificate[] getAcceptedIssuers()
    {
      return null;
    }
  }

  /** connect to server via http/https
   * @param urlString URL string
   * @param arguments arguments or null
   */
  private HttpURLConnection getConnection(String urlString, String arguments)
    throws RedmineException
  {
    HttpURLConnection connection = null;

    try
    {
      if (serverUseSSL)
      {
        URL url = new URL("https://"+serverName+":"+serverPort+urlString+".xml"+((arguments != null) ? "?"+arguments : ""));
        HttpsURLConnection httpsConnection = (HttpsURLConnection)url.openConnection();
        httpsConnection.setHostnameVerifier(anyHostnameVerifier);
        connection = httpsConnection;
      }
      else
      {
        URL url = new URL("http://"+serverName+":"+serverPort+urlString+".xml"+((arguments != null) ? "?"+arguments : ""));
        HttpURLConnection httpConnection = (HttpURLConnection)url.openConnection();
        connection = httpConnection;
      }
    }
    catch (UnknownHostException exception)
    {
      throw new RedmineException("Unknown host '"+serverName+"'");
    }
    catch (IOException exception)
    {
      throw new RedmineException("Connect to server fail",exception);
    }

    return connection;
  }

  /** connect to server via http/https
   * @param urlString URL string
   */
  private HttpURLConnection getConnection(String urlString)
    throws RedmineException
  {
    return getConnection(urlString,null);
  }

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
        connection = getConnection(urlString,"offset="+offset+"&limit="+((length > 0) ? Math.min(length,ENTRY_LIMIT) : ENTRY_LIMIT));
        connection.setRequestMethod("GET");
        connection.setDoOutput(false);
        connection.setRequestProperty("Authorization",authorization);
        if (Settings.debugFlag)
        {
          System.err.println("DEBUG: Get '"+connection.getURL()+"'");
        }

        // parse XML
        document = documentBuilder.parse(connection.getInputStream());
        document.getDocumentElement().normalize();

        // process root element, get number of total entries
        Element rootElement = (Element)document.getDocumentElement();
        parseElementHandler.root(rootElement);
        int totalCount = getIntAttribute(rootElement,"total_count");

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
          if (length > 0) length--;

          if (parseElementHandler.isDone()) break;
        }

        // check if done
        done =    parseElementHandler.isDone()
               || (length == 0)
               || (index >= totalCount)
               || (offset >= totalCount);

        // close connectin
        connection.disconnect(); connection = null;

        // next segment
        offset += ENTRY_LIMIT;
      }
      catch (ParserConfigurationException exception)
      {
        throw new RedmineException("XML parse fail",exception);
      }
      catch (SAXException exception)
      {
        throw new RedmineException("XML parse fail",exception);
      }
      catch (ProtocolException exception)
      {
        throw new RedmineException(exception);
      }
      catch (UnknownHostException exception)
      {
        throw new RedmineException("Unknown host '"+serverName+"'");
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

  /** get all data
   * @param urlString URL string
   * @param name XML entry name
   * @param entityMap entity map for storage
   * @param parseElementHandler element handler
   */
  private void getData(String urlString, String name, ParseElementHandler parseElementHandler)
    throws RedmineException
  {
    getData(urlString,name,0,-1,parseElementHandler);
  }

  /** iterate over all data
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
      connection = getConnection(urlString);
      connection.setRequestMethod("POST");
      connection.setDoOutput(true);
      connection.setRequestProperty("Content-Type","application/xml; charset=utf-8");
      connection.setRequestProperty("Authorization",authorization);
      if (Settings.debugFlag)
      {
        System.err.println("DEBUG: Post '"+connection.getURL()+"'");
      }

      // output data
      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      Transformer transformer = transformerFactory.newTransformer();
      transformer.setOutputProperty(OutputKeys.ENCODING,"UTF-8");
      DOMSource domSource = new DOMSource(document);
      if (Settings.debugFlag)
      {
        System.err.println("DEBUG: xml data ");
        StreamResult streamResult = new StreamResult(System.err);
        transformer.transform(domSource,streamResult);
        System.err.println("");
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
      throw new RedmineException("Unknown host '"+serverName+"'");
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
      connection = getConnection(urlString);
      connection.setRequestMethod("PUT");
      connection.setDoOutput(true);
      connection.setRequestProperty("Content-Type","application/xml; charset=utf-8");
      connection.setRequestProperty("Authorization",authorization);
      if (Settings.debugFlag)
      {
        System.err.println("DEBUG: Put '"+connection.getURL()+"'");
      }

      // output data
      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      Transformer transformer = transformerFactory.newTransformer();
      transformer.setOutputProperty(OutputKeys.ENCODING,"UTF-8");
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
      throw new RedmineException("Unknown host '"+serverName+"'");
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
      connection = getConnection(urlString);
      connection.setRequestMethod("DELETE");
      connection.setDoOutput(true);
      connection.setRequestProperty("Content-Type","application/xml; charset=utf-8");
      connection.setRequestProperty("Authorization",authorization);
      if (Settings.debugFlag)
      {
        System.err.println("DEBUG: Delete '"+connection.getURL()+"'");
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
      throw new RedmineException("Unknown host '"+serverName+"'");
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

  //-----------------------------------------------------------------------

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
    if (System.currentTimeMillis() > (timeEntriesUpdateTimeStamp+Settings.cacheExpireTime*1000))
    {
      synchronized(timeEntries)
      {
        // get total number of time entries
        int timeEntryCount = getDataLength("/time_entries","time_entry");
        timeEntries.ensureCapacity(timeEntryCount);
        for (int i = timeEntries.size(); i < timeEntryCount; i++)
        {
          timeEntries.add(TIME_ENTRY_NULL);
        }

        // get date of first time entry
        timeEntryStartDate = new Date();
        getData("/time_entries","time_entry",timeEntryCount-1,1,new ParseElementHandler<TimeEntry>()
        {
          public void data(Element element)
          {
            timeEntryStartDate = getDateValue(element,"spent_on");
//Dprintf.dprintf("timeEntryStartDate=%s",timeEntryStartDate);
          }
        });

        timeEntriesUpdateTimeStamp = System.currentTimeMillis();
      }
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
        if (!timeEntrySpentOnMap.containsKey(timeEntry.spentOn)) timeEntrySpentOnMap.put(timeEntry.spentOn,index);
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
  private boolean matchTimeEntry(TimeEntry timeEntry, int projectId, int issueId, int userId, SpentOn spentOn)
  {
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
               || spentOn.equals(timeEntry.spentOn)
              );
  }
}

/* end of file */
