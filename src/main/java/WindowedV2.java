import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

public class WindowedV2 {

    private static final Logger logger = LoggerFactory.getLogger(WindowedV2.class);
    public static final long UNKNOWNAMOUNT=-1;
    static int txLength = 64;
    static int window = 1;
    static double bitcoin=Math.pow(10,8);
    static double lower = 0.3d;
    public static void main(String args[]) throws IOException {

        String odir = args[2];

        String dataDir = args[0];
        DirectedSparseGraph<Object, WeightedEdge> graph = new DirectedSparseGraph();
        HashMap<Output, Integer> outputs = new HashMap<>();
        long passed = 0;

        for (int year = 2009; year <= 2018; year++) {
            for (int day = 1; day <= 366; day++) {
                graph = new DirectedSparseGraph();

                String key = year + "_" + day;
                String fPath = dataDir + "outputs" + key + ".txt";
                String oPath = dataDir + "inputs" + key + ".txt";
                if (!new File(fPath).exists()||!new File(oPath).exists()) continue;
                BiMap<String, Integer> nodeIDs = addDayOutputs(graph, outputs, fPath);
                addDayInputs(graph, outputs, nodeIDs, oPath);
                passed++;
                if (passed == window) {
                    logger.info("Unfiltered: " + graph.getVertexCount() + " vertices, " + graph.getEdgeCount() + " edges");
                    filterEdges(graph);
                    logger.info("Filtered: " +graph.getVertexCount() + " vertices, " + graph.getEdgeCount() + " edges");
                    process(odir, graph, nodeIDs, year, day);
                    passed = 0;
                    graph = new DirectedSparseGraph();
                    nodeIDs.clear();
                    outputs.clear();
                }
            }

        }
    }


    static void filterEdges(DirectedSparseGraph<Object, WeightedEdge> graph) {
        for (WeightedEdge edge : new HashSet<>(graph.getEdges())) {
            if (edge.getAmount() < lower * bitcoin) {
                graph.removeEdge(edge);
            }
        }
        for (Object node : new HashSet<>(graph.getVertices())) {
            if (graph.getNeighborCount(node) == 0) {
                graph.removeVertex(node);
            }
        }
    }


    public static void process(String odir, DirectedSparseGraph<Object, WeightedEdge> graph, BiMap<String, Integer> nodeIDs, int year, int day) throws IOException {


        HashSet<Object> starters = new HashSet<>();
        for (Object node : graph.getVertices()) {

            //Is this a transaction node in the graph?
            if (node.toString().length() != txLength) {
                //No it is not
                continue;
            }
            //Is this a starter transaction?
            if (graph.getInEdges(node).size() != 0){
                //Not a starter transaction
                continue;
            }
            starters.add(node);
        }

        HashMap<Integer, Double> weights = new HashMap<>();
        HashMap<Integer, Integer> lengths = new HashMap<>();
        HashMap<Integer, HashMap<String, Integer>> counts = new HashMap<>();
        HashSet<Integer> seen = new HashSet<>();
        //depth first search from starters
        for(Object o:starters){
             traverse(0,1d,lengths,weights,counts,graph,o,seen,o.toString());
        }

        BufferedWriter w = new BufferedWriter(new FileWriter(odir+year + "_" + day + "dayWeights.txt"));
        w.append("year\tday\tnode\tlength\tweight\tcount\tlooped\tneighbors\tincome\r\n");
        BiMap<Integer, String> inverse = nodeIDs.inverse();

        for (int nn : weights.keySet()) {

            String addr = inverse.get(nn);
            long amo = 0;
            for (WeightedEdge we : graph.getInEdges(nn)) {
                amo += we.getAmount();
            }

            HashMap<String, Integer> nodeCounts = counts.get(nn);
            int closesLoopOfTx =0;
            for(int h:nodeCounts.values()){
                if(h>1)closesLoopOfTx++;
            }
            Integer chainLength = lengths.get(nn);
            w.append(year+"\t"+day+"\t"+ addr + "\t" + chainLength/2 + "\t" + weights.get(nn)+ "\t" + nodeCounts.size()+"\t"+closesLoopOfTx + "\t" + graph.getNeighborCount(nn) + "\t" + amo + "\r\n");
        }
        w.close();
    }



    public static boolean traverse(int len, double weight, HashMap<Integer, Integer> lengths, HashMap<Integer, Double> weights,
                                   HashMap<Integer, HashMap<String, Integer>> counts, DirectedSparseGraph graph, Object starterTx, HashSet<Integer> seen,String startNode) {
        //6 blocks every hour, 24 hours, in average
        if (len > (window * 24 * 6)) {
            return true;
        }
        Collection outEdges = new HashSet(graph.getSuccessors(starterTx));
        HashSet<Object> remove = new HashSet<>();
        if (outEdges == null) {
            //("Chain ends here.");
            return false;
        }

        int s = outEdges.size();
        if ((len % 2) == 0) {
            //update the map
            for (Object outNeigh : outEdges) {
                //We are supposed to get an address node, as such it has a numerical id in the graph.
                int neighId = Integer.parseInt(outNeigh.toString());
                if(!counts.containsKey(neighId))counts.put(neighId,new HashMap<String, Integer>());
                HashMap<String, Integer> nodeCounts = counts.get(neighId);
                if(!nodeCounts.containsKey(startNode)) {
                    nodeCounts.put(startNode,0);
                }
                nodeCounts.put(startNode,1+ nodeCounts.get(startNode));
                if (seen.contains(neighId)) {
                    remove.add(outNeigh);
                }
                if (!weights.containsKey(neighId)) {
                    weights.put(neighId, 0.0);
                    lengths.put(neighId, 0);
                }
                weights.put(neighId, weights.get(neighId) + (weight / s));
                int length = lengths.get(neighId) + 1;
                if (len > length)
                    lengths.put(neighId, len);
                seen.add(neighId);
            }
        }

        outEdges.removeAll(remove);
        for (Object N2 : outEdges) {
            traverse(len + 1, weight / s, lengths, weights, counts, graph, N2, seen, startNode);
        }
        return false;
    }
/*
inputs look like this:
blocknumber timestamp   tx1id    tx1outputindex tx2id    tx2outputindex ....
4926	2009-02-19 23:50:35	b52953f7104aa6ffab9391578fc34279ce221700acb2257859657b2901987c63	30f3a6f6991bfdb2825360e676798fdb6958238ccec2a6c8cc8488eca68694bb	0	9a0d369c0124bb38b1a7e685e45631a0433a1f1b0a9ef548739beb7fd5d3177d	0	ccb8be5a3b9d82e52b0cbf069beef64ca4fca068b90cf157e52795229922c58a	0	950cafdcd7f9c493fda41b29037ba7e250061505ed34e5a87658d4e1d2201019	0	8e157ea7794a1d56ebe678117bef1d6049f6a2d9736233377978126601ad5606	0	9b85ec2ed3fe535b0bb42bcbe43462b6b596be4123352b7ba5ce62de00e3c7f1	0	fc598dfb07a26e979fb3a36239ec81bae2b58fd1535c285beb4bc16541763f13	0	85c1103de829fa20385b113b89a5bd6a090b85405a8d755ff426e1371a51e47a	0	2a1f696b72d98fcd5ab5b7f2cab275ddbf8806f170035ded447f4af40f095811	0	4e753cee854fa260c8958894c16bf7ded63978b97a9637a8b1e8364cbe47b095	0	5f96d0181adf2040d57f15d6761f96c54411417e26df31fbfb8a5708e2e1a615	0	b93c5cd66979d138a12474912b93acc075da93e1f979067cb39f81fcc5a4a042	0	19f17a3f8afced92b84fedae64488f587ddc3bc3f35c35a54fa4c00efd091cdf	0	e76a0be5407bca02384a487b7a9b4b393279a2ebfcd227220d7fd9e529a52693	0	db68b2543026b724f768b74830ae8f10ea6b1977a938587e00f45f817fed4aa9	0	df8317be4e3c8e6e17d544521f294d8611877a166f8d53f8472d80819001c0db	0

 */
    public static void addDayInputs(DirectedSparseGraph graph, HashMap<Output, Integer> outputs, BiMap<String, Integer> nodeIDds, String oPath) throws IOException {
        String line;
        BufferedReader br = new BufferedReader(new FileReader(oPath));
        int nodeCount = graph.getVertexCount();
        int edgeNumber = graph.getEdgeCount();
        final int txIndex = 2;
        while ((line = br.readLine()) != null) {
            String arr[] = line.split("\t");
            String currentTx = arr[txIndex];
            for (int j = (1+txIndex); j < arr.length; j = j + 2) {
                String prevTx = arr[j];
                if (currentTx.length() != txLength || prevTx.length() != txLength) {
                    logger.error("Tx id appears to be less than " + txLength + " chars. Ignoring this line:" + line);
                    continue;
                }

                int index = Integer.parseInt(arr[j + 1]);
                Output key = new Output(prevTx, index);
                long amount;
                int nID;
                if (outputs.containsKey(key)) {
                    nID = outputs.get(key);
                    amount = ((WeightedEdge) graph.findEdge(prevTx, nID)).getAmount();

                    WeightedEdge edge1 = new WeightedEdge(amount, edgeNumber);
                    if (graph.addEdge(edge1, nID, currentTx)) {
                        edgeNumber++;
                    }
                } else {
                    //address("unknown");
                    logger.info("We have an input that we did not see in outputs. Line:  "+line);

                    amount = UNKNOWNAMOUNT;
                    graph.addVertex(prevTx);
                    nID = nodeCount;
                    graph.addVertex(nID);
                    nodeIDds.put(key.toString(), nID);
                    nodeCount++;
                    WeightedEdge edge1 = new WeightedEdge(amount, edgeNumber);
                    if (graph.addEdge(edge1, prevTx, nID)) {
                        edgeNumber++;
                    }
                    WeightedEdge edge2 = new WeightedEdge(amount, edgeNumber);
                    if (graph.addEdge(edge2, nID, currentTx)) {
                        edgeNumber++;
                    }
                }
            }
        }
    }
/*
Each line in the outputs file looks like this:
blockheight timestamp   txid    add1    amo1    add2    amo2.....
539567	2018-09-02 00:06:12	92a914706d04c3d3aa5e6f5b16a191c253c37c7bb43c60719de3798d71cd11e0	15e5uTFKNpxvM7ra21bCVHuXfDt7aoBNZn	950000	3AAA1swNX2KXdA7Ha9hBFBZdTNHVCuk1AW	8370887	17A16QmavnUfCW11DAApiJxp7ARnxN5pGX	2593589238

 */

    public static BiMap<String, Integer> addDayOutputs(DirectedSparseGraph graph, HashMap<Output, Integer> outputs, String fPath ) throws IOException {

        String line;
        BiMap<String, Integer> nodeIDs = HashBiMap.create();
        final int txIndex = 2;
        BufferedReader br = new BufferedReader(new FileReader(fPath));
        int nodeCount = graph.getVertexCount();
        int edgeNumber = graph.getEdgeCount();
        while ((line = br.readLine()) != null) {
            String arr[] = line.split("\t");
            if ((arr.length-1) % 2 != 0) {
                logger.error("Malformed line");
                continue;
            }

            String tx = arr[txIndex];
            if (tx.length() != txLength) {
                logger.error("Transaction id is malformed? No recovery from this, exiting. TxID:" + tx);
                System.exit(1);
            }
            //address to the graph

            graph.addVertex(tx);
            final int stIndex=3;
            for (int j = stIndex; j < arr.length; j = j + 2) {
                String address = arr[j];
                if (!address.equalsIgnoreCase("noaddress")) {
                    int index = (j - stIndex) / 2;
                    long amo = Long.parseLong(arr[j + 1]);
                    Output out = new Output(tx, address, index, amo);

                    if (!nodeIDs.containsKey(address)) {
                        graph.addVertex(nodeCount);
                        nodeIDs.put(address, nodeCount);
                        nodeCount++;
                    }
                    int addID = nodeIDs.get(address);

                    WeightedEdge edge = new WeightedEdge(amo, edgeNumber);
                    if (graph.addEdge(edge, tx, addID)) {
                        edgeNumber++;
                    }
                    outputs.put(out, addID);

                }
            }

        }
        return nodeIDs;
    }
}
