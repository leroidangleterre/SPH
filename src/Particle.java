
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Random;

public class Particle {

    // Either rigid bodies or SPH.
    public static boolean rigidCollision = false;

    public static double defaultRadius = 0.5;

    private Vecteur position;
    private boolean movementAllowed; // True for most particles, false for particles that make a rectangle.
    private Vecteur speed, vAvg; // Current and average speed
    private ArrayList<Vecteur> speedList; // Last known speeds
    private ArrayList<Vecteur> positionList; // Last known positions
    private int nbPrevPos;
    private boolean selected;
    private double radius, radiusMin, radiusMax;
    private double viscosity = 0.1;
    private double mass, density, pressure;
    private double pressureCst, refDensity; // Pressure is computed as pressureCst * (density - refDensity);
    private Vecteur force; // The force applied on this particle by others
    private int numLine, numColumn; // Position in the grid. //TODO: this info may have to exist only in the terrain.
    private int red, green, blue, alpha; // Values used to compute the color
    private Color color;
    private ArrayList<PointCouleur> coloredPointList;

    private static int nbParticlesCreated = 0;
    private int serialNumber;

    private int nMoy; // The average speed is computed using that many steps.

    private int requestedNeighbors; // The size of this particle must adapt to have approximately that many neighbors.
    private int nbNeighbors; // The amount of neighbors that this particle currently interacts with.

    private double dRadius;

    private boolean isCollidingWithRectangle;

    private ArrayList<Particle> neighborList;

    public Particle(double xParam, double yParam, double rayonParam, int numLigneParam, int numColonneParam) {
        this.position = new Vecteur(xParam, yParam);
//        System.out.println("New particle at (" + xParam + ", " + yParam + ";)");
        this.speed = new Vecteur();
        this.vAvg = new Vecteur();
        this.force = new Vecteur();
        // if (Math.random() < 0.5){
        this.radius = rayonParam;
        // }else{
        // this.radius = 2 * rayonParam;
        // }
        this.dRadius = 1.1;
        this.radiusMax = 1 * radius;
        this.radiusMin = 1 * radius;
        this.numLine = numLigneParam;
        this.numColumn = numColonneParam;
        this.mass = 0.02;// 1.0
        this.pressureCst = 10000;
        this.refDensity = 1.0;
        Random r = new Random();
        this.alpha = 50;
        this.color = new Color(r.nextInt(206) + 50, r.nextInt(206) + 50, r.nextInt(206) + 50, this.alpha);
        this.serialNumber = Particle.nbParticlesCreated;
        Particle.nbParticlesCreated++;
        this.speedList = new ArrayList<>();
        this.nMoy = 10;
        for (int i = 0; i < nMoy; i++) {
            this.speedList.add(new Vecteur());
        }
        this.positionList = new ArrayList<>();
        this.nbPrevPos = 0;
        for (int i = 0; i < nbPrevPos; i++) {
            this.positionList.add(new Vecteur(this.position));
        }
        this.selected = false;
        this.setColorScale();
        this.requestedNeighbors = 2;

        this.isCollidingWithRectangle = false;
        this.movementAllowed = true;

        this.neighborList = new ArrayList<>();
    }

    public Particle(double xParam, double yParam, double rayonParam, double densiteParam, int numLigneParam, int numColonneParam) {
        this(xParam, yParam, rayonParam, numLigneParam, numColonneParam);
        // The mass is set so that the density is correct.
        this.mass = this.radius * this.radius * densiteParam;
    }

    public Particle clone() {
        return new Particle(this.position.getX(), this.position.getY(), this.radius, this.density, this.numLine, this.numColumn);
    }

    public void setColor(Color c) {
        this.color = c.darker();
    }

    public void setSelected(boolean param) {
        this.selected = param;
    }

    public boolean isSelected() {
        return this.selected;
    }

    public double getX() {
        return this.position.getX();
    }

    public double getY() {
        return this.position.getY();
    }

    public void setX(double x) {
        this.position.setX(x);
    }

    public void setY(double y) {
        this.position.setY(y);
    }

    public double getVx() {
        return this.speed.getX();
    }

    public double getVy() {
        return this.speed.getY();
    }

    public void setVx(double vx) {
        this.speed.setX(vx);
//        System.out.println("particle " + this + " setting vX to " + this.speed.getX());
    }

    public void setVy(double vy) {
        this.speed.setY(vy);
    }

    public void setSpeed(Vecteur v) {
        this.speed = v.clone();
    }

    public void increaseVx(double dvx) {
        this.speed.setX(this.getVx() + dvx);
    }

    public void increaseVy(double dvy) {
        this.speed.setY(this.getVy() + dvy);
    }

    public double getKineticEnergy() { // Ec=(1/2)m*v2
        double v = this.speed.norme();
        double ke = 0.5 * this.mass * v * v;
        return ke;
    }

    public double getPotentialEnergy(double g) { // Ec=m*g*y
        double pE = this.mass * g * this.position.getY();
        return pE;
    }

    public int getLineNum() {
        return this.numLine;
    }

    public int getColomnNum() {
        return this.numColumn;
    }

    public void setLineNum(int n) {
        this.numLine = n;
    }

    public void setColumnNum(int n) {
        this.numColumn = n;
    }

    public void setNbNeighbors(int currentNb) {
        this.nbNeighbors = currentNb;
    }

    public int getNbNeighbors() {
        return this.nbNeighbors;
    }

    public void incrementNbNeighbors() {
        this.nbNeighbors++;
    }

    /**
     * Adapt the radius so that the amount of neighbors gets closer to the
     * requested amount.
     */
    public void updateRadius() {
        double fact = 1.05;
        if (this.radius < this.radiusMax) {
            if (this.nbNeighbors < this.requestedNeighbors / fact) {
                this.radius = this.radius * fact;
                this.mass *= fact * fact;
            }
        }
        if (this.radius > this.radiusMin) {
            if (this.nbNeighbors > this.requestedNeighbors * fact) {
                this.radius = this.radius / fact;
                this.mass /= fact * fact;
            }
        }
        this.density = this.mass / (this.radius * this.radius);
    }

    /**
     * Take into account another particle for the computation of the density.
     */
    public void increaseDensity(Particle p) {
        double distance = this.getDistance(p);
        double k = Kernel.w(distance, this.radius);
        this.density = this.density + p.mass * k;
//        System.out.println("p.mass: " + p.mass);
        if (distance < this.radius + p.radius) {
            this.addNeighbor(p);
        }
    }

    public double getDensity() {
        return this.density;
    }

    /**
     * Compute the pressure.
     */
    public void computePressure() {
        this.pressure = this.pressureCst * (this.density - this.refDensity);
    }

    /**
     * Get the distance to another particle.
     */
    public double getDistance(Particle p) {
        double dx = p.getX() - this.getX();
        double dy = p.getY() - this.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Squared distance to another particle.
     */
    public double distanceAuCarre(Particle p) {
        return (this.getX() - p.getX()) * (this.getX() - p.getX()) + (this.getY() - p.getY()) * (this.getY() - p.getY());
    }

    /**
     * Select the particle if and only if it is located in the specified
     * rectangle.
     */
    public void select(double yH, double yB, double xG, double xD) {
        this.selected = (this.getX() > xG && this.getX() < xD && this.getY() > yB && this.getY() < yH);
    }

    public void resetDensityAndForces() {
        this.density = 0;
        this.force = new Vecteur();
        this.resetNeighbors();
    }

    private void resetNeighbors() {
        neighborList.clear();
    }

    private void addNeighbor(Particle p) {
        if (!neighborList.contains(p)) {
            neighborList.add(p);
        }
    }

    /**
     * Graphically display the speed.
     */
    public void displaySpeed(Graphics g, double x0, double y0, double zoom, int panelHeight, double k, String vInst) {
        // The Graphics2d is used to display bold lines.
        Graphics2D g2 = (Graphics2D) g;

        // Coordinates are expressed in the referential of the panel.
        int xApp = (int) (this.getX() * zoom + x0);
        int yApp = (int) (panelHeight - (this.getY() * zoom + y0));

        double vx, vy;

        int xAppEnd;
        int yAppEnd;

        if (vInst != "_") {
            if (vInst == "vInst") {
                vx = this.speed.getX();
                vy = this.speed.getY();
            } else if (vInst == "vMoy") {
                this.computeAverageSpeed();
                vx = this.vAvg.getX();
                vy = this.vAvg.getY();
            } else {
                vx = 0;
                vy = 0;
            }

            g2.setColor(Color.blue);
            if (this.selected) {
                g2.setStroke(new BasicStroke(3));
            }
            xAppEnd = (int) (xApp + k * zoom * vx);
            yAppEnd = (int) (yApp - k * zoom * vy);
            g2.drawLine(xApp, yApp, xAppEnd, yAppEnd);
            // Reset the stroke to a normal (not bold) one.
            g2.setStroke(new BasicStroke(1));
        }
    }

    public void display(Graphics g, double x0, double y0, double zoom, int panelHeight) {

        // Coordinates are expressed in the referential of the panel.
        int xApp = (int) (this.getX() * zoom + x0);
        int yApp = (int) (panelHeight - (this.getY() * zoom + y0));
        int apparentRadius = (int) (this.radius * zoom);

        // The disk that represents the particle.
        this.computeColor();
        g.setColor(this.color);
        g.fillOval(xApp - apparentRadius, yApp - apparentRadius, 2 * apparentRadius, 2 * apparentRadius);

        // Color for the central dot and the border
        if (this.selected) {
            g.setColor(Color.blue);
        } else {
            g.setColor(this.color);
        }
        g.fillOval(xApp - apparentRadius / 10, yApp - apparentRadius / 10, apparentRadius / 5, apparentRadius / 5); // central dot
        // Border
        g.setColor(new Color(0, 0, 0, 128));
        g.drawOval(xApp - apparentRadius, yApp - apparentRadius, 2 * apparentRadius, 2 * apparentRadius); // border

        // g.setColor(Color.BLACK);
//        g.drawString(this.nbNeighbors + "", xApp + apparentRadius, yApp);
//        g.drawString(this.radius + "", xApp + apparentRadius, yApp + apparentRadius);
        this.displayPreviousPositions(g, x0, y0, zoom, panelHeight);
//        this.displayForce(g, x0, y0, zoom, panelHeight);
    }

    public void displayNeighbors(Graphics g, double x0, double y0, double zoom, int panelHeight) {
//        System.out.println("Painting " + neighborList.size() + " neighbors");
        // Coordinates are expressed in the referential of the panel.
        int xApp0 = (int) (this.getX() * zoom + x0);
        int yApp0 = (int) (panelHeight - (this.getY() * zoom + y0));
        int xApp1, yApp1;

        for (Particle p : this.neighborList) {
            xApp1 = (int) (p.getX() * zoom + x0);
            yApp1 = (int) (panelHeight - (p.getY() * zoom + y0));
            g.drawLine(xApp0, yApp0, xApp1, yApp1);
        }
    }

    public void displayForce(Graphics g, double x0, double y0, double zoom, int panelHeight) {

        double scale = 0.0002 * zoom;

        int xApp = (int) (this.getX() * zoom + x0);
        int yApp = (int) (panelHeight - (this.getY() * zoom + y0));
        int dxApp = (int) (scale * this.force.getX());
        int dyApp = (int) (scale * this.force.getY());
        g.setColor(Color.ORANGE);
        g.drawLine(xApp, yApp, xApp + dxApp, yApp - dyApp);
    }

    public void displayForces(Graphics g, double x0, double y0, double zoom, int panelHeight, double k) {

        // The Graphics2d is used to display bold lines.
        Graphics2D g2 = (Graphics2D) g;

        // Coordinates are expressed in the referential of the panel.
        int xApp = (int) (this.getX() * zoom + x0);
        int yApp = (int) (panelHeight - (this.getY() * zoom + y0));
        int xAppEnd, yAppEnd;

        double fx = this.force.getX();
        double fy = this.force.getY();

        g2.setColor(Color.red);
        if (this.selected) {
            g2.setStroke(new BasicStroke(3));
        }
        xAppEnd = (int) (xApp + k * zoom * fx);
        yAppEnd = (int) (yApp - k * zoom * fy);
        g2.drawLine(xApp, yApp, xAppEnd, yAppEnd);
        // Reset the stroke to a normal (not bold) one.
        g2.setStroke(new BasicStroke(1));
    }

    public void displayPreviousPositions(Graphics g, double x0, double y0, double zoom, int panelHeight) {

        g.setColor(Color.BLACK);
        int apparentRadius = 1;
        for (Vecteur v : this.positionList) {
            int xApp = (int) (v.getX() * zoom + x0);
            int yApp = (int) (panelHeight - (v.getY() * zoom + y0));
            g.fillOval(xApp - apparentRadius, yApp - apparentRadius, 2 * apparentRadius + 1, 2 * apparentRadius + 1);
        }
    }

    public String toString() {
        return "p{ n°" + this.serialNumber + "\n   x=" + this.getX() + ",\n   y=" + this.getY() + ",\n   densite=" + this.density + "\n   vx="
                + this.speed.getX() + "\n   vy=" + this.speed.getY() + "}";
    }

    /**
     * Compute the speed with the forces applied on the particle.
     */
    public void computeSpeed(double dt, double gravite) {

        this.speed.setY(this.speed.getY() + gravite * dt);
        this.speed = this.speed.sum(new Vecteur((dt / this.mass) * this.force.getX(), (dt / this.mass) * this.force.getY()));
    }

    public void resetSpeed() {
        this.speed = new Vecteur();
    }

    /**
     * Move the particle with respect to its current speed. Only a free-moving
     * particle will actually move; a non-allowed one will not.
     */
    public void move(double dt) {

        if (this.isMovementAllowed()) {
            if (this.position.getX() == Double.NaN || this.position.getY() == Double.NaN) {
                System.out.println("NaN particle");
            }

            this.position = new Vecteur(this.position.getX() + this.speed.getX() * dt, this.position.getY() + this.speed.getY() * dt);
            // this.dampenSpeed(0.9999);
//            System.out.println("particle " + this + " moving, vx = " + this + speed.getX());

            // Update the list of the previous speeds.
            this.speedList.remove(0);
            this.speedList.add(this.speed.clone());

            // // Update the list of the previous positions.
            if (this.nbPrevPos > 0) {
                this.positionList.remove(0);
                this.positionList.add(this.position.clone());
            }
        }
    }

    /**
     * Arbitrarily move the particle (independently from its actual speed).
     */
    public void move(double dx, double dy) {
        this.position = this.position.sum(new Vecteur(dx, dy));
    }

    private void computeAverageSpeed() {
        double vx = 0;
        double vy = 0;
        Vecteur v;
        for (int i = 0; i < this.nMoy; i++) {
            v = this.speedList.get(i);
            vx = vx + v.getX();
            vy = vy + v.getY();
        }
        if (this.vAvg == null) {
            System.out.println("null tototototototo");
        }
        this.vAvg.setX(vx / this.nMoy);
        this.vAvg.setY(vy / this.nMoy);
    }

    /**
     * Dampen the speed of the particle.
     */
    public void dampenSpeed(double f) {
        this.speed = new Vecteur(f * this.speed.getX(), f * this.speed.getY());
    }

    /**
     * Dampen the speed of the particle, with a specific factor on each axis.
     */
    public void dampenSpeed(double fX, double fY) {
        this.speed = new Vecteur(fX * this.speed.getX(), fY * this.speed.getY());
    }

    /**
     * Apply an external force on the particle.
     */
    public void receiveExternalForce(Vecteur f1) {
        this.force = this.force.sum(f1);
        // System.out.println("Force: " + this.force);
    }

    /**
     * Apply on this particle a force that comes from another particle.
     */
    public void receiveForces(Particle p) {
        this.receivePressureForces(p);
        this.receiveViscosityForces(p);

        double k = Kernel.w(this.getDistance(p), this.radius);
        if (k > 0) {
            if (this.serialNumber < p.serialNumber) {
                this.incrementNbNeighbors();
                p.incrementNbNeighbors();
            }
        }
        // System.out.println("Force: " + this.force);
    }

    /**
     * Receive a pressure force. This method modifies the values on the other
     * particle as well.
     */
    private void receivePressureForces(Particle p) {

        Vecteur diff = p.position.diff(this.position);

        Vecteur uab = p.position.diff(this.position).normer();
        double overlap = Math.max(0, (this.radius + p.radius) - p.getDistance(this));
//        System.out.println("overlap 1: " + overlap);

        if (diff.estNul()) {
            // Special case, arbitrary random modification.
            Random random = new Random();
            this.force = this.force.sum(new Vecteur(0 /* r.nextFloat()-0.5 */, random.nextDouble() - 0.5));
        } else if (Particle.rigidCollision && overlap > 0) {

//            System.out.println("overlap: " + overlap);
            // RIGID COLLISIONS MODEL
            /*
             * The referential is constructed as follow:
             * Point A is the center of this sphere, point B is the center of parameter 'p';
             * Point O is the contact point, middle of [AB];
             * Unit vector uy starts at O in the direction of A;
             * Unit vector ux starts at O and is such that (ux, uy) has the value +Pi/4;
             */
            Vecteur a = this.position;
            Vecteur b = p.position;
            Vecteur o = a.sum(b).mult(0.5);

            Vecteur uy = a.diff(o).normer();
            Vecteur ux = new Vecteur(uy.getY(), -uy.getX());

            Vecteur vA = new Vecteur(this.getVx(), this.getVy());
            Vecteur vB = new Vecteur(p.getVx(), p.getVy());
            Vecteur vO = vA.sum(vB).mult(0.5);

            // Converting to O-centered referential:
            Vecteur vAo = vA.diff(vO);
            // Converting to O-centered referential:
            Vecteur vBo = vB.diff(vO);

            double relativeSpeed = vA.dot(uy);
            if (relativeSpeed < 0) {
                // On collision, the ux-component of both speeds is conserved, the uy-component is inverted.

                // Computing collision - computing x- and y-components of vAo (i.e. the speed of A relative to O):
                Vecteur vAoX = ux.mult(vAo.dot(ux));
                Vecteur vAoY = uy.mult(vAo.dot(uy));
                Vecteur vAoAfter = vAoX.diff(vAoY);
                // Converting back to the global referential:
                this.setSpeed(vAoAfter.sum(vO));

                // Computing collision - computing x- and y-components of vAo (i.e. the speed of A relative to O):
                Vecteur vBoX = ux.mult(vBo.dot(ux));
                Vecteur vBoY = uy.mult(vBo.dot(uy));
                Vecteur vBoAfter = vBoX.diff(vBoY);
                // Converting back to the global referential:
                p.setSpeed(vBoAfter.sum(vO));

            } else {
                // Particles are going away from each other, or at rest.
                // If the particles are already moving away, the resulting force is less intense or even zero.
                // Vector uy is aligned with the two particles.
                double df = overlap;
                Vecteur repulsionForce = uy.mult(df);
                this.speed.sum(repulsionForce);
                p.speed.diff(repulsionForce);
            }
        } else {

            // SPH-inspired MODEL
            Vecteur dF = uab.clone();
            double value;
            double threshold = (this.radius + p.radius) / 4;
            if (overlap > threshold) {
                value = -overlap;
            } else if (overlap > 0) {
                value = overlap * (-overlap / threshold);
            } else {
                value = 0;
            }

            dF = dF.mult(value * 15);
            this.force = this.force.sum(dF);
            p.force = p.force.diff(dF);
        }
    }

    private void receiveViscosityForces(Particle p) {

        double overlap = Math.max(0, (this.radius + p.radius) - p.getDistance(this));
//        System.out.println("overlap: " + overlap + ", rad sum: " + (this.radius + p.radius));
        if (overlap > 0) {

            // Velocity of particle 1 (this) in the referential of particle 2 (parameter):
            Vecteur v12 = this.speed.diff(p.speed);
            // Force applied by particle 2 on particle 1:

            Vecteur dF21 = this.speed.diff(p.speed).mult(-this.viscosity);

            // We apply the viscosity to both particles. TODO: need to apply it only once.
//            System.out.println("viscosity force: " + dF21.norme());
            this.force = this.force.sum(dF21);
            p.force = p.force.diff(dF21);
        }
    }

    /**
     * Compute the color from the density. We use the color scale and
     * interpolate its values.
     */
    private void computeColor() {

        this.red = 0;
        this.green = 0;
        this.blue = 0;

        // Use a parameter of the particle to compute its color.
        // Version 1: density
        for (int i = 0; i < this.coloredPointList.size(); i++) {
            this.incrementColor(this.coloredPointList.get(i).getColor(this.density));
        }

        this.applyColorCoefficientThreshold();

        this.color = new Color(this.red, this.green, this.blue, this.alpha);

//        if (this.isCollidingWithRectangle) {
//            this.color = Color.red;
//        } else {
//            this.color = Color.blue;
//        }
    }

    /**
     * No color coefficient should excess 255 or be negative.
     */
    private void applyColorCoefficientThreshold() {

        this.red = Math.min(255, Math.max(0, this.red));
        this.green = Math.min(255, Math.max(0, this.green));
        this.blue = Math.min(255, Math.max(0, this.blue));
    }

    /**
     * Add another color to the previously computed one.
     */
    public void incrementColor(int rp, int gp, int bp) {
        this.red = this.red + rp;
        this.green = this.green + gp;
        this.blue = this.blue + bp;
    }

    /**
     * Add another color to the previously computed one.
     */
    private void incrementColor(Color c) {
        this.red += c.getRed();
        this.blue += c.getBlue();
        this.green += c.getGreen();
    }

    /**
     * Determine whether the particle contains a given point.
     */
    public boolean containsPoint(Vecteur p) {
        return this.position.diff(p).norme() < this.radius;
    }

    public void increaseRadius() {
        this.radius *= dRadius;
        this.mass *= dRadius * dRadius;
        System.out.println("r+");
    }

    public void decreaseRadius() {
        this.radius *= 1 / dRadius;
        this.mass *= 1 / (dRadius * dRadius);
    }

    public int getSerialNumber() {
        return this.serialNumber;
    }

    public void setFlagCollidingWithRectangle(boolean param) {
        this.isCollidingWithRectangle = param;
    }

    public boolean isCollidingWithRectangle() {
        return isCollidingWithRectangle;
    }

    /**
     * Know if a particle may or may not move.
     *
     * @return true is the particle is allowed to move, false otherwise.
     */
    public boolean isMovementAllowed() {
        return this.movementAllowed;
    }

    /**
     * Allow (or not) a particle to move.
     *
     * @param allowed
     */
    public void setMovementAllowed(boolean allowed) {
        this.movementAllowed = allowed;
    }

    private void setColorScale() {
        this.coloredPointList = new ArrayList<>();
        if (this.radius < 0.5) {
            this.coloredPointList.add(new PointCouleur(0, 1, 255, 0, 0));
            this.coloredPointList.add(new PointCouleur(1, 1, 0, 0, 255));
        } else if (this.radius < 1) {
            this.coloredPointList.add(new PointCouleur(0, 1, 0, 255, 0));
            this.coloredPointList.add(new PointCouleur(1, 1, 0, 128, 128));
        } else {
            this.coloredPointList.add(new PointCouleur(0, 1, 0, 0, 255));
            this.coloredPointList.add(new PointCouleur(0, 1, 128, 128, 255));
        }

//        this.coloredPointList.add(new PointCouleur(1, 1, 255, 0, 0)); // red
//        this.coloredPointList.add(new PointCouleur(1.1, 1, 255, 128, 0)); // orange
//        this.coloredPointList.add(new PointCouleur(1.2, 1, 255, 255, 0)); // yellow
//        this.coloredPointList.add(new PointCouleur(1.4, 1, 230, 230, 230)); // gray
//        this.coloredPointList.add(new PointCouleur(1.8, 1, 0, 255, 255)); // light blue
//        this.coloredPointList.add(new PointCouleur(2.6, 1, 255, 0, 255)); // violet
//        this.coloredPointList.add(new PointCouleur(4.2, 1, 0, 0, 0)); // black
//        this.coloredPointList.add(new PointCouleur(7.4, 1, 128, 128, 128)); // gray
    }

    /**
     * If the other particle is of a given type (for the moment, the type is
     * defined via the radius), then the two particles change their nature.
     *
     * @param p
     */
    public void react(Particle p) {
        if (this.touches(p)) {
            if (this.radius < 1 && p.radius < 1) {
                // Both particles turn into gas with a much larger radius
                this.radius = 1.0;
                this.setColorScale();
                p.radius = 1.0;
                p.setColorScale();
            }
        }
    }

    /**
     * Return true when the two particles are in contact
     *
     * @param p
     * @return
     */
    public boolean touches(Particle p) {
//        System.out.println("radii: " + this.radius + ", " + p.radius + ", distance: " + Math.sqrt(this.distanceAuCarre(p)));
        return Math.sqrt(this.distanceAuCarre(p)) < this.radius + p.radius;
    }
}
