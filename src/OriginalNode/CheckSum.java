
package OriginalNode;

import Utility.Padding;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CheckSum extends Thread {

    private boolean check;
    private final String serverHost;
    private final int serverPort;
    private final String fileToCheck;

    public CheckSum(String serverHost, int serverPort, String fileToCheck) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.fileToCheck = fileToCheck;
        check = false;
    }

    @Override
    public void run() {
        while (true) {
            if (check) {
                return;
            } else {
                Socket clientSocket = null;
                try {
                    clientSocket = new Socket(serverHost, serverPort);
                } catch (IOException ex) {
                    Logger.getLogger(CheckSum.class.getName()).log(Level.SEVERE, null, ex);
                }
                requestCommand(clientSocket);
                long fileSize = new File(fileToCheck).length();
                System.out.println("Getting byte size from server...");
                long checkSum = getByteSize(clientSocket);
                check = (fileSize == checkSum);
                try {
                    clientSocket.close();
                } catch (IOException ex) {
                    Logger.getLogger(CheckSum.class.getName()).log(Level.SEVERE, null, ex);
                }
                if (check) {
                    System.out.println("Server has done downloaded files");
                    return;
                } else {
                    System.out.println("Wating for server.. (" + checkSum + " " + fileSize + ")");
                }
            }
            try {
                sleep(30 * 1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(CheckSum.class.getName()).log(Level.SEVERE, null, ex);
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

        byte[] command = (Padding.rightPad("CHECK:", 16, ' ').getBytes());

        try {
            outToServer.write(command);
            outToServer.flush();
        } catch (IOException ex) {
            Logger.getLogger(FileUpload.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private long getByteSize(Socket socket) {
        byte[] byteSize = new byte[20];
        InputStream is = null;
        try {
            is = socket.getInputStream();
        } catch (IOException ex) {
            Logger.getLogger(CheckSum.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            is.read(byteSize);
        } catch (IOException ex) {
            Logger.getLogger(CheckSum.class.getName()).log(Level.SEVERE, null, ex);
        }
        return Long.parseLong(new String(byteSize).trim());
    }
}
