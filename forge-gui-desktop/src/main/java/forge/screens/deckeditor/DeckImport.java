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
package forge.screens.deckeditor;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.util.*;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import forge.Singletons;
import forge.deck.*;
import forge.deck.DeckRecognizer.TokenType;
import forge.game.GameFormat;
import forge.game.GameType;
import forge.item.InventoryItem;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.screens.deckeditor.controllers.ACEditorBase;
import forge.screens.deckeditor.controllers.CStatisticsImporter;
import forge.screens.deckeditor.views.VStatisticsImporter;
import forge.toolbox.*;
import forge.util.Localizer;
import forge.view.FDialog;

/**
  *
 * Dialog for quick import of decks.
 *
 * @param <TItem>
 * @param <TModel>
 */
public class DeckImport<TItem extends InventoryItem, TModel extends DeckBase> extends FDialog {
    private static final long serialVersionUID = -5837776824284093004L;

    private final FTextArea txtInput = new FTextArea();
    private static final String STYLESHEET = "<style>"
            + "body, h1, h2, h3, h4, h5, h6, table, tr, td {font-weight: normal; line-height: 1.6; "
            + " font-family: Arial; font-size: 10px;}"
            + " h3 {font-size: 13px; margin: 2px 0; padding: 0px 5px;}"
            + " h4 {font-size: 11px; margin: 2px 0; padding: 0px 5px; font-weight: bold;}"
            + " h5 {font-size: 11px; margin: 0; text-align: justify; padding: 1px 0 1px 8px;}"
            + " ul li {padding: 5px 1px 1px 1px !important; margin: 0 1px !important}"
            + " code {font-size: 10px;}"
            + " p {margin: 2px; text-align: justify; padding: 2px 5px;}"
            + " div {margin: 0; text-align: justify; padding: 1px 0 1px 8px;}"
            + " table {margin: 5px 0;}"
            // Card Matching Colours #4F6070
            + " .knowncard   {color: #89DC9F;}"
            + " .unknowncard {color: #E1E35F;}"
            + " .illegalcard {color: #FF977A;}"
            + " .invalidcard {color: #A9E5DD;}"
            + " .comment     {font-style: italic}"
            // Deck Name
            + " .deckname    {background-color: #332200; color: #ffffff; }"
            + " .sectionname {padding-left: 8px; font-weight: bold; }"
            // Placeholders
            + " .section     {font-weight: bold; background-color: #DDDDDD; color: #000000;}"
            + " .cardtype    {font-weight: bold; background-color: #FFCC66; color: #000000;}"
            + " .cmc         {font-weight: bold; background-color: #C6C7BA; color: #000000;}"
            + " .rarity      {font-weight: bold; background-color: #df8030; color: #000000;}"
            + " .mana        {font-weight: bold; background-color: #38221A; color: #ffffff;}"
            // Colours
            + " .colorless   {font-weight: bold; background-color: #C7BCBA; color: #000000;}"
            + " .blue        {font-weight: bold; background-color: #0D78BF; color: #ffffff;}"
            + " .red         {font-weight: bold; background-color: #ED0713; color: #ffffff;}"
            + " .white       {font-weight: bold; background-color: #FCFCB6; color: #000000;}"
            + " .black       {font-weight: bold; background-color: #787878; color: #000000;}"
            + " .green       {font-weight: bold; background-color: #26AB57; color: #000000;}"
            // Card Edition
            + " .edition     {font-weight: bold; background-color: #78A197; color: #000000;}"
            + " .editioncode {font-weight: bold; color: #ffffff;}"
            + "</style>";
    private static final String COLOUR_CODED_TAGS = String.format(
            "<ul>" +
            "<li> <span class=\"knowncard\">%s</span></li>" +
            "<li> <span class=\"unknowncard\">%s</span></li>" +
            "<li> <span class=\"illegalcard\">%s</span></li>" +
            "<li> <span class=\"invalidcard\">%s</span></li></ul>",
            Localizer.getInstance().getMessage("lblGuideKnownCard"),
            Localizer.getInstance().getMessage("lblGuideUnknownCard"),
            Localizer.getInstance().getMessage("lblGuideIllegalCard"),
            Localizer.getInstance().getMessage("lblGuideInvalidCard")
            );
    private static final String TIPS_LIST = String.format(
            "<ul><li>%s</li><li>%s</li><li>%s</li><li>%s</li><li>%s</li><li>%s</li></ul>",
            Localizer.getInstance().getMessage("lblGuideTipsCount",
                    String.format("<b>%s</b>", Localizer.getInstance().getMessage("lblGuideTipsTitleCount")),
                    String.format("<code>%s</code>", "\"4 Power Sink\""),
                    String.format("<code>%s</code>", "\"4x Power Sink\"")),
            Localizer.getInstance().getMessage("lblGuideTipsSet",
                    String.format("<b>%s</b>", Localizer.getInstance().getMessage("lblGuideTipsTitleSet"))),
            Localizer.getInstance().getMessage("lblGuideTipsCardType",
                    String.format("<b>%s</b>", Localizer.getInstance().getMessage("lblGuideTipsTitleCardType"))),
            Localizer.getInstance().getMessage("lblGuideTipsDeckSection",
                    String.format("<b>%s</b>", Localizer.getInstance().getMessage("lblGuideTipsTitleDeckSections"))),
            Localizer.getInstance().getMessage("lblGuideTipsDeckName",
                    String.format("<b>%s</b>", Localizer.getInstance().getMessage("lblGuideTipsTitleDeckName"))),
            Localizer.getInstance().getMessage("lblGuideTipsDeckFormats",
                    String.format("<b>%s</b>", Localizer.getInstance().getMessage("lblGuideTipsTitleDeckFormat")))
    );

    private static final String EXAMPLES_LIST = String.format(
            "<ul><li><code>%s</code></li></ul>" +
            "<p class=\"example\">%s</p>" +
            "<ul><li><code>%s</code></li></ul>" +
            "<p class=\"example\">%s</p>" +
            "<ul><li><code>%s</code></li></ul>" +
            "<p class=\"example\">%s</p>" +
            "<ul><li><code>%s</code></li></ul>" +
            "<p class=\"example\">%s</p>",
            Localizer.getInstance().getMessage("lblExample1"),
            Localizer.getInstance().getMessage("nlExample1"),
            Localizer.getInstance().getMessage("lblExample2"),
            Localizer.getInstance().getMessage("nlExample2"),
            Localizer.getInstance().getMessage("lblExample3"),
            Localizer.getInstance().getMessage("nlExample3"),
            Localizer.getInstance().getMessage("lblExample4"),
            Localizer.getInstance().getMessage("nlExample4")
    );

    private static final String HTML_WELCOME_TEXT = String.format("<html>"
            + "<head>"
            + DeckImport.STYLESHEET
            + "</head>"
            + "<body>"
            + "<h3 id='how-to-use-the-deck-importer'>%s</h3><div>%s</div> "
            + "<h4>%s</h4><div>%s</div> "
            + "<h4>%s</h4><div>%s</div> "
            + "</body></html>",
            Localizer.getInstance().getMessage("nlGuideTitle"),
            Localizer.getInstance().getMessage("nlGuideQuickInstructions", COLOUR_CODED_TAGS),
            Localizer.getInstance().getMessage("nlGuideTipsTitle"),
            Localizer.getInstance().getMessage("nlGuideTipsText", TIPS_LIST),
            Localizer.getInstance().getMessage("nlGuideExamplesTitle"),
            Localizer.getInstance().getMessage("nlGuideExamplesText", EXAMPLES_LIST)
    );

    private final FHtmlViewer htmlOutput = new FHtmlViewer(DeckImport.HTML_WELCOME_TEXT);
    private final FScrollPane scrollInput = new FScrollPane(this.txtInput, false);
    private final FScrollPane scrollOutput = new FScrollPane(this.htmlOutput, false);

    private final FButton cmdCancel = new FButton(Localizer.getInstance().getMessage("lblCancel"));

    private FButton cmdAccept;  // Not initialised as label will be adaptive.
    private final FCheckBox dateTimeCheck = new FCheckBox(Localizer.getInstance().getMessage("lblUseOnlySetsReleasedBefore"), false);
    private final FCheckBox createNewDeckCheckbox = new FCheckBox(Localizer.getInstance().getMessage("lblNewDeckCheckbox"), false);
    private final DeckFormat deckFormat;

    //don't need wrappers since skin can't change while this dialog is open
    private final FComboBox<String> monthDropdown = new FComboBox<>();
    private final FComboBox<Integer> yearDropdown = new FComboBox<>();

    private final DeckImportController controller;
    private final ACEditorBase<TItem, TModel> host;

    private final String IMPORT_CARDS_CMD_LABEL = Localizer.getInstance().getMessage("lblImportCardsCmd");
    private final String CREATE_NEW_DECK_CMD_LABEL = Localizer.getInstance().getMessage("lblCreateNewCmd");

    public DeckImport(final ACEditorBase<TItem, TModel> g) {
        boolean currentDeckIsNotEmpty = !(g.getDeckController().isEmpty());
        DeckFormat currentDeckFormat = g.getGameType().getDeckFormat();
        this.deckFormat = currentDeckFormat;
        GameType currentGameType = g.getGameType();
        // get the game format with the same name of current game type (if any)
        GameFormat currentGameFormat = FModel.getFormats().get(currentGameType.name());
        if (currentGameFormat == null)
            currentGameFormat = FModel.getFormats().get("Vintage");  // default for constructed
        List<String> allowedSetCodes = currentGameFormat.getAllowedSetCodes();
        this.controller = new DeckImportController(dateTimeCheck, monthDropdown, yearDropdown,
                currentDeckIsNotEmpty, allowedSetCodes, currentDeckFormat);
        this.cmdAccept = new FButton(IMPORT_CARDS_CMD_LABEL);
        this.host = g;
        initMainPanel(g, currentDeckIsNotEmpty, currentGameType);
    }

    private void initMainPanel(ACEditorBase<TItem, TModel> g, boolean currentDeckIsNotEmpty, GameType currentGameType) {
//        GraphicsDevice gd = this.getGraphicsConfiguration().getDevice();
//        final int wWidth = (int)(gd.getDisplayMode().getWidth() * 0.85);
//        final int wHeight = (int)(gd.getDisplayMode().getHeight() * 0.8);
        final int wWidth = (int)(Singletons.getView().getFrame().getSize().width * 0.95);
        final int wHeight = (int)(Singletons.getView().getFrame().getSize().height * 0.9);
        this.setPreferredSize(new Dimension(wWidth, wHeight));
        this.setSize(wWidth, wHeight);

        String gameTypeName = String.format(" %s", currentGameType.name());
        this.setTitle(Localizer.getInstance().getMessage("lblDeckImporterPanelTitle") + gameTypeName);

        txtInput.setFocusable(true);
        txtInput.setEditable(true);

        final FSkin.SkinColor foreColor = FSkin.getColor(FSkin.Colors.CLR_TEXT);
        this.scrollInput.setBorder(new FSkin.TitledSkinBorder(BorderFactory.createEtchedBorder(),
                Localizer.getInstance().getMessage("lblCardListTitle"), foreColor));
        this.scrollInput.setViewportBorder(BorderFactory.createLoweredBevelBorder());

        this.scrollOutput.setBorder(new FSkin.TitledSkinBorder(BorderFactory.createEtchedBorder(),
                Localizer.getInstance().getMessage("lblDecklistTitle"), foreColor));
        this.scrollOutput.setViewportBorder(BorderFactory.createLoweredBevelBorder());

        this.add(this.scrollInput, "cell 0 0, w 40%, growy, pushy");
        this.add(this.scrollOutput, "cell 1 0, w 60%, growx, growy, pushx, pushy");
        this.add(VStatisticsImporter.instance().getMainPanel(),
                "cell 2 0, w 40%, growx, growy, pushx, pushy");

        this.add(this.dateTimeCheck, "cell 0 1, w 50%, ax c");
        this.add(monthDropdown, "cell 0 2, w 20%, ax left, split 2, pad 0 4 0 0");
        this.add(yearDropdown, "cell 0 2, w 20%, ax left, split 2, pad 0 4 0 0");

        if (currentDeckIsNotEmpty)
            this.add(this.createNewDeckCheckbox,"cell 2 1, ax left");
        this.add(this.cmdAccept, "cell 2 2, w 175, align r, h 26");
        this.add(this.cmdCancel, "cell 2 2, w 175, align r, h 26");

        this.cmdCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                DeckImport.this.processWindowEvent(new WindowEvent(DeckImport.this, WindowEvent.WINDOW_CLOSING));
            }
        });

        this.cmdAccept.addActionListener(new ActionListener() {
            @SuppressWarnings("unchecked")
            @Override
            public void actionPerformed(final ActionEvent e) {
                String currentDeckName = g.getDeckController().getModelName();
                final Deck deck = controller.accept(currentDeckName);
                if (deck == null) { return; }
                // If the soon-to-import card list hasn't got any name specified in the list
                // we set it to the current one (if any) or set a new one.
                // In this way, if this deck will replace the current one, the name will be kept the same!
                if (!deck.hasName()){
                    if (currentDeckName.equals(""))
                        deck.setName(Localizer.getInstance().getMessage("lblNewDeckName"));
                    else
                        deck.setName(currentDeckName);
                }

                DeckImport.this.host.getDeckController().loadDeck(deck, controller.getCreateNewDeck());
                DeckImport.this.processWindowEvent(new WindowEvent(DeckImport.this, WindowEvent.WINDOW_CLOSING));
            }
        });

        final ActionListener updateDateCheck = new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                final boolean isSel = dateTimeCheck.isSelected();
                monthDropdown.setEnabled(isSel);
                yearDropdown.setEnabled(isSel);
                parseAndDisplay();
            }
        };
        this.dateTimeCheck.addActionListener(updateDateCheck);

        final ActionListener reparse = new ActionListener() {
            @Override public void actionPerformed(final ActionEvent e) {
                parseAndDisplay();
            }
        };

        final ActionListener toggleNewDeck = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { toggleNewDeck(); }
        };

        this.yearDropdown.addActionListener(reparse);
        this.monthDropdown.addActionListener(reparse);
        updateDateCheck.actionPerformed(null); // update actual state

        this.txtInput.getDocument().addDocumentListener(new OnChangeTextUpdate());
        this.cmdAccept.setEnabled(false);

        if (currentDeckIsNotEmpty){
            this.createNewDeckCheckbox.setSelected(false);
            this.createNewDeckCheckbox.addActionListener(toggleNewDeck);
        }
    }

    /**
     * The Class OnChangeTextUpdate.
     */
    protected class OnChangeTextUpdate implements DocumentListener {
        private void onChange() {
            parseAndDisplay();
        }

        @Override
        public final void insertUpdate(final DocumentEvent e) {
            this.onChange();
        }

        @Override
        public final void removeUpdate(final DocumentEvent e) {
            this.onChange();
        }

        @Override
        public void changedUpdate(final DocumentEvent e) {
        } // Happend only on ENTER pressed
    }

    private void parseAndDisplay() {
        final List<DeckRecognizer.Token> tokens = controller.parseInput(txtInput.getText());
        displayTokens(tokens);
        updateSummaries(tokens);
    }

    private void toggleNewDeck(){
        boolean createNewDeck = this.createNewDeckCheckbox.isSelected();
        this.controller.setCreateNewDeck(createNewDeck);
        String cmdAcceptLabel = createNewDeck ? this.CREATE_NEW_DECK_CMD_LABEL : this.IMPORT_CARDS_CMD_LABEL;
        this.cmdAccept.setText(cmdAcceptLabel);
    }

    private void displayTokens(final List<DeckRecognizer.Token> tokens) {
        if (tokens.isEmpty() || hasOnlyComment(tokens)) {
            htmlOutput.setText(HTML_WELCOME_TEXT);
        } else {
            final StringBuilder sbOut = new StringBuilder("<html>");
            sbOut.append(String.format("<head>%s</head>", DeckImport.STYLESHEET));
            sbOut.append(String.format("<body><h3>%s</h3>", Localizer.getInstance().getMessage("lblCurrentDecklist")));
            for (final DeckRecognizer.Token t : tokens)
                sbOut.append(toHTML(t));
            sbOut.append("</body></html>");
            htmlOutput.setText(sbOut.toString());
        }
    }

    private boolean hasOnlyComment(final List<DeckRecognizer.Token> tokens) {
        for (DeckRecognizer.Token token : tokens) {
            if (token.getType() != TokenType.COMMENT && token.getType() != TokenType.UNKNOWN_TEXT)
                return false;
        }
        return true;
    }

    private void updateSummaries(final List<DeckRecognizer.Token> tokens) {
        int legalCardsCount = 0;
        List<Map.Entry<PaperCard, Integer>> tokenCards = new ArrayList<>();
        for (final DeckRecognizer.Token t : tokens) {
            if (t.getType() == TokenType.LEGAL_CARD_REQUEST) {
                int tokenNumber = t.getNumber();
                legalCardsCount += tokenNumber;
                PaperCard tokenCard = t.getCard();
                tokenCards.add(new AbstractMap.SimpleEntry<>(tokenCard, tokenNumber));
            }
        }
        CStatisticsImporter.instance().updateStats(tokenCards);
        cmdAccept.setEnabled(legalCardsCount > 0);
    }

    private String toHTML(final DeckRecognizer.Token token) {
        if (token == null)
            return "";

        switch (token.getType()) {
            case LEGAL_CARD_REQUEST:
                return String.format("<div class=\"knowncard\">%s x %s " +
                                "<span class=\"editioncode\">(%s)</span> %s %s</div>",
                        token.getNumber(), token.getCard().getName(),
                        token.getCard().getEdition(),
                        token.getCard().getCollectorNumber(),
                        token.getCard().isFoil() ? "<i>(FOIL)</i>" : "");
            case UNKNOWN_CARD_REQUEST:
                return String.format("<div class=\"unknowncard\">%s x %s (%s)</div>",
                        token.getNumber(), token.getText(),
                        Localizer.getInstance().getMessage("lblUnknownCardMsg"));
            case ILLEGAL_CARD_REQUEST:
                return String.format("<div class=\"illegalcard\">%s x %s (%s %s)</div>",
                        token.getNumber(), token.getText(),
                        Localizer.getInstance().getMessage("lblIllegalCardMsg"),
                        this.deckFormat.name());
            case INVALID_CARD_REQUEST:
                return String.format("<div class=\"invalidcard\">%s x %s (%s)</div>",
                        token.getNumber(), token.getText(),
                        Localizer.getInstance().getMessage("lblInvalidCardMsg"));
            case DECK_SECTION_NAME:
                return String.format("<div class=\"section\">%s</div>", token.getText());
            case CARD_TYPE:
                return String.format("<div class=\"cardtype\">%s</div>", token.getText());
            case CARD_RARITY:
                return String.format("<div class=\"rarity\">%s</div>", token.getText());
            case CARD_CMC:
                return String.format("<div class=\"cmc\">%s</div>", token.getText());
            case MANA_COLOUR:
                String cssColorClass = token.getText().toLowerCase().trim();
                return String.format("<div class=\"%s\">%s</div>", cssColorClass, token.getText());
            case DECK_NAME:
                return String.format("<div class=\"deckname\">%s: %s</div>",
                        Localizer.getInstance().getMessage("lblDeckName"),
                        token.getText());
            case COMMENT:
                return String.format("<div class=\"comment\">%s</div>", token.getText());
            case UNKNOWN_TEXT:
            default:
                return "";
        }
    }
}
