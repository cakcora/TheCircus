import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
/*
This class crawls folders of virus datasets and finds virus addresses. All viruses and families are written into a file.
 */
public class VirusReader {

    private static final Logger logger = LoggerFactory.getLogger(VirusReader.class);

    private Map<String, String> family;
    private Map<String,HashMap<String,Integer>> sets;
    private Map<String,Integer> virusID;
    public static void main(String []args) throws IOException {
        VirusReader vr = new VirusReader();
        vr.read();
    }

    public VirusReader() throws IOException {
        this.family = new HashMap<>();
        this.sets = new HashMap<>();
        virusID = new HashMap<>();
        virusID.put("montrealCryptoLocker",0);
        virusID.put("paduaCryptoLocker",0);
        virusID.put("montrealCryptoTorLocker2015",1);
        virusID.put("paduaCryptoTorLocker2015",1);
        virusID.put("montrealCryptXXX",2);
        virusID.put("montrealDMALockerv3",3);
        virusID.put("montrealEDA2",4);
        virusID.put("montrealGlobe",5);
        virusID.put("montrealLocky",6);
        virusID.put("princetonLocky",6);
        virusID.put("montrealSamSam",7);
        virusID.put("paduaCryptoWall",8);
        virusID.put("princetonCerber",9);
    }

    private void read() throws IOException {
        File folder = new File("/home/user/Dropbox/Academic/PostDoc/5-Anomaly/Padua Study/addresses/");
        File[] listOfFiles = folder.listFiles();
        BufferedWriter wr = new BufferedWriter(new FileWriter("allAddresses.txt"));
        for (int i = 0; i < listOfFiles.length; i++) {
            File aFile = listOfFiles[i];
            if (aFile.isFile()) {
                String f = aFile.getName();
                File file = new File(aFile.getAbsolutePath());
                try {
                    process("padua", file, f, wr);
                } catch (IOException e) {
                    logger.error("Padua dataset viruses could not be read.");
                }
            }
        }

        try {
            String pathname = "/home/user/Dropbox/Academic/PostDoc/5-Anomaly/MoneyLaundering/princetonstudy.txt";
            process("princeton", new File(pathname), 1, 4, "\t", wr);
        } catch (IOException e) {
            logger.error("Princeton dataset viruses could not be read.");
        }
        try {
            String pathname = "/home/user/Dropbox/Academic/PostDoc/5-Anomaly/Montreal Study/dataset/blockchain/expanded_addresses_stats.csv";
            process("montreal", new File(pathname), 1, 0, ",", wr);
        } catch (IOException e) {
            logger.error("Princeton dataset viruses could not be read.");
        }

        wr.close();

        for (String s : sets.keySet()) {
            for (String d : sets.get(s).keySet()) {
                if (!s.equalsIgnoreCase(d))
                    System.out.println(s + "\t" + d + "\t" + sets.get(s).get(d));
                else
                    System.out.println(s + "\t\t" + sets.get(s).get(d));
            }
        }
        createIDs();
    }

    public void read(File fi) throws IOException {
        this.family = new HashMap<>();
        this.sets = new HashMap<>();

        try {
            process("",fi,1,0,"\t");
        } catch (IOException e) {
            logger.error("Princeton dataset viruses could not be read.");
        }
        createIDs();
    }

    private   void process(String dset,File virusFile, int virusIndex, int addIndex,String sep) throws IOException {

        String l="";
        BufferedReader br = new BufferedReader(new FileReader(virusFile));
        br.readLine();//header
        while((l=br.readLine())!=null){
            String arr [] =l.split(sep);
            String virusName = (dset+arr[virusIndex]);
            String add = arr[addIndex];
            addVirus(virusName, add);


        }
        br.close();
    }

    private   void process(String dset,File virusFile, int virusIndex, int addIndex,String sep,BufferedWriter wr) throws IOException {

        String l="";
        BufferedReader br = new BufferedReader(new FileReader(virusFile));
        br.readLine();//header
        while((l=br.readLine())!=null){
            String arr [] =l.split(sep);
            String virusName = (dset+arr[virusIndex]);
            String add = arr[addIndex];
            addVirus(virusName, add,wr);


        }
        br.close();
    }

    private String filter(String s) {
        s = s.replaceAll("_addresses.txt","");
        return s;
    }
    private void addVirus(String virusName, String add) throws IOException {
        virusName = filter(virusName);
        if (!family.containsKey(add)){
            family.put(add,virusName);

        } else {
            //do nothing
        }
    }
    private void addVirus(String virusName, String add, BufferedWriter wr) throws IOException {
        virusName = filter(virusName);
        if (!family.containsKey(add)){
            add(virusName,virusName);
            family.put(add,virusName);
            wr.write(add+"\t"+virusName+"\r\n");
        } else {
            String key = family.get(add);
            add(virusName,key);
            wr.write(add + "\t" + key  +"\r\n");
        }
    }

    private void add(String v1, String v2) {
        if(!sets.containsKey(v1)){
            sets.put(v1,new HashMap<>());
            sets.get(v1).put(v2,1);
        }
        else {
            if(!sets.get(v1).containsKey(v2)){
                sets.get(v1).put(v2,1);

            }
            else {
                sets.get(v1).put(v2,1+sets.get(v1).get(v2));
            }
        }
    }

    private  void process(String dset,File virusFile, String vir, BufferedWriter wr) throws IOException {
        String virusName = dset+vir;

        String l="";
        BufferedReader br = new BufferedReader(new FileReader(virusFile));
        while((l=br.readLine())!=null){
            String add = l.trim();
            addVirus(virusName, add,wr);
        }
        br.close();
    }
    public void createIDs(){
        for(String s:new HashSet<>(family.keySet())){
            String f = family.get(s);
            if(!virusID.containsKey(f)){
                int newID = virusID.size();
                virusID.put(f, newID);
            }
        }
    }

    public int getFamily(String address) {
        if(family.containsKey(address))
               return virusID.get(family.get(address));
        return 0;
    }
}
