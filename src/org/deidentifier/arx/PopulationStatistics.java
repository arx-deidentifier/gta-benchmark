package org.deidentifier.arx;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cern.colt.Arrays;

public class PopulationStatistics {

    /** [dimension][value->id]*/
    private static final Map<String, Integer>[] vocabulary = loadVocabulary();
    /** (id, id, id, id)->group-size*/
    private static final Map<List<Integer>, Double> statistics = loadGroupSizes();

    private static Map<List<Integer>, Double> loadGroupSizes() {
        
        Map<List<Integer>, Double> result = new HashMap<>();
        
        Data data = null;
        try {
            // Dimension->ID->Value
            data = Data.create("data/census2010_race5_tn.csv", Charset.defaultCharset(), ';');
            DataHandle handle = data.getHandle();
            for (int i=0; i<handle.getNumRows(); i++) {
                List<Integer> record = new ArrayList<Integer>();
                record.add(Integer.valueOf(handle.getValue(i, 0)));
                record.add(Integer.valueOf(handle.getValue(i, 1)));
                record.add(Integer.valueOf(handle.getValue(i, 2)));
                record.add(Integer.valueOf(handle.getValue(i, 3)));
                double size = Double.valueOf(handle.getValue(i, 4));
                result.put(record, size);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return result;
    }
    private static Map<String, Integer>[] loadVocabulary() {
        
        @SuppressWarnings("unchecked")
        Map<String, Integer>[] result = new Map[4];
        result[0] = new HashMap<String, Integer>();
        result[1] = new HashMap<String, Integer>();
        result[2] = new HashMap<String, Integer>();
        result[3] = new HashMap<String, Integer>();
        
        Data data = null;
        try {
            // Dimension->ID->Value
            data = Data.create("data/vocabrace5tnzip.csv", Charset.defaultCharset(), ';');
            DataHandle handle = data.getHandle();
            for (int i=0; i<handle.getNumRows(); i++) {
                int dimension = Integer.valueOf(handle.getValue(i, 0));
                int id = Integer.valueOf(handle.getValue(i, 1));
                String value = handle.getValue(i, 2);
                result[dimension].put(value, id);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return result;
    }
    
    public static double getSize(String[] record) {
        List<Integer> key = new ArrayList<>();
        key.add(vocabulary[0].get(record[3]));
        key.add(vocabulary[1].get(record[0]));
        key.add(vocabulary[2].get(record[2]));
        key.add(vocabulary[3].get(record[1]));

        if (!statistics.containsKey(key)) {
            //System.out.println(Arrays.toString(record) + "-> NOT FOUND!");
            return 0d;
        } 
        return statistics.get(key);
    }
    public static double load() {
        return vocabulary[0].values().iterator().next() + statistics.values().iterator().next();
    }
}
