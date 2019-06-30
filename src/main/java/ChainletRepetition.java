import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;

public class ChainletRepetition {
    static int window = 1;
    static int txLength = 64;
    static int maxDim =20;
    private static final Logger logger = LoggerFactory.getLogger(ChainletRepetition.class);
    public static void main(String args[]) throws IOException {




        String dataDir = args[0];
        String suspAddFile = args[1];
        DirectedSparseGraph<Object, WeightedEdge> graph = new DirectedSparseGraph();
        HashMap<Output, Integer> outputs = new HashMap<>();
        HashMap<String, Integer> suspAdd = Windowed.read(suspAddFile);
        BiMap<String, Integer> nodeIDs = HashBiMap.create();
        BufferedWriter wr = new BufferedWriter(new FileWriter("repetition.txt"));
        long passed = 0;
        for (int year = 2016; year < 2018; year++) {
            for (int day = 49; day <= 365; day++) {
                graph = new DirectedSparseGraph();
                nodeIDs.clear();
                String fPath = dataDir + "outputs" + year + "Day" + day + ".txt";
                System.out.println(fPath);
                if (!new File(fPath).exists()) continue;
                String oPath = dataDir + "inputs" + year + "Day" + day + ".txt";
                Windowed.addDayOutputs(graph, outputs, nodeIDs, fPath );
                Windowed.addDayInputs(graph, outputs, nodeIDs, oPath);
                passed++;
                if (passed == window) {
                    System.out.println(">" + graph.getVertexCount() + " vertices, " + graph.getEdgeCount() + " edges");
                    Windowed.filterEdges(graph);
                    System.out.println(graph.getVertexCount() + " vertices, " + graph.getEdgeCount() + " edges");

                    process(wr, graph, suspAdd, nodeIDs, year, day);
                    passed = 0;
                    graph = new DirectedSparseGraph();
                    nodeIDs.clear();
                    outputs.clear();
                }

            }

        }
        wr.close();

    }

    private static void process(BufferedWriter wr, DirectedSparseGraph<Object, WeightedEdge> graph, HashMap<String, Integer> suspAdd, BiMap<String, Integer> nodeIDs, int year, int day) throws IOException {
        HashMap<Integer, Double> weights = new HashMap<>();
        HashMap<Integer, Integer> lengths = new HashMap<>();
        HashMap<Integer, HashMap<String, Integer>> counts = new HashMap<>();
        int count = 0;
        int tx = 0, add = 0;
        for (Object n1 : graph.getVertices()) {
            if (n1.toString().length() == txLength) {
                tx++;
                continue;
            }
            add++;
            int type2 =-1;
            int type=-1;

            Collection<WeightedEdge> inEdges = graph.getInEdges(n1);
            if(inEdges!=null) {

                long maxAmount = -1;
                WeightedEdge maxEdge = null;
                for (WeightedEdge edge : inEdges) {
                    long amount = edge.getAmount();
                    if (amount > maxAmount) {
                        maxEdge = edge;
                        maxAmount = amount;
                    }
                }

                Object prevTx = graph.getSource(maxEdge);
                Collection<WeightedEdge> inEdges1 = graph.getInEdges(prevTx);
                int icount = inEdges.size();
                int ocount = graph.getOutEdges(prevTx).size();
                if(icount>maxDim)icount=maxDim;
                if(ocount>maxDim)ocount=maxDim;
                type = (icount-1)*maxDim+ocount-1;
                long maxAmount2 = -1;
                WeightedEdge maxEdge2 = null;
                if (inEdges1 != null) {

                    for (WeightedEdge edge : inEdges1) {
                        long amount = edge.getAmount();
                        if (amount > maxAmount2 ) {
                            maxEdge2  = edge;
                            maxAmount2  = amount;
                        }
                    }
                    Object prevAd = graph.getSource(maxEdge2);
                    Collection<WeightedEdge> inEdges2 = graph.getInEdges(prevAd);
                    long maxAmount3 = -1;
                    WeightedEdge maxEdge3 = null;
                    if(inEdges2!=null) {
                        for (WeightedEdge edge : inEdges2) {
                            long amount = edge.getAmount();
                            if (amount > maxAmount3) {
                                maxEdge3 = edge;
                                maxAmount3 = amount;
                            }
                        }
                        Object prevtX = graph.getSource(maxEdge3);
                        int icount2 = inEdges2.size();
                        int ocount2 = graph.getOutEdges(prevtX).size();

                        if (icount2 > maxDim) icount2 = maxDim;
                        if (ocount2 > maxDim) ocount2 = maxDim;
                        type2 = (icount2 - 1) * maxDim + (ocount2 - 1);
                    }
                }
            }
            String address = nodeIDs.inverse().get(n1);
            int label = 0;
            if(suspAdd.containsKey(address))label = suspAdd.get(address);

            wr.write(year+"\t"+day+"\t"+address+"\t"+label+"\t"+type+"\t"+type2+"\r\n");

        }

    }





}
