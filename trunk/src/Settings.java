/***********************************************************************\
*
* $Revision$
* $Date$
* $Author$
* Contents: load/save program settings
* Systems: all
*
\***********************************************************************/

/****************************** Imports ********************************/
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Constructor;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.EnumSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.StringTokenizer;
import java.util.LinkedHashSet;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;

/****************************** Classes ********************************/

/** setting comment annotation
 */
@Target({TYPE,FIELD})
@Retention(RetentionPolicy.RUNTIME)
@interface SettingComment
{
  String[] text() default {""};                  // comment before value
}

/** setting value annotation
 */
@Target({TYPE,FIELD})
@Retention(RetentionPolicy.RUNTIME)
@interface SettingValue
{
  String name()         default "";              // name of value
  String defaultValue() default "";              // default value
  Class  type()         default DEFAULT.class;   // adapter class

  static final class DEFAULT
  {
  }
}

/** setting value adapter
 */
abstract class SettingValueAdapter<String,Value>
{
  /** convert to value
   * @param string string
   * @return value
   */
  abstract public Value toValue(String string) throws Exception;

  /** convert to string
   * @param value value
   * @return string
   */
  abstract public String toString(Value value) throws Exception;

  /** check if equals
   * @param value0,value1 values to compare
   * @return true if value0==value1
   */
  public boolean equals(Value value0, Value value1)
  {
    return false;
  }
}

/** settings
 */
public class Settings
{
  /** config value adapter String <-> size
   */
  class SettingValueAdapterSize extends SettingValueAdapter<String,Point>
  {
    /** convert to value
     * @param string string
     * @return value
     */
    public Point toValue(String string) throws Exception
    {
      Point point = null;

      StringTokenizer tokenizer = new StringTokenizer(string,"x");
      point = new Point(Integer.parseInt(tokenizer.nextToken()),
                        Integer.parseInt(tokenizer.nextToken())
                       );

      return point;
    }

    /** convert to string
     * @param value value
     * @return string
     */
    public String toString(Point p) throws Exception
    {
      return String.format("%dx%d",p.x,p.y);
    }
  }

  /** column sizes
   */
  static class ColumnSizes
  {
    public final int[] width;

    /** create column sizes
     * @param width width array
     */
    ColumnSizes(int[] width)
    {
      this.width = width;
    }

    /** create column sizes
     * @param width width (int list)
     */
    ColumnSizes(Object... width)
    {
      this.width = new int[width.length];
      for (int z = 0; z < width.length; z++)
      {
        this.width[z] = (Integer)width[z];
      }
    }

    /** create column sizes
     * @param widthList with list
     */
    ColumnSizes(ArrayList<Integer> widthList)
    {
      this.width = new int[widthList.size()];
      for (int z = 0; z < widthList.size(); z++)
      {
        this.width[z] = widthList.get(z);
      }
    }

    /** get width
     * @param columNb column index (0..n-1)
     * @return width or 0
     */
    public int get(int columNb)
    {
      return (columNb < width.length) ? width[columNb] : 0;
    }

    /** convert data to string
     * @return string
     */
    public String toString()
    {
      StringBuilder buffer = new StringBuilder();
      for (int n : width)
      {
        if (buffer.length() > 0) buffer.append(',');
        buffer.append(Integer.toString(n));
      }
      return "ColumnSizes {"+buffer.toString()+"}";
    }
  }

  /** config value adapter String <-> column width array
   */
  class SettingValueAdapterWidthArray extends SettingValueAdapter<String,ColumnSizes>
  {
    /** convert to value
     * @param string string
     * @return value
     */
    public ColumnSizes toValue(String string) throws Exception
    {
      StringTokenizer tokenizer = new StringTokenizer(string,",");
      ArrayList<Integer> widthList = new ArrayList<Integer>();
      while (tokenizer.hasMoreTokens())
      {
        widthList.add(Integer.parseInt(tokenizer.nextToken()));
      }
      return new ColumnSizes(widthList);
    }

    /** convert to string
     * @param value value
     * @return string
     */
    public String toString(ColumnSizes columnSizes) throws Exception
    {
      StringBuilder buffer = new StringBuilder();
      for (int width : columnSizes.width)
      {
        if (buffer.length() > 0) buffer.append(',');
        buffer.append(Integer.toString(width));
      }
      return buffer.toString();
    }
  }

  /** config value adapter String <-> hours
   */
  class SettingValueAdapterHours extends SettingValueAdapter<String,Double>
  {
    /** convert to value
     * @param string string
     * @return value
     */
    public Double toValue(String string) throws Exception
    {
      double n = 0.0;

      StringTokenizer tokenizer = new StringTokenizer(string,":");
      n =   (double)Integer.parseInt(tokenizer.nextToken())
          + (double)Integer.parseInt(tokenizer.nextToken())/60.0;

      return n;
    }

    /** convert to string
     * @param value value
     * @return string
     */
    public String toString(Double n) throws Exception
    {
      return String.format("%d:%d",(int)Math.floor(n),(int)Math.floor(n*60)%60);
    }
  }

  /** color
   */
  static class Color implements Cloneable
  {
    public final RGB DEFAULT_FOREGROUND;
    public final RGB DEFAULT_BACKGROUND;

    public RGB foreground;
    public RGB background;

    /** create color
     * @param foreground,background foreground/background RGB values
     */
    Color(RGB foreground, RGB background)
    {
      this.DEFAULT_FOREGROUND = foreground;
      this.DEFAULT_BACKGROUND = foreground;
      this.foreground         = foreground;
      this.background         = background;
    }

    /** create color
     * @param foreground foreground/background RGB values
     */
    Color(RGB foreground)
    {
      this(foreground,foreground);
    }

    /** clone object
     * @return cloned object
     */
    public Color clone()
    {
      return new Color(foreground,background);
    }

    /** convert data to string
     * @return string
     */
    public String toString()
    {
      return "Color {"+foreground+", "+background+"}";
    }
  }

  /** config value adapter String <-> Color
   */
  class SettingValueAdapterColor extends SettingValueAdapter<String,Color>
  {
    /** convert to value
     * @param string string
     * @return value
     */
    public Color toValue(String string) throws Exception
    {
      Color color = null;

      Object[] data = new Object[6];
      if      (StringParser.parse(string,"%d,%d,%d:%d,%d,%d",data))
      {
        color = new Color(new RGB((Integer)data[0],(Integer)data[1],(Integer)data[2]),
                          new RGB((Integer)data[3],(Integer)data[4],(Integer)data[5])
                         );
      }
      else if (StringParser.parse(string,":%d,%d,%d",data))
      {
        color = new Color(null,new RGB((Integer)data[0],(Integer)data[1],(Integer)data[2]));
      }
      else if (StringParser.parse(string,"%d,%d,%d:",data))
      {
        color = new Color(new RGB((Integer)data[0],(Integer)data[1],(Integer)data[2]),null);
      }
      else if (StringParser.parse(string,"%d,%d,%d",data))
      {
        color = new Color(new RGB((Integer)data[0],(Integer)data[1],(Integer)data[2]));
      }
      else
      {
        throw new Exception(String.format("Cannot parse color definition '%s'",string));
      }

      return color;
    }

    /** convert to string
     * @param value value
     * @return string
     */
    public String toString(Color color) throws Exception
    {
      if      ((color.foreground != null) && (color.background != null))
      {
        if (   (color.foreground.red   != color.background.red  )
            || (color.foreground.green != color.background.green)
            || (color.foreground.blue  != color.background.blue )
           )
        {
          return  ((color.foreground != null) ? color.foreground.red+","+color.foreground.green+","+color.foreground.blue : "")
                 +":"
                 +((color.background != null) ? color.background.red+","+color.background.green+","+color.background.blue : "");
        }
        else
        {
          return color.foreground.red+","+color.foreground.green+","+color.foreground.blue;
        }
      }
      else if (color.foreground != null)
      {
        return color.foreground.red+","+color.foreground.green+","+color.foreground.blue+":";
      }
      else if (color.background != null)
      {
        return ":"+color.background.red+","+color.background.green+","+color.background.blue;
      }
      else
      {
        return "";
      }
    }
  }

  /** config value adapter String <-> Key
   */
  class SettingValueAdapterKey extends SettingValueAdapter<String,Integer>
  {
    /** convert to value
     * @param string string
     * @return value
     */
    public Integer toValue(String string) throws Exception
    {
      int accelerator = 0;
      if (!string.isEmpty())
      {
        accelerator = Widgets.textToAccelerator(string);
        if (accelerator == 0)
        {
          throw new Exception(String.format("Cannot parse key definition '%s'",string));
        }
      }

      return accelerator;
    }

    /** convert to string
     * @param value value
     * @return string
     */
    public String toString(Integer accelerator) throws Exception
    {
      return Widgets.menuAcceleratorToText(accelerator);
    }
  }

  /** vacation date
   */
  static class VacationDate extends Object implements Cloneable
  {
    private final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    private Calendar calendar;

    /** create vacation date
     * @param date date
     */
    VacationDate(Date date)
    {
      calendar = Calendar.getInstance(); calendar.setTime(date);
    }

    /** create vacation date
     * @param string date string
     */
    VacationDate(String string)
      throws ParseException
    {
      calendar = Calendar.getInstance(); calendar.setTime(DATE_FORMAT.parse(string));
    }

    /** check if objects are equal
     * @param object object
     * @return true iff equal
     */
    public boolean equals(Object object)
    {
      return hashCode() == object.hashCode();
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

    /** clone object
     * @return cloned object
     */
    public VacationDate clone()
    {
      return new VacationDate(calendar.getTime());
    }

    /** convert data to string
     * @return string
     */
    public String toString()
    {
      return DATE_FORMAT.format(calendar.getTime());
    }
  }

  /** config value adapter String <-> VacationDate
   */
  class SettingValueAdapterVacationDate extends SettingValueAdapter<String,VacationDate>
  {
    /** convert to value
     * @param string string
     * @return value
     */
    public VacationDate toValue(String string) throws Exception
    {
      return new VacationDate(string);
    }

    /** convert to string
     * @param value value
     * @return string
     */
    public String toString(VacationDate vacationDate) throws Exception
    {
      return vacationDate.toString();
    }
  }

  // --------------------------- constants --------------------------------
  public static final String TIMEMINE_DIRECTORY = System.getProperty("user.home")+File.separator+".timemine";

  /** host system
   */
  public enum HostSystems
  {
    UNKNOWN,
    LINUX,
    SOLARIS,
    WINDOWS,
    MACOS,
    QNX
  };

  private static final String TIMEMINE_CONFIG_FILE_NAME = TIMEMINE_DIRECTORY+File.separator+"timemine.cfg";

  // --------------------------- variables --------------------------------

  private static long lastModified = 0L;

  @SettingComment(text={"Timemine configuration",""})

  // program settings
  public static HostSystems              hostSystem                             = HostSystems.LINUX;

  // server settings
  @SettingValue
  public static String                   serverName                             = "localhost";
  @SettingValue
  public static int                      serverPort                             = 3000;
  @SettingValue
  public static boolean                  serverUseSSL                           = false;
  @SettingValue
  public static String                   loginName                              = "";
  // do not save password
  public static String                   loginPassword                          = "";

  @SettingComment(text={"","Geometry: <width>x<height>","Geometry columns: <width>,..."})
  @SettingValue(type=SettingValueAdapterSize.class)
  public static Point                    geometryMain                           = new Point(600,400);
  @SettingValue(type=SettingValueAdapterWidthArray.class)
  public static ColumnSizes              geometryTodayTimeEntryColumns          = new ColumnSizes(50,70,80,100,200);
  @SettingValue(type=SettingValueAdapterWidthArray.class)
  public static ColumnSizes              geometryTimeEntryColumns               = new ColumnSizes(130,60,70,80,100,200);
  @SettingValue(type=SettingValueAdapterSize.class)
  public static Point                    geometryEditTimeEntry                  = new Point(400,300);
  @SettingValue(type=SettingValueAdapterSize.class)
  public static Point                    geometryPreferences                    = new Point(500,300);

  @SettingComment(text={"","Colors: <rgb foreground>:<rgb background>, <rgb foreground>:, or :<rgb background>, or <rgb foreground+background>"})
  @SettingValue(type=SettingValueAdapterColor.class)
  public static Color                    colorTodayTimeEntries                  = new Color(null,new RGB(255,255,255));
  @SettingValue(type=SettingValueAdapterColor.class)
  public static Color                    colorTimeEntries                       = new Color(null,new RGB(255,255,255));
  @SettingValue(type=SettingValueAdapterColor.class)
  public static Color                    colorTimeEntriesIncomplete             = new Color(null,new RGB(255,128,128));
  @SettingValue(type=SettingValueAdapterColor.class)
  public static Color                    colorTimeEntriesWeekend                = new Color(new RGB(192,192,192),null);
  @SettingValue(type=SettingValueAdapterColor.class)
  public static Color                    colorTimeEntriesVacation               = new Color(new RGB(160,160,160),null);

  @SettingComment(text={"","Accelerator keys"})
  @SettingValue(type=SettingValueAdapterKey.class)
  public static int                      keyTabTodayTimeEntries                 = SWT.F5;
  @SettingValue(type=SettingValueAdapterKey.class)
  public static int                      keyTabTimeEntries                      = SWT.F6;
  @SettingValue(type=SettingValueAdapterKey.class)
  public static int                      keyNewTimeEntry                        = SWT.NONE;
  @SettingValue(type=SettingValueAdapterKey.class)
  public static int                      keyEditTimeEntry                       = SWT.NONE;
  @SettingValue(type=SettingValueAdapterKey.class)
  public static int                      keySaveTimeEntry                       = SWT.CTRL+'S';
  @SettingValue(type=SettingValueAdapterKey.class)
  public static int                      keyDeleteTimeEntry                     = SWT.NONE;

  @SettingValue(type=SettingValueAdapterKey.class)
  public static int                      keyPreferences                         = SWT.CTRL+'R';
  @SettingValue(type=SettingValueAdapterKey.class)
  public static int                      keyQuit                                = SWT.CTRL+'Q';

  // vacation dates
  @SettingComment(text={"","vacation dates [yyyy-mm-dd]"})
  @SettingValue(name="vacationDate", type=SettingValueAdapterVacationDate.class)
  public static HashSet<VacationDate>    vacationDateSet                        = new HashSet<VacationDate>();

  // miscelanous
  @SettingComment(text={"","date/time formats"})
  @SettingValue
  public static String                   dateFormat                             = "yyyy-MM-dd EE";

  @SettingComment(text={"","Required hours sum per day [h]"})
  @SettingValue(type=SettingValueAdapterHours.class)
  public static double                   requiredHoursPerDay                    = 8; // [h]

  @SettingComment(text={"","Min. time delta [min]"})
  @SettingValue
  public static int                      minTimeDelta                           = 30; // [min]

  @SettingComment(text={"","Caching expire times [s]"})
  @SettingValue
  public static int                      cacheExpireTime                        = 30; // [s]

  // show flags
  @SettingComment(text={"","show dialog flags"})
  @SettingValue
  public static Boolean                  showUpdateStatusErrors                 = new Boolean(true);
  @SettingValue
  public static Boolean                  showRestartAfterConfigChanged          = new Boolean(true);

  // miscelanous flags
  public static boolean                  showLoginFlag                          = false;
  public static boolean                  debugFlag                              = false;

  // help
  public static boolean                  helpFlag                               = false;

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** load program settings
   * @param file settings file to load
   */
  public static void load(File file)
  {
    if (file.exists())
    {
      BufferedReader input = null;
      try
      {
        // get setting classes
        Class[] settingClasses = getSettingClasses();

        // open file
        input = new BufferedReader(new FileReader(file));

        // read file
        int      lineNb = 0;
        String   line;
        Object[] data = new Object[2];
        while ((line = input.readLine()) != null)
        {
          line = line.trim();
          lineNb++;

          // check comment
          if (line.isEmpty() || line.startsWith("#"))
          {
            continue;
          }

          // parse
          if (StringParser.parse(line,"%s = % s",data))
          {
            String name   = (String)data[0];
            String string = (String)data[1];

            for (Class clazz : settingClasses)
            {
              for (Field field : clazz.getDeclaredFields())
              {
                for (Annotation annotation : field.getDeclaredAnnotations())
                {
                  if (annotation instanceof SettingValue)
                  {
                    SettingValue settingValue = (SettingValue)annotation;

                    if (((!settingValue.name().isEmpty()) ? settingValue.name() : field.getName()).equals(name))
                    {
                      try
                      {
                        Class type = field.getType();
                        if      (type.isArray())
                        {
                          type = type.getComponentType();
                          if      (SettingValueAdapter.class.isAssignableFrom(settingValue.type()))
                          {
                            // instantiate config adapter class
                            SettingValueAdapter settingValueAdapter;
                            Class enclosingClass = settingValue.type().getEnclosingClass();
                            if (enclosingClass == Settings.class)
                            {
                              Constructor constructor = settingValue.type().getDeclaredConstructor(Settings.class);
                              settingValueAdapter = (SettingValueAdapter)constructor.newInstance(new Settings());
                            }
                            else
                            {
                              settingValueAdapter = (SettingValueAdapter)settingValue.type().newInstance();
                            }

                            // convert to value
                            Object value = settingValueAdapter.toValue(string);
                            field.set(null,addArrayUniq((Object[])field.get(null),value,settingValueAdapter));
                          }
                          else if (type == int.class)
                          {
                            int value = Integer.parseInt(string);
                            field.set(null,addArrayUniq((int[])field.get(null),value));
                          }
                          else if (type == Integer.class)
                          {
                            int value = Integer.parseInt(string);
                            field.set(null,addArrayUniq((Integer[])field.get(null),value));
                          }
                          else if (type == long.class)
                          {
                            long value = Long.parseLong(string);
                            field.set(null,addArrayUniq((long[])field.get(null),value));
                          }
                          else if (type == Long.class)
                          {
                            long value = Long.parseLong(string);
                            field.set(null,addArrayUniq((Long[])field.get(null),value));
                          }
                          else if (type == boolean.class)
                          {
                            boolean value = StringUtils.parseBoolean(string);
                            field.set(null,addArrayUniq((boolean[])field.get(null),value));
                          }
                          else if (type == Boolean.class)
                          {
                            boolean value = StringUtils.parseBoolean(string);
                            field.set(null,addArrayUniq((Boolean[])field.get(null),value));
                          }
                          else if (type == String.class)
                          {
                            field.set(null,addArrayUniq((String[])field.get(null),StringUtils.unescape(string)));
                          }
                          else if (type.isEnum())
                          {
                            field.set(null,addArrayUniq((Enum[])field.get(null),StringUtils.parseEnum(type,string)));
                          }
                          else if (type == EnumSet.class)
                          {
                            field.set(null,addArrayUniq((EnumSet[])field.get(null),StringUtils.parseEnumSet(type,string)));
                          }
                          else
                          {
Dprintf.dprintf("field.getType()=%s",type);
                          }
                        }
                        else if (type == HashSet.class)
                        {
                          type = type.getComponentType();
                          if      (SettingValueAdapter.class.isAssignableFrom(settingValue.type()))
                          {
                            // instantiate config adapter class
                            SettingValueAdapter settingValueAdapter;
                            Class enclosingClass = settingValue.type().getEnclosingClass();
                            if (enclosingClass == Settings.class)
                            {
                              Constructor constructor = settingValue.type().getDeclaredConstructor(Settings.class);
                              settingValueAdapter = (SettingValueAdapter)constructor.newInstance(new Settings());
                            }
                            else
                            {
                              settingValueAdapter = (SettingValueAdapter)settingValue.type().newInstance();
                            }

                            // convert to value
                            Object value = settingValueAdapter.toValue(string);
                            HashSet<Object> hashSet = (HashSet<Object>)field.get(null);
                            hashSet.add(value);
                          }
                          else if (type == Integer.class)
                          {
                            int value = Integer.parseInt(string);
                            HashSet<Integer> hashSet = (HashSet<Integer>)field.get(null);
                            hashSet.add(value);
                          }
                          else if (type == Long.class)
                          {
                            long value = Long.parseLong(string);
                            HashSet<Long> hashSet = (HashSet<Long>)field.get(null);
                            hashSet.add(value);
                          }
                          else if (type == Boolean.class)
                          {
                            boolean value = StringUtils.parseBoolean(string);
                            HashSet<Boolean> hashSet = (HashSet<Boolean>)field.get(null);
                            hashSet.add(value);
                          }
                          else if (type == String.class)
                          {
                            String value = StringUtils.unescape(string);
                            HashSet<String> hashSet = (HashSet<String>)field.get(null);
                            hashSet.add(value);
                          }
                          else if (type.isEnum())
                          {
                            Enum value = StringUtils.parseEnum(type,string);
                            HashSet<Enum> hashSet = (HashSet<Enum>)field.get(null);
                            hashSet.add(value);
                          }
                          else if (type == EnumSet.class)
                          {
                            EnumSet value = StringUtils.parseEnumSet(type,string);
                            HashSet<EnumSet> hashSet = (HashSet<EnumSet>)field.get(null);
                            hashSet.add(value);
                          }
                          else
                          {
Dprintf.dprintf("field.getType()=%s",type);
                          }
                        }
                        else
                        {
                          if      (SettingValueAdapter.class.isAssignableFrom(settingValue.type()))
                          {
                            // instantiate config adapter class
                            SettingValueAdapter settingValueAdapter;
                            Class enclosingClass = settingValue.type().getEnclosingClass();
                            if (enclosingClass == Settings.class)
                            {
                              Constructor constructor = settingValue.type().getDeclaredConstructor(Settings.class);
                              settingValueAdapter = (SettingValueAdapter)constructor.newInstance(new Settings());
                            }
                            else
                            {
                              settingValueAdapter = (SettingValueAdapter)settingValue.type().newInstance();
                            }

                            // convert to value
                            Object value = settingValueAdapter.toValue(string);
                            field.set(null,value);
                          }
                          else if (type == int.class)
                          {
                            int value = Integer.parseInt(string);
                            field.setInt(null,value);
                          }
                          else if (type == Integer.class)
                          {
                            int value = Integer.parseInt(string);
                            field.set(null,new Integer(value));
                          }
                          else if (type == long.class)
                          {
                            long value = Long.parseLong(string);
                            field.setLong(null,value);
                          }
                          else if (type == Long.class)
                          {
                            long value = Long.parseLong(string);
                            field.set(null,new Long(value));
                          }
                          else if (type == boolean.class)
                          {
                            boolean value = StringUtils.parseBoolean(string);
                            field.setBoolean(null,value);
                          }
                          else if (type == Boolean.class)
                          {
                            boolean value = StringUtils.parseBoolean(string);
                            field.set(null,new Boolean(value));
                          }
                          else if (type == String.class)
                          {
                            field.set(null,StringUtils.unescape(string));
                          }
                          else if (type.isEnum())
                          {
                            field.set(null,StringUtils.parseEnum(type,string));
                          }
                          else if (type == EnumSet.class)
                          {
                            Class enumClass = settingValue.type();
                            if (!enumClass.isEnum())
                            {
                              throw new Error(enumClass+" is not an enum class!");
                            }
                            field.set(null,StringUtils.parseEnumSet(enumClass,string));
                          }
                          else
                          {
Dprintf.dprintf("field.getType()=%s",type);
                          }
                        }
                      }
                      catch (NumberFormatException exception)
                      {
                        Timemine.printWarning("Cannot parse number '%s' for configuration value '%s' in line %d",string,name,lineNb);
                      }
                      catch (Exception exception)
                      {
Dprintf.dprintf("exception=%s",exception);
exception.printStackTrace();
                      }
                    }
                  }
                  else
                  {
                  }
                }
              }
            }
          }
          else
          {
            Timemine.printWarning("Unknown configuration value '%s' in line %d",line,lineNb);
          }
        }

        // close file
        input.close(); input = null;
      }
      catch (IOException exception)
      {
        // ignored
      }
      finally
      {
        try
        {
          if (input != null) input.close();
        }
        catch (IOException exception)
        {
          // ignored
        }
      }
    }
  }

  /** load program settings
   * @param fileName settings file name
   */
  public static void load(String fileName)
  {
    load(new File(fileName));
  }

  /** load default program settings
   */
  public static void load()
  {
    File file = new File(TIMEMINE_CONFIG_FILE_NAME);

    // load file
    load(file);

    // save last modified time
    lastModified = file.lastModified();
  }

  /** save program settings
   * @param fileName file nam
   */
  public static void save(File file)
  {
    // create directory
    File directory = file.getParentFile();
    if ((directory != null) && !directory.exists()) directory.mkdirs();

    PrintWriter output = null;
    try
    {
      // get setting classes
      Class[] settingClasses = getSettingClasses();

      // open file
      output = new PrintWriter(new FileWriter(file));

      // write settings
      for (Class clazz : settingClasses)
      {
        for (Field field : clazz.getDeclaredFields())
        {
//Dprintf.dprintf("field=%s",field);
          for (Annotation annotation : field.getDeclaredAnnotations())
          {
            if      (annotation instanceof SettingValue)
            {
              SettingValue settingValue = (SettingValue)annotation;

              // get value and write to file
              String name = (!settingValue.name().isEmpty()) ? settingValue.name() : field.getName();
              try
              {
                Class type = field.getType();
                if      (type.isArray())
                {
                  type = type.getComponentType();
                  if      (SettingValueAdapter.class.isAssignableFrom(settingValue.type()))
                  {
                    // instantiate config adapter class
                    SettingValueAdapter settingValueAdapter;
                    Class enclosingClass = settingValue.type().getEnclosingClass();
                    if (enclosingClass == Settings.class)
                    {
                      Constructor constructor = settingValue.type().getDeclaredConstructor(Settings.class);
                      settingValueAdapter = (SettingValueAdapter)constructor.newInstance(new Settings());
                    }
                    else
                    {
                      settingValueAdapter = (SettingValueAdapter)settingValue.type().newInstance();
                    }

                    // convert to string
                    for (Object object : (Object[])field.get(null))
                    {
                      String value = (String)settingValueAdapter.toString(object);
                      output.printf("%s = %s\n",name,value);
                    }
                  }
                  else if (type == int.class)
                  {
                    for (int value : (int[])field.get(null))
                    {
                      output.printf("%s = %d\n",name,value);
                    }
                  }
                  else if (type == Integer.class)
                  {
                    for (int value : (Integer[])field.get(null))
                    {
                      output.printf("%s = %d\n",name,value);
                    }
                  }
                  else if (type == long.class)
                  {
                    for (long value : (long[])field.get(null))
                    {
                      output.printf("%s = %ld\n",name,value);
                    }
                  }
                  else if (type == Long.class)
                  {
                    for (long value : (Long[])field.get(null))
                    {
                      output.printf("%s = %ld\n",name,value);
                    }
                  }
                  else if (type == boolean.class)
                  {
                    for (boolean value : (boolean[])field.get(null))
                    {
                      output.printf("%s = %s\n",name,value ? "yes" : "no");
                    }
                  }
                  else if (type == Boolean.class)
                  {
                    for (boolean value : (Boolean[])field.get(null))
                    {
                      output.printf("%s = %s\n",name,value ? "yes" : "no");
                    }
                  }
                  else if (type == String.class)
                  {
                    for (String value : (String[])field.get(null))
                    {
                      output.printf("%s = %s\n",name,StringUtils.escape(value));
                    }
                  }
                  else if (type.isEnum())
                  {
                    for (Enum value : (Enum[])field.get(null))
                    {
                      output.printf("%s = %s\n",name,value.toString());
                    }
                  }
                  else if (type == EnumSet.class)
                  {
                    for (EnumSet enumSet : (EnumSet[])field.get(null))
                    {
                      output.printf("%s = %s\n",name,StringUtils.join(enumSet,","));
                    }
                  }
                  else
                  {
Dprintf.dprintf("field.getType()=%s",type);
                  }
                }
                else if (type == HashSet.class)
                {
                  type = type.getComponentType();
                  if      (SettingValueAdapter.class.isAssignableFrom(settingValue.type()))
                  {
                    // instantiate config adapter class
                    SettingValueAdapter settingValueAdapter;
                    Class enclosingClass = settingValue.type().getEnclosingClass();
                    if (enclosingClass == Settings.class)
                    {
                      Constructor constructor = settingValue.type().getDeclaredConstructor(Settings.class);
                      settingValueAdapter = (SettingValueAdapter)constructor.newInstance(new Settings());
                    }
                    else
                    {
                      settingValueAdapter = (SettingValueAdapter)settingValue.type().newInstance();
                    }

                    // convert to string
                    HashSet<Object> hashSet = (HashSet<Object>)field.get(null);
                    for (Object object : hashSet)
                    {
                      String value = (String)settingValueAdapter.toString(object);
                      output.printf("%s = %s\n",name,value);
                    }
                  }
                  else if (type == Integer.class)
                  {
                    HashSet<Integer> hashSet = (HashSet<Integer>)field.get(null);
                    for (int value : hashSet)
                    {
                      output.printf("%s = %d\n",name,value);
                    }
                  }
                  else if (type == Long.class)
                  {
                    HashSet<Long> hashSet = (HashSet<Long>)field.get(null);
                    for (long value : hashSet)
                    {
                      output.printf("%s = %ld\n",name,value);
                    }
                  }
                  else if (type == Boolean.class)
                  {
                    HashSet<Boolean> hashSet = (HashSet<Boolean>)field.get(null);
                    for (boolean value : hashSet)
                    {
                      output.printf("%s = %s\n",name,value ? "yes" : "no");
                    }
                  }
                  else if (type == String.class)
                  {
                    HashSet<String> hashSet = (HashSet<String>)field.get(null);
                    for (String value : hashSet)
                    {
                      output.printf("%s = %s\n",name,StringUtils.escape(value));
                    }
                  }
                  else if (type.isEnum())
                  {
                    HashSet<Enum> hashSet = (HashSet<Enum>)field.get(null);
                    for (Enum value : hashSet)
                    {
                      output.printf("%s = %s\n",name,value.toString());
                    }
                  }
                  else if (type == EnumSet.class)
                  {
                    HashSet<EnumSet> hashSet = (HashSet<EnumSet>)field.get(null);
                    for (EnumSet enumSet : hashSet)
                    {
                      output.printf("%s = %s\n",name,StringUtils.join(enumSet,","));
                    }
                  }
                  else
                  {
Dprintf.dprintf("field.getType()=%s",type);
                  }
                }
                else
                {
                  if      (SettingValueAdapter.class.isAssignableFrom(settingValue.type()))
                  {
                    // instantiate config adapter class
                    SettingValueAdapter settingValueAdapter;
                    Class enclosingClass = settingValue.type().getEnclosingClass();
                    if (enclosingClass == Settings.class)
                    {
                      Constructor constructor = settingValue.type().getDeclaredConstructor(Settings.class);
                      settingValueAdapter = (SettingValueAdapter)constructor.newInstance(new Settings());
                    }
                    else
                    {
                      settingValueAdapter = (SettingValueAdapter)settingValue.type().newInstance();
                    }

                    // convert to string
                    String value = (String)settingValueAdapter.toString(field.get(null));
                    output.printf("%s = %s\n",name,value);
                  }
                  else if (type == int.class)
                  {
                    int value = field.getInt(null);
                    output.printf("%s = %d\n",name,value);
                  }
                  else if (type == Integer.class)
                  {
                    int value = (Integer)field.get(null);
                    output.printf("%s = %d\n",name,value);
                  }
                  else if (type == long.class)
                  {
                    long value = field.getLong(null);
                    output.printf("%s = %ld\n",name,value);
                  }
                  else if (type == Long.class)
                  {
                    long value = (Long)field.get(null);
                    output.printf("%s = %ld\n",name,value);
                  }
                  else if (type == boolean.class)
                  {
                    boolean value = field.getBoolean(null);
                    output.printf("%s = %s\n",name,value ? "yes" : "no");
                  }
                  else if (type == Boolean.class)
                  {
                    boolean value = (Boolean)field.get(null);
                    output.printf("%s = %s\n",name,value ? "yes" : "no");
                  }
                  else if (type == String.class)
                  {
                    String value = (type != null) ? (String)field.get(null) : settingValue.defaultValue();
                    output.printf("%s = %s\n",name,StringUtils.escape(value));
                  }
                  else if (type.isEnum())
                  {
                    Enum value = (Enum)field.get(null);
                    output.printf("%s = %s\n",name,value.toString());
                  }
                  else if (type == EnumSet.class)
                  {
                    EnumSet enumSet = (EnumSet)field.get(null);
                    output.printf("%s = %s\n",name,StringUtils.join(enumSet,","));
                  }
                  else
                  {
Dprintf.dprintf("field.getType()=%s",type);
                  }
                }
              }
              catch (Exception exception)
              {
Dprintf.dprintf("exception=%s",exception);
exception.printStackTrace();
              }
            }
            else if (annotation instanceof SettingComment)
            {
              SettingComment settingComment = (SettingComment)annotation;

              for (String line : settingComment.text())
              {
                if (!line.isEmpty())
                {
                  output.printf("# %s\n",line);
                }
                else
                {
                  output.printf("\n");
                }
              }
            }
          }
        }
      }

      // close file
      output.close();

      // save last modified time
      lastModified = file.lastModified();
    }
    catch (IOException exception)
    {
      // ignored
    }
    finally
    {
      if (output != null) output.close();
    }
  }

  /** save program settings
   * @param fileName settings file name
   */
  public static void save(String fileName)
  {
    save(new File(fileName));
  }

  /** save program settings with default name
   */
  public static void save()
  {
    save(TIMEMINE_CONFIG_FILE_NAME);
  }

  /** check if program settings file is modified
   * @return true iff modified
   */
  public static boolean isFileModified()
  {
    return (lastModified != 0L) && (new File(TIMEMINE_CONFIG_FILE_NAME).lastModified() > lastModified);
  }

  //-----------------------------------------------------------------------

  /** get all setting classes
   * @return classes array
   */
  protected static Class[] getSettingClasses()
  {
    // get all setting classes
    ArrayList<Class> classList = new ArrayList<Class>();

    classList.add(Settings.class);
    for (Class clazz : Settings.class.getDeclaredClasses())
    {
//Dprintf.dprintf("c=%s",clazz);
      classList.add(clazz);
    }

    return classList.toArray(new Class[classList.size()]);
  }

  /** unique add element to int array
   * @param array array
   * @param n element
   * @return extended array or array
   */
  private static int[] addArrayUniq(int[] array, int n)
  {
    int z = 0;
    while ((z < array.length) && (array[z] != n))
    {
      z++;
    }
    if (z >= array.length)
    {
      array = Arrays.copyOf(array,array.length+1);
      array[array.length-1] = n;
    }

    return array;
  }

  /** unique add element to int array
   * @param array array
   * @param n element
   * @return extended array or array
   */
  private static Integer[] addArrayUniq(Integer[] array, int n)
  {
    int z = 0;
    while ((z < array.length) && (array[z] != n))
    {
      z++;
    }
    if (z >= array.length)
    {
      array = Arrays.copyOf(array,array.length+1);
      array[array.length-1] = n;
    }

    return array;
  }

  /** unique add element to long array
   * @param array array
   * @param n element
   * @return extended array or array
   */
  private static long[] addArrayUniq(long[] array, long n)
  {
    int z = 0;
    while ((z < array.length) && (array[z] != n))
    {
      z++;
    }
    if (z >= array.length)
    {
      array = Arrays.copyOf(array,array.length+1);
      array[array.length-1] = n;
    }

    return array;
  }

  /** unique add element to long array
   * @param array array
   * @param n element
   * @return extended array or array
   */
  private static Long[] addArrayUniq(Long[] array, long n)
  {
    int z = 0;
    while ((z < array.length) && (array[z] != n))
    {
      z++;
    }
    if (z >= array.length)
    {
      array = Arrays.copyOf(array,array.length+1);
      array[array.length-1] = n;
    }

    return array;
  }

  /** unique add element to long array
   * @param array array
   * @param n element
   * @return extended array or array
   */
  private static boolean[] addArrayUniq(boolean[] array, boolean n)
  {
    int z = 0;
    while ((z < array.length) && (array[z] != n))
    {
      z++;
    }
    if (z >= array.length)
    {
      array = Arrays.copyOf(array,array.length+1);
      array[array.length-1] = n;
    }

    return array;
  }

  /** unique add element to long array
   * @param array array
   * @param n element
   * @return extended array or array
   */
  private static Boolean[] addArrayUniq(Boolean[] array, boolean n)
  {
    int z = 0;
    while ((z < array.length) && (array[z] != n))
    {
      z++;
    }
    if (z >= array.length)
    {
      array = Arrays.copyOf(array,array.length+1);
      array[array.length-1] = n;
    }

    return array;
  }

  /** unique add element to string array
   * @param array array
   * @param string element
   * @return extended array or array
   */
  private static String[] addArrayUniq(String[] array, String string)
  {
    int z = 0;
    while ((z < array.length) && !array[z].equals(string))
    {
      z++;
    }
    if (z >= array.length)
    {
      array = Arrays.copyOf(array,array.length+1);
      array[array.length-1] = string;
    }

    return array;
  }

  /** unique add element to enum array
   * @param array array
   * @param string element
   * @return extended array or array
   */
  private static Enum[] addArrayUniq(Enum[] array, Enum n)
  {
    int z = 0;
    while ((z < array.length) && (array[z] != n))
    {
      z++;
    }
    if (z >= array.length)
    {
      array = Arrays.copyOf(array,array.length+1);
      array[array.length-1] = n;
    }

    return array;
  }

  /** unique add element to enum set array
   * @param array array
   * @param string element
   * @return extended array or array
   */
  private static EnumSet[] addArrayUniq(EnumSet[] array, EnumSet n)
  {
    int z = 0;
    while ((z < array.length) && (array[z].equals(n)))
    {
      z++;
    }
    if (z >= array.length)
    {
      array = Arrays.copyOf(array,array.length+1);
      array[array.length-1] = n;
    }

    return array;
  }

  /** unique add element to object array
   * @param array array
   * @param object element
   * @param settingAdapter setting adapter (use equals() function)
   * @return extended array or array
   */
  private static Object[] addArrayUniq(Object[] array, Object object, SettingValueAdapter settingValueAdapter)
  {
    int z = 0;
    while ((z < array.length) && !settingValueAdapter.equals(array[z],object))
    {
      z++;
    }
    if (z >= array.length)
    {
      array = Arrays.copyOf(array,array.length+1);
      array[array.length-1] = object;
    }

    return array;
  }
}

/* end of file */
