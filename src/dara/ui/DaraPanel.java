package dara.ui;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.github.weisj.jsvg.view.ViewBox;
import dara.model.Board;
import dara.model.Game;
import dara.model.Player;
import dara.comunication.network.PlayerSlot;
import dara.comunication.protocol.GameAction;
import dara.comunication.protocol.GameActionType;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.Timer;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class DaraPanel extends JPanel {
    private static final int PANEL_WIDTH = 920;
    private static final int PANEL_HEIGHT = 768;
    private static final double SVG_SCALE = 0.75;
    private static final int CHAT_BOX_WIDTH = (int) Math.round(785 * SVG_SCALE);
    private static final int CHAT_BOX_HEIGHT = (int) Math.round(268 * SVG_SCALE);
    private static final int CHAT_BOX_X = (PANEL_WIDTH - CHAT_BOX_WIDTH) / 2;
    private static final int CHAT_BOX_Y = PANEL_HEIGHT - CHAT_BOX_HEIGHT;

    public interface ChatSender {
        void send(String text);
    }

    public interface GameActionSender {
        void send(GameAction action);
    }

    private static final Color SAND_LIGHT = new Color(240, 197, 142);
    private static final Color SAND_MID = new Color(220, 170, 112);
    private static final Color BOARD_FRAME = new Color(145, 118, 79);
    private static final Color CELL_LIGHT = new Color(248, 229, 188);
    private static final Color CELL_DARK = new Color(228, 184, 124);
    private static final Color PLAYER_ONE = new Color(42, 107, 36);
    private static final Color PLAYER_ONE_SHADOW = new Color(20, 61, 17);
    private static final Color PLAYER_TWO = new Color(178, 92, 28);
    private static final Color PLAYER_TWO_SHADOW = new Color(122, 54, 17);
    private static final Color INK = new Color(33, 22, 12);
    private static final Color IVORY = new Color(255, 245, 222);
    private static final Color CHAT_LOCAL = new Color(49, 96, 42);
    private static final Color CHAT_OPPONENT = new Color(145, 76, 23);
    private static final Color CHAT_SYSTEM = new Color(102, 76, 52);

    private static final int TOP_BANNER_MARGIN = 10;
    private static final int BOARD_WIDTH = 477;
    private static final int BOARD_HEIGHT = 376;
    private static final int BOARD_X = (PANEL_WIDTH - BOARD_WIDTH) / 2;
    private static final int BOARD_Y = 192;
    private static final int BOARD_MARGIN = 16;
    private static final int BOARD_GAP = 8;
    private static final int LEFT_RESERVE_LABEL_X = 10;
    private static final int LEFT_RESERVE_LABEL_Y = 130;
    private static final int LEFT_RESERVE_LABEL_WIDTH = 184;
    private static final int LEFT_RESERVE_PIECES_X = 34;
    private static final int LEFT_RESERVE_PIECES_Y = 228;
    private static final int RIGHT_RESERVE_LABEL_X = 726;
    private static final int RIGHT_RESERVE_LABEL_Y = 494;
    private static final int RIGHT_RESERVE_LABEL_WIDTH = 184;
    private static final int RIGHT_RESERVE_PIECES_X = 718;
    private static final int RIGHT_RESERVE_PIECES_Y = 178;
    private static final int ACTION_BUTTON_X = BOARD_X + BOARD_WIDTH + 18;
    private static final double OPPONENT_PIECE_SCALE = 0.6;
    private static final float OPPONENT_PIECE_OPACITY = 0.55f;

    private static final SVGDocument TOP_BANNER_SVG = loadSvg("/dara/ui/images/old_paper_scroll_set.svg");
    private static final SVGDocument CHAT_BOX_SVG = loadSvg("/dara/ui/images/ChatBox.svg");
    private static final Image BACKGROUND_IMAGE = loadImage();

    private final List<ReservePieceHitBox> reserveHitBoxes;
    private final PlayerSlot localSlot;
    private final Player localPlayer;
    private final ChatSender chatSender;
    private final GameActionSender gameActionSender;
    private final Game game;
    private final JButton randomPhaseButton;
    private final JButton surrenderButton;
    private final JButton restartButton;
    private final JTextPane chatTextPane;
    private final JTextField chatInputField;
    private String playerMessage;
    private String opponentMessage;
    private Player selectedReservePlayer;
    private BoardCell selectedBoardCell;
    private boolean randomPhaseUsed;
    private PieceAnimation activeAnimation;
    private Timer animationTimer;

    public DaraPanel(Game game, PlayerSlot localSlot, ChatSender chatSender, GameActionSender gameActionSender,
                     Runnable restartToLobbyAction) {
        this.game = game;
        this.localSlot = localSlot;
        this.localPlayer = localSlot.getControlledPlayer();
        this.chatSender = chatSender;
        this.gameActionSender = gameActionSender;
        this.reserveHitBoxes = new ArrayList<>();

        setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        setBackground(SAND_LIGHT);
        setLayout(null);

        randomPhaseButton = createHelperButton("Auto Fase", ACTION_BUTTON_X, TOP_BANNER_MARGIN + 24, 96, _ -> runRandomPhaseHelper());
        surrenderButton = createHelperButton("Desistir", ACTION_BUTTON_X, TOP_BANNER_MARGIN + 64, 96, _ -> surrenderMatch());
        restartButton = createHelperButton("Novo Jogo", BOARD_X + 168, BOARD_Y + 214, 140, _ -> restartToLobbyAction.run());
        restartButton.setVisible(false);
        randomPhaseButton.setVisible(false);
        randomPhaseButton.setEnabled(false);

        chatTextPane = createChatTextPane();
        JScrollPane chatScrollPane = createChatScrollPane(chatTextPane);
        chatInputField = createChatInputField();

        add(randomPhaseButton);
        add(surrenderButton);
        add(restartButton);
        add(chatScrollPane);
        add(chatInputField);
        addMouseListener(new BoardMouseHandler());

        updateStatusMessages();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        drawBackground(g2);
        drawTopBanner(g2);
        drawBoard(g2);
        drawLeftReserve(g2);
        drawRightReserve(g2);
        drawChatBox(g2);
        if (game.getState() == dara.model.GameState.FINISHED) {
            drawFinishedOverlay(g2);
        } else {
            drawStatusLines(g2);
        }
        drawActiveAnimation(g2);

        g2.dispose();
    }

    private void drawBackground(Graphics2D g2) {
        if (BACKGROUND_IMAGE != null) {
            g2.drawImage(BACKGROUND_IMAGE, 0, 0, getWidth(), getHeight(), this);
            return;
        }

        Paint oldPaint = g2.getPaint();
        g2.setPaint(new GradientPaint(0, 0, SAND_LIGHT, 0, getHeight(), SAND_MID));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.setPaint(oldPaint);
    }

    private void drawTopBanner(Graphics2D g2) {
        Rectangle bounds = drawSvgCentered(g2);

        g2.setFont(new Font("Serif", Font.BOLD, 32));
        g2.setColor(INK);
        g2.drawString(game.getFormattedElapsedTime(), bounds.x + 40, bounds.y + 66);

        g2.setFont(new Font("Serif", Font.BOLD, 21));
        drawCenteredText(g2, getDisplayStateText(), new Rectangle(bounds.x + 160, bounds.y + 24, 200, 62));
    }

    private void drawBoard(Graphics2D g2) {
        reserveHitBoxes.clear();

        g2.setPaint(new GradientPaint(BOARD_X, BOARD_Y, new Color(253, 240, 213), BOARD_X, BOARD_Y + BOARD_HEIGHT, new Color(235, 205, 159)));
        g2.fillRoundRect(BOARD_X, BOARD_Y, BOARD_WIDTH, BOARD_HEIGHT, 8, 8);
        g2.setColor(BOARD_FRAME);
        g2.setStroke(new BasicStroke(3f));
        g2.drawRoundRect(BOARD_X, BOARD_Y, BOARD_WIDTH, BOARD_HEIGHT, 8, 8);

        int cellWidth = (BOARD_WIDTH - BOARD_MARGIN * 2 - BOARD_GAP * (Board.COLUMNS - 1)) / Board.COLUMNS;
        int cellHeight = (BOARD_HEIGHT - BOARD_MARGIN * 2 - BOARD_GAP * (Board.ROWS - 1)) / Board.ROWS;

        for (int row = 0; row < Board.ROWS; row++) {
            for (int column = 0; column < Board.COLUMNS; column++) {
                int x = BOARD_X + BOARD_MARGIN + column * (cellWidth + BOARD_GAP);
                int y = BOARD_Y + BOARD_MARGIN + row * (cellHeight + BOARD_GAP);
                drawCell(g2, x, y, cellWidth, cellHeight, row, column);

                Player piece = game.getBoard().getPiece(row, column);
                if (piece != null && !isAnimatedDestination(row, column)) {
                    int pieceSize = cellHeight - 28;
                    drawPiece(g2, x + cellWidth / 2, y + cellHeight / 2, pieceSize / 2, piece);
                }
            }
        }
    }

    private void drawCell(Graphics2D g2, int x, int y, int width, int height, int row, int column) {
        g2.setPaint(new GradientPaint(x, y, CELL_LIGHT, x + width, y + height, CELL_DARK));
        g2.fillRoundRect(x, y, width, height, 6, 6);
        g2.setColor(new Color(218, 173, 112, 90));
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawRoundRect(x, y, width, height, 6, 6);

        g2.setColor(new Color(188, 126, 76, 42));
        for (int i = 0; i < 24; i++) {
            int px = x + 6 + (i * 11) % (width - 12);
            int py = y + 8 + (i * 17) % (height - 16);
            g2.fillOval(px, py, 2, 2);
        }

        if (selectedReservePlayer != null && game.canPlacePiece(selectedReservePlayer, row, column)) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.22f));
            g2.setColor(selectedReservePlayer == Player.COLOR_ONE ? PLAYER_ONE : PLAYER_TWO);
            g2.fillRoundRect(x, y, width, height, 6, 6);
            g2.setComposite(AlphaComposite.SrcOver);
            g2.setStroke(new BasicStroke(2f));
            g2.setColor(new Color(255, 246, 220));
            g2.drawRoundRect(x + 1, y + 1, width - 2, height - 2, 6, 6);
        }

        if (selectedBoardCell != null && selectedBoardCell.row == row && selectedBoardCell.column == column) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.20f));
            g2.setColor(new Color(255, 245, 220));
            g2.fillRoundRect(x, y, width, height, 6, 6);
            g2.setComposite(AlphaComposite.SrcOver);
            g2.setStroke(new BasicStroke(3f));
            g2.setColor(new Color(255, 247, 225));
            g2.drawRoundRect(x + 1, y + 1, width - 2, height - 2, 6, 6);
        }

        if (selectedBoardCell != null && game.canMovePiece(game.getCurrentTurn(), selectedBoardCell.row, selectedBoardCell.column, row, column)) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.18f));
            g2.setColor(new Color(255, 247, 225));
            g2.fillOval(x + width / 2 - 12, y + height / 2 - 12, 24, 24);
            g2.setComposite(AlphaComposite.SrcOver);
        }

        if (game.isAwaitingRemoval() && game.canRemoveOpponentPiece(game.getCurrentTurn(), row, column)) {
            g2.setStroke(new BasicStroke(3f));
            g2.setColor(new Color(180, 32, 24));
            g2.drawRoundRect(x + 2, y + 2, width - 4, height - 4, 6, 6);
        }
    }

    private void drawLeftReserve(Graphics2D g2) {
        g2.setPaint(new GradientPaint(LEFT_RESERVE_LABEL_X, LEFT_RESERVE_LABEL_Y, new Color(90, 141, 55),
                LEFT_RESERVE_LABEL_X + LEFT_RESERVE_LABEL_WIDTH, LEFT_RESERVE_LABEL_Y, new Color(116, 157, 73)));
        g2.fillRoundRect(LEFT_RESERVE_LABEL_X, LEFT_RESERVE_LABEL_Y, LEFT_RESERVE_LABEL_WIDTH, 68, 32, 32);
        drawReserveHeader(g2, LEFT_RESERVE_LABEL_X, 170, Player.COLOR_ONE);
        drawReservePieces(g2, LEFT_RESERVE_PIECES_X, LEFT_RESERVE_PIECES_Y, game.getReserveCount(Player.COLOR_ONE), Player.COLOR_ONE);
    }

    private void drawRightReserve(Graphics2D g2) {
        g2.setPaint(new GradientPaint(RIGHT_RESERVE_LABEL_X, RIGHT_RESERVE_LABEL_Y, new Color(207, 145, 88),
                RIGHT_RESERVE_LABEL_X + RIGHT_RESERVE_LABEL_WIDTH, RIGHT_RESERVE_LABEL_Y, new Color(196, 123, 61)));
        g2.fillRoundRect(RIGHT_RESERVE_LABEL_X, RIGHT_RESERVE_LABEL_Y, RIGHT_RESERVE_LABEL_WIDTH, 66, 34, 34);
        drawReserveHeader(g2, RIGHT_RESERVE_LABEL_X, 536, Player.COLOR_TWO);
        drawReservePieces(g2, RIGHT_RESERVE_PIECES_X, RIGHT_RESERVE_PIECES_Y, game.getReserveCount(Player.COLOR_TWO), Player.COLOR_TWO);
    }

    private void drawReservePieces(Graphics2D g2, int startX, int startY, int count, Player player) {
        int rows = 5;
        int columns = (int) Math.ceil(count / (double) rows);
        int index = 0;

        for (int column = 0; column < columns; column++) {
            for (int row = 0; row < rows && index < count; row++) {
                int x = startX + column * 56;
                int y = startY + row * 62;
                reserveHitBoxes.add(new ReservePieceHitBox(player, new Rectangle(x - 24, y - 24, 48, 48)));
                if (selectedReservePlayer == player) {
                    drawSelectedReserveHighlight(g2, x, y);
                }
                drawPiece(g2, x, y, 21, player);
                index++;
            }
        }
    }

    private void drawChatBox(Graphics2D g2) {
        drawSvgBottomCentered(g2);
    }

    private void drawPiece(Graphics2D g2, int centerX, int centerY, int radius, Player player) {
        int adjustedRadius = player == localPlayer ? radius : Math.max(6, (int) Math.round(radius * OPPONENT_PIECE_SCALE));
        float opacity = player == localPlayer ? 1.0f : OPPONENT_PIECE_OPACITY;
        Color primary = player == Player.COLOR_ONE ? PLAYER_ONE : PLAYER_TWO;
        Color shadow = player == Player.COLOR_ONE ? PLAYER_ONE_SHADOW : PLAYER_TWO_SHADOW;
        Graphics2D pieceGraphics = (Graphics2D) g2.create();

        pieceGraphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.24f * opacity));
        pieceGraphics.setColor(new Color(22, 14, 8));
        pieceGraphics.fillOval(centerX - adjustedRadius - 5, centerY - adjustedRadius + 8, adjustedRadius * 2 + 10, adjustedRadius * 2 + 12);
        pieceGraphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));

        pieceGraphics.setColor(new Color(247, 233, 204));
        pieceGraphics.fill(new Ellipse2D.Double(
                centerX - adjustedRadius - 6,
                centerY - adjustedRadius - 6,
                (adjustedRadius + 6) * 2.0,
                (adjustedRadius + 6) * 2.0
        ));

        ShapeFactory starOuter = new ShapeFactory(centerX, centerY, adjustedRadius + 2, adjustedRadius - 8, 10);
        ShapeFactory starInner = new ShapeFactory(centerX, centerY, adjustedRadius - 2, adjustedRadius - 12, 10);

        pieceGraphics.setColor(shadow);
        pieceGraphics.fill(starOuter.create());
        pieceGraphics.setColor(primary);
        pieceGraphics.fill(starInner.create());

        pieceGraphics.setColor(new Color(252, 240, 213, 210));
        pieceGraphics.fill(new Ellipse2D.Double(centerX - 4, centerY - 4, 8, 8));

        pieceGraphics.setColor(new Color(255, 246, 220, 180));
        Polygon highlight = new Polygon();
        highlight.addPoint(centerX - 4, centerY - adjustedRadius + 5);
        highlight.addPoint(centerX + 6, centerY - adjustedRadius + 12);
        highlight.addPoint(centerX + 1, centerY - adjustedRadius / 3);
        pieceGraphics.fillPolygon(highlight);
        pieceGraphics.dispose();
    }

    private void drawCenteredText(Graphics2D g2, String text, Rectangle area) {
        int textWidth = g2.getFontMetrics().stringWidth(text);
        int ascent = g2.getFontMetrics().getAscent();
        int drawX = area.x + (area.width - textWidth) / 2;
        int drawY = area.y + (area.height + ascent) / 2 - 6;
        g2.drawString(text, drawX, drawY);
    }

    private void drawReserveHeader(Graphics2D g2, int x, int baselineY, Player player) {
        boolean localReserve = player == localPlayer;
        String title = game.getPlayerName(player).toUpperCase();
        String subtitle = localReserve ? "SUAS PECAS" : "PECAS DO RIVAL";

        g2.setColor(localReserve ? IVORY : new Color(255, 240, 214, 210));
        g2.setFont(new Font("Serif", Font.BOLD, 25));
        g2.drawString(title, x + 20, baselineY);

        g2.setColor(localReserve ? new Color(255, 248, 232, 220) : new Color(88, 53, 27, 180));
        g2.setFont(new Font("SansSerif", Font.BOLD, 11));
        g2.drawString(subtitle, x + 21, baselineY + 18);
    }

    private void drawSelectedReserveHighlight(Graphics2D g2, int centerX, int centerY) {
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f));
        g2.setColor(new Color(255, 247, 225));
        g2.fillOval(centerX - 30, centerY - 30, 60, 60);
        g2.setComposite(AlphaComposite.SrcOver);
        g2.setStroke(new BasicStroke(3f));
        g2.setColor(new Color(255, 247, 225));
        g2.drawOval(centerX - 30, centerY - 30, 60, 60);
    }

    private void drawStatusLines(Graphics2D g2) {
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("Serif", Font.BOLD, 22));
        drawCenteredText(g2, playerMessage, new Rectangle(BOARD_X - 100, 142, BOARD_WIDTH + 200, 26));

        g2.setColor(new Color(74, 48, 24));
        g2.setFont(new Font("Serif", Font.BOLD, 17));
        drawCenteredText(g2, opponentMessage, new Rectangle(BOARD_X - 80, 168, BOARD_WIDTH + 160, 22));
    }

    private void drawFinishedOverlay(Graphics2D g2) {
        Rectangle overlayBounds = new Rectangle(BOARD_X - 28, 126, BOARD_WIDTH + 56, BOARD_HEIGHT + 110);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.52f));
        g2.setColor(new Color(73, 54, 35));
        g2.fillRoundRect(overlayBounds.x, overlayBounds.y, overlayBounds.width, overlayBounds.height, 24, 24);
        g2.setComposite(AlphaComposite.SrcOver);

        g2.setColor(new Color(255, 245, 222));
        g2.setFont(new Font("Serif", Font.BOLD, 34));
        drawCenteredText(g2, game.getCurrentTurnName() + " é o(a) vencedor(a)!", new Rectangle(BOARD_X - 40, BOARD_Y + 116, BOARD_WIDTH + 80, 40));
    }

    private void drawActiveAnimation(Graphics2D g2) {
        if (activeAnimation == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (activeAnimation.isFinished(now)) {
            return;
        }
        double progress = activeAnimation.progress(now);
        int x = (int) Math.round(activeAnimation.fromX + (activeAnimation.toX - activeAnimation.fromX) * progress);
        int y = (int) Math.round(activeAnimation.fromY + (activeAnimation.toY - activeAnimation.fromY) * progress);
        drawPiece(g2, x, y, 18, activeAnimation.player);
    }

    private boolean isAnimatedDestination(int row, int column) {
        return activeAnimation != null
                && !activeAnimation.isFinished(System.currentTimeMillis())
                && activeAnimation.targetRow == row
                && activeAnimation.targetColumn == column;
    }

    private String getDisplayStateText() {
        return switch (game.getState()) {
            case PLACING -> "Fase de Colocação";
            case MOVIMENTATION -> "Fase de Movimentação";
            case WAITING -> "Aguardando";
            case FINISHED -> "Partida Finalizada";
        };
    }

    private Rectangle drawSvgCentered(Graphics2D g2) {
        int width = (int) Math.round(DaraPanel.TOP_BANNER_SVG.size().width * SVG_SCALE);
        int height = (int) Math.round(DaraPanel.TOP_BANNER_SVG.size().height * SVG_SCALE);
        int x = (getWidth() - width) / 2;
        int y = DaraPanel.TOP_BANNER_MARGIN;
        renderSvg(g2, DaraPanel.TOP_BANNER_SVG, x, y, width, height);
        return new Rectangle(x, y, width, height);
    }

    private void drawSvgBottomCentered(Graphics2D g2) {
        int width = (int) Math.round(DaraPanel.CHAT_BOX_SVG.size().width * SVG_SCALE);
        int height = (int) Math.round(DaraPanel.CHAT_BOX_SVG.size().height * SVG_SCALE);
        int x = (getWidth() - width) / 2;
        int y = getHeight() - height;
        renderSvg(g2, DaraPanel.CHAT_BOX_SVG, x, y, width, height);
    }

    private void renderSvg(Graphics2D g2, SVGDocument document, int x, int y, int width, int height) {
        Graphics2D svgGraphics = (Graphics2D) g2.create();
        svgGraphics.translate(x, y);
        document.render(this, svgGraphics, new ViewBox(0, 0, width, height));
        svgGraphics.dispose();
    }

    private static SVGDocument loadSvg(String resourcePath) {
        URL resource = DaraPanel.class.getResource(resourcePath);
        if (resource == null) {
            throw new IllegalStateException("Nao foi possivel carregar o SVG: " + resourcePath);
        }
        SVGDocument document = new SVGLoader().load(resource);
        if (document == null) {
            throw new IllegalStateException("Falha ao renderizar o SVG: " + resourcePath);
        }
        return document;
    }

    private static Image loadImage() {
        URL resource = DaraPanel.class.getResource("/dara/ui/images/fundo_areia.png");
        if (resource == null) {
            return null;
        }
        try {
            return ImageIO.read(resource);
        } catch (IOException exception) {
            return null;
        }
    }

    private Rectangle getBoardBounds() {
        return new Rectangle(BOARD_X, BOARD_Y, BOARD_WIDTH, BOARD_HEIGHT);
    }

    private BoardCell resolveBoardCell(int mouseX, int mouseY) {
        Rectangle boardBounds = getBoardBounds();
        if (!boardBounds.contains(mouseX, mouseY)) {
            return null;
        }

        int cellWidth = (boardBounds.width - BOARD_MARGIN * 2 - BOARD_GAP * (Board.COLUMNS - 1)) / Board.COLUMNS;
        int cellHeight = (boardBounds.height - BOARD_MARGIN * 2 - BOARD_GAP * (Board.ROWS - 1)) / Board.ROWS;

        for (int row = 0; row < Board.ROWS; row++) {
            for (int column = 0; column < Board.COLUMNS; column++) {
                int x = boardBounds.x + BOARD_MARGIN + column * (cellWidth + BOARD_GAP);
                int y = boardBounds.y + BOARD_MARGIN + row * (cellHeight + BOARD_GAP);
                if (new Rectangle(x, y, cellWidth, cellHeight).contains(mouseX, mouseY)) {
                    return new BoardCell(row, column);
                }
            }
        }
        return null;
    }

    private Player resolveReserveSelection(int mouseX, int mouseY) {
        for (ReservePieceHitBox hitBox : reserveHitBoxes) {
            if (hitBox.bounds.contains(mouseX, mouseY)) {
                return hitBox.player;
            }
        }
        return null;
    }

    private void handleBoardClick(int mouseX, int mouseY) {
        if (game.getCurrentTurn() != localPlayer) {
            playerMessage = game.getPlayerName(localPlayer) + ": aguarde a sua vez.";
            opponentMessage = game.getPlayerName(localPlayer.opponent()) + ": jogando agora.";
            repaint();
            return;
        }

        Player reservePlayer = resolveReserveSelection(mouseX, mouseY);
        if (reservePlayer != null) {
            if (game.getState() != dara.model.GameState.PLACING) {
                return;
            }
            if (reservePlayer != localPlayer) {
                playerMessage = game.getPlayerName(localPlayer) + ": voce controla apenas a sua propria cor.";
                opponentMessage = game.getPlayerName(localPlayer.opponent()) + ": aguardando sua jogada.";
                repaint();
                return;
            }
            if (reservePlayer != game.getCurrentTurn()) {
                playerMessage = game.getCurrentTurnName() + ": e a vez da sua cor jogar.";
                opponentMessage = game.getWaitingPlayerName() + ": aguardando sua vez.";
                repaint();
                return;
            }
            selectedReservePlayer = reservePlayer;
            selectedBoardCell = null;
            playerMessage = game.getCurrentTurnName() + ": peca selecionada. Clique em uma casa vazia.";
            repaint();
            return;
        }

        BoardCell cell = resolveBoardCell(mouseX, mouseY);
        if (cell == null) {
            return;
        }

        if (game.getState() == dara.model.GameState.PLACING) {
            handlePlacingClick(cell);
            return;
        }

        if (game.getState() == dara.model.GameState.MOVIMENTATION) {
            handleMovimentationClick(cell);
        }
    }

    private void handlePlacingClick(BoardCell cell) {
        if (selectedReservePlayer == null) {
            return;
        }

        if (!game.canPlacePiece(selectedReservePlayer, cell.row, cell.column)) {
            if (game.wouldCreateLine(selectedReservePlayer, cell.row, cell.column)) {
                playerMessage = game.getCurrentTurnName() + ": nao pode formar linha de 3 nesta fase.";
            } else {
                playerMessage = game.getCurrentTurnName() + ": escolha uma casa vazia valida.";
            }
            repaint();
            return;
        }

        game.placePiece(selectedReservePlayer, cell.row, cell.column);
        BoardCenter start = getReserveAnimationStart(selectedReservePlayer);
        startAnimation(selectedReservePlayer, start.x, start.y, cell.row, cell.column);
        gameActionSender.send(new GameAction(
                GameActionType.PLACE,
                toPlayerSlot(selectedReservePlayer),
                cell.row,
                cell.column,
                -1,
                -1,
                -1,
                -1
        ));
        selectedReservePlayer = null;
        selectedBoardCell = null;
        updateStatusMessages();
        repaint();
    }

    private void handleMovimentationClick(BoardCell cell) {
        Player pieceAtCell = game.getBoard().getPiece(cell.row, cell.column);

        if (game.isAwaitingRemoval()) {
            if (!game.canRemoveOpponentPiece(game.getCurrentTurn(), cell.row, cell.column)) {
                playerMessage = game.getCurrentTurnName() + ": selecione uma peca do adversario para remover.";
                repaint();
                return;
            }

            Player actingPlayer = game.getCurrentTurn();
            game.removeOpponentPiece(actingPlayer, cell.row, cell.column);
            gameActionSender.send(new GameAction(
                    GameActionType.REMOVE,
                    toPlayerSlot(actingPlayer),
                    cell.row,
                    cell.column,
                    -1,
                    -1,
                    -1,
                    -1
            ));
            selectedBoardCell = null;
            updateStatusMessages();
            repaint();
            return;
        }

        if (pieceAtCell == game.getCurrentTurn() && game.canSelectPiece(game.getCurrentTurn(), cell.row, cell.column)) {
            selectedBoardCell = cell;
            selectedReservePlayer = null;
            playerMessage = game.getCurrentTurnName() + ": peca selecionada. Escolha o destino.";
            repaint();
            return;
        }

        if (pieceAtCell == localPlayer.opponent()) {
            playerMessage = game.getPlayerName(localPlayer) + ": voce nao pode mover a cor do oponente.";
            opponentMessage = game.getPlayerName(localPlayer.opponent()) + ": aguardando sua jogada.";
            repaint();
            return;
        }

        if (pieceAtCell != null && pieceAtCell != game.getCurrentTurn()) {
            playerMessage = game.getCurrentTurnName() + ": selecione uma peca da sua propria cor.";
            opponentMessage = game.getWaitingPlayerName() + ": aguardando jogada do oponente.";
            repaint();
            return;
        }

        if (selectedBoardCell == null) {
            return;
        }

        if (!game.canMovePiece(game.getCurrentTurn(), selectedBoardCell.row, selectedBoardCell.column, cell.row, cell.column)) {
            playerMessage = game.getCurrentTurnName() + ": escolha um destino adjacente vazio.";
            repaint();
            return;
        }

        BoardCell fromCell = selectedBoardCell;
        Player actingPlayer = game.getCurrentTurn();
        game.movePiece(actingPlayer, fromCell.row, fromCell.column, cell.row, cell.column);
        BoardCenter start = getCellCenter(fromCell.row, fromCell.column);
        startAnimation(actingPlayer, start.x, start.y, cell.row, cell.column);
        gameActionSender.send(new GameAction(
                GameActionType.MOVE,
                toPlayerSlot(actingPlayer),
                -1,
                -1,
                fromCell.row,
                fromCell.column,
                cell.row,
                cell.column
        ));
        selectedBoardCell = null;
        updateStatusMessages();
        repaint();
    }

    private void updateStatusMessages() {
        if (game.getState() == dara.model.GameState.PLACING) {
            playerMessage = game.getCurrentTurnName() + ": selecione uma peca da reserva.";
            opponentMessage = game.getWaitingPlayerName() + ": aguardando jogada do oponente.";
            syncUiState();
            return;
        }

        if (game.getState() == dara.model.GameState.FINISHED) {
            playerMessage = game.getCurrentTurnName() + ": venceu a partida.";
            opponentMessage = game.getWaitingPlayerName() + ": partida encerrada.";
            syncUiState();
            return;
        }

        if (game.isAwaitingRemoval()) {
            playerMessage = game.getCurrentTurnName() + ": formou linha de 3. Remova uma peca adversaria.";
            opponentMessage = game.getWaitingPlayerName() + ": aguardando remocao.";
            syncUiState();
            return;
        }

        playerMessage = game.getCurrentTurnName() + ": selecione uma peca para mover.";
        opponentMessage = game.getWaitingPlayerName() + ": aguardando jogada do oponente.";
        syncUiState();
    }

    private void syncUiState() {
        boolean finished = game.getState() == dara.model.GameState.FINISHED;

        randomPhaseButton.setEnabled(false);
        randomPhaseButton.setVisible(false);
        surrenderButton.setEnabled(!finished);
        restartButton.setVisible(finished);
        restartButton.setEnabled(finished);
    }

    public void appendChatMessage(String senderName, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        StyledDocument document = chatTextPane.getStyledDocument();
        if (document.getLength() > 0) {
            appendStyledText(document, System.lineSeparator(), createMessageStyle(INK, false));
        }
        appendStyledText(document, senderName + ": ", createSenderStyle(senderName));
        appendStyledText(document, text, createMessageStyle(INK, false));
        chatTextPane.setCaretPosition(document.getLength());
        repaint();
    }

    public Game getGame() {
        return game;
    }

    public void refreshStatus() {
        updateStatusMessages();
    }

    private JTextPane createChatTextPane() {
        JTextPane pane = new JTextPane();
        pane.setEditable(false);
        pane.setOpaque(false);
        pane.setFocusable(false);
        return pane;
    }

    private JScrollPane createChatScrollPane(JTextPane pane) {
        JScrollPane scrollPane = new JScrollPane(pane);
        scrollPane.setBounds(CHAT_BOX_X + 96, CHAT_BOX_Y + 44, CHAT_BOX_WIDTH - 192, 88);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        return scrollPane;
    }

    private JTextField createChatInputField() {
        JTextField field = new JTextField();
        field.setBounds(CHAT_BOX_X + 96, CHAT_BOX_Y + 142, CHAT_BOX_WIDTH - 192, 30);
        field.setFont(new Font("Serif", Font.BOLD, 14));
        field.setForeground(INK);
        field.setBackground(new Color(245, 220, 180, 230));
        field.setCaretColor(INK);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(138, 92, 52), 2, true),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)
        ));
        field.setToolTipText("Envie uma mensagem para o outro jogador");
        field.addActionListener(_ -> submitChatMessage(field));
        return field;
    }

    private JButton createHelperButton(String text, int x, int y, int width, ActionListener listener) {
        JButton button = new JButton(text);
        button.setBounds(x, y, width, 34);
        button.setFont(new Font("Serif", Font.BOLD, 13));
        button.setFocusPainted(false);
        button.setForeground(INK);
        button.setBackground(new Color(232, 191, 140));
        button.addActionListener(listener);
        return button;
    }

    private SimpleAttributeSet createSenderStyle(String senderName) {
        Color color = CHAT_SYSTEM;
        if ("Voce".equalsIgnoreCase(senderName)) {
            color = CHAT_LOCAL;
        } else if ("Adversario".equalsIgnoreCase(senderName)) {
            color = CHAT_OPPONENT;
        }
        return createMessageStyle(color, true);
    }

    private SimpleAttributeSet createMessageStyle(Color color, boolean bold) {
        SimpleAttributeSet attributes = new SimpleAttributeSet();
        StyleConstants.setForeground(attributes, color);
        StyleConstants.setBold(attributes, bold);
        StyleConstants.setFontFamily(attributes, "Serif");
        StyleConstants.setFontSize(attributes, bold ? 16 : 15);
        return attributes;
    }

    private void appendStyledText(StyledDocument document, String text, SimpleAttributeSet attributes) {
        try {
            document.insertString(document.getLength(), text, attributes);
        } catch (BadLocationException exception) {
            throw new IllegalStateException("Nao foi possivel atualizar o chat.", exception);
        }
    }

    private void submitChatMessage(JTextField field) {
        String text = field.getText();
        if (text == null || text.isBlank()) {
            return;
        }

        String trimmedText = text.trim();
        appendChatMessage(game.getPlayerName(localPlayer), trimmedText);
        chatSender.send(trimmedText);
        field.setText("");
    }

    private void runRandomPhaseHelper() {
        try {
            game.randomizePlacingPhase();
            selectedReservePlayer = null;
            selectedBoardCell = null;
            randomPhaseUsed = true;
            updateStatusMessages();
            appendChatMessage("Sistema", "Helper: fase de colocacao preenchida aleatoriamente.");
            repaint();
        } catch (IllegalStateException exception) {
            appendChatMessage("Sistema", "Helper: nao foi possivel gerar a fase aleatoria.");
        }
    }

    private void surrenderMatch() {
        Player surrenderingPlayer = localPlayer;
        game.applyRemoteSurrender(surrenderingPlayer);
        gameActionSender.send(new GameAction(
                GameActionType.SURRENDER,
                toPlayerSlot(surrenderingPlayer),
                -1,
                -1,
                -1,
                -1,
                -1,
                -1
        ));
        updateStatusMessages();
        repaint();
    }

    private void startAnimation(Player player, int fromX, int fromY, int toRow, int toColumn) {
        BoardCenter target = getCellCenter(toRow, toColumn);
        activeAnimation = new PieceAnimation(player, fromX, fromY, target.x, target.y, toRow, toColumn, System.currentTimeMillis(), 220L);
        if (animationTimer != null) {
            animationTimer.stop();
        }
        animationTimer = new Timer(16, event -> {
            if (activeAnimation == null || activeAnimation.isFinished(System.currentTimeMillis())) {
                activeAnimation = null;
                ((Timer) event.getSource()).stop();
            }
            repaint();
        });
        animationTimer.start();
    }

    private BoardCenter getCellCenter(int row, int column) {
        int cellWidth = (BOARD_WIDTH - BOARD_MARGIN * 2 - BOARD_GAP * (Board.COLUMNS - 1)) / Board.COLUMNS;
        int cellHeight = (BOARD_HEIGHT - BOARD_MARGIN * 2 - BOARD_GAP * (Board.ROWS - 1)) / Board.ROWS;
        int x = BOARD_X + BOARD_MARGIN + column * (cellWidth + BOARD_GAP) + cellWidth / 2;
        int y = BOARD_Y + BOARD_MARGIN + row * (cellHeight + BOARD_GAP) + cellHeight / 2;
        return new BoardCenter(x, y);
    }

    private BoardCenter getReserveAnimationStart(Player player) {
        return player == Player.COLOR_ONE
                ? new BoardCenter(LEFT_RESERVE_PIECES_X, LEFT_RESERVE_PIECES_Y)
                : new BoardCenter(RIGHT_RESERVE_PIECES_X, RIGHT_RESERVE_PIECES_Y);
    }

    private PlayerSlot toPlayerSlot(Player player) {
        return PlayerSlot.fromPlayer(player);
    }

    private record BoardCell(int row, int column) {
    }

    private record BoardCenter(int x, int y) {
    }

    private record ReservePieceHitBox(Player player, Rectangle bounds) {
    }

    private record PieceAnimation(Player player, int fromX, int fromY, int toX, int toY, int targetRow,
                                  int targetColumn, long startedAt, long durationMs) {

        private double progress(long now) {
                double value = (double) (now - startedAt) / durationMs;
                return Math.max(0.0, Math.min(1.0, value));
            }

            private boolean isFinished(long now) {
                return now - startedAt >= durationMs;
            }
        }

    private final class BoardMouseHandler extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent event) {
            if (game.getState() == dara.model.GameState.PLACING
                    || game.getState() == dara.model.GameState.MOVIMENTATION) {
                handleBoardClick(event.getX(), event.getY());
            }
        }
    }

    private record ShapeFactory(int centerX, int centerY, int outerRadius, int innerRadius, int points) {

        private GeneralPath create() {
                GeneralPath path = new GeneralPath();
                double angleStep = Math.PI / points;

                for (int i = 0; i < points * 2; i++) {
                    double angle = -Math.PI / 2 + i * angleStep;
                    int radius = i % 2 == 0 ? outerRadius : innerRadius;
                    double px = centerX + Math.cos(angle) * radius;
                    double py = centerY + Math.sin(angle) * radius;

                    if (i == 0) {
                        path.moveTo(px, py);
                    } else {
                        path.lineTo(px, py);
                    }
                }

                path.closePath();
                return path;
            }
        }
}
