package forge.game.ability.effects;

import forge.game.Game;
import forge.game.SixSidedDice;
import forge.game.ability.SpellAbilityEffect;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class RollSixSidedDiceEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        boolean countedTowardsCost = !sa.hasParam("NotCountedTowardsCost");
        final Player activator = sa.getActivatingPlayer();
        final Game game = activator.getGame();

        if(countedTowardsCost) {
            game.getPhaseHandler().incSixSidedDiceRolledthisTurn();
        }
        SixSidedDice result = SixSidedDice.roll(activator, null);
        String message = activator.getName() + " rolled " + result.toString();
        game.getAction().nofityOfValue(sa, activator, message, null);

    }
}
