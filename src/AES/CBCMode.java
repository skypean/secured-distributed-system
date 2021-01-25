package AES;

import Utility.GetBytes;
import Utility.Padding;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CBCMode extends Thread {

    public static final int ENCRYPT = 0;
    public static final int DECRYPT = 1;

    private final String inputFile, outputFile;
    private final long offset;
    private final long byteSize;
    private final int mode;
    private final byte[] key;

    public CBCMode(String inputFile, long offset, long byteSize, int mode, String outputFile) {
        this.inputFile = inputFile;
        this.offset = offset;
        this.byteSize = byteSize;
        this.mode = mode;
        this.outputFile = outputFile;
        key = new GetBytes("key.txt", 16).getBytes(0);
        if (new File(outputFile).exists()) {
            new File(outputFile).delete();
        }
    }

    @Override
    public void run() {
        switch (mode) {
            case ENCRYPT:
                encrypt();
                break;
            case DECRYPT:
                decrypt();
                break;
            default:
                break;
        }
    }

    public void encrypt() {
        byte cbc[] = new byte[16];

        RandomAccessFile raf = null;
        FileChannel fileChannel = null;
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;

        AESEncrypt aesEncrypt = new AESEncrypt(key, 4);

        try {
            raf = new RandomAccessFile(inputFile, "r");
            fos = new FileOutputStream(outputFile);
            bos = new BufferedOutputStream(fos);
            fileChannel = raf.getChannel();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(CBCMode.class.getName()).log(Level.SEVERE, null, ex);
        }

        ByteBuffer in;
        long remainingBytes = byteSize;

        try {
            raf.seek(offset);
        } catch (IOException ex) {
            Logger.getLogger(CBCMode.class.getName()).log(Level.SEVERE, null, ex);
        }

        while (remainingBytes > 0) {
            in = ByteBuffer.allocate(remainingBytes >= 16 ? 16 : Math.toIntExact(remainingBytes));
            try {
                fileChannel.read(in);
            } catch (IOException ex) {
                Logger.getLogger(CBCMode.class.getName()).log(Level.SEVERE, null, ex);
            }

            byte _in[] = Padding.padding(in.array());
            for (int i = 0; i < _in.length; i++) {
                cbc[i] ^= _in[i];
            }

            aesEncrypt.cipher(cbc, cbc);

            try {
                bos.write(cbc, 0, cbc.length);
                bos.flush();
            } catch (IOException ex) {
                Logger.getLogger(CBCMode.class.getName()).log(Level.SEVERE, null, ex);
            }

            in.clear();
            remainingBytes -= 16;
        }
        try {
            fileChannel.close();
            raf.close();
            fos.close();
            bos.close();
        } catch (IOException ex) {
            Logger.getLogger(CBCMode.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void decrypt() {
        byte prevCbc[] = new byte[16];
        long remainingBytes = byteSize;

        RandomAccessFile raf = null;
        FileChannel fileChannel;
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;

        try {
            raf = new RandomAccessFile(inputFile, "r");
            fos = new FileOutputStream(outputFile);
            bos = new BufferedOutputStream(fos);
            raf.seek(offset);
        } catch (IOException ex) {
            Logger.getLogger(CBCMode.class.getName()).log(Level.SEVERE, null, ex);
        }

        fileChannel = raf.getChannel();
        ByteBuffer in;
        AESDecrypt aesDecrypt = new AESDecrypt(key, 4);

        while (remainingBytes > 0) {
            in = ByteBuffer.allocate(remainingBytes >= 16 ? 16 : Math.toIntExact(remainingBytes));
            try {
                fileChannel.read(in);
            } catch (IOException ex) {
                Logger.getLogger(CBCMode.class.getName()).log(Level.SEVERE, null, ex);
            }

            byte _in[] = Padding.padding(in.array());
            byte out[] = new byte[16];
            aesDecrypt.invCipher(_in, out);
            for (int i = 0; i < _in.length; i++) {
                out[i] ^= prevCbc[i];
            }
            prevCbc = Arrays.copyOf(_in, _in.length);

            try {
                bos.write(out, 0, out.length);
                bos.flush();
            } catch (IOException ex) {
                Logger.getLogger(CBCMode.class.getName()).log(Level.SEVERE, null, ex);
            }

            in.clear();
            remainingBytes -= 16;
        }

        try {
            fileChannel.close();
            raf.close();
            bos.close();
            fos.close();
        } catch (IOException ex) {
            Logger.getLogger(CBCMode.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
