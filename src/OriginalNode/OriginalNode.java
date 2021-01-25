package OriginalNode;

import Utility.WriteTime;
import AES.CBCMode;
import Core.CallBack;
import Core.CallBackImpl;
import Core.WordCount;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OriginalNode {

    final private static String[] hosts = {"192.168.0.100", "192.168.0.105", "192.168.0.103"};
    final private static int[] ports = {8081, 8082, 8083};
    final private static String CLIENT_HOST = "192.168.0.106";
    final private static int NB_HOSTS = hosts.length;
    final private static String FILE_RESULT = "OriginalNode/finalResult.txt";
    final private static String FILENAME_SEPARATOR = "-";
    final private static String CONTENT_SEPARATOR = " ";

    static String FILE_NAME;
    static int nbNeededHosts;

    public static boolean isSocketAlive(String hostName, int port) {
        boolean isAlive = false;

        SocketAddress socketAddress = new InetSocketAddress(hostName, port);
        Socket socket = new Socket();

        int timeout = 2000;

        try {
            socket.connect(socketAddress, timeout);
            socket.close();
            isAlive = true;
        } catch (SocketTimeoutException ex) {
            System.out.println("SocketTimeoutExpcetion " + hostName + ":" + port + ". " + ex.getMessage());
        } catch (IOException ex) {
            System.out.println("IOException - Unable to connect to " + hostName + ":" + port + ". " + ex.getMessage());
        }

        return isAlive;
    }

    public static void main(String[] args) {
        long startProgramTime = System.currentTimeMillis();

        if (args.length != 2) {
            System.err.println("java OriginalNode <BigFile Name> <Number of Hosts>");
            if (!(args[1] + "").matches("[1-4]")) {
                System.err.println("Number of hosts must be from 1 to 4");
            }
            return;
        }

        // Check alive socket
        FILE_NAME = args[0];
        nbNeededHosts = Integer.parseInt(args[1] + "");

        int nbActiveHosts = 0;
        boolean isActive[] = new boolean[NB_HOSTS];

        for (int i = 0; i < hosts.length; i++) {
            isActive[i] = isSocketAlive(hosts[i], ports[i]);
            if (isActive[i]) {
                nbActiveHosts++;
            }
        }

        if (nbActiveHosts < nbNeededHosts) {
            return;
        }

        String[] activeHosts = new String[nbNeededHosts];
        int[] activePorts = new int[nbNeededHosts];
        ArrayList<String> encryptedUploadFileNames = new ArrayList<>();

        int indexActiveSock = 0;
        for (int i = 0; i < hosts.length; i++) {
            if (isActive[i]) {
                activeHosts[indexActiveSock] = hosts[i];
                activePorts[indexActiveSock++] = ports[i];
                String encryptedFileName = "OriginalNode/encrypt-upload" + ports[i] + ".txt";
                encryptedUploadFileNames.add(encryptedFileName);
            }
            if (indexActiveSock == nbNeededHosts) {
                break;
            }
        }

        System.setProperty("java.security.policy", "test.policy");

        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }

        Split split = new Split(FILE_NAME);

        long startSplitTime = System.currentTimeMillis();

        CBCMode[] cbcEncrypts = split.splitFileByParts(nbNeededHosts, activePorts, encryptedUploadFileNames);

        long elapsedSplitTime = System.currentTimeMillis() - startSplitTime;

        // Encrypting files
        System.out.println("Encypting files...");

        long startEncryptTime = System.currentTimeMillis();

        for (CBCMode cbcEncrypt : cbcEncrypts) {
            cbcEncrypt.start();
        }

        for (CBCMode cbcEncrypt : cbcEncrypts) {
            while (cbcEncrypt.isAlive()) {
                try {
                    cbcEncrypt.join();
                } catch (InterruptedException ex) {
                    Logger.getLogger(OriginalNode.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        long elapsedEncryptTime = System.currentTimeMillis() - startEncryptTime;

        System.out.println("Encryption done!");

        //Encryption done
        //Uploading files...
        System.out.println("Uploading files...");

        FileUpload[] fileUploads = new FileUpload[nbNeededHosts];

        long startUploadTime = System.currentTimeMillis();

        for (int i = fileUploads.length - 1; i >= 0; i--) {
            fileUploads[i] = new FileUpload(activeHosts[i], activePorts[i], encryptedUploadFileNames.get(i));
            fileUploads[i].start();
        }

        for (FileUpload fileUpload : fileUploads) {
            while (fileUpload.isAlive()) {
                try {
                    fileUpload.join();
                } catch (InterruptedException ex) {
                    Logger.getLogger(OriginalNode.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        System.out.println("Upload done!");

        //Upload done
        //Checking
        System.out.println("Checking...");

        CheckSum[] checks = new CheckSum[fileUploads.length];

        for (int i = checks.length - 1; i >= 0; i--) {
            checks[i] = new CheckSum(fileUploads[i].getHost(), fileUploads[i].getPort(), fileUploads[i].getFileToSend());
            checks[i].start();
        }

        for (CheckSum check : checks) {
            while (check.isAlive()) {
                try {
                    check.join();
                } catch (InterruptedException ex) {
                    Logger.getLogger(OriginalNode.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        long elapsedUploadTime = System.currentTimeMillis() - startUploadTime;

        System.out.println("Check done!");

        //Execute job at server
        System.out.println(java.time.LocalTime.now() + " Executing at servers...");

        long startExcutingTime = System.currentTimeMillis();

        System.setProperty("java.rmi.server.hostname", CLIENT_HOST);

        CallBack cb = null;

        try {
            cb = new CallBackImpl(nbNeededHosts);
        } catch (RemoteException ex) {
            Logger.getLogger(Launch.class.getName()).log(Level.SEVERE, null, ex);
        }
        String cbName = "callback";
        int cbPort = 1100;
        CallBack stub = null;
        try {
            stub = (CallBack) UnicastRemoteObject.exportObject(cb, 0);
        } catch (RemoteException ex) {
            Logger.getLogger(Launch.class.getName()).log(Level.SEVERE, null, ex);
        }
        Registry cbRegistry = null;
        try {
            cbRegistry = LocateRegistry.getRegistry(cbPort);
        } catch (RemoteException ex) {
            Logger.getLogger(Launch.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            cbRegistry.rebind(cbName, stub);
        } catch (RemoteException ex) {
            Logger.getLogger(Launch.class.getName()).log(Level.SEVERE, null, ex);
        }

        //Mapping files in all servers
        Launch[] srvMap = new Launch[nbNeededHosts];
        for (int i = 0; i < nbNeededHosts; i++) {
            srvMap[i] = new Launch(activeHosts[i], activePorts[i], i, cbName, cbPort);
            srvMap[i].start();
        }

        for (Launch launch : srvMap) {
            while (launch.isAlive()) {
                try {
                    launch.join();
                } catch (InterruptedException ex) {
                    Logger.getLogger(OriginalNode.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        System.out.println("Waiting...");
        try {
            cb.waitforall();
        } catch (RemoteException ex) {
            Logger.getLogger(Launch.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("Done mapping");

        long elapsedExecutingTime = System.currentTimeMillis() - startExcutingTime;

        //Executing at server done
        //Downloading files...
        System.out.println("Downloading files...");

        FileDownload[] fileDownloads = new FileDownload[nbNeededHosts];
        ArrayList<String> encryptedDownloadFileNames = new ArrayList<>();
        ArrayList<String> decryptedDownloadFileNames = new ArrayList<>();

        long startDownloadTime = System.currentTimeMillis();

        for (int i = fileDownloads.length - 1; i >= 0; i--) {
            try {
                Socket socket = new Socket(activeHosts[i], activePorts[i]);
                String downloadFileName = "OriginalNode/encrypt-download" + activePorts[i] + ".txt";
                encryptedDownloadFileNames.add(downloadFileName);

                String decryptFileName = "OriginalNode/map" + activePorts[i] + ".txt";
                decryptedDownloadFileNames.add(decryptFileName);

                fileDownloads[i] = new FileDownload(socket, downloadFileName);
                fileDownloads[i].start();
            } catch (IOException ex) {
                Logger.getLogger(Launch.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        long elapsedDownloadTime = System.currentTimeMillis() - startDownloadTime;

        for (int i = 0; i < fileDownloads.length; ++i) {
            while (fileDownloads[i].isAlive()) {
                try {
                    fileDownloads[i].join();
                } catch (InterruptedException ex) {
                    Logger.getLogger(OriginalNode.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        System.out.println("Download done!");
        //Download all fileDownloads successfully

        //Decrypting downloaded files
        System.out.println("Decrypting files...");

        CBCMode[] cbcDecrypts = new CBCMode[encryptedDownloadFileNames.size()];

        long startDecryptTime = System.currentTimeMillis();

        for (int i = 0; i < cbcDecrypts.length; i++) {
            String inputFile = encryptedDownloadFileNames.get(i);
            long byteSize = new File(inputFile).length();
            String outputFile = decryptedDownloadFileNames.get(i);
            cbcDecrypts[i] = new CBCMode(inputFile, 0, byteSize, CBCMode.DECRYPT, outputFile);
            cbcDecrypts[i].start();
        }

        for (CBCMode cbcDecrypt : cbcDecrypts) {
            while (cbcDecrypt.isAlive()) {
                try {
                    cbcDecrypt.join();
                } catch (InterruptedException ex) {
                    Logger.getLogger(OriginalNode.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        long elapsedDecryptTime = System.currentTimeMillis() - startDecryptTime;

        System.out.println("Decrypt done!");
        //Decrypt done!

        long startReduceTime = System.currentTimeMillis();

        new WordCount().executeReduce(decryptedDownloadFileNames, FILE_RESULT);

        // Reduce fle successfully
        long elapsedReduceTime = System.currentTimeMillis() - startReduceTime;

        long elapsedProgramTime = System.currentTimeMillis() - startProgramTime;

        // Write all executing times to a file
        final String timeFileName = "client-timeResult" + FILENAME_SEPARATOR
                + FILE_NAME.substring(0, FILE_NAME.indexOf(".")) + FILENAME_SEPARATOR + nbNeededHosts;
        final String timeResult = elapsedSplitTime + CONTENT_SEPARATOR + elapsedEncryptTime + CONTENT_SEPARATOR + elapsedUploadTime + CONTENT_SEPARATOR
                + elapsedExecutingTime + CONTENT_SEPARATOR + elapsedDownloadTime + CONTENT_SEPARATOR + elapsedDecryptTime + CONTENT_SEPARATOR + elapsedReduceTime
                + CONTENT_SEPARATOR + elapsedProgramTime + "\n";

        WriteTime.writeTimeToFile(timeFileName, timeResult);

        System.exit(0);
    }
}
