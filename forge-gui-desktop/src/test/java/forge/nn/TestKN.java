package forge.nn;

import com.clearspring.analytics.util.Lists;
import com.google.common.base.Function;
import forge.GuiBase;
import forge.GuiDesktop;
import forge.deck.Deck;
import forge.deck.io.DeckSerializer;
import forge.deck.io.DeckStorage;
import forge.game.GameFormat;
import forge.item.PaperCard;
import forge.limited.KNClusterDeckBuilder;
import forge.model.FModel;
import forge.properties.ForgeConstants;
import forge.properties.ForgePreferences;
import forge.util.storage.IStorage;
import forge.util.storage.StorageImmediatelySerialized;
import org.deeplearning4j.clustering.cluster.Cluster;
import org.deeplearning4j.clustering.cluster.ClusterSet;
import org.deeplearning4j.clustering.cluster.Point;
import org.deeplearning4j.clustering.kmeans.KMeansClustering;
import org.junit.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Created by maustin on 08/05/2017.
 */

public class TestKN {

    Map<String,Integer> cardNumbers;
    Map<Integer,PaperCard> cardLookup;
    GameFormat format;
    int maxCardNo;
    String formatName="Standard";

    @Test
    public void testTrain() throws Exception {
        GuiBase.setInterface(new GuiDesktop());
        FModel.initialize(null, new Function<ForgePreferences, Void>()  {
            @Override
            public Void apply(ForgePreferences preferences) {
                preferences.setPref(ForgePreferences.FPref.LOAD_CARD_SCRIPTS_LAZILY, false);
                return null;
            }
        });
        format = FModel.getFormats().getFormat(formatName);
        final Iterable<PaperCard> cards;
        if(format.getName().equals("Vintage")||format.getName().equals("Legacy")){
            cards = FModel.getMagicDb().getCommonCards().getAllCards();
        }else{
            cards = format.getAllCards();
        }

        cardNumbers= new HashMap<String,Integer>();
        cardLookup= new HashMap<>();
        int j=0;
        for (PaperCard card:cards){
            if(!cardNumbers.containsKey(card.getName())) {
                cardNumbers.put(card.getName(), j);
                cardLookup.put(j,card);
                ++j;
            }
        }
        int maxCardNo=j;
        IStorage<Deck> decks = new StorageImmediatelySerialized<Deck>("Generator", new DeckStorage(new File(ForgeConstants.DECK_GEN_DIR+ ForgeConstants.PATH_SEPARATOR+format.getName()),
                ForgeConstants.DECK_GEN_DIR, false),
                true);


        //Get a DataSetIterator that handles vectorization of text into something we can use to train
        // our GravesLSTM network.

        int miniBatchSize=30000;
        DeckIteratorSingle iter = new DeckIteratorSingle(decks,cardNumbers,miniBatchSize,maxCardNo,format);
        int nOut = iter.totalOutcomes();
        DataSet ds =iter.next();


        //1. create a kmeanscluster instance
        int maxIterationCount = 50;
        int clusterCount =200;
        String distanceFunction = "euclidean";
        KMeansClustering kmc = KMeansClustering.setup(clusterCount, maxIterationCount, distanceFunction);

        //2. iterate over rows in the paragraphvector and create a List of paragraph vectors
        List<INDArray> vectors = new ArrayList<INDArray>();
        for (int i=0;i<ds.getFeatures().size(0);++i) {
            vectors.add(ds.getFeatures().getRow(i));
        }
        System.out.println(vectors.size() + " vectors extracted to create Point list");
        List<Point> pointsLst = Point.toPoints(vectors);
        System.out.println(pointsLst.size() + " Points created out of " + vectors.size() + " vectors");

        System.out.println("Start Clustering " + pointsLst.size() + " points/docs");

        ClusterSet cs = kmc.applyTo(pointsLst);

        System.out.println("Time taken to run clustering on " + vectors.size() + " paragraphVectors: ");
        vectors = null;
        pointsLst = null;

        System.out.println("Finish  Clustering");

        List<Cluster> clsterLst = cs.getClusters();
        Comparator<Cluster> cmp = new Comparator<Cluster>() {
            @Override
            public int compare(Cluster o1, Cluster o2) {
                return o2.getPoints().size()-o1.getPoints().size();
            }
        };
        Collections.sort(clsterLst,cmp);

        DeckClustersIO.saveClusters(format,clsterLst);
        int k=0;
        for(Cluster cluster:clsterLst){
            ++k;
            if(cluster.getPoints().size()>5) {
                System.out.println("Cluster number " + k + " size: " + cluster.getPoints().size());
                INDArray out = cluster.getCenter().getArray();
                getDeckFromArray(out,k);
                System.out.println("");
                System.out.println("===============");
                System.out.println("");
            }
        }
    }

    @Test
    public void testReadClusters(){
        GuiBase.setInterface(new GuiDesktop());
        FModel.initialize(null, new Function<ForgePreferences, Void>()  {
            @Override
            public Void apply(ForgePreferences preferences) {
                preferences.setPref(ForgePreferences.FPref.LOAD_CARD_SCRIPTS_LAZILY, false);
                return null;
            }
        });
        format = FModel.getFormats().getFormat(formatName);
        final Iterable<PaperCard> cards;
        if(format.getName().equals("Vintage")||format.getName().equals("Legacy")){
            cards = FModel.getMagicDb().getCommonCards().getAllCards();
        }else{
            cards = format.getAllCards();
        }

        cardNumbers= new HashMap<String,Integer>();
        cardLookup= new HashMap<>();
        int j=0;
        for (PaperCard card:cards){
            if(!cardNumbers.containsKey(card.getName())) {
                cardNumbers.put(card.getName(), j);
                cardLookup.put(j,card);
                ++j;
            }
        }
        maxCardNo=j;


        //reload decklists for lookup
        IStorage<Deck> decks = new StorageImmediatelySerialized<Deck>("Generator", new DeckStorage(new File(ForgeConstants.DECK_GEN_DIR+ ForgeConstants.PATH_SEPARATOR+format.getName()),
                ForgeConstants.DECK_GEN_DIR, false),
                true);

        List<Deck> deckList= Lists.newArrayList(decks);
        List<Deck> finalDeckList = new ArrayList<Deck>();
        for (Deck deck:deckList){
            if(deck.getMain().toFlatList().size()==60 && (format.isDeckLegal(deck))) {
                finalDeckList.add(deck);
            }
        }

        List<Cluster>clsterLst= DeckClustersIO.loadClusters(format);
        int k=0;
        for(Cluster cluster:clsterLst){
            ++k;
            if(cluster.getPoints().size()>2) {
                System.out.println("Cluster number " + k + " size: " + cluster.getPoints().size());
                //INDArray out = cluster.getCenter().getArray();
                int exemplar=0;
                Double exemplarDist=100d;
                for(int m=0;m<cluster.getPoints().size();++m){
                    Double dist=cluster.getDistanceToCenter(cluster.getPoints().get(m));
                    if (dist<exemplarDist){
                        //INDArray tmpout = cluster.getPoints().get(m).getArray();
                        //Deck tmpdeck = getDeckFromArray(tmpout,k);
                        //if(tmpdeck.getMain().toFlatList().size()!=60){
                        //    continue;
                        //}
                        exemplarDist=dist;
                        exemplar=m;
                    }
                }
                INDArray out = cluster.getPoints().get(exemplar).getArray();
                //Deck deck = getDeckFromArray(out,k);
                Deck deck = findOriginalDeck(out,finalDeckList);
                if(deck.getMain().toFlatList().size()!=60){
                    continue;
                }
                String name=format.toString() + " Cluster "+String.format("%03d", k)+" "+getDeckNameFromString(deck.getName());
                Deck namedDeck = new Deck(deck,name);
                DeckSerializer.writeDeck(namedDeck, new File(ForgeConstants.DECK_GEN_DIR+ ForgeConstants.PATH_SEPARATOR+format.getName()+"_clusters"
                        + ForgeConstants.PATH_SEPARATOR+namedDeck.getName()+ DeckStorage.FILE_EXTENSION));
                System.out.println("");
                System.out.println("===============");
                System.out.println("");
            }
        }
    }

    private Deck findOriginalDeck(INDArray deckArray, List<Deck> decks){
        for(Deck d:decks){
            if(getDeckArray(d).sub(deckArray).norm1Number().intValue()==0){
                return d;
            }
        }
        return null;
    }

    private String getDeckNameFromString(String in){
        String REGEX=".*Version - (.*) \\(.*, #.*";
        // Create a Pattern object
        Pattern r = Pattern.compile(REGEX);

        // Now create matcher object.
        Matcher m = r.matcher(in);

        if (m.find()) {
            return m.group(1);
        }else{
            return "deck";
        }
    }

    private INDArray getDeckArray(Deck deck){
        INDArray deckArray= Nd4j.zeros(1,maxCardNo);
        Iterator<Map.Entry<PaperCard,Integer>> cards=deck.getMain().iterator();
        while (cards.hasNext()) {
            Map.Entry<PaperCard, Integer> entry = cards.next();
            Integer cardID=cardNumbers.get(entry.getKey().getName());
            if(cardID==null){
                System.out.println(entry.getKey().getName());
                continue;
            }
            float old=deckArray.getFloat(0,cardID);
            float newValue=old+entry.getValue();
            deckArray.putScalar(0,cardID,newValue);
        }
        return deckArray.divi(4);
    }


    private Deck getDeckFromArray(INDArray array, int n){
        array=array.muli(4);
        INDArray[] sorted=Nd4j.sortWithIndices(array,1,false);
        array=sorted[1];
        INDArray indices=sorted[0];
        int i=0;
        List<PaperCard> cardList = new ArrayList<>();
        Map<PaperCard,Float> cardAmounts = new HashMap<>();
        while(array.getFloat(i)>0){
            PaperCard card = cardLookup.get(indices.getInt(i));
            System.out.println( card + "  :  " + array.getFloat(i));
            cardList.add(card);
            cardAmounts.put(card,array.getFloat(i));
            ++i;
        }
        KNClusterDeckBuilder db = new KNClusterDeckBuilder(cardList,cardAmounts,format,false,n);
        Deck deck = db.buildDeck();
        System.out.println(deck.getMain().toCardList(","));
        System.out.println(deck.getMain().countAll());

        /*INDArray column = array.getRow(0).muli(120);
        Integer j=0;
        for (Integer i:column.data().asInt()){
            if (i>0){
                deck.getMain().add(cardLookup.get(j),i);
            }
            ++j;
        }*/
        return deck;
    }

    /*private static Deck sampleCharactersFromNetwork(String initialization, MultiLayerNetwork net,
                                                        DeckIterator iter, Random rng, int charactersToSample, int numSamples ) {
        //Set up initialization. If no initialization: use a random character
        if (initialization == null) {
            initialization = iter.getRandomCard();
        }

        //Create input for initialization
        INDArray initializationInput = iter.getCardArray(initialization);

        //Sample from network (and feed samples back into input) one character at a time (for all samples)
        //Sampling is done in parallel here
        net.rnnClearPreviousState();
        INDArray output = net.rnnTimeStep(initializationInput);
        output = output.tensorAlongDimension(output.size(2) - 1, 1, 0);    //Gets the last time step output

    }*/
}
