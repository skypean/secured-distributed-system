package Core;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface CallBack extends Remote {

    void completed() throws RemoteException;

    void waitforall() throws RemoteException;
}
