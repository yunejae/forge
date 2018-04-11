package forge.nn;

import com.google.common.base.Function;
import forge.GuiBase;
import forge.GuiDesktop;
import forge.deck.Deck;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.Match;
import forge.game.player.RegisteredPlayer;
import forge.model.FModel;
import forge.player.GamePlayerUtil;
import forge.properties.ForgePreferences;
import forge.view.SimulateMatch;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

/**
 * Created by maustin on 12/05/2017.
 */
public class DeckPlayoffSimulator {

    @Test
    public void testDecks() {
        GuiBase.setInterface(new GuiDesktop());
        FModel.initialize(null, new Function<ForgePreferences, Void>()  {
            @Override
            public Void apply(ForgePreferences preferences) {
                preferences.setPref(ForgePreferences.FPref.LOAD_CARD_SCRIPTS_LAZILY, false);
                return null;
            }
        });
        int nGames = 10;
        int matchSize=2;
        GameType type = GameType.Constructed;
        GameRules rules = new GameRules(type);
        rules.setAppliedVariants(EnumSet.of(type));
        //String[] decks = {"paper 5C Allies","paper Awaken","paper Blue Flash","paper Blue Green Energy","paper Boros Artificers","paper Eldrazi Ramp","paper Eldrazi UB","paper Emrakul","paper GB Toughness","paper Goblins","paper Gonti Servos","paper Paradoxical Cololossus","paper RB Eldrazi","paper Red Burn","paper Red Flash Flames","paper RG Energy","paper RW Tokens","paper Sacrificial Horde","paper Sultai Graveyard","paper Support","paper Vampire Allies","paper Vampire Madness","paper Wastes","paper White Enchantments","paper Wolf Pack","paper Zendikars Roil","paper Zombie Delerium"};
        //String[] decks = {"paper Gonti Servos","paper Paradoxical Cololossus","AKH GR Aggro","AKH Mono B Zombies","GBCounters","MarduVehicles","Saheeli"};
        String[] decks = {"AKH Mono White Tokens","AKH Naya Exert","AKH RG Gods","AKH RW Embalm","AKH Suns","AKH WB God Aristocrats","AKH Zombies","AKH Temur Aetherworks Marvel","AKH Big Red"};
        int[][] wins = new int[decks.length][decks.length];
        for (int i=0;i<decks.length;++i){
            for (int j=0;j<decks.length&&j<i;++j){
                if(i!=j){
                    for(int m=0;m<nGames;++m) {
                        try {
                            List<RegisteredPlayer> pp = new ArrayList<>();

                            Deck d1 = SimulateMatch.deckFromCommandLineParameter(decks[i], type);
                            Deck d2 = SimulateMatch.deckFromCommandLineParameter(decks[j], type);

                            RegisteredPlayer rp1 = new RegisteredPlayer(d1);
                            rp1.setPlayer(GamePlayerUtil.createAiPlayer(decks[i], 0));
                            pp.add(rp1);

                            RegisteredPlayer rp2 = new RegisteredPlayer(d2);
                            rp2.setPlayer(GamePlayerUtil.createAiPlayer(decks[j], 0));
                            pp.add(rp2);

                            Match mc = new Match(rules, pp, "Test");
                            rules.setGamesPerMatch(matchSize);
                            int iGame = 0;
                            while (!mc.isMatchOver()) {
                                // play games until the match ends
                                SimulateMatch.simulateSingleMatch(mc, iGame, false);
                                iGame++;
                            }
                            if (mc.getWinner().equals(rp1)) {
                                wins[i][j] = wins[i][j] + 1;
                            } else {
                                wins[j][i] = wins[j][i] + 1;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        System.out.println(Arrays.deepToString(wins).replace("], ","]\n").replace("]","").replace("[","").replace(" ",""));
    }
}
