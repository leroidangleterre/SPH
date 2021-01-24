
/*
 * This class describes a rectangular object that collides with the particles.
 * Initially, it will be non-moving.
* The rectangle may be rotated.
* The width is defined as the larger side.
 */
import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;

public class Rectangle {

    private double xCenter, yCenter;
    private double width, height; // width >= height
    private double angle;
    private double ratio; // Height divided by width.
    private double elasticity;
    private double repulsion;

    // The four points that represent the rectangle.
    private Vecteur a, b, c, d, center;

    // The points that make the border of the rectangle.
    private ArrayList<Particle> border;

    private boolean isSelected;

    public Rectangle(double x, double y, double width, double height) {
        this.xCenter = x;
        this.yCenter = y;
        this.width = Math.max(width, height);
        this.height = Math.min(width, height);
        if (height > width) {
            this.setAngle(Math.PI / 2);
        } else {
            this.setAngle(0);
        }
        this.elasticity = 0.00;
        this.repulsion = 0.5;

        this.isSelected = false;

//        // Creation of the border.
//        this.border = new ArrayList<>();
//        double rP = 0.5;
//        for(int i = 0; i * rP <= width; i++){
//            double xP = i * rP;
//            System.out.println("i = " + i + ", xP = " + xP);
//            double yP = height;
//            Particle pUp = new Particle(xCenter + xP - width / 2, yCenter + yP - height / 2, rP, -1, -1);
//            pUp.setMovementAllowed(false);
//            this.border.add(pUp);
//            Particle pDown = new Particle(xCenter + xP - width / 2, yCenter + 0 - height / 2, rP, -1, -1);
//            pDown.setMovementAllowed(false);
//            this.border.add(pDown);
//        }
//        for(int i = 0; (i + 1) * rP <= height; i++){
//            double xP = width;
//            double yP = (i + 1) * rP;
//            Particle pRight = new Particle(xCenter + xP - width / 2, yCenter + yP - height / 2, rP, -1, -1);
//            pRight.setMovementAllowed(false);
//            this.border.add(pRight);
//            Particle pLeft = new Particle(xCenter + 0 - width / 2, yCenter + yP - height / 2, rP, -1, -1);
//            pLeft.setMovementAllowed(false);
//            this.border.add(pLeft);
//        }
    }

    public Rectangle(double x, double y, double l, double h, double elasticity) {
        this(x, y, l, h);
        this.elasticity = elasticity;
    }

    public Rectangle(double x, double y, double l, double h, double elasticity, double angle) {
        this(x, y, l, h, elasticity);
        this.setAngle(angle);
    }

    public void move(double dx, double dy) {
        System.out.println("Moving rectangle " + this);
        this.xCenter += dx;
        this.yCenter += dy;
        this.computeCoordinates();
    }

    public void rotate(double dAngle) {
        this.setAngle(angle + dAngle);
    }

    public void setAngle(double angle) {
        this.angle = angle;
        this.computeCoordinates();
    }

    public double getAngle() {
        return this.angle;
    }

    public double getX() {
        return this.xCenter;
    }

    public double getY() {
        return this.yCenter;
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

        if (this.isSelected) {
            g.setColor(Color.red);
        } else {
            g.setColor(Color.black);
        }

        g.drawPolygon(tabX, tabY, 4);

//        for(Particle p : this.border){
//            p.display(g, x0, y0, zoom, hauteurPanneau);
//        }
    }

    /**
     * Is the point of coordinates (x, y) contained in this rectangle ?
     *
     * @param x
     * @param y
     * @return
     */
    public boolean containsPoint(double x, double y) {
        return this.containsPoint(x, y, 1.0);
    }

    /**
     * Is the point of coordinates (x, y) contained in this rectangle, with the
     * specified margin ?
     *
     * @param x x-coordinate of the point
     * @param y y-coordinate of the point
     * @param factor the amount by which we multiply the size of the rectangle
     * before testing for the point inclusion
     * @return
     */
    public boolean containsPoint(double x, double y, double factor) {

        // Convert the coordinates into the ref linked to the rectangle.
        double co = Math.cos(-angle);
        double si = Math.sin(-angle);

        double xTrans = x - xCenter;
        double yTrans = y - yCenter;

        double xConv = xTrans * co - yTrans * si;
        double yConv = xTrans * si + yTrans * co;

        return xConv > -width * factor / 2 && xConv < width * factor / 2 && yConv > -height * factor / 2 && yConv < height * factor / 2;
    }

    /**
     * Is the point of coordinates (x, y) close enough to maybe collide with
     * this rectangle ?
     *
     * @param x
     * @param y
     * @return
     */
    public boolean isCloseToPoint(double x, double y) {
        double factor = 1.5;
        return this.containsPoint(x, y, factor);
    }

    /**
     * When a particle gets close enough to the rectangle (about twice its
     * smoothing radius), a virtual particle is created by the rectangle, by
     * symmetry on the closest faces; that ghost particle acts only on the
     * colliding one.
     *
     * @param p
     */
    public void actOnParticle(Particle p) {

//        System.out.println("Rectangle actOnParticle()");
        if (this.containsPoint(p.getX(), p.getY())) {
//            p.setFlagCollidingWithRectangle(true);
//        } else if (isCloseToPoint(p.getX(), p.getY())) {

            p.setFlagCollidingWithRectangle(false);
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

    /**
     * Get the particles that make the border of this rectangle.
     *
     * @return the list of particles
     */
    public ArrayList<Particle> getParticleList() {
        return this.border;
    }

    public boolean isSelected() {
        return isSelected;
    }

    /**
     * Select the particle if and only if its center is located in the specified
     * rectangle.
     */
    public void select(double xG, double xD, double yB, double yH) {

        this.isSelected = (this.getX() > xG && this.getX() < xD && this.getY() > yB && this.getY() < yH);
        System.out.println("Rectangle " + this.toString()
                + (isSelected ? "" : " not") + "selected."
        );
    }
}
