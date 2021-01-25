package Core;

import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CallBackImpl implements CallBack {

    int nbnode;
    int completedNode;

    public CallBackImpl(int n) throws RemoteException {
        nbnode = n;
        completedNode = 0;
    }

    @Override
    public synchronized void completed() throws RemoteException {
        completedNode++;
        System.out.println(java.time.LocalTime.now() + " Completed!");
        notify();
        System.out.println(java.time.LocalTime.now() + " Notify!");
    }

    @Override
    public synchronized void waitforall() throws RemoteException {
        if (completedNode < nbnode) {
            try {
                wait();
            } catch (InterruptedException ex) {
                Logger.getLogger(CallBackImpl.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
