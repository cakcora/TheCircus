import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import edu.uci.ics.jung.graph.util.Pair;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

/* Experiment 4B
THis file will implement a unified graph from suspicious and virus addresses.
It uses results from the r code 3A that contains a list of suspicious addresses.
 */
public class UnifiedGraph {

    private static final Logger logger = LoggerFactory.getLogger(UnifiedGraph.class);

    public static void main(String args[]) throws IOException {

        String dataDir = args[0];
        String dir=args[2];
        //all addresses are created by the virusreader class
        String virAddressFile = "allAddresses.txt";
         HashSet<String>  virusAddresses = read(virAddressFile);

        HashMap<String, double[]> hyperMap = new HashMap<String, double[]>();
        hyperMap.put("montrealCryptoLocker",new double[]{0.9,0.2,0.65});
        hyperMap.put("paduaCryptoWall",new double[]{0.9,0.5,0.8});
        hyperMap.put("montrealCryptXXX",new double[]{0.7,0.05,0.95});
        hyperMap.put("princetonLocky",new double[]{0.9,0.95,0.65});
        hyperMap.put("princetonCerber",new double[]{0.9,0.05,0.35});
        HashSet<String> susAddresses = new HashSet<>();
        HashSet<String> virTestAddresses = new HashSet<>();
        for(String virus:hyperMap.keySet()) {
            double arr[]=hyperMap.get(virus);
            double q = arr[0], e1VirusSize = arr[2], e2TotalSize = arr[1];

            HashSet<String> suspicious = getAddressesFromTestData(args[1], virus, q, e1VirusSize, e2TotalSize,6);
            HashSet<String> viruses = getAddressesFromTestData(args[1], virus, q, e1VirusSize, e2TotalSize,7);
            if (suspicious.isEmpty()||viruses.isEmpty()) {
                System.out.println(virus +" test data was empty.");
//                System.exit(1);
            }
            System.out.println(virus+" has "+viruses.size()+" addresses.");
            susAddresses.addAll(suspicious);
            virTestAddresses.addAll(viruses);
        }
        System.out.println("total number of suspicious addresses:" + susAddresses.size());
        System.out.println("total number of current virus addresses:" + virTestAddresses.size());
        UnifiedGraph hr = new UnifiedGraph();

                System.out.println("total number of virus addresses:" + virusAddresses.size());
        UndirectedSparseGraph<String,Long> graph = new UndirectedSparseGraph<>();
        for (int year = 2011; year <= 2018; year++) {
            System.out.println(year);
              graph = hr.detectForYear(dataDir, virusAddresses, susAddresses, hr, year,graph );
        }
        BufferedWriter wr = new BufferedWriter(new FileWriter(dir+"4BunifiedGraph.csv"));
        for(Long e:graph.getEdges()){
            Pair<String> nodes = graph.getEndpoints(e);
            String node1 = nodes.getFirst();
            if(virusAddresses.contains(node1))node1="v"+node1;
            else node1="s"+node1;
            String node2=nodes.getSecond();
            if(virusAddresses.contains(node2))node2="v"+node2;
            else node2="s"+node2;
            wr.write(node1+"\t"+node2+"\r\n");
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

    private UndirectedSparseGraph detectForYear(String dataDir, HashSet<String> virusAddresses, HashSet<String> susAddresses, UnifiedGraph hr, int year, UndirectedSparseGraph<String, Long> graph) throws IOException {
        HashMap<String, String> outputs = new HashMap<String, String>();
        HashMap<String, String> inputs = new HashMap<String, String>();
        HashMap<String, String> txDates = new HashMap<String, String>();
        hr.addTxOutputs(dataDir, outputs,year);
        hr.readInputs(dataDir, inputs, txDates,year);

        for(String prevTxOutput:inputs.keySet()){

            if(outputs.containsKey(prevTxOutput)){
                String tx1= prevTxOutput.substring(0,prevTxOutput.indexOf("_"));
                String tx2 = inputs.get(prevTxOutput);
                String address = outputs.get(prevTxOutput);
                //coins from tx1 go into address, and then the address spend them in tx2
                if(virusAddresses.contains(address)||susAddresses.contains(address)) {
                    graph.addVertex(tx1);
                    graph.addVertex(tx2);
                    graph.addVertex(address);
                    graph.addEdge(graph.getEdgeCount()+1l, tx1, address);
                    graph.addEdge(graph.getEdgeCount()+1l, address, tx2);
                }
            }
        }
        inputs.clear();
        outputs.clear();
        System.out.println(graph.getVertexCount()+ " "+graph.getEdgeCount());
        return graph;
    }



    private void readInputs(String dataDir, HashMap<String, String> addressInputList, HashMap<String, String> txDates,int year) throws IOException {
        String line;

        for (int day = 1; day <= 365; day++) {
            String fPath=dataDir+"inputs"+year+"_"+day+".txt";

            if (!new File(fPath).exists()) continue;
            BufferedReader br = new BufferedReader(new FileReader(fPath));
            if(day%365==0) System.out.println(fPath);
            while ((line = br.readLine()) != null) {
                String arr[] = line.split("\t");
                String tx = arr[2];
                for (int j = 3; j < arr.length; j = j + 2) {
                    String prevTx = arr[j];
                    int index = Integer.parseInt(arr[j + 1]);
                    addressInputList.put((prevTx+"_"+index),tx);
                    txDates.put(tx,year+"_"+day);
                }
            }
        }
    }



    private void addTxOutputs(String dataDir, HashMap<String, String> outputs,int year) throws IOException {
        String line;

        for (int day = 1; day <= 365; day++) {
            String fPath=dataDir+"outputs"+year+"_"+day+".txt";
            if (!new File(fPath).exists()) continue;
            BufferedReader br = new BufferedReader(new FileReader(fPath));
            if(day%365==0) System.out.println(fPath);
            while ((line = br.readLine()) != null) {
                String arr[] = line.split("\t");
                if ((arr.length - 1) % 2 != 0) continue;
                String tx = arr[2];


                for (int j = 3; j < arr.length; j = j + 2) {
                    String address = (arr[j]);
                    outputs.put(tx + "_" + j,address);

                }

            }
        }

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
//                 #We do not care about family in graph elimination
                addMap.add(address);


            }
        }
        return addMap;
    }




}
