package Daemon;

import Core.Daemon;
import Core.DaemonImpl;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DaemonRMI extends Thread {

    String path;

    public DaemonRMI(String path) {
        this.path = path;
    }

    @Override
    public void run() {
        String name = "daemon" + this.path;
        Daemon engine = new DaemonImpl();
        Daemon stub = null;
        try {
            stub = (Daemon) UnicastRemoteObject.exportObject(engine, 0);
        } catch (RemoteException ex) {
            Logger.getLogger(DaemonRMI.class.getName()).log(Level.SEVERE, null, ex);
        }

        Registry registry = null;

        try {
            registry = LocateRegistry.getRegistry(1099);
        } catch (RemoteException ex) {
            Logger.getLogger(DaemonRMI.class.getName()).log(Level.SEVERE, null, ex);
        }

        try {
            registry.rebind(name, stub);
        } catch (RemoteException ex) {
            Logger.getLogger(DaemonRMI.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("RMI is ready");
    }
}
