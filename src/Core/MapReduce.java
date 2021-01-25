package Core;

import java.io.Serializable;
import java.util.Collection;

public interface MapReduce extends Serializable {

    public void executeMap(String blockin, String blockout);

    public void executeReduce(Collection<String> blocks, String finalresults);
}
