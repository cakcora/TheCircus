import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

public class ThebesExperiment2 {

    private static final Logger logger = LoggerFactory.getLogger(ThebesExperiment2.class);
    static int txLength = 64;
    static int window = 1;
    static double bitcoin = Math.pow(10, 8);
    static double lower = 0.3d;

    public static void main(String args[]) throws IOException {


        String dataDir = args[0];
        String heistaddressfile = args[1];
        System.out.println(heistaddressfile);
        DirectedSparseGraph<String, WeightedEdge> graph = new DirectedSparseGraph();

        HashMap<String, String> suspAdd = addVirusFile(heistaddressfile);
        HashSet<String> nodeIDs =new HashSet<String>();
        Set<String> nodesOfInterest = suspAdd.keySet();
        for(String n:nodesOfInterest){
            graph.addVertex(n);
            nodeIDs.add(n);
        }
        System.out.println(nodeIDs.size() + " addresses");

        for(int hop=1;hop<4;hop++) {
            HashMap<Output, String> outputs = new HashMap<>();
            for (int year = 2010; year < 2017; year++) {
                for (int month = 1; month <= 12; month++) {
                    String key = year + "_" + month;
                    String fPath = dataDir + "outputs" + key + ".txt";
                    if (!new File(fPath).exists()) continue;
                    addMonthOutputs(graph, outputs, nodeIDs, fPath);
                    System.out.println(year+"\t"+month+"\toutput hop"+hop+":"+graph.getVertexCount()+"\t"+graph.getEdgeCount());
                }

            }
            for (int year = 2010; year < 2017; year++) {
                for (int month = 1; month <= 12; month++) {
                    String key = year + "_" + month;
                    String oPath = dataDir + "inputs" + key + ".txt";
                    if (!new File(oPath).exists()) continue;
                    addMonthInputs(graph, outputs, nodeIDs, oPath);
                    System.out.println(year+"\t"+month+"\tinput hop"+hop+":"+graph.getVertexCount()+"\t"+graph.getEdgeCount());
                }

            }
            System.out.println("\thop"+hop+":"+graph.getVertexCount()+"\t"+graph.getEdgeCount());
           // System.out.println(graph.getVertices().toString());
            nodeIDs.addAll(graph.getVertices());
        }
        filterEdges(graph);
        System.out.println("Filtered\t"+graph.getVertexCount()+"\t"+graph.getEdgeCount());

    }

    static HashMap<String, String> addVirusFile(String suspAddFile) throws IOException {
        HashMap<String, String> suspAdd = new HashMap<>();
        String line = "";

        BufferedReader br = new BufferedReader(new FileReader(suspAddFile));
        while ((line = br.readLine()) != null) {
            String[] split = line.split("\t");
            String add = split[0];
            String family = split[1];
            suspAdd.put(add, family);
        }
        return suspAdd;
    }

    static void filterEdges(DirectedSparseGraph<String, WeightedEdge> graph) {
        for (WeightedEdge edge : new HashSet<>(graph.getEdges())) {
            if (edge.getAmount() < lower * bitcoin) {
                graph.removeEdge(edge);
            }
        }
        for (String node : new HashSet<>(graph.getVertices())) {
            if (graph.getNeighborCount(node) == 0) {
                graph.removeVertex(node);
            }
        }
    }



    public static HashMap<String, Integer> read(String suspAddFile) throws IOException {
        String line = "";
        HashMap<String, Integer> ad = new HashMap<>();
        BufferedReader br = new BufferedReader(new FileReader(suspAddFile));
        br.readLine();//header
        while ((line = br.readLine()) != null) {
            String[] arr = line.split("\t");
            String add = arr[4];
            String family = arr[1];
            if (family.equalsIgnoreCase("locky"))
                ad.put(add, 1);
            else if (family.equalsIgnoreCase("cerber")) {
                ad.put(add, 2);
            }
        }
        return ad;
    }



    public static void addMonthInputs(DirectedSparseGraph<String,WeightedEdge> graph, HashMap<Output, String> outputs,
                                      HashSet<String> nodeIDds, String oPath
                                      ) throws IOException {
        String line;
        BufferedReader br = new BufferedReader(new FileReader(oPath));
        int edgeNumber = graph.getEdgeCount();
        while ((line = br.readLine()) != null) {
            String arr[] = line.split("\t");
            String tx = arr[1];
            for (int j = 2; j < arr.length; j = j + 2) {
                String prevTx = arr[j];

                if (tx.length() != txLength || prevTx.length() != txLength) {
                    System.out.println("Tx id appears to be less than " + txLength + " chars in this line: " + line);
                    continue;
                }

                int index = Integer.parseInt(arr[j + 1]);
                Output key = new Output(prevTx, index);

                String address;
                if (outputs.containsKey(key)) {
                    address = outputs.get(key);
                    long amo = key.getAmount();
                    WeightedEdge edge1 = new WeightedEdge(amo, edgeNumber);
                    if (graph.addEdge(edge1, address, tx)) {
                        edgeNumber++;
                    }
                }
            }
        }
    }

    public static void addMonthOutputs(DirectedSparseGraph graph, HashMap<Output, String> outputs,
                                       HashSet<String> nodeIDs, String fPath) throws IOException {

        String line;
        BufferedReader br = new BufferedReader(new FileReader(fPath));
        int nodeCount = graph.getVertexCount();
        int edgeNumber = graph.getEdgeCount();
        while ((line = br.readLine()) != null) {
            String arr[] = line.split("\t");
            if ((arr.length - 2) % 2 != 0) continue;
            String tx = arr[1];
            if (tx.length() != txLength) {
                System.out.println("Wrong tx id" + tx);
                continue;
            }

            boolean txToRecord = nodeIDs.contains(tx);
            for (int j = 2; j < arr.length; j = j + 2) {
                String address = arr[j];
                if (!address.equalsIgnoreCase("noaddress")) {
                    int index = (j - 2) / 2;
                    long amo = Long.parseLong(arr[j + 1]);

                    Output out = new Output(tx, address, index, amo);
                    //add edges to output addresses to the graph
                    if (nodeIDs.contains(address)) {
                        graph.addVertex(tx);
                        WeightedEdge edge = new WeightedEdge(amo, edgeNumber);
                        if (graph.addEdge(edge, tx, (address))) {
                            edgeNumber++;
                        }
                        outputs.put(out,address);
//                        System.out.println("adding edge from tx to address of interest"+tx+"->"+address);
                    }
                    //add edges from tx output addresses to the graph
                    if(txToRecord){
                        graph.addVertex(address);
                        WeightedEdge edge = new WeightedEdge(amo, edgeNumber);
                        if (graph.addEdge(edge, tx, (address))) {
                            edgeNumber++;
                        }
//                        System.out.println("adding edge from tx of interest to address"+tx+"->"+address);
                    }
                }
            }


        }
    }

}
