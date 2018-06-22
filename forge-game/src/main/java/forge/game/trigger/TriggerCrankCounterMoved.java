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
package forge.game.trigger;

import java.util.Map;

import forge.game.card.Card;
import forge.game.card.CounterType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.Expressions;

/**
 * <p>
 * Trigger_CounterAdded class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class TriggerCrankCounterMoved extends Trigger {

    /**
     * <p>
     * Constructor for Trigger_CounterAdded.
     * </p>
     * 
     * @param params
     *            a {@link java.util.HashMap} object.
     * @param host
     *            a {@link forge.game.card.Card} object.
     * @param intrinsic
     *            the intrinsic
     */
    public TriggerCrankCounterMoved(final Map<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc} */
    public final boolean performTest(final java.util.Map<String, Object> runParams2) {
        if (this.mapParams.containsKey("ValidSprocket")) {
            if (!matchesValid(runParams2.get("Sprocket"), this.mapParams.get("ValidSprocket").split(","),
                    this.getHostCard())) {
                return false;
            }
        }
        if (hasParam("ValidSource")) {
            if (!runParams2.containsKey("Source"))
                return false;

            final Player source = (Player) runParams2.get("Source");

            if (source == null) {
                return false;
            }

            if (!source.isValid(getParam("ValidSource").split(","), getHostCard().getController(),
                    getHostCard(), null)) {
                return false;
            }
        }

        
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public final void setTriggeringObjects(final SpellAbility sa) {
        sa.setTriggeringObject("Source", this.getRunParams().get("Source"));
    }

    @Override
    public String getImportantStackObjects(SpellAbility sa) {
        StringBuilder sb = new StringBuilder();
        sb.append("Source: ").append(sa.getTriggeringObject("Source"));
        sb.append("moves the CRANK! counter to");
        sb.append("sprocket").append(sa.getTriggeringObject("Sprocket"));
        return sb.toString();
    }
}
