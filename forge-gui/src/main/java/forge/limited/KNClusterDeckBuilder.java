package forge.limited;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import forge.card.*;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostShard;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckFormat;
import forge.deck.DeckSection;
import forge.deck.generation.DeckGenPool;
import forge.deck.generation.DeckGeneratorBase;
import forge.game.GameFormat;
import forge.item.IPaperCard;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.util.MyRandom;

import java.util.*;

/**
 * Limited format deck.
 */
public class KNClusterDeckBuilder extends DeckGeneratorBase {
    @Override
    protected final float getLandPercentage() {
        return 0.44f;
    }
    @Override
    protected final float getCreaturePercentage() {
        return 0.33f;
    }
    @Override
    protected final float getSpellPercentage() {
        return 0.23f;
    }

    protected int numSpellsNeeded = 35;
    protected int landsNeeded = 25;
    protected int clusterNumber;
    protected GameFormat gameFormat;

    protected DeckColors deckColors;
    protected Predicate<CardRules> hasColor;
    protected final List<PaperCard> cards;
    protected final List<PaperCard> availableList;
    protected final List<PaperCard> aiPlayables;
    protected final List<PaperCard> lands;
    protected final List<PaperCard> deckList = new ArrayList<PaperCard>();
    protected final List<String> setsWithBasicLands = new ArrayList<String>();
    protected List<PaperCard> rankedColorList;

    // Views for aiPlayable
    protected Iterable<PaperCard> onColorCreatures;
    protected Iterable<PaperCard> onColorNonCreatures;
    protected Iterable<PaperCard> keyCards;

    Map<PaperCard,Float> cardAmounts;

    protected static final boolean logToConsole = true;
    protected static final boolean logColorsToConsole = false;


    /**
     *
     * Constructor.
     *
     */
    public KNClusterDeckBuilder(List<PaperCard> cards, Map<PaperCard,Float> cardAmounts, GameFormat format, boolean isForAI, int clusterNumber) {
        super(FModel.getMagicDb().getCommonCards(), DeckFormat.Limited, format.getFilterPrinted());
        this.gameFormat=format;
        this.clusterNumber=clusterNumber;
        this.cards = new ArrayList<>(cards);
        this.availableList = cards;
        this.cardAmounts=cardAmounts;
        // remove Unplayables
        if(isForAI) {
            final Iterable<PaperCard> playables = Iterables.filter(availableList,
                    Predicates.compose(CardRulesPredicates.IS_KEPT_IN_AI_DECKS, PaperCard.FN_GET_RULES));
            this.aiPlayables = Lists.newArrayList(playables);
        }else{
            this.aiPlayables = Lists.newArrayList(availableList);
        }
        this.availableList.removeAll(aiPlayables);
        deckColors = new DeckColors();
        lands= new ArrayList<>();
        findBasicLandSets();
    }

    public void addCards(int cardsToAdd){
        int cardCount=0;
        List<PaperCard> cards = new ArrayList<>(getAiPlayables());
        for(PaperCard c:cards){
            /*if(c.getRules().getType().isLand()){
                if(!lands.contains(c)&&cardAmounts.get(c)>0.5){
                    lands.add(c);
                }
                continue;
            }*/
            Integer count = Math.round(cardAmounts.get(c));
            if(count>4&&!c.getRules().getType().isLand()){
                count=4;
            }
            if(count<1){
                count=1;
            }
            for(int j=0;j<count;++j){
                deckList.add(c);
                if(logToConsole){
                    System.out.println("Adding: " +c.getName());
                }
                ++cardCount;
                if(cardCount==cardsToAdd){
                    break;
                }
            }
            if(deckColors.canChoseMoreColors()){
                deckColors.addColorsOf(c);
            }
            aiPlayables.remove(c);
            if(cardCount==cardsToAdd){
                break;
            }
        }
    }

    public void addLandCards(int cardsToAdd){
        int cardCount=0;
        List<PaperCard> landcards = new ArrayList<>(lands);
        for(PaperCard c:landcards){
            Integer count = Math.round(cardAmounts.get(c));
            if(count>4 && !c.getRules().getType().isBasicLand()){
                count=4;
            }
            if(count<1){
                count=1;
            }
            for(int j=0;j<count;++j){
                deckList.add(c);
                ++cardCount;
                --landsNeeded;
                if(logToConsole){
                    System.out.println("Adding: " +c.getName());
                }
                if(cardCount==cardsToAdd){
                    break;
                }
            }
            aiPlayables.remove(c);
            lands.remove(c);
            if(cardCount==cardsToAdd){
                break;
            }
        }
    }



    @Override
    public CardPool getDeck(final int size, final boolean forAi) {
        return buildDeck().getMain();
    }

    /**
     * <p>
     * buildDeck.
     * </p>
     *
     * @return the new Deck.
     */
    @SuppressWarnings("unused")
    public Deck buildDeck() {
        //Create Deck
        final Deck result = new Deck(generateName());

        addCards(60);
        colors = deckColors.getChosenColors();
        deckColors = new DeckColors();
/*


        // 6. If there are still on-color cards, and the average cmc is low, add
        // extras.
        double avCMC=getAverageCMC(deckList);
        int maxCMC=getMaxCMC(deckList);
        if (deckList.size() == numSpellsNeeded && avCMC < 4) {
            addCards(1);
            landsNeeded--;
        }
        if (deckList.size() >= numSpellsNeeded && avCMC < 3 && maxCMC<6) {
            addCards(1);
            landsNeeded--;
        }
        if (deckList.size() >= numSpellsNeeded && avCMC < 2.5 && maxCMC<5) {
            addCards(1);
            landsNeeded--;
        }
        if (deckList.size() >= numSpellsNeeded && avCMC < 2.2 && maxCMC<5) {
            addCards(1);
            landsNeeded--;
        }
        if (deckList.size() >= numSpellsNeeded && avCMC < 2 && maxCMC<5) {
            addCards(1);
            landsNeeded--;
        }
        if (logToConsole) {
            System.out.println("Post lowcoc : " + deckList.size());
        }

        //update colors
        FullDeckColors finalDeckColors = new FullDeckColors();
        for(PaperCard c:deckList){
            if(finalDeckColors.canChoseMoreColors()){
                finalDeckColors.addColorsOf(c);
            }
        }
        colors = finalDeckColors.getChosenColors();
        if (logColorsToConsole) {
            System.out.println("Final Colors: " + colors.toEnumSet().toString());
        }

        addLandCards(landsNeeded);

        // 11. Fill up with basic lands.
        final int[] clrCnts = calculateLandNeeds();

        if (landsNeeded > 0) {
            addLands(clrCnts);
        }
        if (logToConsole) {
            System.out.println("Post Lands : " + deckList.size());
        }
        fixDeckSize(clrCnts);
        if (logToConsole) {
            System.out.println("Post Size fix : " + deckList.size());
        }
*/


        result.getMain().add(deckList);

        if (logToConsole) {
            debugFinalDeck();
        }
        return result;

    }

    /**
     * If evolving wilds is in the deck and there are fewer than 4 spaces for basic lands - remove evolving wilds
     */
    protected void checkEvolvingWilds(){
        List<PaperCard> evolvingWilds = Lists.newArrayList(Iterables.filter(deckList,PaperCard.Predicates.name("Evolving Wilds")));
        if((evolvingWilds.size()>0 && landsNeeded<4 ) || colors.countColors()<2){
            deckList.removeAll(evolvingWilds);
            landsNeeded=landsNeeded+evolvingWilds.size();
            aiPlayables.addAll(evolvingWilds);
        }
    }


    protected void addLowCMCCard(){
        final Iterable<PaperCard> nonLands = Iterables.filter(rankedColorList,
                Predicates.compose(CardRulesPredicates.Presets.IS_NON_LAND, PaperCard.FN_GET_RULES));
        final PaperCard card = Iterables.getFirst(nonLands, null);
        if (card != null) {
            deckList.add(card);
            aiPlayables.remove(card);
            landsNeeded--;
            if (logToConsole) {
                System.out.println("Low CMC: " + card.getName());
            }
        }
    }

    /**
     * Set the basic land pool
     * @param edition
     * @return
     */
    protected boolean setBasicLandPool(String edition){
        Predicate<PaperCard> isSetBasicLand;
        if (edition !=null){
            isSetBasicLand = Predicates.and(IPaperCard.Predicates.printedInSet(edition),
                    Predicates.compose(CardRulesPredicates.Presets.IS_BASIC_LAND, PaperCard.FN_GET_RULES));
        }else{
            isSetBasicLand = Predicates.compose(CardRulesPredicates.Presets.IS_BASIC_LAND, PaperCard.FN_GET_RULES);
        }

        landPool = new DeckGenPool(format.getCardPool(fullCardDB).getAllCards(isSetBasicLand));
        return landPool.contains("Plains");
    }

    /**
     * Generate a descriptive name.
     *
     * @return name
     */
    private String generateName() {
        List<PaperCard> nameCards=new ArrayList<>();
        for(PaperCard card:cards){
            if(!card.getRules().getType().isLand()){
                nameCards.add(card);
            }
            if(nameCards.size()>1){
                break;
            }
        }
        return gameFormat.toString() + " Cluster "+String.format("%03d", clusterNumber)+" "+nameCards.get(0).getName() + " - " + nameCards.get(1).getName() +" based deck";
    }

    /**
     * Print out listing of all cards for debugging.
     */
    private void debugFinalDeck() {
        int i = 0;
        System.out.println("DECK");
        for (final PaperCard c : deckList) {
            i++;
            System.out.println(i + ". " + c.toString() + ": " + c.getRules().getManaCost().toString());
        }
        i = 0;
        System.out.println("NOT PLAYABLE");
        for (final PaperCard c : availableList) {
            i++;
            System.out.println(i + ". " + c.toString() + ": " + c.getRules().getManaCost().toString());
        }
        i = 0;
        System.out.println("NOT PICKED");
        for (final PaperCard c : aiPlayables) {
            i++;
            System.out.println(i + ". " + c.toString() + ": " + c.getRules().getManaCost().toString());
        }
    }

    /**
     * If the deck does not have 40 cards, fix it. This method should not be
     * called if the stuff above it is working correctly.
     *
     * @param clrCnts
     *            color counts needed
     */
    private void fixDeckSize(final int[] clrCnts) {
        while (deckList.size() > 60) {
            if (logToConsole) {
                System.out.println("WARNING: Fixing deck size, currently " + deckList.size() + " cards.");
            }
            final PaperCard c = deckList.get(MyRandom.getRandom().nextInt(deckList.size() - 1));
            deckList.remove(c);
            aiPlayables.add(c);
            if (logToConsole) {
                System.out.println(" - Removed " + c.getName() + " randomly.");
            }
        }

        while (deckList.size() < 60) {
            if (logToConsole) {
                System.out.println("WARNING: Fixing deck size, currently " + deckList.size() + " cards.");
            }
            if (aiPlayables.size() > 1) {
                final PaperCard c = aiPlayables.get(MyRandom.getRandom().nextInt(aiPlayables.size() - 1));
                deckList.add(c);
                aiPlayables.remove(c);
                if (logToConsole) {
                    System.out.println(" - Added " + c.getName() + " randomly.");
                }
            } else if (aiPlayables.size() == 1) {
                final PaperCard c = aiPlayables.get(0);
                deckList.add(c);
                aiPlayables.remove(c);
                if (logToConsole) {
                    System.out.println(" - Added " + c.getName() + " randomly.");
                }
            } else {
                // if no playable cards remain fill up with basic lands
                for (int i = 0; i < 5; i++) {
                    if (clrCnts[i] > 0) {
                        final PaperCard cp = getBasicLand(i);
                        deckList.add(cp);
                        if (logToConsole) {
                            System.out.println(" - Added " + cp.getName() + " as last resort.");
                        }
                        break;
                    }
                }
            }
        }
    }

    /**
     * Find the sets that have basic lands for the available cards.
     */
    private void findBasicLandSets() {
        final Set<String> sets = new HashSet<String>();
        for (final PaperCard cp : aiPlayables) {
            final CardEdition ee = FModel.getMagicDb().getEditions().get(cp.getEdition());
            if( !sets.contains(cp.getEdition()) && CardEdition.Predicates.hasBasicLands.apply(ee)) {
                sets.add(cp.getEdition());
            }
        }
        setsWithBasicLands.addAll(sets);
        if (setsWithBasicLands.isEmpty()) {
            setsWithBasicLands.add("BFZ");
        }
    }

    /**
     * Add lands to fulfill the given color counts.
     *
     * @param clrCnts
     *             counts of lands needed, by color
     */
    private void addLands(final int[] clrCnts) {
        // basic lands that are available in the deck
        final Iterable<PaperCard> basicLands = Iterables.filter(aiPlayables, Predicates.compose(CardRulesPredicates.Presets.IS_BASIC_LAND, PaperCard.FN_GET_RULES));
        final Set<PaperCard> snowLands = new HashSet<PaperCard>();

        // total of all ClrCnts
        int totalColor = 0;
        int numColors = 0;
        for (int i = 0; i < 5; i++) {
            totalColor += clrCnts[i];
            if (clrCnts[i] > 0) {
                numColors++;
            }
        }
        if (totalColor == 0) {
            throw new RuntimeException("Add Lands to empty deck list!");
        }

        // do not update landsNeeded until after the loop, because the
        // calculation involves landsNeeded
        for (int i = 0; i < 5; i++) {
            if (clrCnts[i] > 0) {
                // calculate number of lands for each color
                float p = (float) clrCnts[i] / (float) totalColor;
                if (numColors == 2) {
                    // In the normal two-color case, constrain to within 40% and 60% so that the AI
                    // doesn't put too few lands of the lesser color, risking getting screwed on that color.
                    // Don't do this for the odd case where a third color had to be added to the deck.
                    p = Math.min(Math.max(p, 0.4f), 0.6f);
                }
                int nLand = Math.round(landsNeeded * p); // desired truncation to int
                if (logToConsole) {
                    System.out.printf("Basics[%s]: %d/%d = %f%% = %d cards%n", MagicColor.Constant.BASIC_LANDS.get(i), clrCnts[i], totalColor, 100*p, nLand);
                }

                // if appropriate snow-covered lands are available, add them
                for (final PaperCard cp : basicLands) {
                    if (cp.getName().equals(MagicColor.Constant.SNOW_LANDS.get(i))) {
                        snowLands.add(cp);
                        nLand--;
                    }
                }

                for (int j = 0; j < nLand; j++) {
                    deckList.add(getBasicLand(i));
                }
            }
        }

        // A common problem at this point is that p in the above loop was exactly 1/2,
        // and nLand rounded up for both colors, so that one too many lands was added.
        // So if the deck size is > 60, remove the last land added.
        // Otherwise, the fixDeckSize() method would remove random cards.
        while (deckList.size() > 60) {
            deckList.remove(deckList.size() - 1);
        }

        deckList.addAll(snowLands);
        aiPlayables.removeAll(snowLands);
    }

    /**
     * Get basic land.
     *
     * @param basicLand
     *             the set to take basic lands from (pass 'null' for random).
     * @return card
     */
    private PaperCard getBasicLand(final int basicLand) {
        String set;
            if (setsWithBasicLands.size() > 1) {
                set = setsWithBasicLands.get(MyRandom.getRandom().nextInt(setsWithBasicLands.size() - 1));
            } else {
                set = setsWithBasicLands.get(0);
            }
        return FModel.getMagicDb().getCommonCards().getCard(MagicColor.Constant.BASIC_LANDS.get(basicLand), set);
    }

    /**
     * Only adds wastes if present in the card pool but if present adds them all
     */
    private void addWastesIfRequired(){
        List<PaperCard> toAdd = Lists.newArrayList(Iterables.filter(aiPlayables,PaperCard.Predicates.name("Wastes")));
        deckList.addAll(toAdd);
        aiPlayables.removeAll(toAdd);
        rankedColorList.removeAll(toAdd);
        landsNeeded = landsNeeded - toAdd.size();
    }

    /**
     * Attempt to optimize basic land counts according to color representation.
     * Only consider colors that are supposed to be in the deck. It's not worth
     * putting one land in for that random off-color card we had to stick in at
     * the end...
     *
     * @return CCnt
     */
    private int[] calculateLandNeeds() {
        final int[] clrCnts = { 0,0,0,0,0 };
        // count each card color using mana costs
        for (final PaperCard cp : deckList) {
            final ManaCost mc = cp.getRules().getManaCost();

            // count each mana symbol in the mana cost
            for (final ManaCostShard shard : mc) {
                for ( int i = 0 ; i < MagicColor.WUBRG.length; i++ ) {
                    final byte c = MagicColor.WUBRG[i];

                    if ( shard.canBePaidWithManaOfColor(c) && colors.hasAnyColor(c)) {
                        clrCnts[i]++;
                    }
                }
            }
        }
        return clrCnts;
    }

    /**
     * Add non-basic lands to the deck.
     */
    private void addNonBasicLands() {
        final Iterable<PaperCard> lands = Iterables.filter(aiPlayables,
                Predicates.compose(CardRulesPredicates.Presets.IS_NONBASIC_LAND, PaperCard.FN_GET_RULES));
        List<PaperCard> landsToAdd = new ArrayList<>();
        int minBasics=r.nextInt(6)+3;//Keep a minimum number of basics to ensure playable decks
        for (final PaperCard card : lands) {
            if (landsNeeded > minBasics) {
                // Throw out any dual-lands for the wrong colors. Assume
                // everything else is either
                // (a) dual-land of the correct two colors, or
                // (b) a land that generates colorless mana and has some other
                // beneficial effect.
                if (!inverseDLands.contains(card.getName())&&!dLands.contains(card.getName())&&r.nextInt(100)<90) {
                    landsToAdd.add(card);
                    landsNeeded--;
                    if (logToConsole) {
                        System.out.println("NonBasicLand[" + landsNeeded + "]:" + card.getName());
                    }
                }
            }
        }
        deckList.addAll(landsToAdd);
        aiPlayables.removeAll(landsToAdd);
    }

    /**
     * Add a third color to the deck.
     *
     * @param num
     *           number to add
     */
    private void addThirdColorCards(int num) {
        if (num > 0) {
            final Iterable<PaperCard> others = Iterables.filter(aiPlayables,
                    Predicates.compose(CardRulesPredicates.Presets.IS_NON_LAND, PaperCard.FN_GET_RULES));
            // We haven't yet ranked the off-color cards.
            // Compare them to the cards already in the deckList.
            //List<PaperCard> rankedOthers = CardRanker.rankCardsInPack(others, deckList, colors, true);
            List<PaperCard> toAdd = new ArrayList<>();
            for (final PaperCard card : others) {
                // Want a card that has just one "off" color.
                final ColorSet off = colors.getOffColors(card.getRules().getColor());
                if (off.isMonoColor()) {
                    colors = ColorSet.fromMask(colors.getColor() | off.getColor());
                    break;
                }
            }

            hasColor = Predicates.and(CardRulesPredicates.Presets.IS_NON_LAND,Predicates.or(new MatchColorIdentity(colors),
                    DeckGeneratorBase.COLORLESS_CARDS));
            final Iterable<PaperCard> threeColorList = Iterables.filter(aiPlayables,
                    Predicates.compose(hasColor, PaperCard.FN_GET_RULES));
            for (final PaperCard card : threeColorList) {
                if (num > 0) {
                    toAdd.add(card);
                    num--;
                    if (logToConsole) {
                        System.out.println("Third Color[" + num + "]:" + card.getName() + "("
                                + card.getRules().getManaCost() + ")");
                    }
                } else {
                    break;
                }
            }
            deckList.addAll(toAdd);
            aiPlayables.removeAll(toAdd);
        }
    }

    /**
     * Add random cards to the deck.
     *
     * @param num
     *           number to add
     */
    private void addRandomCards(int num) {
        final Iterable<PaperCard> others = Iterables.filter(aiPlayables,
                Predicates.compose(CardRulesPredicates.Presets.IS_NON_LAND, PaperCard.FN_GET_RULES));
        List <PaperCard> toAdd = new ArrayList<>();
        for (final PaperCard card : others) {
            if (num > 0) {
                toAdd.add(card);
                num--;
                if (logToConsole) {
                    System.out.println("Random[" + num + "]:" + card.getName() + "("
                            + card.getRules().getManaCost() + ")");
                }
            } else {
                break;
            }
        }
        deckList.addAll(toAdd);
        aiPlayables.removeAll(toAdd);
        rankedColorList.removeAll(toAdd);
    }

    /**
     * Add highest ranked non-creatures to the deck.
     *
     * @param nonCreatures
     *            cards to choose from
     * @param num
     *            number to add
     */
    private void addNonCreatures(final Iterable<PaperCard> nonCreatures, int num) {
        List<PaperCard> toAdd = new ArrayList<>();
        for (final PaperCard card : nonCreatures) {
            if (num > 0) {
                toAdd.add(card);
                num--;
                if (logToConsole) {
                    System.out.println("Others[" + num + "]:" + card.getName() + " ("
                            + card.getRules().getManaCost() + ")");
                }
            } else {
                break;
            }
        }
        deckList.addAll(toAdd);
        aiPlayables.removeAll(toAdd);
        rankedColorList.removeAll(toAdd);
    }

    /**
     * Add creatures to the deck.
     *
     * @param creatures
     *            cards to choose from
     * @param num
     *            number to add
     */
    private void addCreatures(final Iterable<PaperCard> creatures, int num) {
        List<PaperCard> creaturesToAdd = new ArrayList<>();
        for (final PaperCard card : creatures) {
            if (num > 0) {
                creaturesToAdd.add(card);
                num--;
                if (logToConsole) {
                    System.out.println("Creature[" + num + "]:" + card.getName() + " (" + card.getRules().getManaCost() + ")");
                }
            } else {
                break;
            }
        }
        deckList.addAll(creaturesToAdd);
        aiPlayables.removeAll(creaturesToAdd);
        rankedColorList.removeAll(creaturesToAdd);
    }


    /**
     * Calculate average CMC.
     *
     * @param cards
     *            cards to choose from
     * @return the average
     */
    private static double getAverageCMC(final List<PaperCard> cards) {
        double sum = 0.0;
        for (final IPaperCard cardPrinted : cards) {
            sum += cardPrinted.getRules().getManaCost().getCMC();
        }
        return sum / cards.size();
    }

    /**
     * Calculate max CMC.
     *
     * @param cards
     *            cards to choose from
     * @return the average
     */
    private static int getMaxCMC(final List<PaperCard> cards) {
        int max = 0;
        for (final IPaperCard cardPrinted : cards) {
            if(cardPrinted.getRules().getManaCost().getCMC()>max) {
                max = cardPrinted.getRules().getManaCost().getCMC();
            }
        }
        return max;
    }

    /**
     * @return the colors
     */
    public ColorSet getColors() {
        return colors;
    }

    /**
     * @param colors0
     *            the colors to set
     */
    public void setColors(final ColorSet colors0) {
        colors = colors0;
    }

    /**
     * @return the aiPlayables
     */
    public List<PaperCard> getAiPlayables() {
        return aiPlayables;
    }

}
