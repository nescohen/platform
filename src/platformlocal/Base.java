/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platformlocal;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.*;

/**
 *
 * @author ME2
 */

class SetBuilder<T> {

    void RecFillSubSetList(List<T> BuildSet,int Current,List<List<T>> Result,ArrayList<T> CurrentSet) {
        if(Current>=BuildSet.size()) {
            Result.add((List<T>)CurrentSet.clone());
            return;
        }
        
        RecFillSubSetList(BuildSet,Current+1,Result,CurrentSet);
        CurrentSet.add(BuildSet.get(Current));
        RecFillSubSetList(BuildSet,Current+1,Result,CurrentSet);
        CurrentSet.remove(BuildSet.get(Current));
    }
    
    // строит список подмн-в в лексикографическом порядке
    List<List<T>> BuildSubSetList(List<T> BuildSet) {

        List<List<T>> Result = new ArrayList();
        RecFillSubSetList(BuildSet,0,Result,new ArrayList());
        return Result;
    }
}

class MapBuilder<T,V> {
    
    void RecBuildMap(T[] From,V[] To,int iFr,List<Map<T,V>> Result,HashMap<T,V> CurrentMap) {
        if(iFr==From.length) {
            Result.add((Map<T,V>)CurrentMap.clone());
            return;
        }

        for(int v=0;v<To.length;v++)
            if(!CurrentMap.containsValue(To[v])){
                CurrentMap.put(From[iFr],To[v]);
                RecBuildMap(From,To,iFr+1,Result,CurrentMap);
                CurrentMap.remove(From[iFr]);
            }
    }
    
    List<Map<T,V>> BuildMap(T[] From,V[] To) {
        List<Map<T,V>> Result = new ArrayList<Map<T,V>>();
        RecBuildMap(From,To,0,Result,new HashMap<T,V>(0));
        return Result;
    }            
}

class MapUtils<T,V> {
    
    public T getKey(Map<T,V> m, V v) {
        
        Iterator<T> it = m.keySet().iterator();
        while (it.hasNext()) {
           T t = it.next();
           if (m.get(t) == v) return t;
        }
        return null;
        
    }
    
}

class Pair<Class1, Class2> {

    Class1 first;
    Class2 second;

    public Pair(Class1 ifirst, Class2 isecond) {
        first = ifirst;
        second = isecond;
    }

    public String toString() { return first.toString(); }

}

class DateConverter {

    public static int dateToInt(Date date) {

        if (date == null) return 0;

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.YEAR) * 10000 + calendar.get(Calendar.MONTH) * 100 + calendar.get(Calendar.DATE);       
    }

    public static Date intToDate(int num) {

        if (num == 0) return null;
        
        Calendar calendar = Calendar.getInstance();
        calendar.set(num / 10000, (num / 100) % 100, num % 100);
        return calendar.getTime();
    }


}