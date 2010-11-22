package platform.base;

import java.io.*;
import java.lang.reflect.Method;
import java.util.*;

public class BaseUtils {

    public static boolean nullEquals(Object obj1, Object obj2) {

        if (obj1 == null)
            return obj2 == null;
        else
            return obj1.equals(obj2);
    }

    public static int nullHash(Object obj) {
        return obj==null?0:obj.hashCode();
    }

    public static <T> boolean findByReference(Collection<T> col, Object obj) {
        for (T objCol : col)
            if (objCol == obj) return true;
        return false;
    }

    public static Object multiply(Object obj, Integer coeff) {

        if (obj instanceof Integer) return ((Integer)obj) * coeff;
        if (obj instanceof Long) return ((Long)obj) * coeff;
        if (obj instanceof Double) return ((Double)obj) * coeff;

        return obj;
    }

    public static <KA,VA,KB,VB> boolean mapEquals(Map<KA,VA> mapA,Map<KB,VB> mapB,Map<KA,KB> mapAB) {
        for(Map.Entry<KA,VA> A : mapA.entrySet())
            if(!mapB.get(mapAB.get(A.getKey())).equals(A.getValue()))
                return false;
        return true;
    }

    public static <K,E,V> Map<K,V> nullJoin(Map<K,? extends E> map, Map<E,V> joinMap) {
        return joinMap==null?null:join(map, joinMap);
    }

    public static <K,E,V> Map<K,V> join(Map<K,? extends E> map, Map<E,V> joinMap) {
        Map<K,V> result = new HashMap<K, V>();
        for(Map.Entry<K,? extends E> entry : map.entrySet())
            result.put(entry.getKey(),joinMap.get(entry.getValue()));
        return result;
    }

    public static <K,E,V> List<Map<K,V>> joinList(Map<K,? extends E> map, List<Map<E,V>> list) {
        List<Map<K,V>> result = new ArrayList<Map<K, V>>();
        for(Map<E, V> joinMap : list)
            result.add(BaseUtils.join(map, joinMap));
        return result;
    }

    public static <K,V> List<V> mapList(List<? extends K> list, Map<K,V> map) {
        List<V> result = new ArrayList<V>();
        for(K element : list)
            result.add(map.get(element));
        return result;
    }

    public static <K,E,V> OrderedMap<V,E> mapOrder(OrderedMap<K,E> list, Map<K,V> map) { // map предполагается reversed
        OrderedMap<V,E> result = new OrderedMap<V,E>();
        for(Map.Entry<K,E> entry : list.entrySet())
            result.put(map.get(entry.getKey()), entry.getValue());
        return result;
    }

    public static <K,E,V> Map<K,V> innerJoin(Map<K,E> map, Map<E,V> joinMap) {
        Map<K,V> result = new HashMap<K, V>();
        for(Map.Entry<K,E> entry : map.entrySet()) {
            V joinValue = joinMap.get(entry.getValue());
            if(joinValue!=null) result.put(entry.getKey(), joinValue);
        }
        return result;
    }

    public static <K,E,V> OrderedMap<K,V> innerJoin(OrderedMap<K, E> map, Map<E,V> joinMap) {
        OrderedMap<K,V> result = new OrderedMap<K, V>();
        for(Map.Entry<K,E> entry : map.entrySet()) {
            V joinValue = joinMap.get(entry.getValue());
            if(joinValue!=null) result.put(entry.getKey(), joinValue);
        }
        return result;
    }

    public static <K,V> Map<K,V> filterValues(Map<K,V> map, Collection<V> values) {
        Map<K,V> result = new HashMap<K, V>();
        for(Map.Entry<K,V> entry : map.entrySet())
            if(values.contains(entry.getValue()))
                result.put(entry.getKey(),entry.getValue());
        return result;
    }

    // необходимо чтобы пересоздавал объект !!! потому как на вход идут mutable'ы
    public static <K,V> Collection<K> filterValues(Map<K,V> map, V value) {
        Collection<K> result = new ArrayList<K>();
        for(Map.Entry<K,V> entry : map.entrySet())
            if(value.equals(entry.getValue()))
                result.add(entry.getKey());
        return result;
    }


    public static <BK,K extends BK,V> Map<K,V> filterKeys(Map<BK,V> map, Collection<K> keys) {
        Map<K,V> result = new HashMap<K, V>();
        for(K key : keys) {
            V value = map.get(key);
            if(value!=null) result.put(key,value);
        }
        return result;
    }

    // возвращает более конкретный класс если 
    public static <K,V,CV extends V> Map<K,CV> filterClass(Map<K,V> map, Class<CV> cvClass) {
        for(Map.Entry<K,V> entry : map.entrySet())
            if(!cvClass.isInstance(entry.getValue()))
                return new HashMap<K, CV>();
        return (Map<K,CV>)(Map<K,? extends V>)map;
    }

    public static <K,V> Map<K,V> filterNotKeys(Map<K,V> map, Collection<? extends K> keys) {
        Map<K,V> result = new HashMap<K, V>();
        for(Map.Entry<K,V> entry : map.entrySet()) {
            if(!keys.contains(entry.getKey()))
                result.put(entry.getKey(),entry.getValue());
        }
        return result;
    }

    public static <K> List<K> filterList(List<K> list, Collection<K> filter) {
        List<K> result = new ArrayList<K>();
        for(K element : list)
            if(filter.contains(element))
                result.add(element);
        return result;
    }

    public static <K> List<K> filterNotList(List<K> list, Collection<K> filter) {
        List<K> result = new ArrayList<K>();
        for(K element : list)
            if(!filter.contains(element))
                result.add(element);
        return result;
    }

    public static <BK,K extends BK,V> Map<K,V> splitKeys(Map<BK,V> map, Collection<K> keys, Map<BK,V> rest) {
        Map<K,V> result = new HashMap<K, V>();
        for(Map.Entry<BK,V> entry : map.entrySet())
            if(keys.contains(entry.getKey()))
                result.put((K) entry.getKey(),entry.getValue());
            else
                rest.put(entry.getKey(),entry.getValue());
        return result;
    }

    public static <BV,V extends BV,K> Map<K,V> splitValues(Map<K,BV> map, Collection<V> keys, Map<K,BV> rest) {
        Map<K,V> result = new HashMap<K, V>();
        for(Map.Entry<K,BV> entry : map.entrySet())
            if(keys.contains(entry.getValue()))
                result.put(entry.getKey(), (V) entry.getValue());
            else
                rest.put(entry.getKey(),entry.getValue());
        return result;
    }

    public static <K,V> Map<V,K> reverse(Map<K,V> map) {
        Map<V,K> result = new HashMap<V, K>();
        for(Map.Entry<K,V> entry : map.entrySet()) {
            assert !result.containsKey(entry.getValue());
            result.put(entry.getValue(),entry.getKey());
        }
        return result;
    }

    public static <K,VA,VB> Map<VA,VB> crossJoin(Map<K,VA> map,Map<K,VB> mapTo) {
        return join(reverse(map),mapTo);
    }

    public static <KA,VA,KB,VB> Map<VA,VB> crossJoin(Map<KA,VA> map,Map<KB,VB> mapTo,Map<KA,KB> mapJoin) {
        return join(crossJoin(map,mapJoin),mapTo);
    }

    public static <KA,KB,V> Map<KA,KB> crossValues(Map<KA,V> map,Map<KB,V> mapTo) {
        return join(map,reverse(mapTo));
    }

    public static <KA, KB, V> Map<KA, KB> mapValues(Map<KA, V> map, Map<KB, V> equals) {
        if(map.size()!=equals.size()) return null;

        Map<KA, KB> mapKeys = new HashMap<KA, KB>();
        for(Map.Entry<KA, V> key : map.entrySet()) {
            KB mapKey = null;
            for(Map.Entry<KB, V> equalKey : equals.entrySet())
                if(!hashContainsValue(mapKeys,equalKey.getKey()) &&
                    hashEquals(key.getValue(),equalKey.getValue())) {
                    mapKey = equalKey.getKey();
                    break;
                }
            if(mapKey==null) return null;
            mapKeys.put(key.getKey(),mapKey);
        }
        return mapKeys;
    }

    public static <K> Collection<K> join(Collection<K> col1, Collection<K> col2) {
        Set<K> result = new HashSet<K>(col1);
        result.addAll(col2);
        return result;
    }

    public static <K,V> boolean identity(Map<K,V> map) {
        for(Map.Entry<K,V> entry : map.entrySet())
            if(!entry.getKey().equals(entry.getValue())) return false;
        return true;
    }

    public static <K> Map<K,K> toMap(Set<K> collection) {
        Map<K,K> result = new HashMap<K, K>();
        for(K object : collection)
            result.put(object,object);
        return result;
    }

    public static <K> Map<Integer,K> toMap(List<K> list) {
        Map<Integer,K> result = new HashMap<Integer, K>();
        for(int i=0;i<list.size();i++)
            result.put(i,list.get(i));
        return result;
    }

    public static <K> Map<Integer,K> toMap(K[] list) {
        Map<Integer,K> result = new HashMap<Integer, K>();
        for(int i=0;i<list.length;i++)
            result.put(i,list[i]);
        return result;
    }

    public static <K> List<K> toList(Map<Integer,K> map) {
        List<K> result = new ArrayList<K>();
        for(int i=0;i<map.size();i++)
            result.add(map.get(i));
        return result;
    }

    public static Object deserializeObject(byte[] state) throws IOException {

        return deserializeObject(new DataInputStream(new ByteArrayInputStream(state)));
    }

    public static Object deserializeObject(DataInputStream inStream) throws IOException {

        int objectType = inStream.readByte();

        if (objectType == 0) {
            return null;
        }

        if (objectType == 1) {
            return inStream.readInt();
        }

        if (objectType == 2) {
            return inStream.readUTF();
        }

        if (objectType == 3) {
            return inStream.readDouble();
        }

        if (objectType == 4) {
            return inStream.readLong();
        }

        if (objectType == 5) {
            return inStream.readBoolean();
        }

        if (objectType == 6) {
            return new java.sql.Date(inStream.readLong());
        }

        if (objectType == 7) {
            int len = inStream.readInt();
            return IOUtils.readBytesFromStream(inStream, len);
        }

        throw new IOException();
    }

    public static void serializeObject(DataOutputStream outStream, Object object) throws IOException {

/*        try {
            ObjectOutputStream objectStream = new ObjectOutputStream(outStream);
            objectStream.writeObject(object);
        } catch (IOException e) {
            e.printStackTrace();
        }*/

        if (object == null) {
            outStream.writeByte(0);
            return;
        }

        if (object instanceof Integer) {
            outStream.writeByte(1);
            outStream.writeInt((Integer)object);
            return;
        }

        if (object instanceof String) {
            outStream.writeByte(2);
            outStream.writeUTF(((String)object).trim());
            return;
        }

        if (object instanceof Double) {
            outStream.writeByte(3);
            outStream.writeDouble((Double)object);
            return;
        }

        if (object instanceof Long) {
            outStream.writeByte(4);
            outStream.writeLong((Long)object);
            return;
        }

        if (object instanceof Boolean) {
            outStream.writeByte(5);
            outStream.writeBoolean((Boolean)object);
            return;
        }

        if (object instanceof java.sql.Date) {
            outStream.writeByte(6);
            outStream.writeLong(((java.sql.Date)object).getTime());
            return;
        }

        if (object instanceof byte[]) {
            byte[] obj = (byte[]) object;
            outStream.writeByte(7);
            outStream.writeInt(obj.length);
            outStream.write(obj);
            return;
        }

        throw new IOException();
    }// -------------------------------------- Сериализация классов -------------------------------------------- //

    public static byte[] serializeObject(Object value) throws IOException {

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        serializeObject(new DataOutputStream(outStream), value);
        return outStream.toByteArray();
    }


    public static boolean startsWith(char[] string,int off,char[] check) {
        if(string.length-off<check.length)
            return false;

        for(int i=0;i<check.length;i++)
            if(string[off+i]!=check[i])
                return false;
        return true;
    }

    public static <K,V> Map<K,V> removeKeys(Map<K,V> map,Collection<K> remove) {
        Map<K,V> removeMap = new HashMap<K,V>();
        for(Map.Entry<K,V> property : map.entrySet())
            if(!remove.contains(property.getKey()))
                removeMap.put(property.getKey(),property.getValue());
        return removeMap;
    }

    public static <K,V> Map<K,V> removeKey(Map<K,V> map,K remove) {
        Map<K,V> removeMap = new HashMap<K,V>();
        for(Map.Entry<K,V> property : map.entrySet())
            if(!property.getKey().equals(remove))
                removeMap.put(property.getKey(),property.getValue());
        return removeMap;
    }

    public static <K> Collection<K> add(Collection<K> col,K add) {
        Collection<K> result = new ArrayList<K>(col);
        result.add(add);
        return result;
    }

    public static <K> Set<K> addSet(Set<? extends K> col,K add) {
        Set<K> result = new HashSet<K>(col);
        result.add(add);
        return result;
    }

    public static <K> List<K> add(List<K> col,K add) {
        ArrayList<K> result = new ArrayList<K>(col);
        result.add(add);
        return result;
    }

    public static <K> Collection<K> remove(Collection<? extends K> set,Collection<? extends K> remove) {
        Collection<K> result = new ArrayList<K>(set);
        result.removeAll(remove);
        return result;
    }

    public static <K> Set<K> removeSet(Set<? extends K> set,Collection<? extends K> remove) {
        Set<K> result = new HashSet<K>(set);
        result.removeAll(remove);
        return result;
    }

    public static <K> List<K> removeList(List<K> list,Collection<K> remove) {
        List<K> removeList = new ArrayList<K>();
        for(K property : list)
            if(!remove.contains(property))
                removeList.add(property);
        return removeList;
    }

    public static <K> void moveElement(List<K> list, K elemFrom, K elemTo) {

        int indFrom = list.indexOf(elemFrom);
        int indTo = list.indexOf(elemTo);

        if (indFrom == -1 || indTo == -1 || indFrom == indTo) return;
        
        boolean up = indFrom >= indTo;

        list.remove(elemFrom);
        list.add(list.indexOf(elemTo) + (up ? 0 : 1), elemFrom);
    }

    public static <K> void moveElement(List<K> list, K elemFrom, int index) {

        if (index == -1) {
            list.remove(elemFrom);
            list.add(elemFrom);
        } else {
            boolean up = list.indexOf(elemFrom) >= index;

            list.remove(elemFrom);
            list.add(index + (up ? 0 : -1), elemFrom);
        }
    }

    public static int max(int a,int b) {
        return a>b?a:b;
    }
    public static int min(int a,int b) {
        return a<b?a:b;
    }

    public static <K> Set<K> toSet(Collection<K> collection) {
        Set<K> set = new HashSet<K>();
        for(K element : collection)
            set.add(element);
        return set;
    }

    public static <B,K1 extends B,K2 extends B,V> Map<B,V> merge(Map<K1,? extends V> map1,Map<K2,? extends V> map2) {
        Map<B,V> result = new HashMap<B,V>(map1);
        result.putAll(map2);
        return result;
    }

    public static <B,K1 extends B,K2 extends B,V> Map<B,V> override(Map<K1,? extends V> map1,Map<K2,? extends V> map2) {
        return BaseUtils.<B,K1,K2,V>merge(map1, map2);
    }

    public static <K,V> boolean isSubMap(Map<? extends K,? extends V> map1,Map<K,? extends V> map2) {
        for(Map.Entry<? extends K,? extends V> entry : map1.entrySet()) {
            V value2 = map2.get(entry.getKey());
            if(!(value2!=null && hashEquals(value2,entry.getValue())))
                return false;
        }
        return true;
    }

    public static <B,V> Map<B,V> forceMerge(Map<?,? extends V> map1,Map<?,? extends V> map2) {
        Map<Object,V> result = new HashMap<Object,V>(map1);
        result.putAll(map2);
        return (Map<B,V>)result;
    }

    public static <B,K1 extends B,K2 extends B> Collection<B> merge(Collection<K1> col1,Collection<K2> col2) {
        Collection<B> result = new ArrayList<B>(col1);
        result.addAll(col2);
        return result;
    }

    public static <B,K1 extends B,K2 extends B> Set<B> mergeSet(Set<K1> set1,Set<K2> set2) {
        Set<B> result = new HashSet<B>(set1);
        result.addAll(set2);
        return result;
    }

    public static <B,K1 extends B,K2 extends B> Set<B> mergeItem(Set<K1> set,K2 item) {
        Set<B> result = new HashSet<B>(set);
        result.add(item);
        return result;
    }

    public static <B,K1 extends B,K2 extends B> List<B> mergeList(List<K1> list1,List<K2> list2) {
        List<B> result = new ArrayList<B>(list1);
        result.addAll(list2);
        return result;
    }

    public static <V,MV,EV> Map<Object,EV> mergeMaps(Map<V, EV> map, Map<MV, EV> toMerge,Map<MV, Object> mergedMap) {
        Map<Object,EV> merged = new HashMap<Object,EV>(map);
        Map<EV,Object> reversed = BaseUtils.reverse(merged);
        for(Map.Entry<MV,EV> transEntry : toMerge.entrySet()) {
            Object mergedProp = reversed.get(transEntry.getValue());
            if(mergedProp==null) {
                mergedProp = new Object();
                merged.put(mergedProp, transEntry.getValue());
            }
            mergedMap.put(transEntry.getKey(),mergedProp);
        }
        return merged;
    }

    // строит декартово произведение нескольких упорядоченных множеств
    public static <T> List<List<T>> cartesianProduct(List<List<T>> data) {
        LinkedList<List<T>> queue = new LinkedList<List<T>>();
        queue.add(new ArrayList<T>());
        final int tupleSize = data.size();
        while (!queue.isEmpty()) {
            if (queue.peekFirst().size() == tupleSize) {
                break;
            }
            List<T> queueItem = queue.removeFirst();
            final int currentTupleSize = queueItem.size();
            for (T item : data.get(currentTupleSize)) {
                List<T> newItem = new ArrayList<T>(queueItem);
                newItem.add(item);
                queue.addLast(newItem);
            }
        }
        return queue;
    }

    // ищет в Map рекурсивно тупик
    public static <K> K findDeadEnd(Map<K,K> map,K end) {
        K next = map.get(end);
        if(next==null)
            return end;
        else
            return findDeadEnd(map,end);
    }

    public static <T> boolean equalArraySets(T[] array1,T[] array2) {
        if(array1.length!=array2.length) return false;
        T[] check2 = array2.clone();
        for(T element : array1) {
            boolean found = false;
            for(int i=0;i<check2.length;i++)
                if(check2[i]!=null && BaseUtils.hashEquals(element,check2[i])) {
                    check2[i] = null;
                    found = true;
                    break;
                }
            if(!found) return false;
        }

        return true;
    }

    public static <T> int hashSet(T[] array) {
        int hash = 0;
        for(T element : array)
            hash += element.hashCode();
        return hash;
    }


    public static <T> T nvl(T value1,T value2) {
        return value1==null?value2:value1;
    }

    public static String evl(String primary,String secondary) {
        return (primary.length()==0?secondary:primary);
    }

    public static boolean hashEquals(Object obj1,Object obj2) {
        return obj1==obj2 || (obj1.hashCode()==obj2.hashCode() && obj1.equals(obj2));
    }

    public static <T> boolean contains(T[] array,T element) {
        return contains(array, element, array.length);
    }
    public static <T> boolean contains(T[] array,T element,int num) {
        for(int i=0;i<num;i++)
            if(array[i].equals(element))
                return true;
        return false;
    }

    public static <T> Set<T> toSet(T... array) {
        return new HashSet<T>(Arrays.asList(array));
    }

    public static <T> T getRandom(List<T> list,Random randomizer) {
        return list.get(randomizer.nextInt(list.size()));
    }

    public static String clause(String clause,String data) {
        return (data.length()==0?"":" "+ clause +" "+ data);
    }

    static String clause(String clause,int data) {
        return (data ==0?"":" "+ clause +" "+ data);
    }

    public static <T, K> OrderedMap<T, K> listMap(List<T> list, Map<T, K> map) {
        OrderedMap<T, K> result = new OrderedMap<T, K>();
        for (T element : list)
            result.put(element, map.get(element));
        return result;
    }

    public static <V> Map<V,V> mergeMaps(Map<V,V>[] maps) {
        Map<V, V> result = new HashMap<V, V>();
        for (Map<V, V> map : maps)
            result.putAll(map);
        return result;
    }

    public static <T> void replaceListElements(List<T> list, T from, T to) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) == from)
                list.set(i, to);
        }
    }


    public static abstract class Group<G,K> {
        public abstract G group(K key);
    }
    public static <G,K> Map<G,Collection<K>> group(Group<G, K> getter, Collection<K> keys) {
        Map<G,Collection<K>> result = new HashMap<G, Collection<K>>();        
        for(K key : keys) {
            G group = getter.group(key);
            Collection<K> groupList = result.get(group);
            if(groupList==null) {
                groupList = new ArrayList<K>();
                result.put(group,groupList);
            }
            groupList.add(key);
        }
        return result;
    }

    public static <G,K> Map<G,List<K>> groupList(Group<G, K> getter, List<K> keys) {
        Map<G,List<K>> result = new HashMap<G, List<K>>();
        for(K key : keys) {
            G group = getter.group(key);
            List<K> groupList = result.get(group);
            if(groupList==null) {
                groupList = new ArrayList<K>();
                result.put(group,groupList);
            }
            groupList.add(key);
        }
        return result;
    }

    public static <G,K> Map<G,Set<K>> groupSet(Group<G, K> getter, Set<K> keys) {
        Map<G,Set<K>> result = new HashMap<G, Set<K>>();        
        for(K key : keys) {
            G group = getter.group(key);
            Set<K> groupSet = result.get(group);
            if(groupSet==null) {
                groupSet = new HashSet<K>();
                result.put(group,groupSet);
            }
            groupSet.add(key);
        }
        return result;
    }

    public static <G,K> Map<G,Set<K>> groupSet(final Map<K, G> getter, Set<K> keys) {
        return groupSet(new Group<G,K>() {
            public G group(K key) {
                return getter.get(key);
            }
        },keys);
    }

    public static <G,K> Map<G,List<K>> groupList(final Map<K, G> getter, List<K> keys) {
        return groupList(new Group<G,K>() {
            public G group(K key) {
                return getter.get(key);
            }
        },keys);
    }

    public static <G,K> Map<G,Set<K>> groupSet(final Map<K, G> getter) {
        return groupSet(getter, getter.keySet());
    }

    public static <G,K> Map<G,List<K>> groupList(final OrderedMap<K, G> getter) {
        return groupList(getter, getter.keyList());
    }

    public static <K> Map<K,Integer> multiSet(Collection<K> col) {
        Map<K,Integer> result = new HashMap<K, Integer>();
        for(K element : col) {
            Integer quantity = result.get(element);
            result.put(element,quantity==null?1:quantity+1);
        }
        return result;
    }

    public static <V> V addValue(Map<V,V> values,V value) {
        V addValue = values.get(value); // смотрим может уже есть
        if(addValue==null) { // если нету, находим рекурсивно первое свободное значение
            addValue = BaseUtils.findDeadEnd(BaseUtils.reverse(values), value);
            values.put(value,addValue);
        }
        return addValue;
    }

    public static <K,V> void putNotNull(K key, Map<K,V> from, Map<K,V> to) {
        V value = from.get(key);
        if(value!=null) to.put(key,value);
    }

    public static class Paired<T> {
        public final T[] common;

        private final T[] diff1;
        private final T[] diff2;
        private final boolean invert;

        public T[] getDiff1() { return invert?diff2:diff1; }
        public T[] getDiff2() { return invert?diff1:diff2; }

        public Paired(T[] array1,T[] array2, ArrayInstancer<T> instancer) {
            if(array1.length>array2.length) {
                T[] sw = array2; array2 = array1; array1 = sw; invert = true;
            } else
                invert = false;
            assert array1.length<=array2.length;
            T[] pairedWheres = instancer.newArray(array1.length); int pairs = 0;
            T[] thisWheres = instancer.newArray(array1.length); int thisnum = 0;
            T[] pairedThatWheres = array2.clone();
            for(T opWhere : array1) {
                boolean paired = false;
                for(int i=0;i<pairedThatWheres.length;i++)
                    if(pairedThatWheres[i]!=null && hashEquals(array2[i],opWhere)) {
                        pairedWheres[pairs++] = opWhere;
                        pairedThatWheres[i] = null;
                        paired = true;
                        break;
                    }
                if(!paired) thisWheres[thisnum++] = opWhere;
            }

            if(pairs==0) {
                common = instancer.newArray(0);
                diff1 = array1;
                diff2 = array2;
            } else {
                if(pairs==array1.length) {
                    common = array1;
                    diff1 = instancer.newArray(0);
                } else {
                    common = instancer.newArray(pairs); System.arraycopy(pairedWheres,0,common,0,pairs);
                    diff1 = instancer.newArray(thisnum); System.arraycopy(thisWheres,0,diff1,0,thisnum);
                }

                if(pairs==array2.length)
                    diff2 = diff1;
                else {
                    diff2 = instancer.newArray(array2.length-pairs); int compiledNum = 0;
                    for(T opWhere : pairedThatWheres)
                        if(opWhere!=null) diff2[compiledNum++] = opWhere;
                }
            }
        }
    }

    public static <K,V> boolean hashContainsValue(Map<K,V> map, V value) {
        for(Map.Entry<K,V> entry : map.entrySet())
            if(hashEquals(entry.getValue(),value))
                return true;
        return false;
    }

    public static <K> String toString(Collection<K> array, String separator) {
        String result = "";
        for(K element : array)
            result = (result.length()==0?"":result+separator) + element;
        return result;
    }
    public static <K> String toString(K[] array, String separator) {
        String result = "";
        for(K element : array)
            result = (result.length()==0?"":result+separator) + element;
        return result;
    }

    public static <K,V> boolean containsAll(Map<K,V> map, Map<K,V> contains) {
        for(Map.Entry<K,V> entry : contains.entrySet())
            if(!entry.getValue().equals(map.get(entry.getKey())))
                return false;
        return true;
    }

    public static <K> Map<K,String> mapString(Collection<K> col) {
        Map<K,String> result = new HashMap<K, String>();
        for(K element : col)
            result.put(element,element.toString());
        return result;
    }

    public static Object[] add(Object[] array1, Object element) {
        return add(array1, new Object[] { element });
    }

    public static Object[] add(Object[] array1,Object[] array2) {
        return add(array1,array2,objectInstancer);
    }

    public static Object[] add(List<Object[]> list) {
        int totLength = 0;
        for(Object[] array : list)
            totLength += array.length;
        Object[] result = new Object[totLength];
        int off = 0;
        for(Object[] array : list)
            for(Object object : array)
                result[off++] = object;
        return result;        
    }

    public final static ArrayInstancer<Object> objectInstancer = new ArrayInstancer<Object>() {
        public Object[] newArray(int size) {
            return new Object[size];
        }
    };

    public static <T> T[] add(T[] array1,T[] array2,ArrayInstancer<T> instancer) {
        T[] result = instancer.newArray(array1.length+array2.length);
        System.arraycopy(array1,0,result,0,array1.length);
        System.arraycopy(array2,0,result,array1.length,array2.length);
        return result;
    }

    public static boolean isData(Object object) {
        return object instanceof Number || object instanceof String || object instanceof Boolean || object instanceof byte[];
    }

    public static <I,E extends I> List<E> immutableCast(List<I> list) {
        return (List<E>)(List<? extends I>)list;        
    }

    public static <K,I,E extends I> Map<K,E> immutableCast(Map<K,I> map) {
        return (Map<K,E>)(Map<K,? extends I>)map;        
    }

    public static <K,I> I immutableCast(K object) {
        return (I)(Object)object;        
    }

    public static <I> I single(Collection<I> col) {
        assert col.size()==1;
        return col.iterator().next();
    }

    public static <I> I singleKey(Map<I,?> map) {
        return BaseUtils.single(map.keySet());
    }

    public static <I> I singleValue(Map<?,I> map) {
        return BaseUtils.single(map.values());
    }

    public static <K,I> Map.Entry<K,I> singleEntry(Map<K,I> map) {
        return BaseUtils.single(map.entrySet());
    }

    private static <K> void reverse(Iterator<K> i, List<K> result) {
        if(i.hasNext()) {
            K item = i.next();
            reverse(i,result);
            result.add(item);
        }
    }

    public static <K> List<K> reverse(List<K> col) {
        List<K> result = new ArrayList<K>();
        reverse(col.iterator(),result);
        return result;
    }

    public static int objectToInt(Integer value) {
        if(value==null)
            return -1;
        else
            return value;
    }

    public static Integer intToObject(int value) {
        if(value==-1)
            return null;
        else
            return value;
    }

    public static String nullToString(Object name) {
        if(name==null)
            return "";
        else
            return name.toString();
    }

    public static <K,V> Map<K,V> buildMap(Collection<K> col1, Collection<V> col2) {
        assert col1.size() == col2.size();

        Iterator<K> it1 = col1.iterator();
        Iterator<V> it2 = col2.iterator();
        Map<K,V> result = new HashMap<K, V>();
        while(it1.hasNext())
            result.put(it1.next(),it2.next());
        return result;
    }

    public static <K> List<K> toList(K... elements) {
        List<K> list = new ArrayList<K>();
        Collections.addAll(list,elements);
        return list;
    }

    public static String replicate(char character, int length) {

        char[] chars = new char[length];
        Arrays.fill(chars, character);
        return new String(chars);
    }

    public static String padRight(String s, int n) {
        return String.format("%1$-" + n + "s", s);
    }

    public static String padLeft(String s, int n) {
        return String.format("%1$#" + n + "s", s);
    }

    public static String capitalize(String s) {
        return s.substring(0,1).toUpperCase() + s.substring(1);
    }

    public static Method getSingleMethod(Object object, String method, int paramCount) {
        for(Method methodObject : object.getClass().getMethods())
            if(methodObject.getName().equals(method) && (paramCount == -1 || methodObject.getParameterTypes().length == paramCount))
                return methodObject;
        throw new RuntimeException("no single method");
    }

    public static Method getSingleMethod(Object object, String method) {
        return getSingleMethod(object, method, -1);
    }

    public static void invokeCheckSetter(Object object, String field, Object set) {
        if(!nullEquals(invokeGetter(object, field), set))
            invokeSetter(object, field, set);
    }

    public static void invokeSetter(Object object, String field, Object set) {
        try {
            getSingleMethod(object, "set"+BaseUtils.capitalize(field), 1).invoke(object, set);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Object invokeGetter(Object object, String field) {
        try {
            Method method = object.getClass().getMethod("get"+BaseUtils.capitalize(field));
            return method.invoke(object);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void invokeAdder(Object object, String field, Object add) {
        try {
            getSingleMethod(object, "addTo"+BaseUtils.capitalize(field), 1).invoke(object, add);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void invokeRemover(Object object, String field, Object add) {
        try {
            getSingleMethod(object, "removeFrom"+BaseUtils.capitalize(field), 1).invoke(object, add);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isRedundantString(String s) {
        return s == null || s.trim().length() == 0;
    }

    public static boolean isRedundantString(Object o) {
        return o == null || o.toString().trim().length() == 0;        
    }

    public static String spaces(int spaces) {
        String result = "";
        for(int i=0;i<spaces;i++)
            result += ' ';
        return result;
    }

    // в отличии от padright дает нуж
    public static String padr(String string, int length) {
        if(length>string.length())
            return string + spaces(length-string.length());
        else
            return string.substring(0, length);
    }

    public static String padl(String string, int length) {
        if(length>string.length())
            return spaces(length-string.length()) + string;
        else
            return string.substring(string.length()-length, string.length());
    }

    public static <K> K last(List<K> list) {
        if(list.size() > 0)
            return list.get(list.size()-1);
        else
            return null;
    }

    public static <K> List<K> copyTreeChildren(Vector children) {
        List<K> result = new ArrayList<K>();
        if(children!=null)
            for(Object child : children)
                result.add((K) child);
        return result;
    }
}
