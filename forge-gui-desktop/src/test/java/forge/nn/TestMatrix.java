package forge.nn;

import com.google.common.base.Function;
import forge.GuiBase;
import forge.GuiDesktop;
import forge.deck.Deck;
import forge.deck.DeckgenUtil;
import forge.model.FModel;
import forge.properties.ForgePreferences;
import org.junit.Test;


/**
 * Created by maustin on 08/05/2017.
 */

public class TestMatrix {

    @Test
    public void testMatrix() throws Exception {
        GuiBase.setInterface(new GuiDesktop());
        FModel.initialize(null, new Function<ForgePreferences, Void>()  {
            @Override
            public Void apply(ForgePreferences preferences) {
                preferences.setPref(ForgePreferences.FPref.LOAD_CARD_SCRIPTS_LAZILY, false);
                return null;
            }
        });

        //Deck deck = DeckgenUtil.buildCardGenDeck(FModel.getMagicDb().getCommonCards().getCard("Yahenni's Expertise"));
        for(int i=0;i<100;++i) {
            try {
                Deck deck = DeckgenUtil.buildCardGenDeck(FModel.getFormats().getStandard(), true);
                System.out.println(deck.getMain().toCardList(","));
                System.out.println(deck.getMain().countAll());
            }catch(Exception e){
                System.out.println(e.getMessage());
            }

        }


    }



}
