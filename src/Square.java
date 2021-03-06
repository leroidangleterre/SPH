
import colorramp.ColorRamp;
import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Random;

/*
 * Le carreau est la division de base de l'espace contenant les particules.
 * Il est en lien avec ses voisins, à qui il peut transmettre des particules.
 * NB: en général, un carreau aura quatre voisins enregistrés, mais certains carreaux situés sur les bords
 * du terrain pourront en avoir moins.
 * Un carreau n'aura besoin de connaître que quatre de ses voisins:
 * Pour le schéma ci-dessous:
 * "C" désigne le carreau en cours de traitement;
 * "V" désigne un voisin que C va influencer et dont C va se servir pour se calculer;
 * "." désigne un voisin qui a déjà influencé C et qui a déjà été influencé par C.
 *
 * Schéma:
 * ...
 * .CV
 * VVV
 */
public class Square {

    /* Coordonnées du centre du carreau, taille d'un côté. */
    protected double xCenter, yCenter, size;
    private int numLigne, numColonne;

    /* Les particules présentes dans le carreau */
    protected ArrayList<Particle> particleList;

    protected ArrayList<Food> foodList;

    /**
     * The combined mass of all the particles contained in this square.
     */
    protected double mass;

    private ColorRamp ramp;

    /*
     * Les particules qui sont sorties du carreau (dont les coordonnées
     * dépassent les limites) et qui doivent être placées sous le contrôle
     * de l'un des voisins.
     */
    private ArrayList<Particle> listeTransfert;

    /*
     * Collisions entre particules: on traite chaque particule d'une part avec
     * les autres particules du carreau, et d'autre part avec celles des
     * carreaux "dépendants" (E, SE, S, SO).
     */
    private ArrayList<Square> listeDependances;

    /*
     * Un carreau doit connaître la liste de ses voisins, notamment pour les
     * collisions si le carreau est un mur.
     */
    private Square northNeighbor, southNeighbor, eastNeighbor, westNeighbor;

    private Color color;
    private static Color defaultColor = Color.gray;

    private double speedDamping = 1.0;

    /**
     * Amount of speed that is conserved when a collision on a wall occurs. 0
     * <->
     * All energy absorbed; 1 <-> All energy conserved.
     */
    private double elasticity = 0.9;
    /*
     * Un carreau peut se comporter comme une source, comme un puits, ou comme
     * un mur.
     */
    private boolean isSource;
    private String sourceType;
    private boolean isHole;
    private double debit;
    private double count;
    private boolean isWall;

    private double particleRadius;

    private boolean mustReact;

    public Square(double x, double y, double taille, int numLigne, int numColonne) {
        this.xCenter = x;
        this.yCenter = y;
        this.size = taille;
        this.numLigne = numLigne;
        this.numColonne = numColonne;
        this.particleList = new ArrayList<>();
        this.listeTransfert = new ArrayList<>();
        this.listeDependances = new ArrayList<>();
        this.foodList = new ArrayList<>();
        this.color = defaultColor;
        this.isSource = false;
        this.sourceType = "none";
        this.isHole = false;
        this.isWall = false;

        particleRadius = 0.5;
        mustReact = false;
        mass = 0;
        ramp = new ColorRamp();
        ramp.addValue(0, Color.white);
        ramp.addValue(0.1, Color.blue);
        ramp.addValue(0.3, Color.red);
        ramp.addValue(1, Color.black);
    }

    public Square(double x, double y, double taille, double elasticity, int numLigne, int numColonne) {
        this(x, y, taille, numLigne, numColonne);
        this.elasticity = elasticity;
    }

    public Square clone() {
        return new Square(this.xCenter, this.yCenter, this.size, this.numLigne, this.numColonne);
    }

    /**
     * Set the radius of all the particles this square will produce.
     *
     * @param newRadius
     */
    public void setParticleRadius(double newRadius) {
        this.particleRadius = newRadius;
        System.out.println("set part radius to " + this.particleRadius);
    }

    /*
     * Déclarer un nouveau voisin. Attention: ce voisin ne doit pas avoir
     * déjà été enregistré (sinon BOUM!); Si le paramètre vaut null, on ne
     * fait rien.
     */
    public void setDependancy(Square v) {
        if (v != null) {
            if (v.getNumLine() != this.numLigne || v.getNumColumn() != this.numColonne) {
                this.listeDependances.add(v);
            }
        }
    }

    public void setNeighbor(Square v) {
        if (v != null) {
            /*
             * Il faut savoir dans quelle direction est le nouveau voisin (N, S,
             * E ou O).
             */
            if (v.numLigne == this.numLigne && v.numColonne == this.numColonne - 1) {
                // System.out.println("reglage voisin ouest.");
                this.westNeighbor = v;
            } else if (v.numLigne == this.numLigne && v.numColonne == this.numColonne + 1) {
                // System.out.println("reglage voisin est.");
                this.eastNeighbor = v;
            } else if (v.numLigne == this.numLigne - 1 && v.numColonne == this.numColonne) {
                // System.out.println("reglage voisin nord.");
                this.northNeighbor = v;
            } else if (v.numLigne == this.numLigne + 1 && v.numColonne == this.numColonne) {
                // System.out.println("reglage voisin sud.");
                this.southNeighbor = v;
            }
        }
    }

    /* Vérif. */
    public int getNbDependances() {
        return this.listeDependances.size();
    }

    public void afficherDependances() {
        System.out.println(this.toString2());
        for (int i = 0; i < this.listeDependances.size(); i++) {
            System.out.println("          " + this.listeDependances.get(i).toString2());
        }
    }

    public int getNbParticules() {
        if (this.particleList == null) {
            return -1;
        } else {
            return this.particleList.size();
        }
    }

    public Particle getParticule(int i) {
        return this.particleList.get(i);
    }

    public double getX() {
        return this.xCenter;
    }

    public double getY() {
        return this.yCenter;
    }

    public double getXMin() {
        return this.xCenter - this.size / 2;
    }

    public double getXMax() {
        return this.xCenter + this.size / 2;
    }

    public double getYMin() {
        return this.yCenter - this.size / 2;
    }

    public double getYMax() {
        return this.yCenter + this.size / 2;
    }

    public int getNumLine() {
        return this.numLigne;
    }

    public int getNumColumn() {
        return this.numColonne;
    }

    public double getTaille() {
        return this.size;
    }

    /*
     * Paramètres: offset de position et zoom; hauteur du panneau d'affichage;
     * booléen qui dit si on affiche les vitesses instantanées ou moyennes des
     * particules. Les vitesses moyennes sont calculées sur les N étapes
     * précédentes.
     */
    public void displayBackground(Graphics g, double x0, double y0, double zoom, int hauteurPanneau, String vitessesInstantanees) {
        /*
         * On convertit les coordonnées et la taille du carreau pour les avoir
         * dans le repère lié au panneau.
         */
        int xApp = (int) (this.xCenter * zoom + x0);
        int yApp = (int) (hauteurPanneau - (this.yCenter * zoom + y0));
        int tailleApp = (int) (this.size * zoom);

        g.setColor(Color.gray.brighter());
        g.drawRect(xApp - tailleApp / 2, yApp - tailleApp / 2, tailleApp, tailleApp);
//        g.setColor(ramp.getValue(this.mass));
//        g.fillRect(xApp - tailleApp / 2, yApp - tailleApp / 2, tailleApp + 1, tailleApp + 1);

        /*
         * Si c'est une source, on affiche un fond rouge. Si c'est un puits, on
         * affiche un fond noir. Si c'est un mur, on affiche un fond vert.
         */
        if (this.isSource) {
            g.setColor(this.color);
            g.fillRect(xApp - tailleApp / 2, yApp - tailleApp / 2, tailleApp, tailleApp);
        } else if (this.isHole) {
            g.setColor(Color.black);
            g.fillRect(xApp - tailleApp / 2, yApp - tailleApp / 2, tailleApp, tailleApp);
        } else if (this.isWall) {
            g.setColor(Color.green);
            g.fillRect(xApp - tailleApp / 2, yApp - tailleApp / 2, tailleApp, tailleApp);
        }
    }

    /**
     * Display the particles, and optionally the speeds and forces.
     */
    public void displayParticles(Graphics g, double x0, double y0, double zoom, int hauteurPanneau, String vitessesInstantanees) {

        for (Particle p : this.particleList) {
            p.display(g, x0, y0, zoom, hauteurPanneau);
        }
        for (Food f : foodList) {
            f.display(g, x0, y0, zoom, hauteurPanneau);
        }
    }

    /* Définir les éléments qui doivent être sélectionnés. */
    public void selectParticles(double yH, double yB, double xG, double xD) {
        for (int i = 0; i < this.particleList.size(); i++) {
            this.particleList.get(i).select(yH, yB, xG, xD);
        }
    }

    /* Supprimer tous les éléments qui sont sélectionnés. */
    public void deleteSelection() {
        /*
         * NB: pour éviter d'avoir à modifier l'indice à chaque fois qu'on
         * supprime un élément, on parcourt la liste en sens inverse.
         */
        for (int i = this.particleList.size() - 1; i >= 0; i--) {
            if (this.particleList.get(i).isSelected()) {
                Particle p = particleList.get(i);
                mass -= p.getMass();
                this.particleList.remove(i);
            }
        }
    }

    /*
     * Créer une nouvelle particule à un endroit spécifié. NB: il vaut mieux
     * être certain que la particule appartienne effectivement au carreau, mais
     * ce n'est pas rigoureusement obligatoire.
     */
    public void createParticle(double x, double y, double vx, double vy) {
        createParticle(x, y, vx, vy, this.sourceType);
    }

    public void createParticle(double x, double y, double vx, double vy, String particleType) {
        Particle p = new Particle(x, y, this.particleRadius, this.numLigne, this.numColonne, particleType);
        p.setVx(vx);
        p.setVy(vy);
        this.particleList.add(p);
        this.mass += p.getMass();
    }

    public void createParticle(double x, double y) {
        createParticle(x, y, 0, 0);
    }

    public void createSeveralParticles(int nb, String particleType) {
        double x, y;
        /*
         * Les coordonnées des particules seront choisies au
         * hasard.
         */
        Random gen = new Random();
        for (int i = 0; i < nb; i++) {
            x = this.xCenter + (gen.nextDouble() - 0.5) * this.size;
            y = this.yCenter + (gen.nextDouble() - 0.5) * this.size;
            if (particleType.equals("")) {
                this.createParticle(x, y, 0, 0, this.sourceType);
            } else {
                this.createParticle(x, y, 0, 0, particleType);
            }
        }
    }

    public void duplicateSelection() {
        ArrayList<Particle> duplicateParticles = new ArrayList<>();
        for (Particle p : particleList) {
            if (p.isSelected()) {
                Particle newP = p.clone();
                newP.setMass(p.getMass());
                p.setSelected(false);
                newP.setSelected(true);
                duplicateParticles.add(newP);
            }
        }

        for (Particle newP : duplicateParticles) {
            particleList.add(newP);
            mass += newP.getMass();
        }
    }

    /*
     * Supprimer une particule. On supprime brutalement la première particule
     * de la liste.
     */
    private void supprimerParticule() {
        if (this.particleList.size() > 0) {
            Particle p = particleList.get(0);
            mass -= p.getMass();
            this.particleList.remove(0);
        }
    }

    public void deleteManyParticules(int nb) {
        for (int i = 0; i < nb; i++) {
            this.supprimerParticule();
        }
    }

    /* Recevoir une particule qui existe déjà. */
    public void receiveParticle(Particle p) {
        this.particleList.add(p);
        mass += p.getMass();
    }

    public String toString() {
        return "c{x=" + (this.xCenter - this.size / 2) + ".." + (this.xCenter + this.size / 2) + ", y=" + (this.yCenter - this.size / 2) + ".."
                + (this.yCenter + this.size / 2) + "}";
    }

    public String toString2() {
        return "c{ numLigne=" + this.numLigne + ", numColonne=" + this.numColonne + "}";
    }

    public double getSurface() {
        return this.size * this.size;
    }

    public void razDensitiesAndForces() {

        /*
         * On traite toutes les particules de ce carreau, et seulement de ce
         * carreau.
         */
        for (Particle p : this.particleList) {
            p.resetDensityAndForces();
            p.setNbNeighbors(0);
        }
    }

    public void computeDensities() {

        /*
         * Pour chaque particule du carreau, on traite toutes celles de toutes les
         * d�pendances.
         */
        for (Particle p0 : this.particleList) {
            for (Square voisin : this.listeDependances) {
                for (Particle p1 : voisin.particleList) {

                    /* On incrémente les densités de p0 et de p1. */
                    p0.increaseDensity(p1);
                    p1.increaseDensity(p0);
                }
            }
        }

        /*
         * Pour chaque particule du carreau, on traite toutes celles situées
         * plus loin dans la liste de ce même carreau. NB: la densité d'une
         * particule est calculée notamment avec la particule elle-même.
         */
        for (int i = 0; i < this.particleList.size(); i++) {
            Particle p0 = this.particleList.get(i);
            p0.increaseDensity(p0);
            for (int j = i + 1; j < this.particleList.size(); j++) {
                Particle p1 = this.particleList.get(j);
                /* On incrémente les densités de p0 et de p1. */
                p0.increaseDensity(p1);
                p1.increaseDensity(p0);
            }
        }
    }

    public void computePressure() {
        for (int i = 0; i < this.particleList.size(); i++) {
            Particle p0 = this.particleList.get(i);
            p0.computePressure();
        }
    }

    /**
     * Compute all forces applied on all the particles that are contained in
     * this square.
     */
    public void computeForces() {
        // The particles of this square are matched against those of all 4 dependencies. */
        for (Particle p0 : this.particleList) {
            // Particles of the neighboring squares.
            for (Square voisin : this.listeDependances) {
                for (Particle p1 : voisin.particleList) {
                    p0.receiveForces(p1);
                    p1.receiveForces(p0);
                    if (this.mustReact) {
                        p0.react(p1);
                        p1.react(p0);
                    }
                }
            }
        }
        // Other particles of the same square.
        for (int i = 0; i < this.particleList.size(); i++) {
            Particle p0 = this.particleList.get(i);
            for (int j = i + 1; j < this.particleList.size(); j++) {
                Particle p1 = this.particleList.get(j);
                p0.receiveForces(p1);
                p1.receiveForces(p0);
                if (this.mustReact) {
                    p0.react(p1);
                    p1.react(p0);
                }
            }
        }
    }

    public void applyFriction() {
        for (Particle p : this.particleList) {
            p.dampenSpeed(speedDamping);
        }
    }

    public void computeSpeed(double dt, double gravity) {
        for (int i = 0; i < this.particleList.size(); i++) {
            this.particleList.get(i).computeSpeed(dt, gravity);
        }

    }

    public void processRectangles(ArrayList<Rectangle> list) {
        for (int iRect = 0; iRect < list.size(); iRect++) {
            Rectangle r = list.get(iRect);
            for (int iP = 0; iP < this.particleList.size(); iP++) {
                Particle p = this.particleList.get(iP);

                r.actOnParticle(p);
            }
        }
    }

    public void processSourcesAndHoles() {

        if (this.isSource) {
            this.count = this.count + this.debit;
            this.createSeveralParticles((int) (this.count), "");
            this.count = this.count - Math.floor(this.count);
        }
        if (this.isHole) {
            if (this.debit == -1) {
                this.empty();
            } else {
                this.count = this.count + this.debit;
                this.deleteManyParticules((int) (this.count));
                this.count = this.count - Math.floor(this.count);
            }
        }
    }

    public void processWall() {

    }

    public void setSource(double newDebit) {
        if (newDebit <= 0) {
            this.isSource = false;
        } else {
            this.isSource = true;
            this.isHole = false;
            this.debit = newDebit;
            this.count = 0;
            this.isWall = false;
        }
    }

    public void setSourceType(String newType) {

        this.sourceType = newType;
        if (this.isSource) {

            switch (newType) {
                case "typeA":
                    this.color = Color.red;
                    break;
                case "typeB":
                    this.color = Color.green;
                    break;
                case "typeC":
                    this.color = Color.blue;
                    break;
                default:
                    this.color = Color.gray;
            }
        }
    }

    public boolean isSource() {
        return this.isSource;
    }

    /**
     * Set the amount of particles destroyed at each step. If n == -1, the hole
     * destroys everything at once. If n > 0, the hole destroys that amount of
     * particles at most, every step.
     *
     * @param n
     */
    public void setHole(double n) {
        if (n == 0 || n <= -2) {
            this.isHole = false;
        } else {
            this.isHole = true;
            this.isSource = false;
            this.debit = n;
            this.count = 0;
            this.isWall = false;
        }
    }

    public boolean isHole() {
        return this.isHole;
    }

    // TODO: the features Wall, Source, Hole must be replaced by subclasses of Square.
    public void setWall() {
        this.isWall = true;
        this.isSource = false;
        this.isHole = false;
    }

    public boolean isWall() {
        return this.isWall;
    }

    public void setEmpty() {
        this.isSource = false;
        this.isHole = false;
        this.debit = 0;
        this.count = 0;
        this.isWall = false;
        this.color = defaultColor;
    }

    /**
     * The square is responsible for changing the location of its own particles.
     * When a particle moves too far away, the square sends it to the neighbor
     * square.
     */
    public void moveContent(double dt, ArrayList<Particle> list) {
        for (int i = this.particleList.size() - 1; i >= 0; i--) {
            Particle p = this.particleList.get(i);
            p.move(dt);
            if (p.isMovementAllowed()) {
                boolean transferNecessary = false;
                if (p.getX() > this.xCenter + this.size / 2) {
                    p.setColumnNum(p.getColomnNum() + 1);
                    transferNecessary = true;
                }
                if (p.getX() < this.xCenter - this.size / 2) {
                    p.setColumnNum(p.getColomnNum() - 1);
                    transferNecessary = true;
                }
                if (p.getY() > this.yCenter + this.size / 2) {
                    p.setLineNum(p.getLineNum() - 1);
                    transferNecessary = true;
                }
                if (p.getY() < this.yCenter - this.size / 2) {
                    p.setLineNum(p.getLineNum() + 1);
                    transferNecessary = true;
                }

                if (transferNecessary) {
                    list.add(this.particleList.remove(i));
                }
            }
        }
    }

    /**
     * Remove all particles in this square.
     */
    public void empty() {
        this.particleList = new ArrayList<>(0);
    }

    /**
     * Detects whether the square is included in a given rectangle.
     */
    public boolean isIncludedInRectangle(double xG, double xD, double yB, double yH) {
        double d = this.size / 2;
        return (this.xCenter - d >= xG && this.xCenter + d <= xD && this.yCenter - d >= yB && this.yCenter + d <= yH);
    }

    public double getKineticEnergy() {
        double e = 0;
        for (int i = 0; i < this.particleList.size(); i++) {
            Particle p = this.particleList.get(i);
            e = e + p.getKineticEnergy();
        }
        return e;
    }

    public double getPotentialEnergy(double g) {
        double e = 0;
        for (int i = 0; i < this.particleList.size(); i++) {
            Particle p = this.particleList.get(i);
            e = e + p.getPotentialEnergy(g);
        }
        return e;
    }

    /**
     * Determine whether the given point is located inside at least one selected
     * particle.
     */
    public boolean pointAppartientASelection(double x, double y) {
        boolean res = false;
        int i = 0;
        while (res == false && i < this.particleList.size()) {
            Particle p = this.particleList.get(i);
            if (p.isSelected()) {
                if (p.containsPoint(new Vecteur(x, y))) {
                    res = true;
                }
            }
            i++;
        }
        return res;
    }

    public void moveSelectedContent(double dx, double dy) {
        for (Particle p : this.particleList) {
            if (p.isSelected()) {
                p.move(dx, dy);
            }
        }
    }

    public void cancelSpeedOfSelection() {
        for (Particle p : this.particleList) {
            if (p.isSelected()) {
                p.resetSpeed();
            }
        }
    }

    public void updateParticleRadii() {
        for (Particle p : this.particleList) {
            p.updateRadius();
        }
    }

    public void increaseParticleRadii() {
        for (Particle p : this.particleList) {
            p.increaseRadius();
        }
    }

    public void decreaseParticleRadii() {
        for (Particle p : this.particleList) {
            p.decreaseRadius();
        }
    }

    /**
     * Select all particles contained in this square.
     */
    public void selectEverything() {
        for (Particle p : this.particleList) {
            p.setSelected(true);
        }
    }

    /**
     * Sets to zero the speed of each particle.
     */
    public void blockSpeeds() {
        for (Particle p : this.particleList) {
            p.resetSpeed();
        }
    }

    /**
     * Artificial terminal velocity for all particles in this square.
     */
    public void influenceMeanSpeed(double terminalVx, double terminalVy) {
        // Every step brings the particle's speed 10% closer to the limit.
        double factor = 0.1;
        for (Particle p : this.particleList) {
            p.setVx((1 - factor) * p.getVx() + factor * terminalVx);
            p.setVy((1 - factor) * p.getVy() + factor * terminalVy);
        }
    }

    public void setMustReact(boolean newMustReact) {
        mustReact = newMustReact;
    }

    public void computeMass() {
        mass = 0;
        for (Particle p : particleList) {
            mass += p.getMass();
        }
    }

    /**
     * Apply to all particles of this square the force of gravity coming from
     * other paticles in the given square.
     *
     */
    public void applyGravity(double dt, Square other) {
        for (Particle p : particleList) {
            for (Particle otherP : other.particleList) {
                p.pullWithGravity(otherP, dt);
            }
        }
    }

    /**
     * Apply to all particles of this square the force of gravity coming from
     * all other particles of the same square.
     *
     */
    public void applyGravity(double dt) {
        for (int i = 0; i < particleList.size(); i++) {
            for (int j = i + 1; j < particleList.size(); j++) {
                // Change the speed of both particles i and j.
                Particle pi = particleList.get(i);
                Particle pj = particleList.get(j);
                pi.pullWithGravity(pj, dt);
            }
        }
    }

    /**
     * Increase or decrease the amount of resources available on the terrain for
     * the particles to feed.
     *
     * @param mustIncrease true to increase the amount of resources, false to
     * decrease it.
     */
    public void increaseResources(boolean mustIncrease) {
        int increment = 1;// The amount of foods that is added or removed each time.
        for (int i = 0; i < increment; i++) {
            if (mustIncrease) {
                double x = xCenter + size * (new Random().nextDouble() - 0.5);
                double y = yCenter + size * (new Random().nextDouble() - 0.5);
                foodList.add(new Food(x, y));
            } else {
                // Delete food
                if (foodList.size() > 0) {
                    int index = new Random().nextInt();
                    foodList.remove(index);
                }
            }
        }
    }

    protected double getMaxPressure() {
        double maxPressure = 0;
        for (Particle p : particleList) {
            if (p.getPressure() > maxPressure) {
                maxPressure = p.getPressure();
            }
        }
        return maxPressure;
    }

    double getMaxKineticEnergy() {
        double max = 0;
        for (Particle p : particleList) {
            double kE = p.getKineticEnergy();
            if (kE > max) {
                max = kE;
            }
        }
        return max;
    }

}
