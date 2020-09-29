import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

import static java.util.Calendar.*;

public class ThebesExperiment1 {

    private static final Logger logger = LoggerFactory.getLogger(ThebesExperiment1.class);
    static int txLength = 64;


    enum Strategy {
        SAMEDAY,
        SAMEANDNEXTDAY,
        SAMEANDPREVDAY,
        SAMEPREVANDNEXTDAY
    }


    public static void main(String[] args) throws FileNotFoundException, Exception {

        String dataDir = args[0];
        String suspAddFile = args[1] + args[2];//allAdresses.txt
        Strategy strategy = Strategy.SAMEANDNEXTDAY;
        HashMap<Long, HashSet<Merchandise>> salesHolder = new HashMap<Long, HashSet<Merchandise>>();
        HashMap<Long, HashSet<Output>> outputsHolder = new HashMap<Long, HashSet<Output>>();
        String salesFile = args[3];
        final File folder = new File(salesFile);
        readSales(folder, salesHolder);
        for(long secs:salesHolder.keySet()){
            strategy = Strategy.SAMEDAY;
            readOutputs(dataDir, secs, outputsHolder, strategy);
            startSearch(strategy, salesHolder, outputsHolder,secs);
            strategy = Strategy.SAMEANDPREVDAY;
            outputsHolder = new HashMap<Long, HashSet<Output>>();
            readOutputs(dataDir, secs, outputsHolder, strategy);
            startSearch(strategy, salesHolder, outputsHolder,secs);
            strategy = Strategy.SAMEANDNEXTDAY;
            outputsHolder = new HashMap<Long, HashSet<Output>>();
            readOutputs(dataDir, secs, outputsHolder, strategy);
            startSearch(strategy, salesHolder, outputsHolder,secs);
            strategy = Strategy.SAMEPREVANDNEXTDAY;
            outputsHolder = new HashMap<Long, HashSet<Output>>();
            readOutputs(dataDir, secs, outputsHolder, strategy);
            startSearch(strategy, salesHolder, outputsHolder,secs);
        }

    }

    private static void startSearch(Strategy strategy, HashMap<Long, HashSet<Merchandise>> salesHolder, HashMap<Long, HashSet<Output>> outputsHolder,long secs) {
        int totalSuspect = 0;

        int foundUniquelyNto1 = 0;
        int foundUniquelyNtoN = 0;
        int foundMultipleNto1 = 0;
        int foundMultipleNtoN = 0;
        HashSet<Merchandise> merchs = salesHolder.get(secs);
        HashSet<Output> outputs = outputsHolder.get(secs);
        HashMap<Long, HashSet<Output>> candidates = new HashMap<Long, HashSet<Output>>();
        for (Output o : outputs) {
            long amount = o.getAmount();
            if (!candidates.containsKey(amount)) {
                candidates.put(amount, new HashSet<>());
            }
            candidates.get(amount).add(o);
        }
        for (Merchandise m : merchs) {
            long price = m.getPrice();
            if (candidates.containsKey(price)) {
                HashSet<Output> outputs1 = candidates.get(price);
                int numOfCandidates = outputs1.size();

                if (numOfCandidates == 1) {
                    int outputType = outputs1.iterator().next().getOutputType();
                    if (outputType <=2)
                        foundUniquelyNto1++;
                    else foundUniquelyNtoN++;
                } else {
                    for (Output o3 : outputs1) {
                        if (o3.getOutputType() <= 2) {
                            foundMultipleNto1++;
                        } else foundMultipleNtoN++;
                    }
                    foundMultipleNto1++;
                }
                totalSuspect += numOfCandidates;
            }
        }

        System.out.println(secs +"\t"+strategy.toString() + "\t" +
                foundUniquelyNto1 + "\t" + foundUniquelyNtoN + "\t" +
                foundMultipleNto1 + "\t" + foundMultipleNtoN + "\t" +
                totalSuspect + "\t out of " + salesHolder.get(secs).size());
    }


    private static void readOutputs(String dataDir, long secs, HashMap<Long, HashSet<Output>> outputsHolder, Strategy strategy) throws Exception {
        LocalDate date;
        date = Instant.ofEpochMilli(secs * 1000).atZone(ZoneId.systemDefault()).toLocalDate();

        int day = date.getDayOfMonth();
        int month = date.getMonthValue();
        int year = date.getYear();
        String key = year + "_" + month;
        String fileName = dataDir + "outputs" + key + ".txt";

        LocalDate date2;
        date2 = Instant.ofEpochMilli(secs * 1000).atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate dateNext = date2.plusDays(1);
        LocalDate datePrev = date2.minusDays(1);
        HashSet<Output> dayOutputs = readOutputs(fileName, date);
        outputsHolder.put(secs, dayOutputs);
        if (strategy == Strategy.SAMEANDNEXTDAY) {
            HashSet<Output> dayNextOutputs = readOutputs(fileName, dateNext);
            outputsHolder.get(secs).addAll(dayNextOutputs);
        } else if (strategy == Strategy.SAMEANDPREVDAY) {
            HashSet<Output> dayPrevOutputs = readOutputs(fileName, datePrev);
            outputsHolder.get(secs).addAll(dayPrevOutputs);
        } else if (strategy == Strategy.SAMEPREVANDNEXTDAY) {
            HashSet<Output> dayNextOutputs = readOutputs(fileName, dateNext);
            outputsHolder.get(secs).addAll(dayNextOutputs);
            HashSet<Output> dayPrevOutputs = readOutputs(fileName, datePrev);
            outputsHolder.get(secs).addAll(dayPrevOutputs);
        }
    }


    public static void readSales(final File folder, HashMap<Long, HashSet<Merchandise>> holder) throws Exception {

        String regex = "-";
        for (final File fileEntry : Objects.requireNonNull(folder.listFiles())) {
            if (fileEntry.isDirectory()) {
                readSales(fileEntry, holder);
            } else {
                String[] a = folder.getName().split(regex);
                int year = Integer.parseInt(a[0]);
                int month = Integer.parseInt(a[1]);
                int day = Integer.parseInt(a[2]);
                long time = inSec(year, month, day);
                BufferedReader br = new BufferedReader(new FileReader(fileEntry));
                String line = "";
                br.readLine();//read header
                while ((line = br.readLine()) != null) {
                    line = line.replaceAll(",\n", ",");
                    line = line.replaceAll("\"", "");
                    String arr[] = line.split(",");
                    if (arr.length > 5) {
                        //"hash","market_name","item_link","vendor_name","price","name","description","image_link","add_time","ship_from",

                        try {
                            long pri;
                            if (arr[4].contains("0.\\")) {
                                arr[4] = arr[4].replaceAll("0\\.", "");
                                pri = (long) (Math.pow(10, 7) * Double.parseDouble(arr[4]));
                            } else pri = (long) (Math.pow(10, 8) * Double.parseDouble(arr[4]));
                            if (pri == 0) continue;
                            Merchandise m = new Merchandise(arr[1], arr[3], pri, arr[5]);
                            if (!holder.containsKey(time)) {
                                holder.put(time, new HashSet<>());
                            }
                            holder.get(time).add(m);
                        } catch (NumberFormatException e) {

                            System.out.println("ERROR Number format:" + line);
                        }
                    } else System.out.println("Error Generic:" + fileEntry + line);

                }
            }
        }
    }


    public static HashSet<Output> readOutputs(String fileName, LocalDate date) throws Exception {
        HashSet<Output> holder = new HashSet<>();
        String line;
        BufferedReader br = new BufferedReader(new FileReader(fileName));
        int daySale = date.getDayOfMonth();


        Date date2 = new Date();
        Calendar cal2 = getInstance();


        while ((line = br.readLine()) != null) {
            String[] arr = line.split("\t");
            date2.setTime(Long.parseLong(arr[0]) * 1000);
            cal2.setTime(date2);
            int dayOutput = cal2.get(DAY_OF_MONTH);
            if (dayOutput != daySale) continue;
            int length = arr.length;
            if ((length - 2) % 2 != 0) continue;
            String txID = arr[1];
            if (txID.length() != txLength) {
                System.out.println("Wrong txID id" + txID);
            }

            for (int j = 2; j < length; j = j + 2) {
                String address = arr[j];
                if (!address.equalsIgnoreCase("noaddress")) {
                    int index = (j - 2) / 2;
                    long amo = Long.parseLong(arr[j + 1]);
                    int numOutputs = (length - 2) / 2;
                    Output o = new Output(txID, address, index, amo, numOutputs);
                    holder.add(o);
                }
            }
        }
        return holder;
    }


    private static long inSec(int year, int month, int day) {
        return new GregorianCalendar(year, month, day).getTime().getTime() / 1000;
    }

}
