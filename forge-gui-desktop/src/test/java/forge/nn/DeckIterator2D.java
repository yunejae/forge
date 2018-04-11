package forge.nn;

import forge.deck.Deck;
import forge.item.PaperCard;
import forge.model.FModel;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.DataSetPreProcessor;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4j;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Created by maustin on 08/05/2017.
 */
public class DeckIterator2D implements DataSetIterator {

    private int minibatchSize;
    Map<String,Integer> cardIDs;
    Iterator<Deck> decks;
    Iterator<Map.Entry<PaperCard,Integer>> cards = null;
    INDArray deckArray = null;
    int maxCardNo;
    int tempcount=0;

    //for each deck create a vector of count of cards for each card in format
    public DeckIterator2D(Iterator<Deck> decks0, Map<String,Integer>cardIDs0, int minibatchSize0, int maxCardNo0){
        decks=decks0;
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
        while(j<i){
                if(cards==null||!cards.hasNext()){
                    Deck deck=decks.next();

                    if(!FModel.getFormats().getStandard().isDeckLegal(deck)){
                        continue;
                    }
                    deckArray = getDeckArray(deck);
                    cards=deck.getMain().iterator();
                }
                ++j;

                Map.Entry<PaperCard, Integer> entry = cards.next();
                System.out.println(entry.getKey().getName());

                INDArray cardArray = getCardArray(entry.getKey().getName());
                if(input ==null){
                    labels=cardArray;
                    input=deckArray;
                }else{
                    labels=Nd4j.concat(0,labels,cardArray);
                    input=Nd4j.concat(0,input,deckArray);
                }
            System.out.println(input.rows());
        }

        return new DataSet(input, labels);
    }

    private INDArray getDeckArray(Deck deck){
        INDArray deckArray= Nd4j.zeros(1,maxCardNo);
        Iterator<Map.Entry<PaperCard,Integer>> cards=deck.getMain().iterator();
        while (cards.hasNext()) {
            Map.Entry<PaperCard, Integer> entry = cards.next();
            Integer cardID=cardIDs.get(entry.getKey().getName());
            deckArray.putScalar(0,cardID,entry.getValue());
        }
        return deckArray.divi(60);
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
        return false;
    }

    @Override
    public boolean asyncSupported() {
        return false;
    }

    @Override
    public void reset() {

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
        return decks.hasNext()||cards.hasNext();
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
