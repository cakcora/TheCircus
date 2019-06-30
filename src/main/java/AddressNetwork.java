import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;

public class AddressNetwork {

    private static final Logger logger = LoggerFactory.getLogger(AddressNetwork.class);
    static int txLength = 64;
    public static void main(String args[]) throws IOException {

        String dataDir = args[0];
        String suspAddFile = args[1];
        String virFile2=args[3];
        String virFile3=args[4];
        HashMap<Output, Integer> outputs = new HashMap<>();
        HashMap<String, Integer> suspAdd = Windowed.read(suspAddFile);
        Windowed.addVirusFile(suspAdd,virFile2);
        Windowed.addVirusFile(suspAdd,virFile3);
        BiMap<String, Integer> nodeIDs = HashBiMap.create();
        System.out.println(suspAdd.size()+" addresses");
        DirectedSparseGraph<String, WeightedEdge> graph = new DirectedSparseGraph();
        for (int year = 2009; year <= 2018; year++) {
            for (int day = 1; day <= 366; day++) {

                String key = year + "_" + day;
                String fPath = dataDir + "outputs" + key + ".txt";
                System.out.println(fPath);
                if (!new File(fPath).exists()) continue;
                addDayOutputs(graph, outputs, nodeIDs, fPath,suspAdd);
//                System.out.println(graph.getVertexCount()+" "+graph.getEdgeCount());
            }
        }

        for (int year = 2009; year <= 2018; year++) {
            for (int day = 1; day <= 366; day++) {
                String key = year + "_" + day;
                String fPath = dataDir + "outputs" + key + ".txt";
                System.out.println(fPath);
                if (!new File(fPath).exists()) continue;
                String oPath = dataDir + "inputs" + key + ".txt";
                addDayInputs(graph, outputs,  oPath);
//                System.out.println(graph.getVertexCount()+" "+graph.getEdgeCount());
            }
        }
        BiMap<Integer, String> inverse = nodeIDs.inverse();
        for(String vertex: graph.getVertices()){
            if (vertex.length() != txLength) {
                boolean f=false;
                  for( String v2: graph.getPredecessors(vertex)){
                      if(graph.getPredecessorCount(v2)!=0){
                          f=true;
                      }
                  }
                  if(!f){
                      //a virus address sent money to this address, this is not level 1
                      System.out.println(inverse.get(Integer.parseInt(vertex))+" initial payment ");
                  }
                  else System.out.println(inverse.get(Integer.parseInt(vertex))+" next layers");
            }
        }
    }




    public static void addDayInputs(DirectedSparseGraph<String, WeightedEdge> graph, HashMap<Output, Integer> outputs,  String oPath ) throws IOException {
        String line;
        BufferedReader br = new BufferedReader(new FileReader(oPath));
        int edgeNumber = graph.getEdgeCount();
        while ((line = br.readLine()) != null) {
            String arr[] = line.split("\t");
            String tx = arr[2];
            for (int j = 3; j < arr.length; j = j + 2) {
                String prevTx = arr[j];

                if (tx.length() != txLength || prevTx.length() != txLength) {
                    logger.error("Tx id appears to be less than " + txLength + " chars in this line: " + line);
                    continue;
                }


                int index = Integer.parseInt(arr[j + 1]);
                Output key = new Output(prevTx, index);
                long amo;
                int nID;
                if (outputs.containsKey(key)) {
                    nID = outputs.get(key);
                    amo = ((WeightedEdge) graph.findEdge(prevTx, nID+"")).getAmount();

                    WeightedEdge edge1 = new WeightedEdge(amo, edgeNumber);
                    if (graph.addEdge(edge1, nID+"", tx)) {
                        edgeNumber++;
                    }
                } else {
                     //do not do anything, this output was not done to a virus address
                }
            }
        }
    }

    public static void addDayOutputs(DirectedSparseGraph<String, WeightedEdge> graph, HashMap<Output, Integer> outputs, BiMap<String, Integer> nodeIDds, String fPath, HashMap<String, Integer> suspAdd) throws IOException {

        String line;
        BufferedReader br = new BufferedReader(new FileReader(fPath));
        int nodeCount = graph.getVertexCount();
        int edgeNumber = graph.getEdgeCount();
        while ((line = br.readLine()) != null) {
            String arr[] = line.split("\t");
            if ((arr.length-1) % 2 != 0) continue;
            String tx = arr[2];
            if (tx.length() != txLength) {
                logger.error("Wrong tx id" + tx);
                System.exit(1);
            }
            //address to the graph


            for (int j = 3; j < arr.length; j = j + 2) {
                String address = arr[j].trim();
                if (!address.equalsIgnoreCase("noaddress")) {
                    int index = (j - 2) / 2;
                    long amo = Long.parseLong(arr[j + 1]);

                    if(suspAdd.containsKey(address)) {

                        graph.addVertex(tx);
                        Output out = new Output(tx, address, index, amo);

                        if (!nodeIDds.containsKey(address)) {
                            graph.addVertex(nodeCount+"");
                            nodeIDds.put(address, nodeCount);
                            nodeCount++;
                        }
                        int addID = nodeIDds.get(address);

                        WeightedEdge edge = new WeightedEdge(amo, edgeNumber);
                        if (graph.addEdge(edge, tx, addID+"")) {
                            edgeNumber++;
                        }
                        outputs.put(out, addID);
                    }
                }
            }

        }
    }










}
