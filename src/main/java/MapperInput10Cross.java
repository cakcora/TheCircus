import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

public class MapperInput10Cross {

    private static final Logger logger = LoggerFactory.getLogger(MapperInput10Cross.class);

    public static void main(String args[]) throws IOException {
        VirusReader vr = new VirusReader();
        vr.read(new File(args[0]));
        String iDir = args[1];
        int d = vr.getFamily("15QzHEbNZWp2w1i2mfZSx7pV5YNM4ahszB");//testing the id system.


        FileUtils.cleanDirectory(new File("pieces/"));
        int pieces = 100;
        VirusReader virusReader = new VirusReader();
        ArrayList<String> normal = new ArrayList<>();
        HashMap<Integer,ArrayList<String>> virus = new HashMap<>();

        for (int year = 2009; year <= 2018; year++) {
            for (int day = 1; day < 366; day++) {
                String fileName = iDir+year + "_" + day + "dayWeights.txt";
                if (!new File(fileName).exists()) {
                    System.out.println("day" +fileName);
                    continue;
                }

                BufferedReader weReader = new BufferedReader(new FileReader(fileName));
                String line = "";

                weReader.readLine();//header

                while((line=weReader.readLine())!=null){
                    String [] arr = line.split("\t");


                    int label =vr.getFamily(arr[2].trim());
                    line = arr[0]+"\t"+arr[1]+"\t"+arr[2]+"\t"+arr[3]+"\t"+arr[4]+"\t"+arr[5]+"\t"+arr[6]+"\t"+arr[7]+"\t"+arr[8]+"\t"+label;
                    if(label >0){
                        if(!virus.containsKey(label)) virus.put(label,new ArrayList<>());
                        virus.get(label).add(line);
                    }
                    else normal.add(line);
                }
            }

        }
        int nSize = normal.size();

        for(int piece = 0;piece<pieces;piece++) {
            int nStart = piece * (nSize / pieces);
            int nEnd = nStart + nSize / pieces;


            BufferedWriter w = new BufferedWriter(new FileWriter("pieces/" + piece, true));
            for (String l : normal.subList(nStart, nEnd)) {
                w.write(l + "\r\n");
            }
            for (Integer v : virus.keySet()) {
                ArrayList<String> vi = virus.get(v);
                int aSize = vi.size();
                if (aSize > pieces) {
                    int aStart = piece * (aSize / pieces);
                    int aEnd = aStart + aSize / pieces;
                    if (aEnd > aStart) {
                        for (String m : vi.subList(aStart, aEnd)) {
                            w.write(m + "\r\n");
                        }
                    }

                } else {
                    //System.out.println("not enough virus addresses for piece "+piece);
                }

            }
            w.close();
        }

    }




}
