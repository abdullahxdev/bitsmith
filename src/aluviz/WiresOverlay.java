package aluviz;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Transparent overlay that draws permanent dim wires connecting the three main
 * panels, and an animated value-blob along the active wire during execution.
 *
 * Sits on top of the DataFlowPanel's component layer (via JLayeredPane).
 */
public class WiresOverlay extends JPanel {

    private static final Color WIRE_IDLE   = new Color(0xCFD8DC);
    private static final Color WIRE_ACTIVE = new Color(0xFFC107);
    private static final Color BLOB_FILL   = new Color(0xFFC107);
    private static final Color BLOB_BORDER = new Color(0xF57F17);
    private static final Color BLOB_TEXT   = new Color(0x3E2723);

    private final List<Point[]> staticWires = new ArrayList<>();
    private WireAnimation current;

    public WiresOverlay() {
        setOpaque(false);
    }

    public void setStaticWires(List<Point[]> wires) {
        this.staticWires.clear();
        this.staticWires.addAll(wires);
        repaint();
    }

    public void setActive(WireAnimation anim) {
        this.current = anim;
        repaint();
    }

    public void clearActive() {
        this.current = null;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Static wires (always faint)
        g.setStroke(new BasicStroke(2f));
        g.setColor(WIRE_IDLE);
        for (Point[] w : staticWires) {
            drawConnector(g, w[0], w[1]);
        }

        // Active wire — drawn highlighted
        if (current != null) {
            g.setStroke(new BasicStroke(3f));
            g.setColor(WIRE_ACTIVE);
            drawConnector(g, current.src, current.dst);

            // Blob
            Point pos = current.currentPos();
            String text = current.label;
            Font f = new Font(Font.MONOSPACED, Font.BOLD, 12);
            g.setFont(f);
            FontMetrics fm = g.getFontMetrics();
            int textW = fm.stringWidth(text);
            int blobW = textW + 18;
            int blobH = 22;
            int x = pos.x - blobW / 2;
            int y = pos.y - blobH / 2;

            g.setColor(BLOB_FILL);
            g.fillRoundRect(x, y, blobW, blobH, blobH, blobH);
            g.setColor(BLOB_BORDER);
            g.setStroke(new BasicStroke(1.6f));
            g.drawRoundRect(x, y, blobW, blobH, blobH, blobH);
            g.setColor(BLOB_TEXT);
            g.drawString(text, x + 9, y + 16);
        }
    }

    /** Draw a straight connector from a to b. */
    private void drawConnector(Graphics2D g, Point a, Point b) {
        g.drawLine(a.x, a.y, b.x, b.y);
    }
}
