package forge.nn;

import com.clearspring.analytics.util.Lists;
import forge.deck.Deck;
import forge.game.GameFormat;
import forge.item.PaperCard;
import forge.util.storage.IStorage;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.DataSetPreProcessor;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4j;

import java.util.*;

/**
 * Created by maustin on 08/05/2017.
 */
public class DeckIteratorSingle implements DataSetIterator {

    private int minibatchSize;
    Map<String,Integer> cardIDs;
    Iterator<Deck> decks;
    IStorage<Deck> deckStorage;
    int maxCardNo;
    int tempcount=0;
    GameFormat format;

    //for each deck create a vector of count of cards for each card in format
    public DeckIteratorSingle(IStorage<Deck> decks0, Map<String,Integer>cardIDs0, int minibatchSize0, int maxCardNo0, GameFormat format0){
        deckStorage=decks0;
        format=format0;
        List<Deck> deckList= Lists.newArrayList(deckStorage);
        List<Deck> finalDeckList = new ArrayList<Deck>();
        for (Deck deck:deckList){
            if(deck.getMain().toFlatList().size()==60){
                finalDeckList.add(deck);
            }
        }
        Collections.shuffle(finalDeckList);
        decks=finalDeckList.iterator();
        cardIDs=cardIDs0;
        minibatchSize=minibatchSize0;
        maxCardNo=maxCardNo0;
    }

    public String getRandomCard(){
        return (String)cardIDs.keySet().toArray()[(new Random(123)).nextInt(cardIDs.keySet().size())];
    }

    @Override
    public DataSet next(int i) {
        //1, numcards, numminibatch
        //1, numcards, numminibatch
        INDArray input=null;
        INDArray labels = null;
        int j=0;
        while (decks.hasNext()&&j < i) {
            Deck deck = decks.next();

            if (!format.isDeckLegal(deck)) {
                continue;
            }
            INDArray deckArray = getDeckArray(deck);

            ++j;

            if (input == null) {
                input = deckArray;
                labels=deckArray;
            } else {
                input = Nd4j.concat(0, input, deckArray);
                labels = Nd4j.concat(0, labels, deckArray);
            }

        }
        return new DataSet(input, labels);
    }

    private INDArray getDeckArray(Deck deck){
        INDArray deckArray= Nd4j.zeros(1,maxCardNo);
        Iterator<Map.Entry<PaperCard,Integer>> cards=deck.getMain().iterator();
        while (cards.hasNext()) {
            Map.Entry<PaperCard, Integer> entry = cards.next();
            Integer cardID=cardIDs.get(entry.getKey().getName());
            float old=deckArray.getFloat(0,cardID);
            float newValue=old+entry.getValue();
            deckArray.putScalar(0,cardID,newValue);
        }
        return deckArray.divi(4);
    }

    public INDArray getCardArray(String card){
        INDArray cardArray=Nd4j.zeros(1,maxCardNo);
        cardArray.putScalar(0,cardIDs.get(card),1);
        return cardArray;
    }

    @Override
    public int totalExamples() {
        return 0;
    }

    @Override
    public int inputColumns() {
        return maxCardNo;
    }

    @Override
    public int totalOutcomes() {
        return maxCardNo;
    }

    @Override
    public boolean resetSupported() {
        return true;
    }

    @Override
    public boolean asyncSupported() {
        return false;
    }

    @Override
    public void reset() {
        List<Deck> deckList= Lists.newArrayList(deckStorage);
        Collections.shuffle(deckList);
        decks=deckList.iterator();
    }

    @Override
    public int batch() {
        return 0;
    }

    @Override
    public int cursor() {
        return 0;
    }

    @Override
    public int numExamples() {
        return 0;
    }

    @Override
    public boolean hasNext() {
        /*tempcount++;
        if(tempcount>30){
            return false;
        }*/
        return decks.hasNext();
    }

    @Override
    public DataSet next() {
        return next(minibatchSize);
    }

    @Override
    public void setPreProcessor(DataSetPreProcessor preProcessor) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public DataSetPreProcessor getPreProcessor() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public List<String> getLabels() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
