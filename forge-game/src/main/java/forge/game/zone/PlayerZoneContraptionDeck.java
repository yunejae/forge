/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.game.zone;


import com.google.common.collect.Maps;

import forge.game.ability.ApiType;
import forge.game.ability.effects.CrankEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbility.EmptySa;
import forge.game.player.Player;
import forge.game.staticability.StaticAbility;
import forge.game.trigger.TriggerType;
import forge.match.input.InputSelectCardsFromList;
import forge.player.PlayerControllerHuman;

import java.util.Map;

/**
 * <p>
 * PlayerZoneContraptionDeck class.
 * </p>
 * 
 * @author Forge

 * @version $Id$
 */
public class PlayerZoneContraptionDeck extends PlayerZone {
    /** Constant <code>serialVersionUID=5750837078903423978L</code>. */
    private static final long serialVersionUID = 5750837078903423978L;

    private boolean sprocket1 = false;
    private boolean sprocket2 = false;
    private boolean sprocket3 = true;
    
    private CardCollection meldedCards = new CardCollection();

    public PlayerZoneContraptionDeck(final ZoneType zone, final Player player) {
        super(zone, player);
    }
    
    public final void moveCrankCounter(){
        moveCrankCounter(sprocket1, sprocket2, sprocket3);
    }
    
    //if sprocket order gets a replacement effect, this can be adapted later. Currently it is always 1-2-3-1.
    //there shouldn't be a second CRANK! counter, but stranger things have happened.
    public final void moveCrankCounter(boolean s1, boolean s2, boolean s3){
        sprocket1 = s3;
        sprocket2 = s1;
        sprocket3 = s2;
        System.out.println("Sprockets activated: " + (sprocket1 ? 1 : " ") + " " + (sprocket2 ? 2 : " ") + " " + (sprocket3 ? 3 : " "));
        
        CardCollectionView choices = getPlayer().getCardsIn(ZoneType.Battlefield);
        choices = CardLists.filterSprocket(choices, (sprocket1 ? 1 : (sprocket2 ? 2 : 3)));
        
        int sprocket = sprocket1 ? 1 : (sprocket2 ? 2 : 3);
        
        for (Card contraption : chooseContraptionsToCrank(sprocket, choices)) {
            System.out.println(getPlayer().getName() + " cranked " + contraption.getName());// Run triggers
            final Map<String, Object> runParams = Maps.newHashMap();
            runParams.put("Player", getPlayer());
            runParams.put("Cranked", contraption);
            runParams.put("Sprocket", sprocket);
            game.getTriggerHandler().runTrigger(TriggerType.Cranked, runParams, false);
        }
    }
    
    public final CardCollection chooseContraptionsToCrank(int sprocket, CardCollectionView valid){
    //TODO: instanceof pch or pcai
        //PlayerControllerHuman
        if (getPlayer().getController() instanceof PlayerControllerHuman){
           final InputSelectCardsFromList inp = new InputSelectCardsFromList((PlayerControllerHuman)getPlayer().getController(), 0, valid.size(), valid);
           inp.setMessage("Select Contraptions on sprocket " + sprocket + " to crank:");
           inp.setCancelAllowed(true);
           inp.showAndWait();
           return new CardCollection(inp.getSelected());
        }
        //AiController
        return new CardCollection(getPlayer().getController().chooseCardsForEffect(valid, new EmptySa(ApiType.Crank), null, 0, valid.size(), true));
    }    
}
