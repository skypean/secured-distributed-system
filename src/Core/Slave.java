package Core;

import Daemon.DaemonSocket;
import java.net.Socket;

public class Slave extends Thread {

    Socket clientSocket;
    int port;

    public Slave(Socket clientSocket, int port) {
        this.clientSocket = clientSocket;
        this.port = port;
    }

    public void run() {
        new DaemonSocket(clientSocket, port).start();
    }

}
