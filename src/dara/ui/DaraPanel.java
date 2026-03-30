package dara.ui;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.github.weisj.jsvg.view.ViewBox;
import dara.model.Board;
import dara.model.Game;
import dara.model.Player;
import dara.network.PlayerSlot;
import dara.protocol.GameAction;
import dara.protocol.GameActionType;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.Timer;
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
    public interface ChatSender {
        void send(PlayerSlot slot, String text);
    }

    public interface GameActionSender {
        void send(GameAction action);
    }

    private static final int PANEL_WIDTH = 1080;
    private static final int PANEL_HEIGHT = 768;

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

    private static final int TOP_BANNER_MARGIN = 10;
    private static final int TOP_BANNER_X = 280;
    private static final double SVG_SCALE = 0.75;
    private static final int BOARD_X = 301;
    private static final int BOARD_Y = 192;
    private static final int BOARD_WIDTH = 477;
    private static final int BOARD_HEIGHT = 376;
    private static final int BOARD_MARGIN = 16;
    private static final int BOARD_GAP = 8;
    private static final int CHAT_X = 246;
    private static final int CHAT_Y = 567;
    private static final int CHAT_WIDTH = 589;
    private static final int CHAT_HEIGHT = 201;
    private static final int SIDE_CHAT_WIDTH = 180;
    private static final int LEFT_CHAT_X = 18;
    private static final int LEFT_CHAT_Y = 92;
    private static final int RIGHT_CHAT_Y = 448;

    private static final SVGDocument TOP_BANNER_SVG = loadSvg("/dara/ui/images/old_paper_scroll_set.svg");
    private static final SVGDocument CHAT_BOX_SVG = loadSvg("/dara/ui/images/ChatBox.svg");
    private static final Image BACKGROUND_IMAGE = loadImage("/dara/ui/images/fundo_areia.png");

    private final List<ReservePieceHitBox> reserveHitBoxes;
    private final ChatSender chatSender;
    private final GameActionSender gameActionSender;
    private final Runnable restartToLobbyAction;
    private final Game game;
    private final JButton randomPhaseButton;
    private final JButton surrenderButton;
    private final JButton restartButton;
    private final JTextField playerOneChatField;
    private final JTextField playerTwoChatField;
    private final JComponent playerOneChatIcon;
    private final JComponent playerTwoChatIcon;
    private final JTextArea chatTextArea;
    private final JScrollPane chatScrollPane;
    private String playerMessage;
    private String opponentMessage;
    private Player selectedReservePlayer;
    private BoardCell selectedBoardCell;
    private boolean randomPhaseUsed;
    private PieceAnimation activeAnimation;
    private Timer animationTimer;

    public DaraPanel(Game game, ChatSender chatSender, GameActionSender gameActionSender, Runnable restartToLobbyAction) {
        this.game = game;
        this.chatSender = chatSender;
        this.gameActionSender = gameActionSender;
        this.restartToLobbyAction = restartToLobbyAction;
        this.reserveHitBoxes = new ArrayList<>();

        setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        setBackground(SAND_LIGHT);
        setLayout(null);

        randomPhaseButton = createHelperButton("Auto Fase", TOP_BANNER_X + 384, TOP_BANNER_MARGIN + 24, 96, event -> runRandomPhaseHelper());
        surrenderButton = createHelperButton("Desistir", TOP_BANNER_X + 384, TOP_BANNER_MARGIN + 64, 96, event -> surrenderMatch());
        restartButton = createHelperButton("Novo Jogo", BOARD_X + 168, BOARD_Y + 214, 140, event -> restartToLobbyAction.run());
        restartButton.setVisible(false);

        playerTwoChatIcon = createMessageIcon(LEFT_CHAT_X, LEFT_CHAT_Y);
        playerTwoChatField = createSideChatField(LEFT_CHAT_X + 30, LEFT_CHAT_Y, PlayerSlot.PLAYER_2);
        playerOneChatIcon = createMessageIcon(864, RIGHT_CHAT_Y);
        playerOneChatField = createSideChatField(894, RIGHT_CHAT_Y, PlayerSlot.PLAYER_1);

        chatTextArea = createChatTextArea();
        chatScrollPane = createChatScrollPane(chatTextArea);

        add(randomPhaseButton);
        add(surrenderButton);
        add(restartButton);
        add(playerTwoChatIcon);
        add(playerTwoChatField);
        add(playerOneChatIcon);
        add(playerOneChatField);
        add(chatScrollPane);
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
        Rectangle bounds = drawSvgCentered(g2, TOP_BANNER_SVG, TOP_BANNER_MARGIN);

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
                    int pieceSize = Math.min(cellWidth, cellHeight) - 28;
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
        g2.setPaint(new GradientPaint(0, 130, new Color(90, 141, 55), 216, 130, new Color(116, 157, 73)));
        g2.fillRoundRect(-34, 130, 250, 68, 32, 32);
        g2.setColor(INK);
        g2.setFont(new Font("Serif", Font.BOLD, 30));
        g2.drawString(game.getPlayerTwoName(), 48, 170);
        drawReservePieces(g2, 92, 228, game.getReserveCount(Player.COLOR_ONE), Player.COLOR_ONE);
    }

    private void drawRightReserve(Graphics2D g2) {
        g2.setPaint(new GradientPaint(830, 494, new Color(207, 145, 88), 1075, 494, new Color(196, 123, 61)));
        g2.fillRoundRect(830, 494, 268, 66, 34, 34);
        g2.setColor(INK);
        g2.setFont(new Font("Serif", Font.BOLD, 30));
        g2.drawString(game.getPlayerOneName(), 866, 536);
        drawReservePieces(g2, 856, 178, game.getReserveCount(Player.COLOR_TWO), Player.COLOR_TWO);
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
        drawSvgBottomCentered(g2, CHAT_BOX_SVG);
    }

    private void drawPiece(Graphics2D g2, int centerX, int centerY, int radius, Player player) {
        Color primary = player == Player.COLOR_ONE ? PLAYER_ONE : PLAYER_TWO;
        Color shadow = player == Player.COLOR_ONE ? PLAYER_ONE_SHADOW : PLAYER_TWO_SHADOW;

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.24f));
        g2.setColor(new Color(22, 14, 8));
        g2.fillOval(centerX - radius - 5, centerY - radius + 8, radius * 2 + 10, radius * 2 + 12);
        g2.setComposite(AlphaComposite.SrcOver);

        g2.setColor(new Color(247, 233, 204));
        g2.fill(new Ellipse2D.Double(centerX - radius - 6, centerY - radius - 6, (radius + 6) * 2.0, (radius + 6) * 2.0));

        ShapeFactory starOuter = new ShapeFactory(centerX, centerY, radius + 2, radius - 8, 10);
        ShapeFactory starInner = new ShapeFactory(centerX, centerY, radius - 2, radius - 12, 10);

        g2.setColor(shadow);
        g2.fill(starOuter.create());
        g2.setColor(primary);
        g2.fill(starInner.create());

        g2.setColor(new Color(252, 240, 213, 210));
        g2.fill(new Ellipse2D.Double(centerX - 4, centerY - 4, 8, 8));

        g2.setColor(new Color(255, 246, 220, 180));
        Polygon highlight = new Polygon();
        highlight.addPoint(centerX - 4, centerY - radius + 5);
        highlight.addPoint(centerX + 6, centerY - radius + 12);
        highlight.addPoint(centerX + 1, centerY - radius / 3);
        g2.fillPolygon(highlight);
    }

    private void drawCenteredText(Graphics2D g2, String text, Rectangle area) {
        int textWidth = g2.getFontMetrics().stringWidth(text);
        int ascent = g2.getFontMetrics().getAscent();
        int drawX = area.x + (area.width - textWidth) / 2;
        int drawY = area.y + (area.height + ascent) / 2 - 6;
        g2.drawString(text, drawX, drawY);
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
        drawCenteredText(g2, game.getCurrentTurnName() + " e o vencedor", new Rectangle(BOARD_X - 40, BOARD_Y + 116, BOARD_WIDTH + 80, 40));
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

    private Rectangle drawSvgCentered(Graphics2D g2, SVGDocument document, int topMargin) {
        int width = (int) Math.round(document.size().width * SVG_SCALE);
        int height = (int) Math.round(document.size().height * SVG_SCALE);
        int x = (getWidth() - width) / 2;
        int y = topMargin;
        renderSvg(g2, document, x, y, width, height);
        return new Rectangle(x, y, width, height);
    }

    private Rectangle drawSvgBottomCentered(Graphics2D g2, SVGDocument document) {
        int width = (int) Math.round(document.size().width * SVG_SCALE);
        int height = (int) Math.round(document.size().height * SVG_SCALE);
        int x = (getWidth() - width) / 2;
        int y = getHeight() - height;
        renderSvg(g2, document, x, y, width, height);
        return new Rectangle(x, y, width, height);
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

    private static Image loadImage(String resourcePath) {
        URL resource = DaraPanel.class.getResource(resourcePath);
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
        Player reservePlayer = resolveReserveSelection(mouseX, mouseY);
        if (reservePlayer != null) {
            if (game.getState() != dara.model.GameState.PLACING) {
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
        boolean allowRandomPhase = game.getState() == dara.model.GameState.PLACING && !randomPhaseUsed;

        randomPhaseButton.setEnabled(allowRandomPhase);
        surrenderButton.setEnabled(!finished);
        restartButton.setVisible(finished);
        restartButton.setEnabled(finished);
    }

    public void appendChatMessage(String senderName, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        if (!chatTextArea.getText().isBlank()) {
            chatTextArea.append(System.lineSeparator());
        }
        chatTextArea.append(senderName + ": " + text);
        chatTextArea.setCaretPosition(chatTextArea.getDocument().getLength());
        repaint();
    }

    public Game getGame() {
        return game;
    }

    public void refreshStatus() {
        updateStatusMessages();
    }

    private JTextArea createChatTextArea() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font("Serif", Font.BOLD, 16));
        area.setForeground(INK);
        area.setOpaque(false);
        area.setFocusable(false);
        return area;
    }

    private JScrollPane createChatScrollPane(JTextArea area) {
        JScrollPane scrollPane = new JScrollPane(area);
        scrollPane.setBounds(CHAT_X + 96, CHAT_Y + 52, CHAT_WIDTH - 192, 122);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        return scrollPane;
    }

    private JTextField createSideChatField(int x, int y, PlayerSlot slot) {
        JTextField field = new JTextField();
        field.setBounds(x, y, SIDE_CHAT_WIDTH, 34);
        field.setFont(new Font("Serif", Font.BOLD, 14));
        field.setForeground(INK);
        field.setBackground(new Color(245, 220, 180, 230));
        field.setCaretColor(INK);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(138, 92, 52), 2, true),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)
        ));
        field.addActionListener(event -> submitChatMessage(slot, field));
        return field;
    }

    private JComponent createMessageIcon(int x, int y) {
        JComponent icon = new JComponent() {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g2 = (Graphics2D) graphics.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(114, 77, 42));
                g2.fillRoundRect(2, 2, 22, 18, 8, 8);
                Polygon tail = new Polygon();
                tail.addPoint(9, 19);
                tail.addPoint(12, 26);
                tail.addPoint(16, 19);
                g2.fillPolygon(tail);
                g2.setColor(new Color(245, 220, 180));
                g2.fillOval(7, 8, 3, 3);
                g2.fillOval(12, 8, 3, 3);
                g2.fillOval(17, 8, 3, 3);
                g2.dispose();
            }
        };
        icon.setBounds(x, y + 4, 28, 28);
        return icon;
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

    private void submitChatMessage(PlayerSlot slot, JTextField field) {
        String text = field.getText();
        if (text == null || text.isBlank()) {
            return;
        }

        chatSender.send(slot, text.trim());
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
        Player surrenderingPlayer = game.getCurrentTurn();
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
        return player == Player.COLOR_ONE ? new BoardCenter(92, 228) : new BoardCenter(856, 178);
    }

    private PlayerSlot toPlayerSlot(Player player) {
        return player == Player.COLOR_TWO ? PlayerSlot.PLAYER_1 : PlayerSlot.PLAYER_2;
    }

    private static final class BoardCell {
        private final int row;
        private final int column;

        private BoardCell(int row, int column) {
            this.row = row;
            this.column = column;
        }
    }

    private static final class BoardCenter {
        private final int x;
        private final int y;

        private BoardCenter(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    private static final class ReservePieceHitBox {
        private final Player player;
        private final Rectangle bounds;

        private ReservePieceHitBox(Player player, Rectangle bounds) {
            this.player = player;
            this.bounds = bounds;
        }
    }

    private static final class PieceAnimation {
        private final Player player;
        private final int fromX;
        private final int fromY;
        private final int toX;
        private final int toY;
        private final int targetRow;
        private final int targetColumn;
        private final long startedAt;
        private final long durationMs;

        private PieceAnimation(Player player, int fromX, int fromY, int toX, int toY, int targetRow, int targetColumn, long startedAt, long durationMs) {
            this.player = player;
            this.fromX = fromX;
            this.fromY = fromY;
            this.toX = toX;
            this.toY = toY;
            this.targetRow = targetRow;
            this.targetColumn = targetColumn;
            this.startedAt = startedAt;
            this.durationMs = durationMs;
        }

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

    private static final class ShapeFactory {
        private final int centerX;
        private final int centerY;
        private final int outerRadius;
        private final int innerRadius;
        private final int points;

        private ShapeFactory(int centerX, int centerY, int outerRadius, int innerRadius, int points) {
            this.centerX = centerX;
            this.centerY = centerY;
            this.outerRadius = outerRadius;
            this.innerRadius = innerRadius;
            this.points = points;
        }

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
