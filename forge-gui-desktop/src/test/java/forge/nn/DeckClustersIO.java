package forge.nn;

import forge.game.GameFormat;
import forge.properties.ForgeConstants;
import org.deeplearning4j.clustering.cluster.Cluster;

import java.io.*;
import java.util.List;

/**
 * Created by maustin on 11/05/2017.
 */
public class DeckClustersIO {

    /** suffix for all gauntlet data files */
    public static final String SUFFIX_DATA = "_clusters.dat";

    public static void saveClusters(GameFormat format, List<Cluster> clsterLst){
        File file = getFile(format);
        ObjectOutputStream s = null;
        try {
            FileOutputStream f = new FileOutputStream(file);
            s = new ObjectOutputStream(f);
            s.writeObject(clsterLst);
            s.close();
        } catch (IOException e) {
            System.out.println("Error writing cluster data: " + e);
        } finally {
            if(s!=null) {
                try {
                    s.close();
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

    public static List<Cluster> loadClusters(GameFormat format){
        try {
            FileInputStream fin = new FileInputStream(getFile(format));
            ObjectInputStream s = new ObjectInputStream(fin);
            List<Cluster> clusters = (List<Cluster>) s.readObject();
            s.close();
            return clusters;
        }catch (Exception e){
            System.out.println("Error reading cluster data: " + e);
            return null;
        }

    }

    public static File getFile(final String name) {
        return new File(ForgeConstants.DECK_GEN_DIR, name + SUFFIX_DATA);
    }

    public static File getFile(final GameFormat gf) {
        return getFile(gf.getName());
    }
}
