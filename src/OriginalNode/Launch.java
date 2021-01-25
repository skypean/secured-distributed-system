package OriginalNode;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import Core.Daemon;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Launch extends Thread {

    private static final String CLIENT_HOST = "192.168.0.106";
//    private static final String CLIENT_HOST = "localhost";
    String host;
    int port;
    int index;
    String cbName;
    int cbPort;

    public Launch(String host, int port, int index, String cbName, int cbPort) {
        this.host = host;
        this.port = port;
        this.index = index;
        this.cbName = cbName;
        this.cbPort = cbPort;
    }

    @Override
    public void run() {
        //Mapping files in all servers
            try {
                String name = "daemon" + port;
                int serverPort = 1099;
                Registry registry = LocateRegistry.getRegistry(host, serverPort);
                Daemon daemon = (Daemon) registry.lookup(name);

                System.out.println(java.time.LocalTime.now() + " Got daemon");

                String blockIn = "Daemon/encrypt-file" + port + ".txt";
                String blockOut = "Daemon/map" + port + ".txt";

                System.out.println(java.time.LocalTime.now() + " Running RMI: " + (index + 1));
                daemon.call(blockIn, blockOut, port, CLIENT_HOST, cbName, cbPort,
                        "timeResult" + "-" + OriginalNode.FILE_NAME.substring(0, OriginalNode.FILE_NAME.indexOf(".")) + "-" + OriginalNode.nbNeededHosts);
                System.out.println(java.time.LocalTime.now() + " Completed RMI: " + (index + 1));
            } catch (NotBoundException | RemoteException ex) {
                Logger.getLogger(Launch.class.getName()).log(Level.SEVERE, null, ex);
            }
        
    }
}
