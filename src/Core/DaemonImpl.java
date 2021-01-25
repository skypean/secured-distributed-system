package Core;

import AES.CBCMode;
import Utility.WriteTime;
import java.io.File;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DaemonImpl implements Daemon {

    @Override
    public void call(String blockin, String blockout, int salt, String clientHost, String cbName, int cbPort, String fileNameToSaveTime) throws RemoteException {
        String decryptedBlockIn = "Daemon/file" + salt + ".txt";
        String encryptedBlockOut = "Daemon/encrypt-map" + salt + ".txt";

        CBCMode cbcDecrypt = new CBCMode(blockin, 0, new File(blockin).length(), CBCMode.DECRYPT, decryptedBlockIn);

        long startDecryptTime = System.currentTimeMillis();

        cbcDecrypt.run();

        long elapsedDecryptTime = System.currentTimeMillis() - startDecryptTime;

        MapReduce m = new WordCount();

        System.out.println(java.time.LocalTime.now() + " Mapping...");    
        
        long startMapTime = System.currentTimeMillis();

        m.executeMap(decryptedBlockIn, blockout);

        long endMapTime = System.currentTimeMillis();

        CBCMode cbcEncrypt = new CBCMode(blockout, 0, new File(blockout).length(), CBCMode.ENCRYPT, encryptedBlockOut);

        long startEncryptTime = System.currentTimeMillis();

        cbcEncrypt.run();

        long elapsedEncryptTime = System.currentTimeMillis() - startEncryptTime;

        System.setProperty("java.security.policy", "test.policy");

        if (System.getSecurityManager() == null) {
            System.out.println("NULL");
            System.setSecurityManager(new SecurityManager());
        }

        System.out.println(java.time.LocalTime.now() + " Getting registry");
        Registry registry = LocateRegistry.getRegistry(clientHost, cbPort);
        try {
            CallBack cb = (CallBack) registry.lookup(cbName);
            cb.completed();
        } catch (NotBoundException | AccessException ex) {
            Logger.getLogger(DaemonImpl.class.getName()).log(Level.SEVERE, null, ex);
        }

        WriteTime.writeTimeToFile("server-" + fileNameToSaveTime, elapsedDecryptTime
                + " (" + endMapTime + "-" + startMapTime + ") " + elapsedEncryptTime + "\n");
    }
}
