package Utility;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GetBytes {

    private String fileName;
    private int arraySize;
    private RandomAccessFile in;

    public GetBytes(String file, int n) {
        fileName = file;
        arraySize = n;
        try {
            in = new RandomAccessFile(fileName, "r");
        } catch (FileNotFoundException ex) {
            Logger.getLogger(GetBytes.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public byte[] getBytes(final long offset) {
        try {
            in.seek(offset);
        } catch (IOException ex) {
            Logger.getLogger(GetBytes.class.getName()).log(Level.SEVERE, null, ex);
        }
        byte[] ret = new byte[arraySize];
        try {
            in.read(ret);
        } catch (IOException ex) {
            Logger.getLogger(GetBytes.class.getName()).log(Level.SEVERE, null, ex);
        }
        return ret;
    }
}
