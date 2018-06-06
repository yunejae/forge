package forge.game.trigger;

import forge.game.SixSidedDice;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;
import forge.util.Expressions;


import java.util.Map;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class TriggerSixSidedDice extends Trigger {

    /**
     * <p>
     * Constructor for TriggerSixSidedDice.
     * </p>
     * 
     * @param params
     *            a {@link java.util.HashMap} object.
     * @param host
     *            a {@link forge.game.card.Card} object.
     * @param intrinsic
     *            the intrinsic
     */
    public TriggerSixSidedDice(final java.util.Map<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /* (non-Javadoc)
     * @see forge.card.trigger.Trigger#performTest(java.util.Map)
     */
    @Override
    public boolean performTest(Map<String, Object> runParams2) {
        if (this.mapParams.containsKey("ValidPlayer")) {
            if (!matchesValid(runParams2.get("Player"), this.mapParams.get("ValidPlayer").split(","),
                    this.getHostCard())) {
                return false;
            }
        }

        if (this.mapParams.containsKey("ValidResult")) {
            final String fullParam = this.mapParams.get("ValidResult");
            
            final String operator = fullParam.substring(0, 2);
            final int operand = Integer.parseInt(fullParam.substring(2));
            final int actualAmount = (Integer) runParams2.get("Result");
            
            if (!Expressions.compare(actualAmount, operator, operand)) {
                return false;
            }
        }
        
        if (this.mapParams.containsKey("OriginalResult")) {
            SixSidedDice cond = SixSidedDice.smartValueOf(this.mapParams.get("OriginalResult"));
            if (cond != ((SixSidedDice) runParams2.get("OriginalResult"))) {
                return false;
            }
        }

        return true;
    }

    /* (non-Javadoc)
     * @see forge.card.trigger.Trigger#setTriggeringObjects(forge.card.spellability.SpellAbility)
     */
    @Override
    public void setTriggeringObjects(SpellAbility sa) {
        sa.setTriggeringObject("Player", this.getRunParams().get("Player"));
    }

    @Override
    public String getImportantStackObjects(SpellAbility sa) {
        StringBuilder sb = new StringBuilder();
        sb.append("Roller: ").append(sa.getTriggeringObject("Player"));
        return sb.toString();
    }
}
