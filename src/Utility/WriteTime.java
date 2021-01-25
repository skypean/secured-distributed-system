package Utility;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class WriteTime {

    public static void writeTimeToFile(String fileName, String content) {
        String fileNameOut = fileName + ".txt";
        RandomAccessFile out = null;
        try {
            out = new RandomAccessFile(fileNameOut, "rw");
        } catch (FileNotFoundException ex) {
            System.err.println("Exception opening file: " + ex.getMessage());
        }
        try {
            out.seek(out.length());
        } catch (IOException ex) {
            System.err.println("Exception seek file pointer " + ex.getMessage());
        }
        try {
            out.write(content.getBytes());
        } catch (IOException ex) {
            System.err.println("Exception writing file: " + ex.getMessage());
        }
        try {
            out.close();
        } catch (IOException ex) {
            System.err.println("Exception closing file: " + ex.getMessage());
        }

    }
}
