package org.deidentifier.arx;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
    
    public static void main(String[] args) throws IOException {
        @SuppressWarnings("unchecked")
        Map<Integer, String>[] inverse = new Map[4];
        for (int i = 0; i < vocabulary.length; i++) {
            Map<String, Integer> dictionary = vocabulary[i];
            inverse[i] = new HashMap<Integer, String>();
            for (Entry<String, Integer> entry : dictionary.entrySet()) {
                inverse[i].put(entry.getValue(), entry.getKey());
            }
        }
        
        BufferedWriter writer = new BufferedWriter(new FileWriter(new File("table.csv")));
        writer.write("sex;zip;age;race\n");
        
        int index = 0;
        int total = statistics.size();
        for (Entry<List<Integer>, Double> entry : statistics.entrySet()) {
            index++;
            if (index % 100 == 0) {
                System.out.println(index+"/"+total);
            }
            List<String> record = new ArrayList<String>();
            for (int i=0; i<entry.getKey().size(); i++) {
                record.add(inverse[i].get(entry.getKey().get(i)));
            }
            String line = record.get(1)+";"+ record.get(3)+";"+ record.get(2)+";"+record.get(0)+"\n";
            int count = (int)Math.ceil(entry.getValue());
            for (int j=0; j<count; j++) {
                writer.write(line);
            }
        }
        writer.close();
    }
}
