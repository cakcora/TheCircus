import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class SortFile {
    private static final Logger logger = LoggerFactory.getLogger(SortFile.class);

    public static void main(String args[]) throws IOException {
        String dataDir = args[0];
        String outDir = args[1];
        FileUtils.cleanDirectory(new File(outDir));

        splitIntoDays(dataDir, "inputs", outDir);
        splitIntoDays(dataDir, "outputs", outDir);
    }

    private static void splitIntoDays(String dataDir, String type, String outDir) throws IOException {
        String line;

        HashMap<String, List<String>> dayHolder = new HashMap<>();
        String fPath = dataDir + type +".txt";
        BufferedReader br = new BufferedReader(new FileReader(fPath));
        DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

        while ((line = br.readLine()) != null) {
            String arr[] = line.split("\t");
            LocalDate dt =  LocalDate.parse(arr[1], formatter);
            int day = dt.getDayOfYear();
            int year = dt.year().get();

            String key = year + "_" + day;
            if (!dayHolder.containsKey(key)) {
                dayHolder.put(key, new ArrayList<String>());
            }
            dayHolder.get(key).add(line);
            if (dayHolder.get(key).size() > 5000) {
                writeContent(type, key,dayHolder, outDir);
                dayHolder.get(key).clear();
            }
        }
        br.close();
        for (String key : dayHolder.keySet()) {
            if (dayHolder.get(key).size() > 0) {
                writeContent(type, key, dayHolder, outDir);
            }
        }
    }

    private static void writeContent(String type, String key, HashMap<String, List<String>> con, String outDir) throws IOException {
        BufferedWriter wr = new BufferedWriter(new FileWriter(outDir + type + key+ ".txt", true));
        for (String s : con.get(key)) {
            wr.append(s + "\r\n");
        }
        wr.close();
    }
}