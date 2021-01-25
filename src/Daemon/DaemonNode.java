package Daemon;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import Core.Slave;
import java.net.ServerSocket;
import java.net.Socket;

public class DaemonNode {

    final static String[] hosts = {"192.168.0.100", "192.168.0.105", "192.168.0.103"};
    final static int[] ports = {8081, 8082, 8083};

    public static void main(String[] args) {
        final int serverPortIndex = Integer.parseInt(args[0]);
        System.setProperty("java.rmi.server.hostname", hosts[serverPortIndex]);
        System.setProperty("java.security.policy", "test.policy");

        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }

        new DaemonRMI(ports[serverPortIndex] + "").start();
        ServerSocket serverSocket;
        Socket clientSocket;
        try {
            serverSocket = new ServerSocket(ports[serverPortIndex]);
            System.out.println("Server is ready");
            while (true) {
                clientSocket = serverSocket.accept();
                new Slave(clientSocket, ports[serverPortIndex]).start();
            }
        } catch (IOException ex) {
            Logger.getLogger(DaemonSocket.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
