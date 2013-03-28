/***********************************************************************\
*
* $Source$
* $Revision$
* $Author$
* Contents: soft hash map
* Systems: all
*
\***********************************************************************/

/****************************** Imports ********************************/
import java.io.Serializable;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/****************************** Classes ********************************/

/** soft hash map: entries are removed automatically on low memory
 * Original source from: http://www.javaspecialists.eu/archive/Issue098.html
 */
public class SoftHashMap<K,V> extends AbstractMap<K,V> implements Serializable
{
  // --------------------------- constants --------------------------------

  // --------------------------- variables --------------------------------
  private final Map<K,SoftReference<V>> hashMap        = new HashMap<K, SoftReference<V>>();
  private final Map<SoftReference<V>,K> reverseHashMap = new HashMap<SoftReference<V>,K>();

  // reference queue for puring unused objects
  private final ReferenceQueue<V> referenceQueue = new ReferenceQueue<V>();

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** get value from map
   * @param key key
   * @return value or null
   */
  public V get(Object key)
  {
    V value = null;

    purgeGarbageEntries();

    // get soft reference
    SoftReference<V> softRefererence = hashMap.get(key);
    if (softRefererence != null)
    {
      // get value
      value = softRefererence.get();
      if (value == null)
      {
        // value has been garbage collected => remove entry from the maps
        hashMap.remove(key);
        reverseHashMap.remove(softRefererence);
      }
    }

    return value;
  }

  /** store value in map
   * @param key key
   * @param value value
   */
  public V put(K key, V value)
  {
    purgeGarbageEntries();

    // create new soft reference
    SoftReference<V> softRefererence = new SoftReference<V>(value,referenceQueue);

    // store in maps
    reverseHashMap.put(softRefererence,key);
    SoftReference<V> result = hashMap.put(key,softRefererence);
    if (result != null)
    {
      reverseHashMap.remove(result);
      return result.get();
    }
    else
    {
      return null;
    }
  }

  /** get entry set
   * @return entry set
   */
  public Set<Entry<K,V>> entrySet()
  {
    purgeGarbageEntries();

    Set<Entry<K,V>> entrySet = new LinkedHashSet<Entry<K,V>>();
    for (final Entry<K,SoftReference<V>> entry : hashMap.entrySet())
    {
      final V value = entry.getValue().get();
      if (value != null)
      {
        entrySet.add(new Entry<K,V>()
        {
          public K getKey()
          {
            return entry.getKey();
          }
          public V getValue()
          {
            return value;
          }
          public V setValue(V newValue)
          {
            entry.setValue(new SoftReference<V>(newValue,referenceQueue));
            return value;
          }
        });
      }
    }

    return entrySet;
  }

  /** remove entry
   * @param key key
   */
  public V remove(Object key)
  {
    purgeGarbageEntries();

    SoftReference<V> softReference = hashMap.remove(key);
    return (softReference != null) ? softReference.get() : null;
  }

  /** clear map
   */
  public void clear()
  {
    hashMap.clear();
    reverseHashMap.clear();
  }

  /** get size of map
   * @return number of entries in map
   */
  public int size()
  {
    purgeGarbageEntries();

    return hashMap.size();
  }

  // ----------------------------------------------------------------------

  /** purge entries which are garbage collected
   */
  private void purgeGarbageEntries()
  {
    Reference<? extends V> reference;

    while ((reference = referenceQueue.poll()) != null)
    {
Dprintf.dprintf("purge %s",reference);
      hashMap.remove(reverseHashMap.remove(reference));
    }
  }
}

/* end of file */
