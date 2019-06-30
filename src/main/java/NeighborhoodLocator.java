import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

public class NeighborhoodLocator {

    private static final Logger logger = LoggerFactory.getLogger(NeighborhoodLocator.class);
    static int txLength = 64;
    static int window = Windowed.window;
    static double bitcoin = Math.pow(10, 8);
    static double lower = 0.5;
    static String pathname = "neighbors/";
    public static void main(String args[]) throws IOException {

//        System.exit(1);
//        FileUtils.cleanDirectory(new File(pathname));
        BufferedWriter wr = new BufferedWriter(new FileWriter("stats.csv"));


        String dataDir = args[0];
        String suspAddFile = args[1];
        DirectedSparseGraph<Object, WeightedEdge> graph = new DirectedSparseGraph();
        HashMap<Output, Integer> outputs = new HashMap<>();
        HashMap<String, Integer> suspAdd = Windowed.read(suspAddFile);
        BiMap<String, Integer> nodeIDs = HashBiMap.create();
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
                    Windowed.filterEdges(graph);

                    findNeighbors(graph,suspAdd,nodeIDs);
                    passed = 0;
                    graph = new DirectedSparseGraph();
                    nodeIDs.clear();
                    outputs.clear();
                }
            }

        }
        wr.close();
    }

    private static void findNeighbors(DirectedSparseGraph<Object, WeightedEdge> graph, HashMap<String, Integer> suspAdd, BiMap<String, Integer> nodeIDs){

        int virus=0;
        for(String o: suspAdd.keySet()){
            if(nodeIDs.containsKey(o)){
                int o1 = nodeIDs.get(o);
                if(graph.containsVertex(o1)){
                    virus++;
                    HashSet nes = new HashSet();
                    Collection<Object> ines = graph.getNeighbors(o1);
                    for(Object n1:ines){
                        nes.addAll(graph.getNeighbors(n1));
                    }
                    nes.remove(o1);
                    System.out.println("virus "+o1+" has "+nes.size());
                }
            }
            //System.out.println(o1+" not today");
        }
        for (Object n : graph.getVertices()) {
            if (n.toString().length() == txLength) {
                continue;
            }
            //1 hop neighborhood
            if(!suspAdd.containsKey(n)){
                //normal address
                if(virus-->0){
                    Object m1 =n;
                    HashSet nes = new HashSet();
                    Collection<Object> ines = graph.getNeighbors(m1);
                    for(Object n1:ines){
                        nes.addAll(graph.getNeighbors(n1));
                    }
                    nes.remove(m1);
                    System.out.println("normal "+m1+" has "+nes.size());
                }
            }
        }

    }

    private static void process(BufferedWriter wr, DirectedSparseGraph<Object, WeightedEdge> graph, HashMap<String,Integer> suspAdd, BiMap<String, Integer> nodeIDs, int year, int day) throws IOException {

        HashMap<Integer, Double> weights = new HashMap<>();
        HashMap<Integer, Integer> lengths = new HashMap<>();
        HashMap<Integer, HashMap<String, Integer>> counts = new HashMap<>();
        int count = 0;
        for (Object n1 : graph.getVertices()) {
//            logger.info(weights.size()+" visited vertices");
            if (n1.toString().length() != txLength) {
                continue;
            }
            if (graph.getInEdges(n1).size() != 0) continue;
            HashSet<Integer> seen = new HashSet(4);
            boolean err = Windowed.traverse(0, 1.0, lengths, weights, counts, graph, n1, seen,n1.toString());
            if (err) { //this should not happen. Recursion goes wrong.
                logger.error("Recursion gone wrong, returning");
            }
        }
        BufferedWriter w = new BufferedWriter(new FileWriter(pathname+year + "_" + day + "dayWeights.txt"));
        w.append("node\tlength\tweight\tcount\tlooped\tneighbors\tincome\tlabel\r\n");
        for (int nn : weights.keySet()) {

            String addr = nodeIDs.inverse().get(nn);

            //if (weights.get(nn) > 1 || suspAdd.containsKey(addr))
            {
                int label = 0;

                if (suspAdd.containsKey(addr)) {
                    logger.info("suspicious");
                    label = suspAdd.get(addr);
                }
                long amo = 0;
                for (WeightedEdge we : graph.getInEdges(nn)) {
                    amo += we.getAmount();
                }

                HashMap<String, Integer> stringIntegerHashMap = counts.get(nn);
                int closesLoopOfTx =0;
                for(int h:stringIntegerHashMap.values()){
                    if(h>1)closesLoopOfTx++;
                }
                w.append(year+"\t"+day+"\t"+nodeIDs.inverse().get(nn) + "\t" + lengths.get(nn) + "\t" + weights.get(nn)+ "\t" + stringIntegerHashMap.size()+"\t"+closesLoopOfTx + "\t" + graph.getNeighborCount(nn) + "\t" + amo + "\t" + label + "\r\n");
                count++;
            }
        }
        w.close();


        DescriptiveStatistics ds = new DescriptiveStatistics();
        for (Object o : graph.getVertices()) {
            String s = o.toString();
            if (s.length() != txLength) {
                ds.addValue(graph.getNeighborCount(o));
            }
        }
        String msg = year + "\t" + day + "\t" + count + "\t" + graph.getVertexCount() + "\t" + graph.getEdgeCount() + "\t" + ds.getMean() + "\t" + ds.getMax() + "\t" + ds.getN();
        logger.info(msg);
        wr.append(msg + "\r\n");
    }


}
