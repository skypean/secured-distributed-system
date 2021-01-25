package Core;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Daemon extends Remote {

    public void call(String blockin, String blockout, int salt, String host, String cbName, int cbPort, String fileNameToSaveTime) throws RemoteException;
}
