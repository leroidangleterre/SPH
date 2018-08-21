
import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

/**
 * The terrain contains all moving particles and all objects that interact with
 * them.
 */
public class World {

    // The terrain is divided in squares.
    private ArrayList<ArrayList<Square>> tab;
    private ArrayList<Particle> transferList;
    private int nbLines, nbColumns;
    private double xMin, xMax, yMin, yMax;
    private double elemSize;

    // Elasticity of the outer boundaries of the terrain
    private double boundaryElasticity = 0.1;

    // The objects that interact with the particles.
    private ArrayList<Rectangle> rectangleList;

    // Coordinates of the points of mouse click and mouse release.
    private double xClick, yClick, xRelease, yRelease, xMouse, yMouse;
    private double xRightClick, yRightClick;

    // This flag is set when a rectangle is being drawn.
    private boolean rectangleBeingDrawn;

    // Amount of CTRL keys being pressed.
    private int nbCTRLPressed;

    // Density of particles; used to create random particles.
    private int particleDensity = 5;

    // Radius of the particles.
    private double radius = 1.0;

    private double gravitySave = -450.0;
    private double gravity;

    // Initial values of the sources and holes at their creation.
    private double sourceOutflow;
    private double holeInflow;

    // Possible values: "vInst", "vMoy", "_", to display the current speed, the average speed, or nothing at all.
    private String instantSpeedDisplay;

    private boolean selectionBeingMoved;

    // Flag that indicates that the simulation is running.
    private boolean isRunning;

    // This tool is used to prevent two concurrent modifications of the data,
    // as well as a display during a modification.
    private Semaphore sem;

    private int step;

    public World(int nbLines, int nbColumns, double elemSize) {

        this.nbLines = nbLines;
        this.nbColumns = nbColumns;

        this.tab = new ArrayList<ArrayList<Square>>();
        for (int i = 0; i < this.nbLines; i++) {

            ArrayList<Square> line = new ArrayList<Square>();
            this.tab.add(line);

            double yC = (this.nbLines / 2 - i) * elemSize;
            for (int j = 0; j < this.nbColumns; j++) {
                double xC = (j - this.nbColumns / 2) * elemSize;
                line.add(new Square(xC, yC, elemSize, boundaryElasticity, i, j));
            }
        }

        // Choose the neighbors that the current square will know and interact with:
        // The dependancies are:
        // south-west; south; south-east; east.
        // The neighbors are:
        // north; south; east; west.
        for (int i = 0; i < this.tab.size(); i++) {
            for (int j = 0; j < this.tab.get(i).size(); j++) {
                Square c = this.getSquare(i, j);

                // Dependancies.
                c.setDependancy(this.getSquare(i + 1, j - 1));
                c.setDependancy(this.getSquare(i + 1, j));
                c.setDependancy(this.getSquare(i + 1, j + 1));
                c.setDependancy(this.getSquare(i, j + 1));

                // Neighbors
                c.setNeighbor(this.getSquare(i + 1, j));
                c.setNeighbor(this.getSquare(i - 1, j));

                c.setNeighbor(this.getSquare(i, j + 1));
                c.setNeighbor(this.getSquare(i, j - 1));
            }
        }

        this.xMin = this.getSquare(0, 0).getXMin();
        this.xMax = this.getSquare(0, this.nbColumns - 1).getXMax();
        this.yMin = this.getSquare(this.nbLines - 1, 0).getYMin();
        this.yMax = this.getSquare(0, 0).getYMax();
        this.elemSize = elemSize;
        this.transferList = new ArrayList<Particle>(0);
        this.rectangleList = new ArrayList<Rectangle>(0);
        this.rectangleBeingDrawn = false;
        this.nbCTRLPressed = 0;
        this.gravity = 0;// gravitySave;
        this.instantSpeedDisplay = "v";
        this.selectionBeingMoved = false;
        this.isRunning = false;
        this.sem = new Semaphore(1);
        this.step = 0;

//        this.setWallsForTest();
//        this.setRectanglesForTest();
    }

    /**
     * Place several rectangles in the form of a rocket engine bell.
     */
    private void createBellShape(double x0, double y0, double verticScale) {
        double fact = 0.2;
        double fact4 = 0.007;

        double xPrev = 0;//x0;
        double yPrev = y0;

        for (int i = 1; i < 5; i++) {
            double xNext = 2 * i;
            double yNext = verticScale * (-fact * xNext * xNext - fact4 * xNext * xNext * xNext * xNext + y0);

            double x = (xPrev + xNext) / 2;
            double y = (yPrev + yNext) / 2;

            double dx = xNext - xPrev;
            double dy = yNext - yPrev;

            double margin = 2;
            double width = Math.sqrt(dx * dx + dy * dy) + margin;
            double height = 1.0;

            double angle = Math.atan(dy / dx);
            System.out.println("dx = " + dx + ", dy = " + dy + ", angle = " + angle);

            double elasticity = 0;

            this.rectangleList.add(new Rectangle(x, y, width, height, elasticity, angle));
            this.rectangleList.add(new Rectangle(-x, y, width, height, elasticity, -angle));
            xPrev = xNext;
            yPrev = yNext;
        }
        // Source
        double coefSource = 0.1;
        this.getSquare((int) (16 - y0 / (float) elemSize), (int) (8 + x0 / (float) elemSize))
                .setSource(coefSource);
    }

    /**
     * Build a reaction chamber linked to an engine bell.
     */
    public void createBell2(double x0, double y0, double scale) {

        this.getSquare(16, 8).setSource(1);

        double elasticity = 0;
        double angle;

        angle = 3.5 * Math.PI / 8;
        double length = 3 * scale;
        this.rectangleList.add(new Rectangle(x0, y0 * scale - 0.35, 0.3 * length, 0.2 * scale, elasticity, 0));
        this.rectangleList.add(new Rectangle(x0 + 0.6 * scale, y0 - 1.57 * scale, length, 0.2 * scale, elasticity, -angle));
        this.rectangleList.add(new Rectangle(-x0 - 0.6 * scale, y0 - 1.57 * scale, length, 0.2 * scale, elasticity, +angle));
        this.rectangleList.add(new Rectangle(x0 + 0.3 * scale, y0 - 0.2 * scale, 0.6 * scale, 0.2 * scale, elasticity, -angle / 2));
        this.rectangleList.add(new Rectangle(-x0 - 0.3 * scale, y0 - 0.2 * scale, 0.6 * scale, 0.2 * scale, elasticity, +angle / 2));

//        // Chamber
//        this.rectangleList.add(new Rectangle(x0, y0, scale, 0.2 * scale, elasticity));
//        angle = Math.PI / 2;
//        this.rectangleList.add(new Rectangle(x0 + scale / 2, y0 - scale / 2, scale, 0.2 * scale, elasticity, -angle));
//        this.rectangleList.add(new Rectangle(-x0 - scale / 2, y0 - scale / 2, scale, 0.2 * scale, elasticity, +angle));
        //// Bottleneck and bell
        //        angle = 4.5 * Math.PI / 8;
        //        this.rectangleList.add(new Rectangle(x0 + 0.4 * scale, y0 - 1.3 * scale, scale, 0.2 * scale, elasticity, -angle));
        //        this.rectangleList.add(new Rectangle(-x0 - 0.4 * scale, y0 - 1.3 * scale, scale, 0.2 * scale, elasticity, +angle));
        //        angle = 3.5 * Math.PI / 8;
        //        this.rectangleList.add(new Rectangle(x0 + 0.5 * scale, y0 - 2.74 * scale, 2 * scale, 0.2 * scale, elasticity, -angle));
        //        this.rectangleList.add(new Rectangle(-x0 - 0.5 * scale, y0 - 2.74 * scale, 2 * scale, 0.2 * scale, elasticity, +angle));
        //// Open bell only
        //        angle = 3.5 * Math.PI / 8;
        //        this.rectangleList.add(new Rectangle(x0 + 0.7 * scale, y0 - 1.97 * scale, 2 * scale, 0.2 * scale, elasticity, -angle));
        //        this.rectangleList.add(new Rectangle(-x0 - 0.7 * scale, y0 - 1.97 * scale, 2 * scale, 0.2 * scale, elasticity, +angle));
    }

    public void setRectanglesForTest() {

        double verticScale = 5.0;

        double x0 = verticScale * 0;
        double y0 = 0;
//        createBellShape(x0, y0, verticScale);
        createBell2(x0, y0, verticScale);

        // Well at the botton of the simulation
        for (int j = 0; j < this.nbColumns; j++) {
            Square s = this.getSquare(nbLines - 1, j);
            s.setHole(5);
        }
    }

    /**
     * Add walls to form a closed area, to test that particles do not cross any
     * walls.
     */
    private void setWallsForTest() {
        int iMin = this.nbLines / 2 - 3;
        int iMax = this.nbLines / 2 + 3;
        int jMin = this.nbColumns / 2 - 3;
        int jMax = this.nbColumns / 2 + 3;

        // Columns
        for (int i = iMin; i <= iMax; i++) {
            this.getSquare(i, jMin).setWall();
            this.getSquare(i, jMax).setWall();
        }
        // Lines
        for (int j = jMin; j <= jMax; j++) {
            this.getSquare(iMin, j).setWall();
            this.getSquare(iMax, j).setWall();
        }

        this.getSquare(nbLines / 2, nbColumns / 2).setSource(0.1);
    }

    public void addSquare(Square c) {
        this.tab.get(c.getNumLine()).add(c.getNumColumn(), c);
    }

    // Set a given square at a given position: that replaces the previous square.
    public void setCarreau(Square sParam) {
        System.out.println("avant ajout: taille=" + this.tab.size());

        // Remove any potential double.
        for (int i = this.tab.size() - 1; i >= 0; i--) {
            ArrayList<Square> line = this.tab.get(i);
            for (int j = line.size() - 1; j >= 0; j--) {
                Square s = this.getSquare(i, j);
                if (s.getNumLine() == sParam.getNumLine() && s.getNumColumn() == sParam.getNumColumn()) {
                    // This is a double that must be replaced.
                    line.remove(j);
                }
            }
        }

        // The new square is added.
        this.addSquare(sParam);
    }

    /**
     * Get a square from its line and column numbers.
     */
    public Square getSquare(int numLine, int numColumn) {
        try {
            if (this.tab != null) {
                ArrayList<Square> line = this.tab.get(numLine);
                if (line != null) {
                    return line.get(numColumn);
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            return null;
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    /**
     * Get the square that contains the specified coordinates.
     */
    public Square getSquareFromCoordinates(double x, double y) {
        int numLine = (int) ((this.yMax - y) / this.elemSize);
        int numColumn = (int) ((x - this.xMin) / this.elemSize);

        return this.getSquare(numLine, numColumn);
    }

    /**
     * Get the terrain width.
     */
    public double getWidth() {
        return this.xMax - this.xMin;
    }

    /**
     * Get the terrain height.
     */
    public double getHeight() {
        return this.yMax - this.yMin;
    }

    /**
     * Get the height/width ratio.
     */
    public double getRatio() {
        return (double) this.nbLines / (double) this.nbColumns;
    }

    /**
     * Get the min abscissa.
     */
    public double getXMin() {
        return this.xMin;
    }

    /**
     * Get the max abscissa.
     */
    public double getXMax() {
        return this.xMax;
    }

    /**
     * Get the min ordinate.
     */
    public double getYMin() {
        return this.yMin;
    }

    /**
     * Get the max ordinate.
     */
    public double getYMax() {
        return this.yMax;
    }

    /**
     * Get the amount of lines.
     */
    public int getNbLines() {
        return this.nbLines;
    }

    /**
     * Get the amount of columns.
     */
    public int getNbColumns() {
        return this.nbColumns;
    }

    /**
     * Get the elementary size.
     */
    public double getElemSize() {
        return this.elemSize;
    }

    /**
     * Switch between play and pause.
     */
    public void switchPlayPause() {
        this.isRunning = !this.isRunning;
    }

    /**
     * At each timer tick, compute one evolve step if the terrain is not on
     * pause.
     */
    public void evoluerAuto(double dt) {

        if (this.isRunning) {
            this.evoluer(dt);
        }
    }

    /**
     * Execute one step of evolution, only if the terrain is not already on play
     * mode.
     */
    public void evoluerManuel(double dt) {
        if (!this.isRunning) {
            this.evoluer(dt);
        }
    }

    /**
     * Compute one step of evolution.
     */
    private void evoluer(double dt) {
        long startDate = System.currentTimeMillis();
        this.step++;
        try {
            sem.acquire();

            // Reset previous values
            for (ArrayList<Square> line : this.tab) {
                for (Square s : line) {
                    s.razDensitiesAndForces();
                }
            }

            // Process sources and holes.
            for (int i = 0; i < this.nbLines; i++) {
                for (int j = 0; j < this.nbColumns; j++) {
                    this.getSquare(i, j).processSourcesAndHoles();
                }
            }

//            Density computation
            for (int i = 0; i < this.nbLines; i++) {
                for (int j = 0; j < this.nbColumns; j++) {
                    this.getSquare(i, j).computeDensities();
                }
            }
//            Pressure computation
            for (int i = 0; i < this.nbLines; i++) {
                for (int j = 0; j < this.nbColumns; j++) {
                    this.getSquare(i, j).computePressure();
                }
            }
            // Collisions on wall squares.
            for (int i = 0; i < this.nbLines; i++) {
                for (int j = 0; j < this.nbColumns; j++) {
                    Square c = this.getSquare(i, j);
                    if (c.isWall()) {
                        c.processWall();
                    }
                }
            }
            // Viscosity and pressure forces computation
            for (int i = 0; i < this.nbLines; i++) {
                for (int j = 0; j < this.nbColumns; j++) {
                    this.getSquare(i, j).computeForces();
                }
            }
            // Uniform friction applied to all particles (test purpose, TODO: delete this.)
            for (int i = 0; i < this.nbLines; i++) {
                for (int j = 0; j < this.nbColumns; j++) {
                    this.getSquare(i, j).applyFriction();
                }
            }

            // Rectangle repulsion on the particles.
            for (int i = 0; i < this.nbLines; i++) {
                for (int j = 0; j < this.nbColumns; j++) {
                    this.getSquare(i, j).processRectangles(this.rectangleList);
                }
            }

            // Compute the speed of all particles of each square.
            for (int i = 0; i < this.nbLines; i++) {
                for (int j = 0; j < this.nbColumns; j++) {
                    this.getSquare(i, j).computeSpeed(dt, this.gravity);
                }
            }

            // Some squares may have an influence on the speeds.
//            for (int i = 0; i < 4; i++) {
//                int line = this.nbLines - 1 - i;
//                int col = 0;
//                this.getSquare(line, col).influenceMeanSpeed(0.0, 100.0);
//                this.getSquare(line - 4, col).influenceMeanSpeed(200.0, 90.0);
//            }
            // Move the particles at their current speed.
            for (int i = 0; i < this.tab.size(); i++) {
                ArrayList<Square> ligne = this.tab.get(i);
                for (int j = 0; j < ligne.size(); j++) {
                    this.getSquare(i, j).moveContent(dt, this.transferList);
                }
            }

            // Replace in the proper square the particles that just left their previous square,
            // and bounce the speed of those that reach the terrain limits.
            while (!this.transferList.isEmpty()) {
                Particle p = this.transferList.remove(0);

                // Lower boundary
                if (p.getY() < this.yMin && p.getVy() <= 0) {
                    p.setVy(-p.getVy() * boundaryElasticity);
                    p.setY(this.yMin + (this.yMin - p.getY()));
                }

                // Upper boundary
                if (p.getY() > this.yMax && p.getVy() >= 0) {
                    p.setVy(-p.getVy() * boundaryElasticity);
                    p.setY(this.yMax + (this.yMax - p.getY()));
                }

                // Right boundary
                if (p.getX() > this.xMax && p.getVx() >= 0) {
                    p.setVx(-p.getVx() * boundaryElasticity);
                    p.setX(this.xMax + (this.xMax - p.getX()));
                }

                // Left boundary
                if (p.getX() < this.xMin && p.getVx() <= 0) {
                    p.setVx(-p.getVx() * boundaryElasticity);
                    p.setX(this.xMin + (this.xMin - p.getX()));
                }

                this.reinjectParticle(p, dt);
            }

            System.out.println(this.getNbParticles() + " p");

        } catch (InterruptedException ex) {
            System.out.println("World.evolve: InterruptedException");
        }
        sem.release();

        // Test:
        boolean found = false;
        for (ArrayList<Square> line : this.tab) {
            for (Square s : line) {
                for (int i = 0; i < s.getNbParticules(); i++) {
                    Particle p = s.getParticule(i);
//					System.out.println("Particle : " + p);
                    found = true;
                }
            }
        }
        if (!found) {
            System.out.println("No particles");
        }
    }

    /**
     * Replace a particle that left its square into the correct square.
     */
    private void reinjectParticle(Particle p, double dt) {
        boolean done = false;
        int i;
        int j;

        double deltaSpeed = 0.1;

        double speedDamping = 0.0;

        // Special case: particles that reach the outer limits of the terrain.
        if (p.getLineNum() < 0) {
            p.setLineNum(0);
        }
        if (p.getLineNum() == this.nbLines) {
            p.setLineNum(this.nbLines - 1);
        }
        if (p.getColomnNum() < 0) {
            p.setColumnNum(0);
        }
        if (p.getColomnNum() >= this.nbColumns) {
            p.setColumnNum(this.nbColumns - 1);
        }

        i = 0;
        while (!done && i < this.tab.size()) {
            ArrayList<Square> line = this.tab.get(i);
            j = 0;
            while (!done && j < line.size()) {
                // Test square (i, j) for compatibility.
                Square c = this.getSquare(i, j);
                if (c.getNumLine() == p.getLineNum() && c.getNumColumn() == p.getColomnNum()) {
                    // We found the correct square.
                    c.receiveParticle(p);
                    done = true;
                }
                j++;
            }
            i++;
        }
    }

    /**
     * Display the current selection rectangle.
     */
    private void displaySelectionRectangle(Graphics g, double x0, double y0, double zoom, int panelHeight) {

        // Coordinates of the rectangle in the referential of the terrain.
        double xLeft = Math.min(this.xMouse, this.xClick);
        double xRight = Math.max(this.xMouse, this.xClick);
        double yTop = Math.max(this.yMouse, this.yClick);
        double yBottom = Math.min(this.yMouse, this.yClick);

        // These values are converted to be expressed in the panel ref.
        /*
		 * NB: int xApp=(int)(this.getX()*zoom+x0); int
		 * yApp=(int)(hauteurPanneau - (this.getY()*zoom+y0));
         */
        int xLeftApp = (int) (xLeft * zoom + x0);
        int xRightApp = (int) (xRight * zoom + x0);
        int yTopApp = (int) (panelHeight - (yTop * zoom + y0));
        int yBottomApp = (int) (panelHeight - (yBottom * zoom + y0));

        // Actually display the rectangle
        g.setColor(Color.green);
        g.drawRect(xLeftApp, yTopApp, xRightApp - xLeftApp, yBottomApp - yTopApp);
    }

    /**
     * Display the terrain.
     */
    public void display(Graphics g, double x0, double y0, double zoom, int panelHeight, int panelWidth) {

        try {
            // System.out.println("Display is trying to get the semaphore.");
            sem.acquire();
            // System.out.println(" Display got the semaphore.");

            // Display rectangles
            for (int i = 0; i < this.rectangleList.size(); i++) {
                this.rectangleList.get(i).display(g, x0, y0, zoom, panelHeight);
            }
            for (int i = 0; i < this.rectangleList.size(); i++) {
                this.rectangleList.get(i).displayBorders(g, x0, y0, zoom, panelHeight);
            }

            for (int i = 0; i < this.tab.size(); i++) {
                for (int j = 0; j < this.tab.get(i).size(); j++) {
                    this.getSquare(i, j).displayBackground(g, x0, y0, zoom, panelHeight, this.instantSpeedDisplay);
                }
            }
            for (int i = 0; i < this.tab.size(); i++) {
                for (int j = 0; j < this.tab.get(i).size(); j++) {
                    this.getSquare(i, j).displayParticles(g, x0, y0, zoom, panelHeight, this.instantSpeedDisplay);
                }
            }
            if (this.rectangleBeingDrawn) {
                this.displaySelectionRectangle(g, x0, y0, zoom, panelHeight);
            }
        } catch (InterruptedException ex) {
            System.out.println("Display threw an exception.");
        }
        sem.release();
        // System.out.println(" Display released the semaphore.");

    }

    /**
     * Action done when a left click occurs.
     */
    public void leftClickAction(double x, double y) {
        this.xClick = x;
        this.yClick = y;

        if (this.clickIsInSelection(x, y)) {
            this.selectionBeingMoved = true;
        } else {
            this.rectangleBeingDrawn = true;
        }
    }

    /**
     * Action done when a right click occurs.
     */
    public void rightClicAction(double x, double y) {
        this.xRightClick = x;
        this.yRightClick = y;
    }

    /**
     * Action done when the left button is released.
     */
    public void leftReleaseAction(double x, double y, Tool tool) {
        this.xRelease = x;
        this.yRelease = y;

        if (this.selectionBeingMoved) {
            this.selectionBeingMoved = false;
        } else {
            this.rectangleBeingDrawn = false;

            switch (tool) {
            case PARTICLE_SELECTION:
                this.selectParticles();
                break;
            case PARTICLE_CREATION:
                this.createParticles();
                break;
            case RECTANGLE:
                this.createRectangle();
                break;
            case WALL_SQUARE:
                this.addWalls();
                break;
            case SOURCE_SQUARE:
                this.addSources(true);
                break;
            case HOLE_SQUARE:
                this.addSources(false);
                break;
            case EMPTY_SQUARE:
                this.addEmptySquares();
                break;
            default:
                break;
            }
        }
    }

    /**
     * Update the mouse position, and change content accordingly.
     *
     * @param the new position of the mouse.
     */
    public void applyMouseMovement(double xParam, double yParam) {

        double dx = xParam - this.xMouse;
        double dy = yParam - this.yMouse;

        if (selectionBeingMoved) {
            for (int i = 0; i < this.tab.size(); i++) {
                for (int j = 0; j < this.tab.get(i).size(); j++) {
                    Square c = this.getSquare(i, j);
                    c.moveSelectedContent(dx, dy);
                    c.cancelSpeedOfSelection();
                }
            }
        }

        this.xMouse = xParam;
        this.yMouse = yParam;
    }

    /**
     * Increase the amount of CTRL keys being pressed.
     */
    public void increaseNbCtrlPressed() {
        this.nbCTRLPressed++;
    }

    /**
     * Decrease the amount of CTRL keys being pressed.
     */
    public void decreaseNbCtrlPressed() {
        this.nbCTRLPressed--;
    }

    /**
     * Select all particles in this world.
     */
    public void selectEverything() {
        System.out.println("World.selectEverything()");
        for (ArrayList<Square> list : this.tab) {
            for (Square s : list) {
                s.selectEverything();
            }
        }
    }

    /**
     * Sets to zero the speed of each particle.
     */
    public void blockSpeeds() {
        for (ArrayList<Square> list : this.tab) {
            for (Square s : list) {
                s.blockSpeeds();
            }
        }
    }

    /**
     * Choose which particles must be selected by the selection rectangle.
     */
    private void selectParticles() {

        double xG = Math.min(xClick, xRelease);
        double xD = Math.max(xClick, xRelease);
        double yH = Math.max(yClick, yRelease);
        double yB = Math.min(yClick, yRelease);

        for (int i = 0; i < this.tab.size(); i++) {
            for (int j = 0; j < this.tab.get(i).size(); j++) {
                this.getSquare(i, j).selectParticles(yH, yB, xG, xD);
            }
        }
    }

    /**
     * Delete all selected elements.
     */
    public synchronized void deleteSelection() {
        try {
            sem.acquire();
            for (int i = 0; i < this.tab.size(); i++) {
                for (int j = 0; j < this.tab.get(i).size(); j++) {
                    this.getSquare(i, j).deleteSelection();
                }

            }
            sem.release();
        } catch (InterruptedException e) {
        }
    }

    /**
     * Add particles. If the click and release happen at the same place, add
     * only one particle; if a movement occurred, add particles in all the
     * specified area, with the correct density.
     */
    private void createParticles() {

        try {
            sem.acquire();

            if (this.xClick == this.xRelease && this.yClick == this.yRelease) {
                Square target = this.getSquareFromCoordinates(this.xClick, this.yClick);
                if (target != null) {
                    target.createParticle(this.xClick, this.yClick);
                }
            } else {
                double dyClick = this.yMax - this.yClick;
                int numLigneClic = (int) (dyClick / this.elemSize);
                if (this.yClick > this.yMax) {
                    numLigneClic--;
                }
                double dxClick = xClick - this.xMin;
                int numColonneClic = (int) (dxClick / this.elemSize);
                if (this.xClick < this.xMin) {
                    numColonneClic--;
                }
                double dyRelease = this.yMax - this.yRelease;
                int numLigneDeclic = (int) (dyRelease / this.elemSize);
                if (this.yRelease > this.yMax) {
                    numLigneDeclic--;
                }
                double dxRelease = xRelease - this.xMin;
                int numColonneDeclic = (int) (dxRelease / this.elemSize);
                if (this.xRelease < this.xMin) {
                    numColonneDeclic--;
                }

                System.out.println(
                        "click at l " + numLigneClic + ", c " + numColonneClic + ", release at l " + numLigneDeclic + ", c " + numColonneDeclic);

                for (int i = Math.min(numLigneClic, numLigneDeclic) + 1; i < Math.max(numLigneClic, numLigneDeclic); i++) {
                    for (int j = Math.min(numColonneClic, numColonneDeclic) + 1; j < Math.max(numColonneClic, numColonneDeclic); j++) {

                        Square s = this.getSquare(i, j);
                        if (s != null) {
                            int nbParticles = 1;// (int)(0.1 * this.particleDensity * (int)s.getSurface());
                            s.createSeveralParticles(nbParticles);
                        }
                    }
                }
            }
            sem.release();
        } catch (InterruptedException e) {
            System.out.println("World.createParticles(): InterruptedException");
        }
    }

    /**
     * Set the outflow of all future new sources.
     */
    public void setSourceOutflow(double outflow) {
        this.sourceOutflow = outflow;
    }

    /**
     * Get the outflow of all future new sources.
     */
    public double getSourceOutflow() {
        return this.sourceOutflow;
    }

    public void setHoleInflow(double debit) {
        this.holeInflow = debit;
    }

    public double getHoleInflow() {
        return this.holeInflow;
    }

    /**
     * Replace each square contained in the selection area with: - a source
     * square if the flag is true; - a hole square if the flag is false.
     */
    private void addSources(boolean source) {

        double xG = Math.min(xClick, xRelease);
        double xD = Math.max(xClick, xRelease);
        double yH = Math.max(yClick, yRelease);
        double yB = Math.min(yClick, yRelease);

        for (int i = 0; i < this.tab.size(); i++) {
            for (int j = 0; j < this.tab.get(i).size(); j++) {
                Square s = this.getSquare(i, j);

                if (s.isIncludedInRectangle(xG, xD, yB, yH)) {
                    if (source) {
                        s.setSource(this.sourceOutflow);
                    } else {
                        s.setHole(this.holeInflow);
                    }
                }
            }
        }
    }

    /**
     * Replace with a wall all squares contained in the selection area. If only
     * one point was clicked, replace with a wall only the square containing
     * that point.
     */
    private void addWalls() {

        double xG = Math.min(xClick, xRelease);
        double xD = Math.max(xClick, xRelease);
        double yH = Math.max(yClick, yRelease);
        double yB = Math.min(yClick, yRelease);

        // Find the click/release squares.
        Square upLeft = this.getSquareFromCoordinates(xG, yH);
        Square lowRight = this.getSquareFromCoordinates(xD, yB);

        try {
            if (upLeft == lowRight) {
                upLeft.setWall();
            } else {
                for (int i = 0; i < this.tab.size(); i++) {
                    for (int j = 0; j < this.tab.get(i).size(); j++) {
                        Square s = this.getSquare(i, j);

                        if (s.isIncludedInRectangle(xG, xD, yB, yH)) {
                            s.setWall();
                        }
                    }
                }
            }
        } catch (NullPointerException e) {
            // Do nothing
        }
    }

    /**
     * Remove the source or hole attributes.
     */
    private void addEmptySquares() {

        double xG = Math.min(xClick, xRelease);
        double xD = Math.max(xClick, xRelease);
        double yH = Math.max(yClick, yRelease);
        double yB = Math.min(yClick, yRelease);

        for (int i = 0; i < this.tab.size(); i++) {
            for (int j = 0; j < this.tab.get(i).size(); j++) {
                Square c = this.getSquare(i, j);

                if (c.isIncludedInRectangle(xG, xD, yB, yH)) {
                    c.setEmpty();
                }
            }
        }
    }

    /**
     * Replace a square with a hole.
     */
    private void addHole() {

        double xG = Math.min(xClick, xRelease);
        double xD = Math.max(xClick, xRelease);
        double yH = Math.max(yClick, yRelease);
        double yB = Math.min(yClick, yRelease);

        for (int i = 0; i < this.tab.size(); i++) {
            for (int j = 0; j < this.tab.get(i).size(); j++) {

                Square c = this.getSquare(i, j);

                if (c.isIncludedInRectangle(xG, xD, yB, yH)) {
                    c.setHole(-1);
                }
            }
        }
    }

    /**
     * Replace the existing square with an empty one.
     */
    private void addEmptySquare() {

        double xG = Math.min(xClick, xRelease);
        double xD = Math.max(xClick, xRelease);
        double yH = Math.max(yClick, yRelease);
        double yB = Math.min(yClick, yRelease);

        for (int i = 0; i < this.tab.size(); i++) {
            for (int j = 0; j < this.tab.get(i).size(); j++) {

                Square c = this.getSquare(i, j);

                if (c.isIncludedInRectangle(xG, xD, yB, yH)) {
                    if (c.isSource() || c.isHole()) {
                        // The square is not a source anymore
                        c.setSource(0);
                        // The square is not a hole anymore
                        c.setHole(0);
                    }
                }
            }
        }
    }

    /**
     * Get the total kinetic energy of the particles.
     */
    private double getKineticEnergy() {
        double e = 0;
        for (int i = 0; i < this.nbLines; i++) {
            for (int j = 0; j < this.nbColumns; j++) {
                Square s = this.getSquare(i, j);
                e = e + s.getKineticEnergy();
            }
        }
        return e;
    }

    private double getPotentialEnergy(double g) {
        double e = 0;
        for (int i = 0; i < this.nbLines; i++) {
            for (int j = 0; j < this.nbColumns; j++) {
                Square c = this.getSquare(i, j);
                e = e + c.getPotentialEnergy(g);
            }
        }
        return e;
    }

    public void toggleGravity() {
        if (this.gravity == 0) {
            this.gravity = this.gravitySave;
            System.out.println("Terrain: gravity ON");
        } else {
            this.gravity = 0;
            System.out.println("Terrain: gravity OFF");
        }
    }

    /**
     * Toggle the speed display mode: instant speed, mean speed, no display.
     */
    public void changerChoixVitesses(String vInst) {

        this.instantSpeedDisplay = vInst;
    }

    /**
     * Determine whether or not the user clicked on a selected element.
     */
    private boolean clickIsInSelection(double x, double y) {
        boolean res = false;
        int i = 0;
        int j;
        while (res == false && i < this.tab.size()) {
            ArrayList<Square> line = this.tab.get(i);
            j = 0;
            while (res == false && j < line.size()) {
                res = line.get(j).pointAppartientASelection(x, y);
                j++;
            }
            i++;
        }
        return res;
    }

    /**
     * Get the total amount of particles.
     */
    public int getNbParticules() {
        int res = 0;
        for (int i = 0; i < this.tab.size(); i++) {
            for (int j = 0; j < this.tab.get(i).size(); j++) {
                Square s = this.getSquare(i, j);
                res = res + s.getNbParticules();
            }
        }
        return res;
    }

    private void createRectangle() {
        if (this.rectangleList == null) {
            this.rectangleList = new ArrayList<Rectangle>();
        }
        double width = Math.abs(xClick - xRelease);
        double height = Math.abs(yClick - yRelease);
        double xCenter = (xClick + xRelease) / 2;
        double yCenter = (yClick + yRelease) / 2;
        this.rectangleList.add(new Rectangle(xCenter, yCenter, width, height, boundaryElasticity));
    }

    public void increaseParticleRadii() {
        for (ArrayList<Square> list : tab) {
            for (Square s : list) {
                System.out.println("world r+");
                s.increaseParticleRadii();
            }
        }
    }

    public void decreaseParticleRadii() {
        for (ArrayList<Square> list : tab) {
            for (Square s : list) {
                s.decreaseParticleRadii();
            }
        }
    }

    public void rotateAllRectangles(double dAngle) {
        for (Rectangle r : this.rectangleList) {
            r.rotate(dAngle);
        }
    }

    public void printRectangleList() {
        for (Rectangle r : this.rectangleList) {
            System.out.println(r.toString());
        }
    }

    public int getNbParticles() {
        int res = 0;
        for (ArrayList<Square> list : this.tab) {
            for (Square s : list) {
                res += s.getNbParticules();
            }
        }
        return res;
    }
}
