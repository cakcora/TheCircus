import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

/*
THis file will implement graph elimination.
It uses results from the r code 3A that contains a list of suspicious addresses. Graph elimination will find
how distant they are from known virus addresses on the graph.
This will be done by a breadth first search on the graph, starting from known ransomware addresses.
 */
public class GraphElimination {

    private static final Logger logger = LoggerFactory.getLogger(GraphElimination.class);

    public static void main(String args[]) throws IOException {

        String dataDir = args[0];

        //all addresses are created by the virusreader class
        String virAddressFile = "allAddresses.txt";
         HashSet<String>  virusAddresses = read(virAddressFile);

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
//                System.exit(1);
            }
            System.out.println(virus+" has "+viruses.size()+" addresses.");
            susAddresses.addAll(suspicious);
            virTestAddresses.addAll(viruses);
        }
        System.out.println("total number of suspicious addresses:" + susAddresses.size());
        System.out.println("total number of current virus addresses:" + virTestAddresses.size());
        GraphElimination hr = new GraphElimination();

        virusAddresses.removeAll(virTestAddresses);
                System.out.println("total number of virus addresses:" + virusAddresses.size());
        for (int year = 2012; year <= 2018; year++) {
            System.out.println(year);
            hr.detectForYear(dataDir, virusAddresses,virTestAddresses, susAddresses, hr, year );
        }
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

    private void detectForYear(String dataDir,  HashSet<String>  virusAddresses,HashSet<String> virTestAddresses, HashSet<String> susAddresses, GraphElimination hr,   int year ) throws IOException {
        HashMap<String, String> outputs = new HashMap<String, String>();
        HashMap<String, String> inputs = new HashMap<String, String>();
        HashMap<String, String> txDates = new HashMap<String, String>();
        hr.addTxOutputs(dataDir, outputs,year);
        System.out.println(outputs.size()+" prev tx");
        hr.readInputs(dataDir, inputs, txDates,year);
        int has=0;
        int no=0;
        UndirectedSparseGraph<String,Long> graph = new UndirectedSparseGraph<>();
        long e=0;
        for(String prevTxOutput:inputs.keySet()){

            if(outputs.containsKey(prevTxOutput)){
                has++;
                String tx1= prevTxOutput.substring(0,prevTxOutput.indexOf("_"));
                String tx2 = inputs.get(prevTxOutput);
                String address = outputs.get(prevTxOutput);
                //coins from tx1 go into address, and then the address spend them in tx2
                graph.addVertex(tx1);
                graph.addVertex(tx2);
                graph.addVertex(address);
                graph.addEdge(e++,tx1,address);
                graph.addEdge(e++,address,tx2);

            }
            else no++;
        }
        inputs.clear();
        outputs.clear();
        System.out.println(graph.getVertexCount()+ " addresses in the graph.");
        HashSet<String>  startNodes = new HashSet<String>();
        startNodes.addAll(virusAddresses);
        HashMap<String,Integer> foundInWave = new HashMap<>();
        HashMap<String,Long> foundTimes = new HashMap<>();
        int wave=0;

        while(!startNodes.isEmpty()){
            System.out.println("Wave "+wave+" starts with "+startNodes.size()+" nodes.");
            HashMap<String, Long> found = GraphElimination.wave(startNodes,graph);
            for(String f:found.keySet()){
                foundInWave.put(f,wave+1);
                foundTimes.put(f,found.get(f));
            }
            for(String s:startNodes){
                graph.removeVertex(s);
            }
            startNodes=new HashSet<String>(found.keySet());
            wave++;
        }
        System.out.println(has+" "+no);

        DescriptiveStatistics ds = new DescriptiveStatistics();
        for(int s:foundInWave.values()){
            ds.addValue(s);
        }
        int [] sarr = new int[500];
        int sizeS = susAddresses.size();
        for(String s:susAddresses){
            if(foundInWave.containsKey(s)){
                int t = foundInWave.get(s);
                System.out.println(sizeS +"/"+s+" suspicious is discovered at wave "+t+" "+foundTimes.get(s)+" times");
                sarr[t]++;
            }
        }
        int size = virTestAddresses.size();
        int [] varr = new int[500];
        for(String s:virTestAddresses){
            if(foundInWave.containsKey(s)){
                int t = foundInWave.get(s);
                System.out.println(size +"/"+s+" virus is discovered at wave "+t+" "+foundTimes.get(s)+" times");
                varr[t]++;
            }
        }
        for (int i=0;i<varr.length;i++){
            if(varr[i]>0)
                System.out.println(i+" virus:"+varr[i]);
        }
        for (int i=0;i<sarr.length;i++){
            if(sarr[i]>0)
                System.out.println(i+" suspicious:"+sarr[i]);
        }
        System.out.println(ds.getMin()+" "+ds.getMean()+" "+ds.getMax()+" "+ds.getN());
    }

    public static HashMap<String, Long> wave(HashSet<String> nodes, UndirectedSparseGraph graph){
        HashMap<String,Long> newNodes = new  HashMap<String,Long>();

        for(String node:nodes){
            if(!graph.containsVertex(node))continue;
            Collection<String> transactions = graph.getNeighbors(node);
            for(String tx:transactions){

                Collection<String> neighbors = graph.getNeighbors(tx);
                for(String n:neighbors){
                    if(!newNodes.containsKey(n))newNodes.put(n,0l);
                    newNodes.put(n,1+newNodes.get(n));
                }
            }
        }
        for(String n:nodes){
            if(newNodes.containsKey(n))newNodes.remove(n);
        }
        return newNodes;
    }

    void readInputs(String dataDir, HashMap<String, String> addressInputList, HashMap<String, String> txDates, int year) throws IOException {
        String line;

        for (int day = 1; day <= 365; day++) {
            String fPath=dataDir+"inputs"+year+"_"+day+".txt";

            if (!new File(fPath).exists()) continue;
            BufferedReader br = new BufferedReader(new FileReader(fPath));
            if(day%150==0) System.out.println(fPath);
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

    void addTxOutputs(String dataDir, HashMap<String, String> outputs, int year) throws IOException {
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
