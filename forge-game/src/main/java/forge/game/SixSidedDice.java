package forge.game;

import forge.game.player.Player;
import forge.game.trigger.TriggerType;

import java.util.HashMap;

import com.google.common.collect.ImmutableList;

/** 
 * Represents the six-sided dice for silver-bordered games.
 *
 */
public enum SixSidedDice {
    One,
    Two,
    Three,
    Four,
    Five,
    Six;
    
    public static SixSidedDice roll(Player roller, SixSidedDice riggedResult)
    {
        SixSidedDice res = One;
        int i = 1 + forge.util.MyRandom.getRandom().nextInt(6);
        if (riggedResult != null)
            res = riggedResult;
        else switch(i){
            case 1: res = One; break;
            case 2: res = Two; break;
            case 3: res = Three; break;
            case 4: res = Four; break;
            case 5: res = Five; break;
            case 6: res = Six; break;
        }
        
        SixSidedDice trigRes = res;
        int result = 0;
        result = (trigRes == One ? 1 :
                  trigRes == Two ? 2 :
                  trigRes == Three ? 3 :
                  trigRes == Four ? 4 :
                  trigRes == Five ? 5 :
                                6);
        //commenting this part out because it looks potentially useful for replacement
/*        if(roller.getGame().getStaticEffects().getGlobalRuleChange(GlobalRuleChange.blankIsChaos)
                && res == Blank)
        {
            trigRes = Chaos;
        }
  */      
        HashMap<String,Object> runParams = new HashMap<String,Object>();
        runParams.put("Player", roller);
        runParams.put("OriginalResult", trigRes);
        runParams.put("Result", result);
        roller.getGame().getTriggerHandler().runTrigger(TriggerType.SixSidedDice, runParams,false);
    
        
        return trigRes;
    }
    
    /**
     * Parses a string into an enum member.
     * @param string to parse
     * @return enum equivalent
     */
    public static SixSidedDice smartValueOf(String value) {

        final String valToCompare = value.trim();
        for (final SixSidedDice v : SixSidedDice.values()) {
            if (v.name().compareToIgnoreCase(valToCompare) == 0) {
                return v;
            }
        }

        throw new RuntimeException("Element " + value + " not found in SixSidedDice enum");
    }

    public static final ImmutableList<SixSidedDice> values = ImmutableList.copyOf(values());
}
