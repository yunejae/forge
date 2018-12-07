package forge.game.ability.effects;

import java.util.List;

import com.google.common.collect.Lists;

import forge.GameCommand;
import forge.ImageKeys;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementHandler;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;

public class PreventDamageEffect extends SpellAbilityEffect {

    public PreventDamageEffect() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card hostCard = sa.getHostCard();
        final Game game = hostCard.getGame();
        String effectRemembered = null;
        Player ownerEff = null;

        String name = sa.getParam("Name");
        if (name == null) {
            name = hostCard.getName() + "'s Effect";
        }

        if (sa.hasParam("RememberObjects")) {
            effectRemembered = sa.getParam("RememberObjects");
        }
        if (sa.hasParam("EffectOwner")) {
            final List<Player> effectOwner = AbilityUtils.getDefinedPlayers(sa.getHostCard(), sa.getParam("EffectOwner"), sa);
            ownerEff = effectOwner.get(0);
        }

        final Player controller = sa.hasParam("EffectOwner") ? ownerEff : sa.getActivatingPlayer();

        String image;
        if (sa.hasParam("Image")) {
            image = ImageKeys.getTokenKey(sa.getParam("Image"));
        } else { // use host image
            image = hostCard.getImageKey();
        }

        final Card eff = createEffect(hostCard, controller, name, image);

        // add ReplacementEffect
        String rep = "Event$ DamageDone | Prevent$ True | ActiveZones$ Command ";
        if (sa.hasParam("IsCombat")) {
            rep += "| IsCombat$ " + sa.getParam("IsCombat");
        }
        if (sa.hasParam("ValidSource")) {
            rep += "| ValidSource$ " + sa.getParam("ValidSource");
        }
        if (sa.hasParam("ValidTarget")) {
            rep += "| ValidTarget$ " + sa.getParam("ValidTarget");
        }

        rep += " | Description$ " + sa.getDescription();

        ReplacementEffect re = ReplacementHandler.parseReplacement(rep, eff, sa.isIntrinsic());

        eff.addReplacementEffect(re);

        // Set Remembered
        if (effectRemembered != null) {
            for (final String rem : effectRemembered.split(",")) {
                for (final Object o : AbilityUtils.getDefinedObjects(hostCard, rem, sa)) {
                    eff.addRemembered(o);
                }
            }
            if (sa.hasParam("ForgetOnMoved")) {
                addForgetOnMovedTrigger(eff, sa.getParam("ForgetOnMoved"));
            } else if (sa.hasParam("ExileOnMoved")) {
                addExileOnMovedTrigger(eff, sa.getParam("ExileOnMoved"));
            }
        }

        // Set Chosen Color(s)
        if (hostCard.hasChosenColor()) {
            eff.setChosenColors(Lists.newArrayList(hostCard.getChosenColors()));
        }

        // Set Chosen Cards
        if (hostCard.hasChosenCard()) {
            eff.setChosenCards(new CardCollection(hostCard.getChosenCards()));
        }

        // Set Chosen Player
        if (hostCard.getChosenPlayer() != null) {
            eff.setChosenPlayer(hostCard.getChosenPlayer());
        }

        // Set Chosen Type
        if (!hostCard.getChosenType().isEmpty()) {
            eff.setChosenType(hostCard.getChosenType());
        }

        // Set Chosen name
        if (!hostCard.getNamedCard().isEmpty()) {
            eff.setNamedCard(hostCard.getNamedCard());
        }

        // Copy text changes
        if (sa.isIntrinsic()) {
            eff.copyChangedTextFrom(hostCard);
        }

        if (sa.hasParam("AtEOT")) {
            registerDelayedTrigger(sa, sa.getParam("AtEOT"), Lists.newArrayList(hostCard));
        }

        // Duration
        final String duration = sa.getParam("Duration");
        if ((duration == null) || !duration.equals("Permanent")) {
            final GameCommand endEffect = new GameCommand() {
                private static final long serialVersionUID = -5861759814760561373L;

                @Override
                public void run() {
                    game.getAction().exile(eff, null);
                }
            };

            if ((duration == null) || duration.equals("EndOfTurn")) {
                game.getEndOfTurn().addUntil(endEffect);
            }
            else if (duration.equals("UntilHostLeavesPlay")) {
                hostCard.addLeavesPlayCommand(endEffect);
            }
            else if (duration.equals("HostLeavesOrEOT")) {
                game.getEndOfTurn().addUntil(endEffect);
                hostCard.addLeavesPlayCommand(endEffect);
            }
            else if (duration.equals("UntilYourNextTurn")) {
                game.getCleanup().addUntil(controller, endEffect);
            }
            else if (duration.equals("UntilYourNextUpkeep")) {
                game.getUpkeep().addUntil(controller, endEffect);
            }
            else if (duration.equals("UntilEndOfCombat")) {
                game.getEndOfCombat().addUntil(endEffect);
            }
            else if (duration.equals("UntilTheEndOfYourNextTurn")) {
                if (game.getPhaseHandler().isPlayerTurn(controller)) {
                    game.getEndOfTurn().registerUntilEnd(controller, endEffect);
                } else {
                    game.getEndOfTurn().addUntilEnd(controller, endEffect);
                }
            }
            else if (duration.equals("ThisTurnAndNextTurn")) {
                game.getUntap().addAt(new GameCommand() {
                    private static final long serialVersionUID = -5054153666503075717L;

                    @Override
                    public void run() {
                        game.getEndOfTurn().addUntil(endEffect);
                    }
                });
            }
        }

        eff.updateStateForView();

        // TODO: Add targeting to the effect so it knows who it's dealing with
        game.getTriggerHandler().suppressMode(TriggerType.ChangesZone);
        game.getAction().moveTo(ZoneType.Command, eff, sa);
        game.getTriggerHandler().clearSuppression(TriggerType.ChangesZone);
        //if (effectTriggers != null) {
        //    game.getTriggerHandler().registerActiveTrigger(cmdEffect, false);
        //}
    }

}
