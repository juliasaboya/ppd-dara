package dara.ui;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.github.weisj.jsvg.view.ViewBox;
import dara.comunication.network.PlayerSlot;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.net.URL;

public class LobbyPanel extends JPanel {
    private static final int PANEL_WIDTH = 920;
    private static final int PANEL_HEIGHT = 768;

    private static final Color SAND_LIGHT = new Color(240, 197, 142);
    private static final Color SAND_MID = new Color(220, 170, 112);
    private static final Color INK = new Color(33, 22, 12);
    private static final Color ACTION_BUTTON = new Color(187, 92, 15);
    private static final Color DISABLED_BUTTON = new Color(202, 138, 81);
    private static final Color WAITING_TEXT = new Color(54, 54, 54);
    private static final double MENU_BOX_SCALE = 0.82;
    private static final SVGDocument MENU_BOX_SVG = loadSvg("/dara/ui/images/menuBox.svg");

    private final JButton searchButton;
    private String headline;
    private String detailLine;
    private String footerMessage;
    private Integer waitingSeconds;

    public LobbyPanel(Runnable onSearchClick) {
        setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        setLayout(null);
        setOpaque(true);

        headline = "PRONTO PARA JOGAR";
        detailLine = "CLIQUE NO BOTAO PARA ENTRAR NA FILA";
        footerMessage = "O SERVIDOR INICIARA A PARTIDA ASSIM QUE HOUVER DOIS CLIENTES";

        searchButton = createButton("PROCURAR PARTIDA", (PANEL_WIDTH - 310) / 2, 456, onSearchClick);
        add(searchButton);
    }

    public void showConnecting() {
        headline = "CONECTANDO AO SERVIDOR";
        detailLine = "AGUARDE...";
        footerMessage = "ESTABELECENDO CONEXAO COM O LOBBY";
        waitingSeconds = null;
        searchButton.setEnabled(false);
        repaint();
    }

    public void showSearching(PlayerSlot slot) {
        headline = "PROCURANDO PARTIDA";
        detailLine = "VOCE ENTROU NA FILA";
        footerMessage = "AGUARDANDO ADVERSARIO";
        waitingSeconds = 0;
        searchButton.setEnabled(false);
        repaint();
    }

    public void updateSearchSeconds(int seconds) {
        waitingSeconds = seconds;
        repaint();
    }

    public void showMatchFound(PlayerSlot slot) {
        headline = "PARTIDA ENCONTRADA";
        detailLine = "VOCE X ADVERSARIO";
        footerMessage = "INICIANDO O TABULEIRO";
        waitingSeconds = null;
        repaint();
    }

    public void showConnectionError(String message) {
        headline = "FALHA DE CONEXAO";
        detailLine = message;
        footerMessage = "VERIFIQUE O SERVIDOR E TENTE NOVAMENTE";
        waitingSeconds = null;
        searchButton.setEnabled(true);
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
        drawCounter(g2);

        g2.dispose();
    }

    private JButton createButton(String text, int x, int y, Runnable action) {
        RoundedButton button = new RoundedButton(text);
        button.setBounds(x, y, 310, 56);
        button.addActionListener(event -> action.run());
        return button;
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
        drawCentered(g2, "DARA GAME", 0, 244, getWidth());

        g2.setFont(new Font("Serif", Font.BOLD, 26));
        drawCentered(g2, headline, 0, 354, getWidth());

        g2.setFont(new Font("Serif", Font.BOLD, 18));
        drawCentered(g2, detailLine, 0, 394, getWidth());

        g2.setColor(WAITING_TEXT);
        g2.setFont(new Font("Serif", Font.BOLD, 15));
        drawCenteredLines(g2, splitFooterMessage(), 0, 562, getWidth(), 22);
    }

    private void drawCounter(Graphics2D g2) {
        if (waitingSeconds == null) {
            return;
        }

        g2.setColor(INK);
        g2.setFont(new Font("Serif", Font.BOLD, 72));
        String secondsText = waitingSeconds + "s";
        drawCentered(g2, secondsText, 0, 438, getWidth());
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
        if ("O SERVIDOR INICIARA A PARTIDA ASSIM QUE HOUVER DOIS CLIENTES".equals(footerMessage)) {
            return new String[]{
                    "O SERVIDOR INICIARA A PARTIDA",
                    "ASSIM QUE HOUVER DOIS CLIENTES"
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
        private RoundedButton(String text) {
            super(text);
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
            g2.setColor(isEnabled() ? ACTION_BUTTON : DISABLED_BUTTON);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 28, 28);
            g2.setColor(new Color(115, 92, 72));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 28, 28);
            g2.dispose();

            super.paintComponent(graphics);
        }
    }
}
