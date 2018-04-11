package forge.nn;

import com.google.common.base.Function;
import forge.GuiBase;
import forge.GuiDesktop;
import forge.deck.Deck;
import forge.deck.io.DeckStorage;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.properties.ForgeConstants;
import forge.properties.ForgePreferences;
import forge.util.storage.IStorage;
import forge.util.storage.StorageImmediatelySerialized;
import org.deeplearning4j.nn.api.Layer;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.*;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.deeplearning4j.util.ModelSerializer;
import org.junit.Test;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;


/**
 * Created by maustin on 08/05/2017.
 */

public class TestNN2 {

    Map<String,Integer> cardNumbers;
    Map<Integer,PaperCard> cardLookup;

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
        final Iterable<PaperCard> cards = FModel.getFormats().getStandard().getAllCards();


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
        IStorage<Deck> decks = new StorageImmediatelySerialized<Deck>("Generator", new DeckStorage(new File(ForgeConstants.DECK_GEN_DIR+ ForgeConstants.PATH_SEPARATOR+"Standard"),
                ForgeConstants.DECK_GEN_DIR, false),
                true);


        int lstmLayerSize = 3000;					//Number of units in each GravesLSTM layer
        int miniBatchSize = 128;						//Size of mini batch to use when  training
        int exampleLength = 1000;					//Length of each training example sequence to use. This could certainly be increased
        int tbpttLength = 1;                       //Length for truncated backpropagation through time. i.e., do parameter updates ever 50 characters
        int numEpochs = 1;							//Total number of training epochs
        int generateSamplesEveryNMinibatches = 10;  //How frequently to generate samples from the network? 1000 characters / 50 tbptt length: 20 parameter updates per minibatch
        int nSamplesToGenerate = 4;					//Number of samples to generate after each training epoch
        int nCharactersToSample = 300;				//Length of each sample to generate
        String generationInitialization = null;		//Optional character initialization; a random character is used if null
        // Above is Used to 'prime' the LSTM with a character sequence to continue/complete.
        // Initialization characters must all be in CharacterIterator.getMinimalCharacterSet() by default
        Random rng = new Random(12345);

        //Get a DataSetIterator that handles vectorization of text into something we can use to train
        // our GravesLSTM network.

        DeckIteratorSingle iter = new DeckIteratorSingle(decks,cardNumbers,miniBatchSize,maxCardNo,null);
        int nOut = iter.totalOutcomes();

/*        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(1234)
                .iterations(1)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .learningRate(0.01)
*//*                .updater(Updater.NESTEROVS)
                .momentum(0.5)*//*
                .weightInit(WeightInit.XAVIER)
                .list()
                .layer(0, new RBM.Builder().nIn(iter.inputColumns()).nOut(2000).lossFunction(LossFunctions.LossFunction.MSE).build())
                //.layer(1, new RBM.Builder().nIn(2000).nOut(500).lossFunction(LossFunctions.LossFunction.KL_DIVERGENCE).build())
                .layer(1, new RBM.Builder().nIn(2000).nOut(300).lossFunction(LossFunctions.LossFunction.MSE).build())
                //.layer(3, new RBM.Builder().nIn(250).nOut(100).lossFunction(LossFunctions.LossFunction.KL_DIVERGENCE).build())
                //.layer(4, new RBM.Builder().nIn(100).nOut(30).lossFunction(LossFunctions.LossFunction.KL_DIVERGENCE).build()) //encoding stops
                //.layer(5, new RBM.Builder().nIn(30).nOut(100).lossFunction(LossFunctions.LossFunction.KL_DIVERGENCE).build()) //decoding starts
                //.layer(6, new RBM.Builder().nIn(100).nOut(250).lossFunction(LossFunctions.LossFunction.KL_DIVERGENCE).build())
                .layer(2, new RBM.Builder().nIn(300).nOut(2000).lossFunction(LossFunctions.LossFunction.MSE).build())
                //.layer(4, new RBM.Builder().nIn(500).nOut(2000).lossFunction(LossFunctions.LossFunction.KL_DIVERGENCE).build())
                //.layer(9, new RBM.Builder().nIn(2000).nOut(iter.inputColumns()).lossFunction(LossFunction.MEAN_ABSOLUTE_ERROR).build())
                .layer(3, new OutputLayer.Builder(LossFunction.RECONSTRUCTION_CROSSENTROPY).activation(Activation.SIGMOID).nIn(2000).nOut(iter.inputColumns()).build())
                .pretrain(true).backprop(true)
                .build();*/
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(2)
                .iterations(1)
                .optimizationAlgo(OptimizationAlgorithm.LINE_GRADIENT_DESCENT)
                .list()
                .layer(0, new RBM.Builder().nIn(iter.inputColumns()).nOut(1000).lossFunction(LossFunctions.LossFunction.KL_DIVERGENCE).build())
                .layer(1, new RBM.Builder().nIn(1000).nOut(500).lossFunction(LossFunctions.LossFunction.KL_DIVERGENCE).build())
                .layer(2, new RBM.Builder().nIn(500).nOut(250).lossFunction(LossFunctions.LossFunction.KL_DIVERGENCE).build())
                .layer(3, new RBM.Builder().nIn(250).nOut(100).lossFunction(LossFunctions.LossFunction.KL_DIVERGENCE).build())
                .layer(4, new RBM.Builder().nIn(100).nOut(30).lossFunction(LossFunctions.LossFunction.KL_DIVERGENCE).build()) //encoding stops
                .layer(5, new RBM.Builder().nIn(30).nOut(100).lossFunction(LossFunctions.LossFunction.KL_DIVERGENCE).build()) //decoding starts
                .layer(6, new RBM.Builder().nIn(100).nOut(250).lossFunction(LossFunctions.LossFunction.KL_DIVERGENCE).build())
                .layer(7, new RBM.Builder().nIn(250).nOut(500).lossFunction(LossFunctions.LossFunction.KL_DIVERGENCE).build())
                .layer(8, new RBM.Builder().nIn(500).nOut(1000).lossFunction(LossFunctions.LossFunction.KL_DIVERGENCE).build())
                .layer(9, new OutputLayer.Builder(LossFunctions.LossFunction.MSE).activation(Activation.SIGMOID).nIn(1000).nOut(iter.inputColumns()).build())
                .pretrain(true).backprop(true)
                .build();


        MultiLayerNetwork net = new MultiLayerNetwork(conf);
        net.init();
        net.setListeners(new ScoreIterationListener(net.getLayers().length));

        //Print the  number of parameters in the network (and for each layer)
        Layer[] layers = net.getLayers();
        int totalNumParams = 0;
        for( int i=0; i<layers.length; i++ ){
            int nParams = layers[i].numParams();
            System.out.println("Number of parameters in layer " + i + ": " + nParams);
            totalNumParams += nParams;
        }
        System.out.println("Total number of network parameters: " + totalNumParams);

        //Do training, and then generate and print samples from network
        int miniBatchNumber = 0;
        for( int i=0; i<numEpochs; i++ ){
            while(iter.hasNext()){
                DataSet ds = iter.next();
                net.fit(ds);
                /*if(++miniBatchNumber % generateSamplesEveryNMinibatches == 0){
                    System.out.println("--------------------");
                    System.out.println("Completed " + miniBatchNumber + " minibatches of size " + miniBatchSize + "x" + exampleLength + " characters" );
                    System.out.println("Sampling characters from network given initialization \"" + (generationInitialization == null ? "" : generationInitialization) + "\"");
                    String[] samples = sampleCharactersFromNetwork(generationInitialization,net,iter,rng,nCharactersToSample,nSamplesToGenerate);
                    for( int k=0; j<samples.length; k++ ){
                        System.out.println("----- Sample " + k + " -----");
                        System.out.println(samples[j]);
                        System.out.println();
                    }
                }*/
            }
            /*String cardString ="Lord of the Accursed";//iter.getRandomCard();
            net.rnnClearPreviousState();
            INDArray output = net.rnnTimeStep(iter.getCardArray(cardString));
            System.out.println(cardString);
            System.out.println(output.toString());
            Deck outDeck=getDeckFromArray(output);
            int k=0;
            for (final PaperCard c : outDeck.getMain().toFlatList()) {
                k++;
                System.out.println(k + ". " + c.toString() + ": " + c.getRules().getManaCost().toString());
            }*/
            DeckIteratorSingle iterTest = new DeckIteratorSingle(decks,cardNumbers,1,maxCardNo,null);

            for(int k=0;k<5;++k) {
                INDArray in =iterTest.next().getFeatureMatrix();
                System.out.println("INPUT");
                Deck inDeck=getDeckFromArray(in);
                System.out.println("OUTPUT");
                INDArray output=net.output(in);
                Deck outDeck=getDeckFromArray(output);
            }
            iter.reset();	//Reset iterator for another epoch
        }

        //Save the model
        File locationToSave = new File("MyMultiLayerNetwork.zip");      //Where to save the network. Note: the file is in .zip format - can be opened externally
        boolean saveUpdater = true;                                             //Updater: i.e., the state for Momentum, RMSProp, Adagrad etc. Save this if you want to train your network more in the future
        ModelSerializer.writeModel(net, locationToSave, saveUpdater);

        System.out.println("\n\nExample complete");

        System.out.println(decks.size());
    }

    @Test
    public void testGenerate() throws Exception{
        GuiBase.setInterface(new GuiDesktop());
        FModel.initialize(null, new Function<ForgePreferences, Void>()  {
            @Override
            public Void apply(ForgePreferences preferences) {
                preferences.setPref(ForgePreferences.FPref.LOAD_CARD_SCRIPTS_LAZILY, false);
                return null;
            }
        });
        final Iterable<PaperCard> cards = FModel.getFormats().getStandard().getAllCards();


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

        //Load the model
        File locationToSave = new File("MyMultiLayerNetwork.zip");
        MultiLayerNetwork net = ModelSerializer.restoreMultiLayerNetwork(locationToSave);
        String cardString ="Lord of the Accursed";//iter.getRandomCard();
        //net.rnnClearPreviousState();
        IStorage<Deck> decks = new StorageImmediatelySerialized<Deck>("Generator", new DeckStorage(new File(ForgeConstants.DECK_GEN_DIR+ ForgeConstants.PATH_SEPARATOR+"Standard"),
                ForgeConstants.DECK_GEN_DIR, false),
                true);
        DeckIteratorSingle iter = new DeckIteratorSingle(decks,cardNumbers,1,maxCardNo,null);
        for(int i=0;i<100;++i) {
            INDArray in =iter.next().getFeatureMatrix();
            System.out.println("INPUT");
            Deck inDeck=getDeckFromArray(in);
            System.out.println("OUTPUT");
            INDArray output=net.output(in);
            Deck outDeck=getDeckFromArray(output);
        }

        /*int i=0;
        for (final PaperCard c : outDeck.getMain().toFlatList()) {
            i++;
            System.out.println(i + ". " + c.toString() + ": " + c.getRules().getManaCost().toString());
        }*/
    }

    private Deck getDeckFromArray(INDArray array){
        array=array.muli(4);
        Deck deck = new Deck();
        INDArray[] sorted=Nd4j.sortWithIndices(array,1,false);
        array=sorted[1];
        INDArray indices=sorted[0];
        for(int i=0;i<20;++i){
            System.out.println(cardLookup.get(indices.getInt(i)) + "  :  " + array.getFloat(i));
        }
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
