package Core;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WordCount implements MapReduce {

    public static final String SEPARATOR = " - ";

    @Override
    public void executeMap(String blockin, String blockout) {
        // read from blockin, compute count table, write to blockout
        System.out.println("Start mapping");
        HashMap<String, Integer> hm = new HashMap<>();
        try {
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(new FileInputStream(blockin)));
            BufferedWriter bw = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(blockout)));

            String line;
            while ((line = br.readLine()) != null) {
                StringTokenizer st = new StringTokenizer(line);
                while (st.hasMoreTokens()) {
                    String tok = st.nextToken();
                    if (hm.containsKey(tok)) {
                        hm.put(tok, hm.get(tok) + 1);
                    } else {
                        hm.put(tok, 1);
                    }
                }
            }

            for (String k : hm.keySet()) {
                bw.write(k + SEPARATOR + hm.get(k).toString());
                bw.newLine();
            }
            br.close();
            bw.close();
            System.out.println("Done mapping");
        } catch (IOException ex) {
            Logger.getLogger(WordCount.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void executeReduce(Collection<String> blocks, String finalresults) {
        // read all files in blocks, merge and write to finalresults
        HashMap<String, Integer> hm = new HashMap<>();
        try {
            for (String block : blocks) {
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(new FileInputStream(block)));
                String line;
                while ((line = br.readLine()) != null) {
                    try {
                        if (line.trim().equals("")) {
                            continue;
                        }
                        String kv[] = line.split(SEPARATOR);
                        String k = kv[0];
                        int v = Integer.parseInt(kv[1]);
                        if (hm.containsKey(k)) {
                            hm.put(k, hm.get(k) + v);
                        } else {
                            hm.put(k, v);
                        }
                    } catch (NumberFormatException ex) {
                        System.out.println("Error at split: " + ex.getMessage());
                    }
                }
                br.close();
            }
            BufferedWriter bw = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(finalresults)));
            for (String k : hm.keySet()) {
                bw.write(k + SEPARATOR + hm.get(k).toString());
                bw.newLine();
            }
            bw.close();
        } catch (IOException ex) {
            Logger.getLogger(WordCount.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
