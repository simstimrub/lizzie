package wagner.stephanie.lizzie.gui;

import wagner.stephanie.lizzie.analysis.MoveData;
import wagner.stephanie.lizzie.rules.Board;
import wagner.stephanie.lizzie.Lizzie;
import wagner.stephanie.lizzie.rules.Stone;

import java.awt.*;
import java.util.List;

public class BoardRenderer {
    private static final double MARGIN = 0.03; // percentage of the boardWidth to offset before drawing black lines
    private static final double STAR_POINT_WIDTH = 0.015;

    private int x, y;
    private int boardWidth;

    /**
     * Calculates the widths and pixel margins from a given boardWidth.
     * Precondition: the boardWidth must be boardWidth >= BOARD_SIZE - 1
     *
     * @return an array containing the three outputs: newWidth, scaledMargin, availableWidth
     */
    public static int[] calculatePixelMargins(int size) {
        if (size < Board.BOARD_SIZE - 1)
            throw new IllegalArgumentException("boardWidth may not be less than " + (Board.BOARD_SIZE - 1) + ", but was " + size);

        int scaledMargin;
        int availableWidth;

        // decrease boardWidth until the availableWidth will result in square board intersections
        size++;
        do {
            size--;
            scaledMargin = (int) (MARGIN * size);
            availableWidth = size - 2 * scaledMargin;
        }
        while (!((availableWidth - 1) % (Board.BOARD_SIZE - 1) == 0));
        // this will be true if BOARD_SIZE - 1 square intersections plus one line will fit

        return new int[]{size, scaledMargin, availableWidth};
    }

    /**
     * Generates a random stone arrangement -- not necessarily a legal one
     *
     * @return a random stone array
     */
    public static Stone[] getRandomStones() {
        Stone[] stones = new Stone[Board.BOARD_SIZE * Board.BOARD_SIZE];
        for (int i = 0; i < stones.length; i++) {
            stones[i] = Math.random() > 0.5 ? Stone.EMPTY : Math.random() > 0.5 ? Stone.BLACK : Stone.WHITE;
        }
        return stones;
    }

    /**
     * Draw a go board
     *
     * @param g0 graphics instance
     */
    public void draw(Graphics g0) {
        Graphics2D g = (Graphics2D) g0;
        int scaledMargin; // the pixel boardWidth of the margins
        int availableWidth; // the pixel boardWidth of the game board without margins

        // calculate a good set of boardWidth, scaledMargin, and availableWidth to use
        int[] calculatedPixelMargins = calculatePixelMargins();
        boardWidth = calculatedPixelMargins[0];
        scaledMargin = calculatedPixelMargins[1];
        availableWidth = calculatedPixelMargins[2];

        // draw the wooden background
        g.setColor(Color.ORANGE.darker());
        g.fillRect(x, y, boardWidth, boardWidth);

        // draw the lines
        g.setColor(Color.BLACK);
        int squareSize = calculateSquareSize(availableWidth);
        for (int i = 0; i < Board.BOARD_SIZE; i++) {
            g.drawLine(x + scaledMargin, y + scaledMargin + squareSize * i,
                    x + scaledMargin + availableWidth - 1, y + scaledMargin + squareSize * i);
        }
        for (int i = 0; i < Board.BOARD_SIZE; i++) {
            g.drawLine(x + scaledMargin + squareSize * i, y + scaledMargin,
                    x + scaledMargin + squareSize * i, y + scaledMargin + availableWidth - 1);
        }

        // draw the star points
        int starPointRadius = (int) (STAR_POINT_WIDTH * boardWidth) / 2;

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                int centerX = x + scaledMargin + squareSize * (3 + 6 * i) - starPointRadius;
                int centerY = y + scaledMargin + squareSize * (3 + 6 * j) - starPointRadius;
                g.fillOval(centerX, centerY, 2 * starPointRadius, 2 * starPointRadius);
            }
        }

        // draw the stones
        int stoneRadius = squareSize / 2 - 1;
        if (Lizzie.board != null) {
            for (int i = 0; i < Board.BOARD_SIZE; i++) {
                for (int j = 0; j < Board.BOARD_SIZE; j++) {
                    // TODO for some reason these are always off center?!
                    int stoneX = x + scaledMargin + squareSize * i - stoneRadius;
                    int stoneY = y + scaledMargin + squareSize * j - stoneRadius;

                    switch (Lizzie.board.getStones()[Board.getIndex(i, j)]) {
                        case EMPTY:
                            break;
                        case BLACK:
                            g.setColor(Color.BLACK);
                            g.fillOval(stoneX, stoneY, stoneRadius * 2 + 1, stoneRadius * 2 + 1);
                            break;
                        case WHITE:
                            g.setColor(Color.WHITE);
                            g.fillOval(stoneX, stoneY, stoneRadius * 2 + 1, stoneRadius * 2 + 1);
                            g.setColor(Color.BLACK);
                            g.drawOval(stoneX, stoneY, stoneRadius * 2 + 1, stoneRadius * 2 + 1);
                            break;
                        default:
                    }


                }
            }

            // mark the last coordinate
            int[] lastMove = Lizzie.board.getLastMove();
            if (lastMove != null) {
                int circleRadius = squareSize / 4;

                int stoneX = x + scaledMargin + squareSize * lastMove[0] - circleRadius;
                int stoneY = y + scaledMargin + squareSize * lastMove[1] - circleRadius;

                // set color to the opposite color of whatever is on the board
                g.setColor(Lizzie.board.getStones()[Board.getIndex(lastMove[0], lastMove[1])] == Stone.WHITE ?
                        Color.BLACK : Color.WHITE);
                g.drawOval(stoneX, stoneY, circleRadius * 2 + 1, circleRadius * 2 + 1);
            }
        }

        // draw Leelaz suggestions
        // TODO clean up this MESS
        List<MoveData> bestMoves = Lizzie.leelaz.getBestMoves();
        if (!bestMoves.isEmpty()) {
            final double MIN_ACCEPTABLE_PLAYOUTS = 0.0;

            int maxPlayouts = 0;
            for (MoveData move : bestMoves) {
                if (move.playouts < MIN_ACCEPTABLE_PLAYOUTS)
                    continue;
                if (move.playouts > maxPlayouts)
                    maxPlayouts = move.playouts;
            }

            final int MIN_ALPHA = 32;
            final int MAX_ALPHA = 240;

            for (int i = 0; i < bestMoves.size(); i++) {
                MoveData move = bestMoves.get(i);
                double percentPlayouts = (Math.max(0, (double) move.playouts) / Math.max(1, maxPlayouts));
                if (percentPlayouts < MIN_ACCEPTABLE_PLAYOUTS) {
                    continue;
                }
                int[] coordinates = Board.convertNameToCoordinates(move.coordinate);
                int suggestionX = x + scaledMargin + squareSize * coordinates[0] - stoneRadius;
                int suggestionY = y + scaledMargin + squareSize * coordinates[1] - stoneRadius;

                // -0.32 = Greenest, 0 = Reddest
                float hue = (float) (-0.32 * Math.max(0, Math.log(percentPlayouts) / 3 + 1));
                float saturation = 0.75f; //saturation
                float brightness = 0.85f; //brightness
                int alpha = (int) (MIN_ALPHA + (MAX_ALPHA - MIN_ALPHA) * Math.max(0, Math.log(percentPlayouts) / 5 + 1));

                Color hsbColor = Color.getHSBColor(hue, saturation, brightness);
                Color color = new Color(hsbColor.getRed(), hsbColor.getBlue(), hsbColor.getGreen(), alpha);

                g.setColor(color);
                g.fillOval(suggestionX, suggestionY, stoneRadius * 2 + 1, stoneRadius * 2 + 1);
                int strokeWidth = 0;
                // highlight the top recommended move
                if (i == 0) {
                    strokeWidth = 2;
                    g.setColor(Color.ORANGE);
                    g.setStroke(new BasicStroke(strokeWidth));
                } else {
                    g.setColor(color.darker());
                }
                g.drawOval(suggestionX + strokeWidth / 2, suggestionY + strokeWidth / 2, stoneRadius * 2 + 1 - strokeWidth, stoneRadius * 2 + 1 - strokeWidth);
                g.setStroke(new BasicStroke(1));

                if (alpha > 64) {
                    g.setColor(Color.BLACK);
                    Font font = new Font("Sans Serif", Font.BOLD, (int) (stoneRadius * 0.85));
                    g.setFont(font);
                    String winrateString = String.format("%.0f", move.winrate) + "%";
                    g.drawString(winrateString, suggestionX + stoneRadius - g.getFontMetrics(font).stringWidth(winrateString) / 2, suggestionY + stoneRadius);
                    String playouts;
                    int fontSize = (int) (stoneRadius * 0.8);
                    do {
                        font = new Font("Sans Serif", Font.PLAIN, fontSize--);
                        g.setFont(font);
                        playouts = "" + move.playouts;
                    } while (g.getFontMetrics(font).stringWidth(playouts) > stoneRadius * 1.7);

                    g.drawString("" + move.playouts, suggestionX + stoneRadius - g.getFontMetrics(font).stringWidth(playouts) / 2, suggestionY + stoneRadius + font.getSize());
                }
            }
        }
    }

    private int[] calculatePixelMargins() {
        return calculatePixelMargins(boardWidth);
    }

    /**
     * Set the location to render the board
     *
     * @param x x coordinate
     * @param y y coordinate
     */
    public void setLocation(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Set the maximum boardWidth to render the board
     *
     * @param boardWidth the boardWidth of the board
     */
    public void setBoardWidth(int boardWidth) {
        this.boardWidth = boardWidth;
    }

    /**
     * Converts a location on the screen to a location on the board
     *
     * @param x x pixel coordinate
     * @param y y pixel coordinate
     * @return if there is a valid coordinate, an array (x, y) where x and y are between 0 and BOARD_SIZE - 1. Otherwise, returns null
     */
    public int[] convertScreenToCoordinates(int x, int y) {
        int marginWidth; // the pixel width of the margins
        int boardWidthWithoutMargins; // the pixel width of the game board without margins

        // calculate a good set of boardWidth, scaledMargin, and boardWidthWithoutMargins to use
        int[] calculatedPixelMargins = calculatePixelMargins();
        boardWidth = calculatedPixelMargins[0];
        marginWidth = calculatedPixelMargins[1];
        boardWidthWithoutMargins = calculatedPixelMargins[2];

        int squareSize = calculateSquareSize(boardWidthWithoutMargins);

        // transform the pixel coordinates to board coordinates
        x = (x - this.x - marginWidth + squareSize / 2) / squareSize;
        y = (y - this.y - marginWidth + squareSize / 2) / squareSize;

        // return these values if they are valid board coordinates
        if (Board.isValid(x, y))
            return new int[]{x, y};
        else
            return null;
    }

    /**
     * Calculate the boardWidth of each intersection square
     *
     * @param availableWidth the pixel boardWidth of the game board without margins
     * @return the boardWidth of each intersection square
     */
    public int calculateSquareSize(int availableWidth) {
        return availableWidth / (Board.BOARD_SIZE - 1);
    }
}