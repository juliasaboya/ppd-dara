package dara.ui;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.github.weisj.jsvg.view.ViewBox;
import dara.network.PlayerSlot;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.Rectangle;
import java.net.URL;

public class LobbyPanel extends JPanel {
    private static final int PANEL_WIDTH = 1080;
    private static final int PANEL_HEIGHT = 768;

    private static final Color SAND_LIGHT = new Color(240, 197, 142);
    private static final Color SAND_MID = new Color(220, 170, 112);
    private static final Color INK = new Color(33, 22, 12);
    private static final Color PLAYER_ONE_BUTTON = new Color(187, 92, 15);
    private static final Color PLAYER_TWO_BUTTON = new Color(19, 126, 11);
    private static final Color DISABLED_BUTTON = new Color(202, 138, 81);
    private static final Color WAITING_TEXT = new Color(54, 54, 54);
    private static final Color INPUT_BG = new Color(251, 224, 185);
    private static final double MENU_BOX_SCALE = 0.92;
    private static final SVGDocument MENU_BOX_SVG = loadSvg("/dara/ui/images/menuBox.svg");

    private final JButton playerOneButton;
    private final JButton playerTwoButton;
    private final JTextField playerOneField;
    private final JTextField playerTwoField;
    private String playerOneStatus;
    private String playerTwoStatus;
    private String footerMessage;
    private Integer countdownValue;

    public LobbyPanel(Runnable onPlayerOneClick, Runnable onPlayerTwoClick) {
        setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        setLayout(null);
        setOpaque(true);

        playerOneStatus = "CLIQUE PARA ENTRAR";
        playerTwoStatus = "CLIQUE PARA ENTRAR";
        footerMessage = "A PARTIDA SERA INICIADA QUANDO DOIS JOGADORES SE CONECTAREM";

        playerOneField = createNameField("Player 1", 265, 422);
        playerTwoField = createNameField("Player 2", 580, 422);
        playerOneButton = createButton("ENTRAR", PLAYER_ONE_BUTTON, 265, 474, onPlayerOneClick);
        playerTwoButton = createButton("ENTRAR", PLAYER_TWO_BUTTON, 580, 474, onPlayerTwoClick);

        add(playerOneField);
        add(playerTwoField);
        add(playerOneButton);
        add(playerTwoButton);
    }

    public void markConnected(PlayerSlot slot) {
        if (slot == PlayerSlot.PLAYER_1) {
            playerOneStatus = "AGUARDANDO...";
            playerOneButton.setEnabled(false);
            playerOneField.setEnabled(false);
        } else {
            playerTwoStatus = "AGUARDANDO...";
            playerTwoButton.setEnabled(false);
            playerTwoField.setEnabled(false);
        }
        repaint();
    }

    public void startCountdown() {
        playerOneField.setVisible(false);
        playerTwoField.setVisible(false);
        playerOneButton.setVisible(false);
        playerTwoButton.setVisible(false);
        countdownValue = 3;
        footerMessage = "JOGADORES CONECTADOS";
        repaint();
    }

    public void updateCountdown(int value) {
        countdownValue = value;
        repaint();
    }

    public void showConnectionError(String message) {
        footerMessage = message;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawBackground(g2);
        drawScroll(g2);
        drawTexts(g2);
        drawCountdown(g2);

        g2.dispose();
    }

    private JButton createButton(String text, Color color, int x, int y, Runnable action) {
        RoundedButton button = new RoundedButton(text, color);
        button.setBounds(x, y, 205, 56);
        button.addActionListener(event -> action.run());
        return button;
    }

    private JTextField createNameField(String text, int x, int y) {
        JTextField field = new JTextField(text);
        field.setBounds(x, y, 205, 42);
        field.setHorizontalAlignment(JTextField.CENTER);
        field.setFont(new Font("Serif", Font.BOLD, 20));
        field.setForeground(INK);
        field.setBackground(INPUT_BG);
        field.setCaretColor(INK);
        field.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(new Color(115, 92, 72), 2, true),
                javax.swing.BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        return field;
    }

    public String getPlayerOneName() {
        return playerOneField.getText();
    }

    public String getPlayerTwoName() {
        return playerTwoField.getText();
    }

    private void drawBackground(Graphics2D g2) {
        Paint oldPaint = g2.getPaint();
        g2.setPaint(new GradientPaint(0, 0, SAND_LIGHT, 0, getHeight(), SAND_MID));
        g2.fillRect(0, 0, getWidth(), getHeight());

        g2.setColor(new Color(184, 141, 87, 38));
        g2.setStroke(new BasicStroke(46f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawArc(-120, 10, 480, 220, 10, 160);
        g2.drawArc(250, -40, 520, 250, 5, 170);
        g2.drawArc(640, 0, 500, 240, 0, 165);
        g2.drawArc(-60, 500, 540, 220, 200, 165);
        g2.drawArc(520, 470, 620, 260, 185, 160);
        g2.setPaint(oldPaint);
    }

    private void drawScroll(Graphics2D g2) {
        drawCenteredSvg(g2, MENU_BOX_SVG, 65);
    }

    private void drawTexts(Graphics2D g2) {
        g2.setColor(INK);
        g2.setFont(new Font("Serif", Font.BOLD, 52));
        g2.drawString("DARA GAME", 366, 262);

        g2.setFont(new Font("Serif", Font.BOLD, 18));
        g2.drawString(playerOneStatus, 278, 360);
        g2.drawString(playerTwoStatus, 592, 360);

        g2.setColor(WAITING_TEXT);
        g2.setFont(new Font("Serif", Font.BOLD, 14));
        drawCenteredLines(g2, splitFooterMessage(), 0, 580, getWidth(), 20);
    }

    private void drawCountdown(Graphics2D g2) {
        if (countdownValue == null) {
            return;
        }

        g2.setColor(INK);
        g2.setFont(new Font("Serif", Font.BOLD, 92));
        String text = String.valueOf(countdownValue);
        int width = g2.getFontMetrics().stringWidth(text);
        g2.drawString(text, (getWidth() - width) / 2, 408);
    }

    private void drawCentered(Graphics2D g2, String text, int x, int y, int width) {
        int textWidth = g2.getFontMetrics().stringWidth(text);
        g2.drawString(text, x + (width - textWidth) / 2, y);
    }

    private void drawCenteredLines(Graphics2D g2, String[] lines, int x, int startY, int width, int lineHeight) {
        for (int i = 0; i < lines.length; i++) {
            drawCentered(g2, lines[i], x, startY + i * lineHeight, width);
        }
    }

    private String[] splitFooterMessage() {
        if ("A PARTIDA SERA INICIADA QUANDO DOIS JOGADORES SE CONECTAREM".equals(footerMessage)) {
            return new String[]{
                    "A PARTIDA SERA INICIADA QUANDO DOIS",
                    "JOGADORES SE CONECTAREM"
            };
        }
        return new String[]{footerMessage};
    }

    private Rectangle drawCenteredSvg(Graphics2D g2, SVGDocument document, int topMargin) {
        int width = (int) Math.round(document.size().width * MENU_BOX_SCALE);
        int height = (int) Math.round(document.size().height * MENU_BOX_SCALE);
        int x = (getWidth() - width) / 2;
        int y = topMargin;

        Graphics2D svgGraphics = (Graphics2D) g2.create();
        svgGraphics.translate(x, y);
        document.render(this, svgGraphics, new ViewBox(0, 0, width, height));
        svgGraphics.dispose();

        return new Rectangle(x, y, width, height);
    }

    private static SVGDocument loadSvg(String resourcePath) {
        URL resource = LobbyPanel.class.getResource(resourcePath);
        if (resource == null) {
            throw new IllegalStateException("Nao foi possivel carregar o SVG: " + resourcePath);
        }

        SVGDocument document = new SVGLoader().load(resource);
        if (document == null) {
            throw new IllegalStateException("Falha ao renderizar o SVG: " + resourcePath);
        }

        return document;
    }

    private static final class RoundedButton extends JButton {
        private final Color color;

        private RoundedButton(String text, Color color) {
            super(text);
            this.color = color;
            setBorderPainted(false);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setOpaque(false);
            setForeground(INK);
            setFont(new Font("Serif", Font.BOLD, 22));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(isEnabled() ? color : DISABLED_BUTTON);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 28, 28);
            g2.setColor(new Color(115, 92, 72));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 28, 28);
            g2.dispose();

            super.paintComponent(graphics);
        }
    }
}
