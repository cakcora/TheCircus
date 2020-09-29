import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;

public class ThebesExperiment3 {

    private static final Logger logger = LoggerFactory.getLogger(ThebesExperiment3.class);
    static int txLength = 64;
    static int window = 1;
    static double bitcoin = Math.pow(10, 8);
    static double lower = 0.3d;
    public static void main(String args[]) throws IOException {


        String dataDir = args[0];


        HashMap<Long, Long> outputs = new HashMap<>();


        for (int year = 2014; year <= 2018; year++) {
            for (int month = 1; month <= 12; month++) {
                String key = year + "_" + month;
                String fPath = dataDir + "outputs" + key + ".txt";
                System.out.println(fPath);
                if (!new File(fPath).exists()) continue;
                addDayOutputs(outputs,fPath);

            }

        }
        for(long k:outputs.keySet()){
            if(outputs.get(k)>1000)
                System.out.println(k+"\t"+outputs.get(k));
        }

    }


    public static void addDayOutputs(  HashMap<Long, Long> outputs, String fPath ) throws IOException {

        String line;
        BufferedReader br = new BufferedReader(new FileReader(fPath));

        while ((line = br.readLine()) != null) {
            String arr[] = line.split("\t");
            if ((arr.length-2) % 2 != 0) continue;
            String tx = arr[1];
            if (tx.length() != txLength) {
                System.out.println("Wrong tx id" + tx);
            }
            //address to the graph

            for (int j = 2; j < arr.length; j = j + 2) {
                String address = arr[j];
                if (!address.equalsIgnoreCase("noaddress")) {
                    int index = (j - 2) / 2;
                    long amo = Long.parseLong(arr[j + 1]);
                    if(!outputs.containsKey(amo))
                        outputs.put(amo,0l);
                    outputs.put(amo, outputs.get(amo)+1);
                }
            }

        }
    }


}
