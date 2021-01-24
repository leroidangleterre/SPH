
import java.awt.Color;
import java.awt.Graphics;

/**
 * The Food is eaten by particles when they walk on it, and consumed when the
 * particle needs to move.
 *
 * @author arthu
 */
public class Food {

    // Coordinates
    private double x, y;
    private double radius;
    private Color color;

    public Food(double newX, double newY) {
        x = newX;
        y = newY;
        color = Color.green;
        radius = 0.25;
    }

    public void display(Graphics g, double x0, double y0, double zoom, int panelHeight) {

        // Coordinates are expressed in the referential of the panel.
        int xApp = (int) (x * zoom + x0);
        int yApp = (int) (panelHeight - (y * zoom + y0));
        int apparentRadius = (int) (this.radius * zoom);
        g.setColor(this.color);
        g.fillOval(xApp - apparentRadius, yApp - apparentRadius, 2 * apparentRadius, 2 * apparentRadius);
    }
}
