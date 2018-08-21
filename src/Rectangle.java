
/*
 * This class describes a rectangular object that collides with the particles.
 * Initially, it will be non-moving.
* The rectangle may be rotated.
* The width is defined as the larger side.
 */
import java.awt.Color;
import java.awt.Graphics;

public class Rectangle {

    private double xCenter, yCenter;
    private double width, height; // width >= height
    private double angle;
    private double ratio; // Height divided by width.
    private double elasticity;
    private double repulsion;

    // The four points that represent the rectangle.
    Vecteur a, b, c, d, center;

    public Rectangle(double x, double y, double l, double h) {
        this.xCenter = x;
        this.yCenter = y;
        this.width = Math.max(l, h);
        this.height = Math.min(l, h);
        if (h > l) {
            this.setAngle(Math.PI / 2);
        } else {
            this.setAngle(0);
        }
        this.elasticity = 0.00;
        this.repulsion = 0.5;
    }

    public Rectangle(double x, double y, double l, double h, double elasticity) {
        this(x, y, l, h);
        this.elasticity = elasticity;
    }

    public Rectangle(double x, double y, double l, double h, double elasticity, double angle) {
        this(x, y, l, h, elasticity);
        this.setAngle(angle);
    }

    public void rotate(double dAngle) {
        this.setAngle(angle + dAngle);
    }

    public void setAngle(double angle) {
        this.angle = angle;
        this.computeCoordinates();
    }

    // Compute the coordinates of the rotated summits of the rectangle.
    private void computeCoordinates() {
        double co = Math.cos(angle);
        double si = Math.sin(angle);

        // Middle points of the segments of the rectangle.
        Vecteur m = new Vecteur(0 + 0.5 * width * co, 0 + 0.5 * width * si);
        Vecteur n = new Vecteur(0 + -0.5 * height * si, 0 + 0.5 * height * co);
        Vecteur p = new Vecteur(0 + -0.5 * width * co, 0 + -0.5 * width * si);
        Vecteur q = new Vecteur(0 + 0.5 * height * si, 0 + -0.5 * height * co);

        center = new Vecteur(xCenter, yCenter);

        a = m.sum(n).sum(center);
        b = n.sum(p).sum(center);
        c = p.sum(q).sum(center);
        d = q.sum(m).sum(center);
    }

    public void display(Graphics g, double x0, double y0, double zoom, int hauteurPanneau) {

        // Points of the rectangle are given in the following order:
        // Top right, top left, bottom left, botton right.
        int tabX[] = {(int) (a.getX() * zoom + x0),
            (int) (b.getX() * zoom + x0),
            (int) (c.getX() * zoom + x0),
            (int) (d.getX() * zoom + x0)};
        int tabY[] = {(int) (hauteurPanneau - (a.getY() * zoom + y0)),
            (int) (hauteurPanneau - (b.getY() * zoom + y0)),
            (int) (hauteurPanneau - (c.getY() * zoom + y0)),
            (int) (hauteurPanneau - (d.getY() * zoom + y0))};

        g.setColor(Color.gray);

        g.fillPolygon(tabX, tabY, 4);
    }

    public void displayBorders(Graphics g, double x0, double y0, double zoom, int hauteurPanneau) {

        // Points of the rectangle are given in the following order:
        // Top right, top left, bottom left, botton right.
        int tabX[] = {(int) (a.getX() * zoom + x0),
            (int) (b.getX() * zoom + x0),
            (int) (c.getX() * zoom + x0),
            (int) (d.getX() * zoom + x0)};
        int tabY[] = {(int) (hauteurPanneau - (a.getY() * zoom + y0)),
            (int) (hauteurPanneau - (b.getY() * zoom + y0)),
            (int) (hauteurPanneau - (c.getY() * zoom + y0)),
            (int) (hauteurPanneau - (d.getY() * zoom + y0))};

        g.setColor(Color.black);

        g.drawPolygon(tabX, tabY, 4);
    }

    /**
     * Is the point of coordinates (x, y) contained in this rectangle ?
     *
     * @param x
     * @param y
     * @return
     */
    public boolean containsPoint(double x, double y) {

        // Convert the coordinates into the ref linked to the rectangle.
        double co = Math.cos(-angle);
        double si = Math.sin(-angle);

        double xTrans = x - xCenter;
        double yTrans = y - yCenter;

        double xConv = xTrans * co - yTrans * si;
        double yConv = xTrans * si + yTrans * co;

        return xConv > -width / 2 && xConv < width / 2 && yConv > -height / 2 && yConv < height / 2;
    }

    public void actOnParticle(Particle p) {

        if (this.containsPoint(p.getX(), p.getY())) {
//            p.setFlagCollidingWithRectangle(true);
//        } else {
//            p.setFlagCollidingWithRectangle(false);
            // Convert the coordinates of the particle into the ref linked to the rectangle.
            double co = Math.cos(-angle);
            double si = Math.sin(-angle);

            double xTrans = p.getX() - xCenter;
            double yTrans = p.getY() - yCenter;

            // Coordinates of the particle in the referential linked to the rectangle.
            double xLoc = xTrans * co - yTrans * si;
            double yLoc = xTrans * si + yTrans * co;

            // Convert the speed into the ref linked to the rectangle.
            double vxConv = p.getVx() * co - p.getVy() * si;
            double vyConv = p.getVx() * si + p.getVy() * co;

            if (yLoc <= xLoc + height / 2 - width / 2 && yLoc >= -xLoc - height / 2 + width / 2) {
//                System.out.println("bounce east");
                if (vxConv < 0) {
                    // Bounce on the east face:
                    vxConv = -vxConv;
                } else {
                    // Push the particle away from the east face:
                    vxConv += repulsion;
                }
                xLoc = width / 2;
            } else if (yLoc <= -xLoc + height / 2 - width / 2 && yLoc >= xLoc - height / 2 + width / 2) {
//                System.out.println("bounce west");
                if (vxConv > 0) {
                    // Bounce on the west face:
                    vxConv = -vxConv;
                } else {
                    // Push the particle away from the west face:
                    vxConv -= repulsion;
                }
                xLoc = -width / 2;
            } else if (yLoc >= 0) {
//                System.out.println("bounce north");
                if (vyConv < 0) {
                    // Bounce on the north face:
                    vyConv = -vyConv;
                } else {
                    // Push the particle away from the north face:
                    vyConv += repulsion;
                }
                yLoc = height / 2;
            } else {
//                System.out.println("bounce south");
                if (vyConv > 0) {
                    // Bounce on the south face:
                    vyConv = -vyConv;
                } else {
                    // Push the particle away from the south face:
                    vyConv -= repulsion;
                }
                yLoc = -height / 2;
            }

            // Convert the position and speed back into the initial referential:
            p.setX(xLoc * co + yLoc * si + xCenter);
            p.setY(-xLoc * si + yLoc * co + yCenter);
            p.setVx(vxConv * co + vyConv * si);
            p.setVy(-vxConv * si + vyConv * co);
        }
    }
}
