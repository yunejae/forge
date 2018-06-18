package forge.game.ability.effects;


import com.google.common.collect.Maps;
//import forge.card.CardStateName;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.player.PlayerController;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;

import forge.util.Lang;

import java.util.List;
import java.util.Map;

public class AssembleEffect extends SpellAbilityEffect {
    /* (non-Javadoc)
     * @see forge.game.ability.SpellAbilityEffect#getStackDescription(forge.game.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        List<Card> tgt = getTargetCards(sa);

        sb.append(Lang.joinHomogenous(tgt));
        sb.append(" ");
        sb.append(tgt.size() > 1 ? "assemble" : "assembles");
        sb.append(". ");

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        // check if only the activating player counts
        final Player pl = sa.getActivatingPlayer();
        final PlayerController pc = pl.getController();
        final Game game = pl.getGame();
        List<Card> tgt = getTargetCards(sa);
        
        for (final Card c : tgt) {
            // revealed land card
            int amount = sa.hasParam("Amount") ? AbilityUtils.calculateAmount(c, sa.getParam("Amount"), sa) : 1;
            CardCollection top = pl.getTopXCardsFromContraptionDeck(amount);
            
            for (int i = 0; i<amount; i++){
               if (!top.isEmpty()) {
                  game.getAction().reveal(top, pl, false, "Revealed for Assemble - ");
                   //TO DO: then choose one of your sprockets
                   final Card r = top.getFirst();
                   game.getAction().moveTo(ZoneType.Battlefield, r, sa);
                
               }
            
               // a creature does assemble even if it isn't on the battlefield anymore
               final Map<String, Object> runParams = Maps.newHashMap();
               runParams.put("Card", c);
               game.getTriggerHandler().runTrigger(TriggerType.Assembled, runParams, false);
            }
        }
    }
}
