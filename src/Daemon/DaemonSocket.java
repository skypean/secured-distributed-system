package Daemon;

import Utility.GetBytes;
import Utility.Padding;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DaemonSocket extends Thread {

    private final static int LIMIT = Integer.MAX_VALUE / 8;
//    private final static int LIMIT = 10 * 1024 * 1024;

    byte[] key;
    Socket clientSocket;
    int port;
    String blockIn, blockOut;

    public DaemonSocket(Socket clientSocket, int port) {
        this.clientSocket = clientSocket;
        this.port = port;
        blockIn = "Daemon/encrypt-file" + port + ".txt";
        blockOut = "Daemon/encrypt-map" + port + ".txt";
        key = new GetBytes("key.txt", 16).getBytes(0);
    }

    @Override
    public void run() {

        String command = getCommand();
        System.out.println(java.time.LocalTime.now() + " GET COMMAND: " + command);
        if (command.startsWith("POST")) {
            getFileFromClient();
        } else if (command.startsWith("GET")) {
            sendFileToClient();
        } else if (command.startsWith("CHECK")) {
            sendByteSize();
        }

        try {
            clientSocket.close();
        } catch (IOException ex) {
            Logger.getLogger(DaemonSocket.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private String getCommand() {
        byte[] command = new byte[16];
        InputStream is = null;
        try {
            is = clientSocket.getInputStream();
        } catch (IOException ex) {
            Logger.getLogger(DaemonSocket.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            is.read(command);
        } catch (IOException ex) {
            Logger.getLogger(DaemonSocket.class.getName()).log(Level.SEVERE, null, ex);
        }
        return new String(command).trim();
    }

    private void sendByteSize() {
        BufferedOutputStream outToClient = null;
        try {
            outToClient = new BufferedOutputStream(clientSocket.getOutputStream());
        } catch (IOException ex) {
            Logger.getLogger(DaemonSocket.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (outToClient != null) {
            long byteSize = new File(blockIn).length();
            byte[] sizeInBytes = Padding.rightPad(byteSize + "", 20, ' ').getBytes();

            try {
                outToClient.write(sizeInBytes, 0, sizeInBytes.length);
                outToClient.flush();
            } catch (IOException ex) {
                Logger.getLogger(DaemonSocket.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void getFileFromClient() {
        System.out.println(java.time.LocalTime.now() + " Getting files from client...");
        byte[] aByte = new byte[1];
        int bytesRead;

        InputStream is = null;
        try {
            is = clientSocket.getInputStream();
        } catch (IOException ex) {
            Logger.getLogger(DaemonSocket.class.getName()).log(Level.SEVERE, null, ex);
        }

        ByteArrayOutputStream baos;
        if (is != null) {
            int count = 1;
            FileOutputStream fos;
            BufferedOutputStream bos;

            try {
                fos = new FileOutputStream(blockIn);
                bos = new BufferedOutputStream(fos);
                bytesRead = is.read(aByte, 0, aByte.length);

                baos = new ByteArrayOutputStream(1);
                do {
                    baos.write(aByte);
                    bytesRead = is.read(aByte, 0, aByte.length);

                    if (baos.size() >= LIMIT) {
                        System.out.println(java.time.LocalTime.now() + " Get segment " + count);
                        count++;
                        bos.write(baos.toByteArray());
                        bos.flush();
                        baos = new ByteArrayOutputStream(1);
                    }
                } while (bytesRead != -1);

                if (baos.size() > 0) {
                    System.out.println(java.time.LocalTime.now() + " Get remaining segment " + count);
                    bos.write(Padding.padding(baos.toByteArray()));
                    bos.flush();
                }
                bos.close();
            } catch (IOException ex) {
                Logger.getLogger(DaemonSocket.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        System.out.println(java.time.LocalTime.now() + " Get files done!");
    }

    private void sendFileToClient() {
        BufferedOutputStream outToClient = null;

        try {
            outToClient = new BufferedOutputStream(clientSocket.getOutputStream());
        } catch (IOException ex) {
            Logger.getLogger(DaemonSocket.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (outToClient != null) {
            File myFile = new File(blockOut);
            RandomAccessFile raf = null;

            try {
                raf = new RandomAccessFile(myFile, "r");
            } catch (FileNotFoundException ex) {
                System.out.println("File Not Found - " + blockOut + ex);
            }

            long remainingByte = myFile.length();

            long count = 0;
            while (remainingByte > 0) {
                System.out.println(java.time.LocalTime.now() + " Sending segment " + (count + 1));
                byte[] mybytearray = new byte[remainingByte > LIMIT ? LIMIT : Math.toIntExact(remainingByte)];

                try {
                    raf.seek(count * LIMIT);
                    raf.read(mybytearray, 0, mybytearray.length);
                    count++;
                    outToClient.write(mybytearray, 0, mybytearray.length);
                    outToClient.flush();
                } catch (IOException ex) {
                    Logger.getLogger(DaemonSocket.class.getName()).log(Level.SEVERE, null, ex);
                }

                remainingByte -= LIMIT;
                System.out.println(java.time.LocalTime.now() + " Send segment " + count + " successfully");
            }

            try {
                raf.close();
            } catch (IOException ex) {
                Logger.getLogger(DaemonSocket.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
