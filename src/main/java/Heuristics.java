import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

/*
THis file will implement co-spending and transiton heuristics.
 */
public class Heuristics {

    private static final Logger logger = LoggerFactory.getLogger(Heuristics.class);
    static int txLength = 64;
    static int window = 1;
    static double bitcoin = Math.pow(10, 8);
    static double lower = 0.3d;

    public static void main(String args[]) throws IOException {

        String dataDir = args[0];

        //all addresses are created by the virusreader class
        String suspAddFile = "allAddresses.txt";
        HashMap<String, HashSet<String>> suspAdd = read(suspAddFile);
        String line;
        Heuristics hr = new Heuristics();
        HashMap<String, Address> addressOutputList = new HashMap<String, Address>();
        HashMap<String,  String > addressInputList = new HashMap<String, String>();
        HashMap<String,  String > txDates = new HashMap<String, String>();
        System.out.println("total number of addresses:"+suspAdd.size());
        hr.addTxOutputs(dataDir, suspAdd, addressOutputList);
        System.out.println(addressOutputList.size()+" prev tx");
        for(int year = 2011; year<=2018; year++) {
            for (int day = 1; day <= 365; day++) {
                String fPath=dataDir+"inputs"+year+"_"+day+".txt";
                if(day%100==0)
                System.out.println(fPath);

                if (!new File(fPath).exists()) continue;
                BufferedReader br = new BufferedReader(new FileReader(fPath));

                while ((line = br.readLine()) != null) {
                    String arr[] = line.split("\t");
                    String tx = arr[2];
                    for (int j = 3; j < arr.length; j = j + 2) {
                        String prevTx = arr[j];
                        int index = Integer.parseInt(arr[j + 1]);

                        if(addressOutputList.containsKey(prevTx+"_"+index)){
                            //System.out.println("found "+prevTx);
                            addressInputList.put((prevTx+"_"+index),tx);
                            txDates.put(tx,year+"_"+day);
                        }
                    }
                }

            }
        }

        HashMap<String,  HashSet<Address> > txAddresses = new HashMap<String, HashSet<Address>>();

        for(String txInput:addressInputList.keySet()){
            String tx = addressInputList.get(txInput);
            if(!txAddresses.containsKey(tx)){
                txAddresses.put(tx,new HashSet<>());
            }

            Address e = addressOutputList.get(txInput);
            //System.out.println("adding "+e.toString()+" to "+tx);
            txAddresses.get(tx).add(e);
        }
        BufferedWriter wr = new BufferedWriter(new FileWriter("cospendingAddresses.txt"));
        HashMap<String, HashSet<Address>> knownAddresses= new HashMap<>()
                ;        for(int y=2009;y<=2018;y++)
            for(int d=1;d<=366;d++) {
                HashMap<String,HashSet<Address>> ofToday = new HashMap<>();
                for (String tx : txAddresses.keySet()) {
                    String s1 = txDates.get(tx);
                    int year = Integer.parseInt(s1.substring(0, 4));
                    int day = Integer.parseInt(s1.substring(5));
                    if(year==y&&day==d){

                        HashSet<Address> s = txAddresses.get(tx);
                        int c=0;
                        for(Address a:s){
                            String name = a.getName();
                            for(String virus:suspAdd.get(name)){
                                if(knownAddresses.containsKey(virus)&&knownAddresses.get(virus).contains(a)){
                                    c++;
                                }
                            }
                        }
                        if(c==s.size()) {
                            //System.out.println("all were known viruses");
                        }
                        else if(c>0&&c< s.size()){
                            for(Address a:s){
                                String name = a.getName();
                                for(String virus:suspAdd.get(name)){
                                    if(knownAddresses.containsKey(virus)&&!knownAddresses.get(virus).contains(a)){
                                        System.out.println(year+","+day+","+virus+" learns "+ name);
                                        wr.write(year+","+day+","+virus+","+ name+"\r\n");


                                    }
                                    else{
                                        if(!ofToday.containsKey(virus)){
                                            ofToday.put(virus,new HashSet<>());
                                        }
                                        //may or may not be aware of this address
                                        ofToday.get(virus).add(a);
//                                        System.out.println("1 learnin about add "+a);
                                    }
                                }
                            }
                        }
                        else {
//                            all are new addresses
                            for(Address a:s){
                                String name = a.getName();
                                for(String virus:suspAdd.get(name)){

                                        if(!ofToday.containsKey(virus)){
                                            ofToday.put(virus,new HashSet<>());
                                        }
                                        //may or may not be aware of this address
                                        ofToday.get(virus).add(a);
//                                        System.out.println("2 learnin about add "+a);

                                }
                            }
                        }
                    }
                }
                for(String vir:ofToday.keySet()){
                    if(!knownAddresses.containsKey(vir)){
                        knownAddresses.put(vir,new HashSet<>());
                    }
                    knownAddresses.get(vir).addAll(ofToday.get(vir));
                }
            }
        wr.close();

    }

    private static String flatten(HashSet<Address> set){
        String s="";
        for(Address a:set){
            s=s+","+a.getName();
        }
        return s;
    }

    private void addTxOutputs(String dataDir, HashMap<String, HashSet<String>> suspAdd, HashMap<String, Address> addressTxList) throws IOException {
        String line;
        for(int year = 2011; year<=2018; year++) {
            for (int day = 1; day <= 365; day++) {
                String fPath=dataDir+"outputs"+year+"_"+day+".txt";
                if(day%100==0)
                System.out.println(fPath);
                if (!new File(fPath).exists()) continue;
                BufferedReader br = new BufferedReader(new FileReader(fPath));

                while ((line = br.readLine()) != null) {
                    String arr[] = line.split("\t");
                    if ((arr.length - 1) % 2 != 0) continue;
                    String tx = arr[2];


                    for (int j = 3; j < arr.length; j = j + 2) {
                        Address address = new Address(arr[j]);
                        if (suspAdd.containsKey(address.getName())) {
                            addressTxList.put(tx + "_" + j,address);
                            //System.out.println("adding "+address);
                        }
                    }

                }
            }
        }
    }






    public static HashMap<String,HashSet<String>> read(String suspAddFile) throws IOException {
        String line = "";
        HashMap<String,HashSet<String>> addMap = new HashMap<>();
        BufferedReader br = new BufferedReader(new FileReader(suspAddFile));
        br.readLine();//header
        while ((line = br.readLine()) != null) {
            String[] arr = line.split("\t");
            String address = arr[0];

            for(int i=1;i<arr.length;i++) {
                String family = arr[i];
                if(!addMap.containsKey(address)){
                    addMap.put(address,new HashSet<String>());
                }
                addMap.get(address).add(family);
            }
        }
        return addMap;
    }




    public class Address{
        String name;
        Address(String add){
            this.name=add;
        }

        public String getName() {
            return this.name;
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Address address = (Address) o;
            return Objects.equals(name, address.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }
}
