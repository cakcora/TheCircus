import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

public class ChainletElimination {
    public static void main(String args[]) throws IOException {
        String dataDir = args[0];

        //all addresses are created by the virusreader class
        String virAddressFile = "allAddresses.txt";
        HashSet<String> virusAddresses = read(virAddressFile);

        HashMap<String, double[]> hyperMap = new HashMap<String, double[]>();
        hyperMap.put("montrealCryptoLocker",new double[]{0.9,0.65,0.95});
        hyperMap.put("paduaCryptoWall",new double[]{0.9,0.65,0.8});
        hyperMap.put("montrealCryptXXX",new double[]{0.7,0.35,0.35});
        hyperMap.put("princetonLocky",new double[]{0.9,0.8,0.5});
        hyperMap.put("princetonCerber",new double[]{0.9,0.35,0.5});
        HashSet<String> susAddresses = new HashSet<>();
        HashSet<String> virTestAddresses = new HashSet<>();
        for(String virus:hyperMap.keySet()) {
            double arr[]=hyperMap.get(virus);
            double q = arr[0], e1VirusSize = arr[2], e2TotalSize = arr[1];

            HashSet<String> suspicious = getAddressesFromTestData(args[1], virus, q, e1VirusSize, e2TotalSize,6);
            HashSet<String> viruses = getAddressesFromTestData(args[1], virus, q, e1VirusSize, e2TotalSize,7);
            if (suspicious.isEmpty()||viruses.isEmpty()) {
                System.out.println(virus +" test data was empty.");
            }
            System.out.println(virus+" has "+viruses.size()+" addresses.");
            susAddresses.addAll(suspicious);
            virTestAddresses.addAll(viruses);
        }
        virusAddresses.removeAll(virTestAddresses);
        System.out.println("total number of suspicious addresses:" + susAddresses.size());
        System.out.println("total number of current virus addresses:" + virTestAddresses.size());
        System.out.println("total number of past virus addresses:" + virusAddresses.size());

        BufferedWriter wr = new BufferedWriter(new FileWriter(args[2]));
        for(int year=2011;year<=2018;year++) {
            HashMap<String, String> outputs = new HashMap<String, String>();
            addTxOutputs(dataDir, outputs, year);
            HashSet<String> strings = new HashSet<>(outputs.values());
            System.out.println(year+"\t"+strings.size());
            for(String address: strings) {
                //coins from tx1 go into address, and then the address spend them in tx2
                if (susAddresses.contains(address)) {
                    wr.write("suspicious\t"+address+"\r\n");
                }
                if (virTestAddresses.contains(address)) {
                    wr.write("virus\t"+address+"\r\n");
                }
                if (virusAddresses.contains(address)) {
                    wr.write("pastvirus\t"+address+"\r\n");
                }
            }



            outputs.clear();

        }
        wr.close();
    }



    private static HashSet<String> getAddressesFromTestData(String susAddressFile, String virus, double q, double e1, double e2, int dataInd) throws IOException {
        String line = "";
        BufferedReader br = new BufferedReader(new FileReader(susAddressFile));

        HashSet<String> addresses = new HashSet<>();
        while ((line = br.readLine()) != null) {
            String arr[] = line.split("\t");
            if (line.isEmpty()) continue;
            if(!arr[0].equalsIgnoreCase(virus)) continue;
            if (Double.parseDouble(arr[3]) != q) continue;
            if (Double.parseDouble(arr[4]) != e1) continue;
            if (Double.parseDouble(arr[5]) != e2) continue;
            String[] ads = arr[dataInd].split(" ");
            addresses.addAll(Arrays.asList(ads));
        }
        return addresses;
    }

    public static HashSet<String> read(String suspAddFile) throws IOException {
        String line = "";
        HashSet<String> addMap = new HashSet<>();
        BufferedReader br = new BufferedReader(new FileReader(suspAddFile));
        br.readLine();//header
        while ((line = br.readLine()) != null) {
            String[] arr = line.split("\t");
            String address = arr[0];

            for(int i=1;i<arr.length;i++) {
                String family = arr[i];
//                 #We do not care about family in elimination
                addMap.add(address);
            }
        }
        return addMap;
    }
    static void addTxOutputs(String dataDir, HashMap<String, String> outputs, int year) throws IOException {
        String line;

        for (int day = 1; day <= 365; day++) {
            String fPath=dataDir+"outputs"+year+"_"+day+".txt";
            if (!new File(fPath).exists()) continue;
            BufferedReader br = new BufferedReader(new FileReader(fPath));
            if(day%150==0) System.out.println(fPath);
            while ((line = br.readLine()) != null) {
                String arr[] = line.split("\t");
                if ((arr.length - 1) % 2 != 0) continue;
                String tx = arr[2];
                if(arr.length<8){//length<9 implies that the transaction has one or two outputs

                    for (int j = 3; j < arr.length; j = j + 2) {
                        String address = (arr[j]);
                        outputs.put(tx + "_" + j,address);
                    }
                }
            }
        }
    }

}
