package de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.featureextraction.spelling;

import java.util.HashMap;
import java.util.Map;

public class FeatureMap<T>
{

    private Map<T,Integer> idMap;
    private Integer currentId;
    
    public FeatureMap()
    {
        idMap = new HashMap<T, Integer>();
        currentId = 1;
    }
    
    public void register(T object) {
        if (!idMap.containsKey(object)) {
            idMap.put(object, currentId);
            currentId++;
        }
    }
    
    public Integer getId(T object) {
        if (idMap.containsKey(object)) {
            return idMap.get(object);
        }
        else {
            return -1;
        }
    }
}