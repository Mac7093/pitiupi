package pitiupi.batalha_naval.GUI;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.BiConsumer;

import javax.swing.JPanel;

import pitiupi.batalha_naval.control.Board;
import pitiupi.batalha_naval.control.CellState;

public class GradePanel extends JPanel{
    @FunctionalInterface
    public interface CellProvider {
        CellState stateAt(int row, int col);
    }

    private static final Color GRID_LINE = new Color(60, 90, 120);

    private final CellProvider provider;
    private final int cellSize;

    public GradePanel(CellProvider provider, int cellSize, BiConsumer<Integer, Integer> onClick) {
        this.provider = provider;
        this.cellSize = cellSize;
        int side = Board.DIMENSION * cellSize + 1;
        setPreferredSize(new Dimension(side, side));

        if (onClick != null) {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    int row = e.getY() / GradePanel.this.cellSize;
                    int col = e.getX() / GradePanel.this.cellSize;
                    if (Board.isInside(row, col)) {
                        onClick.accept(row, col);
                    }
                }
            });
        }
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (int row = 0; row < Board.DIMENSION; row++) {
            for (int col = 0; col < Board.DIMENSION; col++) {
                int x = col * cellSize;
                int y = row * cellSize;
                CellState state = provider.stateAt(row, col);

                g.setColor(colorFor(state));
                g.fillRect(x, y, cellSize, cellSize);
                g.setColor(GRID_LINE);
                g.drawRect(x, y, cellSize, cellSize);

                drawMark(g, state, x, y);
            }
        }
        g.dispose();
    }

    private void drawMark(Graphics2D g, CellState state, int x, int y) {
        int margin = cellSize / 4;
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2f));
        switch (state) {
            case HIT, SUNK -> {
                g.drawLine(x + margin, y + margin, x + cellSize - margin, y + cellSize - margin);
                g.drawLine(x + cellSize - margin, y + margin, x + margin, y + cellSize - margin);
            }
            default -> {
            }
        }
    }

    private static Color colorFor(CellState state) {
        return switch (state) {
            case UNKNOWN -> new Color(173, 216, 230);
            case SHIP -> new Color(90, 90, 90);
            case MISS -> new Color(100, 149, 237);
            case HIT -> new Color(230, 60, 60);
            case SUNK -> new Color(120, 0, 0);
        };
    }
}
