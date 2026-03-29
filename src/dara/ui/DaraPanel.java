package dara.ui;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.github.weisj.jsvg.view.ViewBox;
import dara.model.Board;
import dara.model.Game;
import dara.model.Player;

import javax.swing.JPanel;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class DaraPanel extends JPanel {
    private static final int PANEL_WIDTH = 1080;
    private static final int PANEL_HEIGHT = 768;

    private static final Color SAND_LIGHT = new Color(240, 197, 142);
    private static final Color SAND_MID = new Color(220, 170, 112);
    private static final Color SAND_DARK = new Color(180, 128, 76);
    private static final Color BOARD_FRAME = new Color(145, 118, 79);
    private static final Color CELL_LIGHT = new Color(248, 229, 188);
    private static final Color CELL_DARK = new Color(228, 184, 124);
    private static final Color PLAYER_ONE = new Color(42, 107, 36);
    private static final Color PLAYER_ONE_SHADOW = new Color(20, 61, 17);
    private static final Color PLAYER_TWO = new Color(178, 92, 28);
    private static final Color PLAYER_TWO_SHADOW = new Color(122, 54, 17);
    private static final Color INK = new Color(33, 22, 12);
    private static final int TOP_BANNER_MARGIN = 10;
    private static final double SVG_SCALE = 0.75;
    private static final int BOARD_X = 301;
    private static final int BOARD_Y = 166;
    private static final int BOARD_WIDTH = 477;
    private static final int BOARD_HEIGHT = 376;
    private static final int BOARD_MARGIN = 16;
    private static final int BOARD_GAP = 8;

    private static final SVGDocument TOP_BANNER_SVG = loadSvg("/dara/ui/images/old_paper_scroll_set.svg");
    private static final SVGDocument CHAT_BOX_SVG = loadSvg("/dara/ui/images/ChatBox.svg");

    private final List<ReservePieceHitBox> reserveHitBoxes;
    private final Game game;
    private String playerMessage;
    private String opponentMessage;
    private Player selectedReservePlayer;
    private BoardCell selectedBoardCell;

    public DaraPanel(Game game) {
        this.game = game;
        this.reserveHitBoxes = new ArrayList<>();
        updateStatusMessages();

        setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        setBackground(SAND_LIGHT);
        addMouseListener(new BoardMouseHandler());
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
        drawStatusLines(g2);
        drawBoard(g2);
        drawLeftReserve(g2);
        drawRightReserve(g2);
        drawChatBox(g2);

        g2.dispose();
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
        int boardX = BOARD_X;
        int boardY = BOARD_Y;
        int boardWidth = BOARD_WIDTH;
        int boardHeight = BOARD_HEIGHT;

        g2.setPaint(new GradientPaint(boardX, boardY, new Color(253, 240, 213), boardX, boardY + boardHeight, new Color(235, 205, 159)));
        g2.fillRoundRect(boardX, boardY, boardWidth, boardHeight, 8, 8);
        g2.setColor(BOARD_FRAME);
        g2.setStroke(new BasicStroke(3f));
        g2.drawRoundRect(boardX, boardY, boardWidth, boardHeight, 8, 8);

        int margin = BOARD_MARGIN;
        int gap = BOARD_GAP;
        int cellWidth = (boardWidth - margin * 2 - gap * (Board.COLUMNS - 1)) / Board.COLUMNS;
        int cellHeight = (boardHeight - margin * 2 - gap * (Board.ROWS - 1)) / Board.ROWS;

        for (int row = 0; row < Board.ROWS; row++) {
            for (int column = 0; column < Board.COLUMNS; column++) {
                int x = boardX + margin + column * (cellWidth + gap);
                int y = boardY + margin + row * (cellHeight + gap);
                drawCell(g2, x, y, cellWidth, cellHeight, row, column);

                Player piece = game.getBoard().getPiece(row, column);
                if (piece != null) {
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
        g2.setColor(new Color(74, 48, 24));
        g2.setFont(new Font("Serif", Font.BOLD, 18));
        drawCenteredText(g2, playerMessage, new Rectangle(BOARD_X - 80, 122, BOARD_WIDTH + 160, 22));
        drawCenteredText(g2, opponentMessage, new Rectangle(BOARD_X - 80, 144, BOARD_WIDTH + 160, 22));
    }

    private String getDisplayStateText() {
        return switch (game.getState()) {
            case PLACING -> "Fase de Colocacao";
            case MOVIMENTATION -> "Fase de Movimentacao";
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

    private Rectangle getBoardBounds() {
        return new Rectangle(BOARD_X, BOARD_Y, BOARD_WIDTH, BOARD_HEIGHT);
    }

    private BoardCell resolveBoardCell(int mouseX, int mouseY) {
        Rectangle boardBounds = getBoardBounds();
        if (!boardBounds.contains(mouseX, mouseY)) {
            return null;
        }

        int margin = BOARD_MARGIN;
        int gap = BOARD_GAP;
        int cellWidth = (boardBounds.width - margin * 2 - gap * (Board.COLUMNS - 1)) / Board.COLUMNS;
        int cellHeight = (boardBounds.height - margin * 2 - gap * (Board.ROWS - 1)) / Board.ROWS;

        for (int row = 0; row < Board.ROWS; row++) {
            for (int column = 0; column < Board.COLUMNS; column++) {
                int x = boardBounds.x + margin + column * (cellWidth + gap);
                int y = boardBounds.y + margin + row * (cellHeight + gap);
                Rectangle cellBounds = new Rectangle(x, y, cellWidth, cellHeight);
                if (cellBounds.contains(mouseX, mouseY)) {
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

            game.removeOpponentPiece(game.getCurrentTurn(), cell.row, cell.column);
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

        game.movePiece(game.getCurrentTurn(), selectedBoardCell.row, selectedBoardCell.column, cell.row, cell.column);
        selectedBoardCell = null;
        updateStatusMessages();
        repaint();
    }

    private void updateStatusMessages() {
        if (game.getState() == dara.model.GameState.PLACING) {
            playerMessage = game.getCurrentTurnName() + ": selecione uma peca da reserva.";
            opponentMessage = game.getWaitingPlayerName() + ": aguardando jogada do oponente.";
            return;
        }

        if (game.getState() == dara.model.GameState.FINISHED) {
            playerMessage = game.getCurrentTurnName() + ": venceu a partida.";
            opponentMessage = game.getWaitingPlayerName() + ": ficou com apenas 2 pecas.";
            return;
        }

        if (game.isAwaitingRemoval()) {
            playerMessage = game.getCurrentTurnName() + ": formou linha de 3. Remova uma peca adversaria.";
            opponentMessage = game.getWaitingPlayerName() + ": aguardando remocao.";
            return;
        }

        playerMessage = game.getCurrentTurnName() + ": selecione uma peca para mover.";
        opponentMessage = game.getWaitingPlayerName() + ": aguardando jogada do oponente.";
    }

    private static final class BoardCell {
        private final int row;
        private final int column;

        private BoardCell(int row, int column) {
            this.row = row;
            this.column = column;
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
