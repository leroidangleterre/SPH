
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
public class Square{

    /* Coordonnées du centre du carreau, taille d'un côté. */
    protected double xCenter, yCenter, size;
    private int lineNum, colNum;

    /* Les particules présentes dans le carreau */
    protected ArrayList<Particle> particleList;
    /*
	 * Les particules qui sont sorties du carreau (dont les coordonnées
	 * dépassent les limites) et qui doivent être placées sous le contrôle
	 * de l'un des voisins.
     */
    private ArrayList<Particle> transferList;

    /*
	 * Collisions entre particules: on traite chaque particule d'une part avec
	 * les autres particules du carreau, et d'autre part avec celles des
	 * carreaux "dépendants" (E, SE, S, SO).
     */
    private ArrayList<Square> dependencyList;

    /*
	 * Un carreau doit connaître la liste de ses voisins, notamment pour les
	 * collisions si le carreau est un mur.
     */
    private Square northNeighbor, southNeighbor, eastNeighbor, westNeighbor;

    private Color color;

    private double speedDamping = 1.0;

    /*
	 * Un carreau peut se comporter comme une source, comme un puits, ou comme
	 * un mur.
     */
    private boolean isSource;
    private boolean isHole;
    private double debit;
    private double count;
    private boolean isWall;

    public Square(double x, double y, double taille, int numLigne, int numColonne){
        this.xCenter = x;
        this.yCenter = y;
        this.size = taille;
        this.lineNum = numLigne;
        this.colNum = numColonne;
        this.particleList = new ArrayList<>();
        this.transferList = new ArrayList<>();
        this.dependencyList = new ArrayList<>();
        /* Couleur unique pour tous les carreaux. */
        this.color = Color.gray;
        this.isSource = false;
        this.isHole = false;
        this.isWall = false;

        // System.out.println("new Carreau(" + this.numLigne + ", " + this.numColonne + ");");
    }

    public Square clone(){
        return new Square(this.xCenter, this.yCenter, this.size, this.lineNum, this.colNum);
    }

    /*
	 * Déclarer un nouveau voisin. Attention: ce voisin ne doit pas avoir
	 * déjà été enregistré (sinon BOUM!); Si le paramètre vaut null, on ne
	 * fait rien.
     */
    public void setDependancy(Square v){
        if(v != null){
            if(v.getNumLine() != this.lineNum || v.getNumColumn() != this.colNum){
                this.dependencyList.add(v);
            }
        }
    }

    public void setNeighbor(Square v){
        if(v != null){
            // System.out.println("Carreau: (" + this.numLigne + ", " + this.numColonne + "), voisin: (" + v.numLigne + ", " + v.numColonne + ");");

            /*
			 * Il faut savoir dans quelle direction est le nouveau voisin (N, S,
			 * E ou O).
             */
            if(v.lineNum == this.lineNum && v.colNum == this.colNum - 1){
                // System.out.println("reglage voisin ouest.");
                this.westNeighbor = v;
            } else if(v.lineNum == this.lineNum && v.colNum == this.colNum + 1){
                // System.out.println("reglage voisin est.");
                this.eastNeighbor = v;
            } else if(v.lineNum == this.lineNum - 1 && v.colNum == this.colNum){
                // System.out.println("reglage voisin nord.");
                this.northNeighbor = v;
            } else if(v.lineNum == this.lineNum + 1 && v.colNum == this.colNum){
                // System.out.println("reglage voisin sud.");
                this.southNeighbor = v;
            }
        }
    }

    /* Vérif. */
    public int getNbDependances(){
        return this.dependencyList.size();
    }

    public void printDependencies(){
        System.out.println(this.toString2());
        for(int i = 0; i < this.dependencyList.size(); i++){
            System.out.println("          " + this.dependencyList.get(i).toString2());
        }
    }

    public int getParticlesAmount(){
        if(this.particleList == null){
            return -1;
        } else{
            return this.particleList.size();
        }
    }

    public Particle getParticle(int i){
        return this.particleList.get(i);
    }

    public double getX(){
        return this.xCenter;
    }

    public double getY(){
        return this.yCenter;
    }

    public double getXMin(){
        return this.xCenter - this.size / 2;
    }

    public double getXMax(){
        return this.xCenter + this.size / 2;
    }

    public double getYMin(){
        return this.yCenter - this.size / 2;
    }

    public double getYMax(){
        return this.yCenter + this.size / 2;
    }

    public int getNumLine(){
        return this.lineNum;
    }

    public int getNumColumn(){
        return this.colNum;
    }

    public double getSize(){
        return this.size;
    }

    /*
	 * public void afficher(Graphics g, double x0, double y0, double zoom, int
	 * hauteurPanneau){ this.afficher(g, x0, y0, zoom, hauteurPanneau, true); }
     */

 /*
	 * Paramètres: offset de position et zoom; hauteur du panneau d'affichage;
	 * booléen qui dit si on affiche les vitesses instantanées ou moyennes des
	 * particules. Les vitesses moyennes sont calculées sur les N étapes
	 * précédentes.
     */
    public void displayBackground(Graphics g, double x0, double y0, double zoom, int hauteurPanneau, String vitessesInstantanees){
        /*
		 * On convertit les coordonnées et la taille du carreau pour les avoir
		 * dans le repère lié au panneau.
         */
        int xApp = (int) (this.xCenter * zoom + x0);
        int yApp = (int) (hauteurPanneau - (this.yCenter * zoom + y0));
        int tailleApp = (int) (this.size * zoom);

        g.setColor(this.color);
        g.drawRect(xApp - tailleApp / 2, yApp - tailleApp / 2, tailleApp, tailleApp);

        /*
		 * Si c'est une source, on affiche un fond rouge. Si c'est un puits, on
		 * affiche un fond noir. Si c'est un mur, on affiche un fond vert.
         */
        if(this.isSource){
            g.setColor(Color.red);
            g.fillRect(xApp - tailleApp / 2, yApp - tailleApp / 2, tailleApp, tailleApp);
        } else if(this.isHole){
            g.setColor(Color.black);
            g.fillRect(xApp - tailleApp / 2, yApp - tailleApp / 2, tailleApp, tailleApp);
        } else if(this.isWall){
            g.setColor(Color.green);
            g.fillRect(xApp - tailleApp / 2, yApp - tailleApp / 2, tailleApp, tailleApp);
        }
    }

    /**
     * Display the particles, and optionally the speeds and forces.
     */
    public void displayParticles(Graphics g, double x0, double y0, double zoom, int hauteurPanneau, String vitessesInstantanees){

        for(Particle p : this.particleList){
            p.display(g, x0, y0, zoom, hauteurPanneau);
        }

        for(Particle p : this.particleList){
            p.displaySpeed(g, x0, y0, zoom, hauteurPanneau, 1.0, vitessesInstantanees);
        }

        for(Particle p : this.particleList){
            p.displayForce(g, x0, y0, zoom, hauteurPanneau);
        }
    }

    /* Définir les éléments qui doivent être sélectionnés. */
    public void selectParticles(double yH, double yB, double xG, double xD){
        for(int i = 0; i < this.particleList.size(); i++){
            this.particleList.get(i).select(yH, yB, xG, xD);
        }
    }

    /* Supprimer tous les éléments qui sont sélectionnés. */
    public void deleteSelection(){
        /*
		 * NB: pour éviter d'avoir à modifier l'indice à chaque fois qu'on
		 * supprime un élément, on parcourt la liste en sens inverse.
         */
        for(int i = this.particleList.size() - 1; i >= 0; i--){
            if(this.particleList.get(i).isSelected()){
                this.particleList.remove(i);
            }
        }
    }

    /*
	 * Créer une nouvelle particule à un endroit spécifié. NB: il vaut mieux
	 * être certain que la particule appartienne effectivement au carreau, mais
	 * ce n'est pas rigoureusement obligatoire.
     */
    public void createParticle(double x, double y){
        Particle p = new Particle(x, y, this.size / 4, 3, this.lineNum, this.colNum);
        this.particleList.add(p);
    }

    public void createSeveralParticles(int nb){
        double x, y;
        /*
						 * Les coordonnées des particules seront choisies au
						 * hasard.
         */
        Random gen = new Random();
        for(int i = 0; i < nb; i++){
            x = this.xCenter + (gen.nextDouble() - 0.5) * this.size;
            y = this.yCenter + (gen.nextDouble() - 0.5) * this.size;
            this.createParticle(x, y);
        }
    }

    /*
	 * Supprimer une particule. On supprime brutalement la première particule
	 * de la liste.
     */
    private void supprimerParticule(){
        if(this.particleList.size() > 0){
            this.particleList.remove(0);
        }
    }

    public void deleteManyParticules(int nb){
        for(int i = 0; i < nb; i++){
            this.supprimerParticule();
        }
    }

    /* Recevoir une particule qui existe déjà. */
    public void receiveParticle(Particle p){
        this.particleList.add(p);
    }

    @Override
    public String toString(){
        return "c{x=" + (this.xCenter - this.size / 2) + ".." + (this.xCenter + this.size / 2) + ", y=" + (this.yCenter - this.size / 2) + ".."
                + (this.yCenter + this.size / 2) + "}";
    }

    public String toString2(){
        return "c{ numLigne=" + this.lineNum + ", numColonne=" + this.colNum + "}";
    }

    public double getSurface(){
        return this.size * this.size;
    }

    public void razDensitiesAndForces(){

        /*
		 * On traite toutes les particules de ce carreau, et seulement de ce
		 * carreau.
         */
        for(Particle p : this.particleList){
            p.resetDensityAndForces();
            p.setNbNeighbors(0);
        }
    }

    public void computeDensities(){

        // System.out.println("Carreau.calculerDensites()");

        /*
		 * Pour chaque particule du carreau, on traite toutes celles de toutes les
		 * dependances.
         */
        for(Particle p0 : this.particleList){
            for(Square voisin : this.dependencyList){
                for(Particle p1 : voisin.particleList){

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
        for(int i = 0; i < this.particleList.size(); i++){
            Particle p0 = this.particleList.get(i);
            p0.increaseDensity(p0);
            for(int j = i + 1; j < this.particleList.size(); j++){
                Particle p1 = this.particleList.get(j);
                /* On incrémente les densités de p0 et de p1. */
                p0.increaseDensity(p1);
                p1.increaseDensity(p0);
            }
        }

        // vérif
        // for(Particle p : this.particleList){
        // System.out.println(p.getSerialNumber() + ": " + p.getDensity());
        // }
    }

    public void computePressure(){
        for(int i = 0; i < this.particleList.size(); i++){
            Particle p0 = this.particleList.get(i);
            p0.computePressure();
        }
    }

    /**
     * Compute all forces applied on all the particles that are contained in
     * this square.
     */
    public void computeForces(){
        // The particles of this square are matched against those of all 4 dependencies. */
        for(Particle p0 : this.particleList){
            // Particles of the neighboring squares.
            for(Square voisin : this.dependencyList){
                for(Particle p1 : voisin.particleList){
                    p0.receiveForces(p1);
                    p1.receiveForces(p0);
                }
            }
        }
        // Other particles of the same square.
        for(int i = 0; i < this.particleList.size(); i++){
            Particle p0 = this.particleList.get(i);
            for(int j = i + 1; j < this.particleList.size(); j++){
                Particle p1 = this.particleList.get(j);
                p0.receiveForces(p1);
                p1.receiveForces(p0);
            }
        }
    }

    public void applyFriction(){
        for(Particle p : this.particleList){
            p.dampenSpeed(speedDamping);
        }
    }

    public void computeSpeed(double dt, double gravity){
        for(int i = 0; i < this.particleList.size(); i++){
            this.particleList.get(i).computeSpeed(dt, gravity);
        }
    }

    public void processRectangles(ArrayList<Rectangle> list){
        for(int iRect = 0; iRect < list.size(); iRect++){
            Rectangle r = list.get(iRect);
            for(int iP = 0; iP < this.particleList.size(); iP++){
                Particle p = this.particleList.get(iP);

                r.actOnParticle(p);
            }
        }
    }

    public void processSourcesAndHoles(){

        if(this.isSource){
            this.count = this.count + this.debit;
            this.createSeveralParticles((int) (this.count));
            this.count = this.count - Math.floor(this.count);
        }
        if(this.isHole){
            if(this.debit == -1){
                this.empty();
            } else{
                this.count = this.count + this.debit;
                this.deleteManyParticules((int) (this.count));
                this.count = this.count - Math.floor(this.count);
            }
        }
    }

    public void processWall(){

        /*
		 * Rebond des particules vers l'extérieur, en fonction notamment du
		 * type des carreaux voisins.
         */

 /*
		 * Déterminer dans quel quart du carreau la particule se trouve: nord,
		 * sud, est, ouest. Chaque quart est un triangle formé par un bord du
		 * carré et le centre.
         */

 /*
		 * TODO: trouver la case vide la plus proche (voir fichier TODO annexe).
         */

 /*
		 * if(this.listeParticules.size() > 0){
		 * System.out.println("Mur contient " + this.listeParticules.size() +
		 * " particules."); }
         */
        for(int i = 0; i < this.particleList.size(); i++){
            Particle p = this.particleList.get(i);
            this.processWall(p);
        }
    }

    /**
     * Change the movement of a particle that might collide with a wall.
     */
    private void processWall(Particle p){

        // Coordinates of the particle in the square's reference.
        double x0 = p.getX() - this.xCenter;
        double y0 = p.getY() - this.yCenter;

        // We need to know on which side of the square the particle is located, and whether or not it will bounce.
        if(x0 > y0 && x0 > -y0){
            if(p.getVx() < 0 && this.eastNeighbor != null){
                // Bounce on the east side.
                p.setX(this.xCenter + this.size / 2);
                p.setVx(-p.getVx());
            }
        } else if(y0 > x0 && y0 > -x0){
            if(p.getVy() < 0 && this.northNeighbor != null){
                // Bounce on the north face.
                p.setY(this.yCenter + this.size / 2);
                p.setVy(-p.getVy());
            }
        } else if(y0 < x0 && y0 < -x0){
            if(p.getVy() > 0 && this.southNeighbor != null){
                // Bounce on the south face.
                p.setY(this.yCenter - this.size / 2);
                p.setVy(-p.getVy());
            }
        } else if(y0 < -x0 && y0 > x0){
            if(p.getVx() > 0 && this.westNeighbor != null){
                // Bounce on the west face.
                p.setVx(-p.getVx());
                p.setX(this.xCenter - this.size / 2);
            }
        }
    }

    public void setSource(double n){
        if(n <= 0){
            this.isSource = false;
        } else{
            this.isSource = true;
            this.isHole = false;
            this.debit = n;
            this.count = 0;
            this.isWall = false;
        }
    }

    public boolean isSource(){
        return this.isSource;
    }

    public void setHole(double n){
        if(n == 0 || n <= -2){
            this.isHole = false;
        } else{
            this.isHole = true;
            this.isSource = false;
            this.debit = n;
            this.count = 0;
            this.isWall = false;
        }
    }

    public boolean isHole(){
        return this.isHole;
    }

    // TODO: the features Wall, Source, Hole must be replaced by subclasses of Square.
    public void setWall(){
        this.isWall = true;
        this.isSource = false;
        this.isHole = false;
    }

    public boolean isWall(){
        return this.isWall;
    }

    public void setEmpty(){
        this.isSource = false;
        this.isHole = false;
        this.debit = 0;
        this.count = 0;
        this.isWall = false;
    }

    /**
     * The square is responsible for changing the location of its own particles.
     * When a particle moves too far away, the square sends it to the neighbor
     * square.
     */
    public void moveContent(double dt, ArrayList<Particle> list){
        for(int i = this.particleList.size() - 1; i >= 0; i--){
            Particle p = this.particleList.get(i);
            p.move(dt);
            boolean transferNecessary = false;
            if(p.getX() > this.xCenter + this.size / 2){
                p.setColumnNum(p.getColomnNum() + 1);
                transferNecessary = true;
            }
            if(p.getX() < this.xCenter - this.size / 2){
                p.setColumnNum(p.getColomnNum() - 1);
                transferNecessary = true;
            }
            if(p.getY() > this.yCenter + this.size / 2){
                p.setLineNum(p.getLineNum() - 1);
                transferNecessary = true;
            }
            if(p.getY() < this.yCenter - this.size / 2){
                p.setLineNum(p.getLineNum() + 1);
                transferNecessary = true;
            }

            if(transferNecessary){
                list.add(this.particleList.remove(i));
            }
        }
    }

    /**
     * Remove all particles in this square.
     */
    public void empty(){
        this.particleList = new ArrayList<>(0);
    }

    /**
     * Detects whether the square is included in a given rectangle.
     */
    public boolean isIncludedInRectangle(double xG, double xD, double yB, double yH){
        double d = this.size / 2;
        return (this.xCenter - d >= xG && this.xCenter + d <= xD && this.yCenter - d >= yB && this.yCenter + d <= yH);
    }

    public double getKineticEnergy(){
        double e = 0;
        for(int i = 0; i < this.particleList.size(); i++){
            Particle p = this.particleList.get(i);
            e = e + p.getKineticEnergy();
        }
        return e;
    }

    public double getPotentialEnergy(double g){
        double e = 0;
        for(int i = 0; i < this.particleList.size(); i++){
            Particle p = this.particleList.get(i);
            e = e + p.getPotentialEnergy(g);
        }
        return e;
    }

    /*
	 * public void setChoixVitesses(String affVInst){ this.affVInst=affVInst;
	 *
	 * for(int i=0; i<this.listeParticules.size(); i++){
	 * this.listeParticules.get(i).setChoixVitesses(affVInst); }
	 *
	 * }
     */
    /**
     * Determine whether the given point is located inside at least one selected
     * particle.
     */
    public boolean pointAppartientASelection(double x, double y){
        boolean res = false;
        int i = 0;
        while(res == false && i < this.particleList.size()){
            Particle p = this.particleList.get(i);
            if(p.isSelected()){
                if(p.containsPoint(new Vecteur(x, y))){
                    res = true;
                }
            }
            i++;
        }
        return res;
    }

    public void moveSelectedContent(double dx, double dy){
        for(Particle p : this.particleList){
            if(p.isSelected()){
                p.move(dx, dy);
            }
        }
    }

    public void cancelSpeedOfSelection(){
        for(Particle p : this.particleList){
            if(p.isSelected()){
                p.resetSpeed();
            }
        }
    }

    public void updateParticleRadii(){
        for(Particle p : this.particleList){
            p.updateRadius();
        }
    }

    public void increaseParticleRadii(){
        for(Particle p : this.particleList){
            p.increaseRadius();
        }
    }

    public void decreaseParticleRadii(){
        for(Particle p : this.particleList){
            p.decreaseRadius();
        }
    }

    /**
     * Select all particles contained in this square.
     */
    public void selectEverything(){
        for(Particle p : this.particleList){
            p.setSelected(true);
        }
    }

    /**
     * Sets to zero the speed of each particle.
     */
    public void blockSpeeds(){
        for(Particle p : this.particleList){
            p.resetSpeed();
        }
    }

    /**
     * Artificial terminal velocity for all particles in this square.
     */
    public void influenceMeanSpeed(double terminalVx, double terminalVy){
        // Every step brings the particle's speed 10% closer to the limit.
        double factor = 0.1;
        for(Particle p : this.particleList){
            p.setVx((1 - factor) * p.getVx() + factor * terminalVx);
            p.setVy((1 - factor) * p.getVy() + factor * terminalVy);
        }
    }
}
