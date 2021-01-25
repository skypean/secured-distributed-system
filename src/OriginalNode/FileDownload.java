package OriginalNode;

import Utility.Padding;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileDownload extends Thread {

    private final static int LIMIT = Integer.MAX_VALUE / 5;
//    private final static int LIMIT = 10 * 1024 * 1024;

    Socket socket;
    String fileName;

    public FileDownload(Socket socket, String fileName) {
        this.socket = socket;
        this.fileName = fileName;
    }

    @Override
    public void run() {

        requestCommand();
        getFileFromServer();

        try {
            socket.close();
        } catch (IOException ex) {
            Logger.getLogger(FileDownload.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void requestCommand() {
        BufferedOutputStream outToServer = null;

        try {
            outToServer = new BufferedOutputStream(socket.getOutputStream());
        } catch (IOException ex) {
            Logger.getLogger(FileUpload.class.getName()).log(Level.SEVERE, null, ex);
        }

        byte[] command = (Padding.rightPad("GET:", 16, ' ').getBytes());
        try {
            outToServer.write(command);
            outToServer.flush();
        } catch (IOException ex) {
            Logger.getLogger(FileDownload.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void getFileFromServer() {
        InputStream is = null;
        try {
            is = socket.getInputStream();
        } catch (IOException ex) {
            Logger.getLogger(FileUpload.class.getName()).log(Level.SEVERE, null, ex);
        }
        byte[] aByte = new byte[1];
        int bytesRead;

        ByteArrayOutputStream baos;
        if (is != null) {
            int count = 1;
            FileOutputStream fos = null;
            BufferedOutputStream bos = null;
            try {
                fos = new FileOutputStream(fileName);
                bos = new BufferedOutputStream(fos);
                bytesRead = is.read(aByte, 0, aByte.length);

                baos = new ByteArrayOutputStream();
                do {
                    baos.write(aByte);
                    bytesRead = is.read(aByte);
                    if (baos.size() >= LIMIT) {
                        System.out.println(java.time.LocalTime.now() + " " + socket.getPort() + " Get segment " + count);
                        count++;
                        bos.write(baos.toByteArray());
                        bos.flush();
                        baos = new ByteArrayOutputStream();
                    }
                } while (bytesRead != -1);

                if (baos.size() > 0) {
                    System.out.println(java.time.LocalTime.now() + " " + socket.getPort() + " Get segment " + count);
                    bos.write(baos.toByteArray());
                    bos.flush();
                }
                bos.close();
            } catch (IOException ex) {
                Logger.getLogger(FileUpload.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
