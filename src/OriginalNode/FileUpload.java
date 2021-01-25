package OriginalNode;

import Utility.Padding;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileUpload extends Thread {

    private final static int LIMIT = Integer.MAX_VALUE / 8;
//    private final static int LIMIT = 10 * 1024 * 1024;

    private final String host;
    private final int port;

    private final String fileToSend;
    private final long start;
    private final long byteSize;

    public FileUpload(String host, int port, String fileToSend) {
        this.host = host;
        this.port = port;
        this.fileToSend = fileToSend;
        this.start = 0;
        this.byteSize = new File(fileToSend).length();
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getFileToSend() {
        return fileToSend;
    }

    @Override
    public void run() {
        Socket socket = null;

        try {
            socket = new Socket(host, port);
        } catch (IOException ex) {
            System.out.println("IOException - Opening file:" + ex.getMessage());
        }

        requestCommand(socket);
        sendFileToServer(socket);

        try {
            socket.close();
        } catch (IOException ex) {
            Logger.getLogger(FileUpload.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void sendFileToServer(Socket socket) {
        BufferedOutputStream outToServer = null;
        try {
            outToServer = new BufferedOutputStream(socket.getOutputStream());
        } catch (IOException ex) {
            Logger.getLogger(FileUpload.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (outToServer != null) {
            File myFile = new File(fileToSend);
            RandomAccessFile raf = null;

            try {
                raf = new RandomAccessFile(myFile, "r");
            } catch (FileNotFoundException ex) {
                System.out.println("File not found: " + myFile + ex);
            }

            long remainingByte = myFile.length();

            long count = 0;
            while (remainingByte > 0) {
                System.out.println(java.time.LocalTime.now() + " " + host + ":" + port + " Sending segment " + (count + 1));
                byte[] mybytearray = new byte[remainingByte > LIMIT ? LIMIT : Math.toIntExact(remainingByte)];

                try {
                    raf.seek(start + count * LIMIT);
                    raf.read(mybytearray, 0, mybytearray.length);
                    count++;
                    outToServer.write(mybytearray, 0, mybytearray.length);
                    outToServer.flush();
                } catch (IOException ex) {
                    Logger.getLogger(FileUpload.class.getName()).log(Level.SEVERE, null, ex);
                }
                remainingByte -= LIMIT;
                System.out.println(java.time.LocalTime.now() + " " + host + ":" + port + " Send segment " + count + " successfully");
            }

            try {
                raf.close();
            } catch (IOException ex) {
                Logger.getLogger(FileUpload.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void requestCommand(Socket socket) {
        BufferedOutputStream outToServer = null;
        try {
            outToServer = new BufferedOutputStream(socket.getOutputStream());
        } catch (IOException ex) {
            Logger.getLogger(FileUpload.class.getName()).log(Level.SEVERE, null, ex);
        }

        byte[] command = (Padding.rightPad("POST:", 16, ' ').getBytes());

        try {
            outToServer.write(command);
            outToServer.flush();
        } catch (IOException ex) {
            Logger.getLogger(FileUpload.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
