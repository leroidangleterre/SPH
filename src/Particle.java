import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Random;

public class Particle{

	// Either rigid bodies or SPH.
	public static boolean rigidCollision = true;

	private Vecteur position;
	private Vecteur speed, vAvg; // Current and average speed
	private ArrayList<Vecteur> speedList; // Last known speeds
	private ArrayList<Vecteur> positionList; // Last known positions
	private int nbPrevPos;
	private boolean selected;
	private double radius, radiusMin, radiusMax;
	private double viscosity;
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

	public Particle(double xParam, double yParam, double rayonParam, int numLigneParam, int numColonneParam){
		this.position = new Vecteur(xParam, yParam);
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
		this.mass = 0.2;// 1.0
		this.viscosity = 1.0;
		this.pressureCst = 10000;
		this.refDensity = 1.0;
		Random r = new Random();
		this.alpha = 10;
		this.color = new Color(r.nextInt(206) + 50, r.nextInt(206) + 50, r.nextInt(206) + 50, this.alpha);
		this.serialNumber = Particle.nbParticlesCreated;
		Particle.nbParticlesCreated++;
		this.speedList = new ArrayList<Vecteur>();
		this.nMoy = 10;
		for(int i = 0; i < nMoy; i++){
			this.speedList.add(new Vecteur());
		}
		this.positionList = new ArrayList<Vecteur>();
		this.nbPrevPos = 0;
		for(int i = 0; i < nbPrevPos; i++){
			this.positionList.add(new Vecteur(this.position));
		}
		this.selected = false;
		this.coloredPointList = new ArrayList<PointCouleur>();

		this.coloredPointList.add(new PointCouleur(1, 1, 255, 0, 0)); // red
		this.coloredPointList.add(new PointCouleur(2, 1, 255, 128, 0)); // orange
		this.coloredPointList.add(new PointCouleur(3, 1, 255, 255, 0)); // yellow
		this.coloredPointList.add(new PointCouleur(4, 1, 230, 230, 230)); // gray
		this.coloredPointList.add(new PointCouleur(5, 1, 0, 255, 255)); // light blue
		this.coloredPointList.add(new PointCouleur(6, 1, 255, 0, 255)); // violet
		this.coloredPointList.add(new PointCouleur(7, 1, 0, 0, 0)); // black
		this.coloredPointList.add(new PointCouleur(8, 1, 128, 128, 128)); // gray

		this.requestedNeighbors = 2;
	}

	public Particle(double xParam, double yParam, double rayonParam, double densiteParam, int numLigneParam, int numColonneParam){
		this(xParam, yParam, rayonParam, numLigneParam, numColonneParam);
		// The mass is set so that the density is correct.
		this.mass = this.radius * this.radius * densiteParam;
	}

	public void setColor(Color c){
		this.color = c.darker();
	}

	public void setSelected(boolean param){
		this.selected = param;
	}

	public boolean isSelected(){
		return this.selected;
	}

	public double getX(){
		return this.position.getX();
	}

	public double getY(){
		return this.position.getY();
	}

	public void setX(double x){
		this.position.setX(x);
	}

	public void setY(double y){
		this.position.setY(y);
	}

	public double getVx(){
		return this.speed.getX();
	}

	public double getVy(){
		return this.speed.getY();
	}

	public void setVx(double vx){
		this.speed.setX(vx);
	}

	public void setVy(double vy){
		this.speed.setY(vy);
	}

	public void setSpeed(Vecteur v){
		this.speed = v.clone();
	}

	public void increaseVx(double dvx){
		this.speed.setX(this.getVx() + dvx);
	}

	public void increaseVy(double dvy){
		this.speed.setY(this.getVy() + dvy);
	}

	public double getKineticEnergy(){ // Ec=(1/2)m*v2
		double v = this.speed.norme();
		return this.mass * v * v;
	}

	public double getPotentialEnergy(double g){ // Ec=m*g*y
		return -this.mass * g * this.position.getY();
	}

	public int getLineNum(){
		return this.numLine;
	}

	public int getColomnNum(){
		return this.numColumn;
	}

	public void setLineNum(int n){
		this.numLine = n;
	}

	public void setColumnNum(int n){
		this.numColumn = n;
	}

	public void setNbNeighbors(int currentNb){
		this.nbNeighbors = currentNb;
	}

	public int getNbNeighbors(){
		return this.nbNeighbors;
	}

	public void incrementNbNeighbors(){
		this.nbNeighbors++;
	}

	/** Adapt the radius so that the amount of neighbors gets closer to the requested amount. */
	public void updateRadius(){
		double fact = 1.05;
		if (this.radius < this.radiusMax){
			if (this.nbNeighbors < this.requestedNeighbors / fact){
				this.radius = this.radius * fact;
				this.mass *= fact * fact;
			}
		}
		if (this.radius > this.radiusMin){
			if (this.nbNeighbors > this.requestedNeighbors * fact){
				this.radius = this.radius / fact;
				this.mass /= fact * fact;
			}
		}
		this.density = this.mass / (this.radius * this.radius);
	}

	/** Take into account another particle for the computation of the density. */
	public void increaseDensity(Particle p){
		double k = Kernel.w(this.getDistance(p), this.radius);
		this.density = this.density + p.mass * k;
	}

	public double getDensity(){
		return this.density;
	}

	/** Compute the pressure. */
	public void computePressure(){
		this.pressure = this.pressureCst * (this.density - this.refDensity);
	}

	/** Get the distance to another particle. */
	public double getDistance(Particle p){
		double dx = p.getX() - this.getX();
		double dy = p.getY() - this.getY();
		return Math.sqrt(dx * dx + dy * dy);
	}

	/** Squared distance to another particle. */
	public double distanceAuCarre(Particle p){
		return (this.getX() - p.getX()) * (this.getX() - p.getX()) + (this.getY() - p.getY()) * (this.getY() - p.getY());
	}

	/** Select the particle if and only if it is located in the specified rectangle. */
	public void select(double yH, double yB, double xG, double xD){
		this.selected = (this.getX() > xG && this.getX() < xD && this.getY() > yB && this.getY() < yH);
	}

	public void resetDensityAndForces(){
		this.density = 0;
		this.force = new Vecteur();
	}

	/** Graphically display the speed. */
	public void displaySpeed(Graphics g, double x0, double y0, double zoom, int panelHeight, double k, String vInst){
		// The Graphics2d is used to display bold lines.
		Graphics2D g2 = (Graphics2D)g;

		// Coordinates are expressed in the referential of the panel.
		int xApp = (int)(this.getX() * zoom + x0);
		int yApp = (int)(panelHeight - (this.getY() * zoom + y0));

		double vx, vy;

		int xAppEnd;
		int yAppEnd;

		if (vInst != "_"){
			if (vInst == "vInst"){
				vx = this.speed.getX();
				vy = this.speed.getY();
			}else if (vInst == "vMoy"){
				this.computeAverageSpeed();
				vx = this.vAvg.getX();
				vy = this.vAvg.getY();
			}else{
				vx = 0;
				vy = 0;
			}

			g2.setColor(Color.blue);
			if (this.selected){
				g2.setStroke(new BasicStroke(3));
			}
			xAppEnd = (int)(xApp + k * zoom * vx);
			yAppEnd = (int)(yApp - k * zoom * vy);
			g2.drawLine(xApp, yApp, xAppEnd, yAppEnd);
			// Reset the stroke to a normal (not bold) one.
			g2.setStroke(new BasicStroke(1));
		}
	}

	public void displayForces(Graphics g, double x0, double y0, double zoom, int panelHeight, double k){

		// The Graphics2d is used to display bold lines.
		Graphics2D g2 = (Graphics2D)g;

		// Coordinates are expressed in the referential of the panel.
		int xApp = (int)(this.getX() * zoom + x0);
		int yApp = (int)(panelHeight - (this.getY() * zoom + y0));
		int xAppEnd, yAppEnd;

		double fx = this.force.getX();
		double fy = this.force.getY();

		g2.setColor(Color.red);
		if (this.selected){
			g2.setStroke(new BasicStroke(3));
		}
		xAppEnd = (int)(xApp + k * zoom * fx);
		yAppEnd = (int)(yApp - k * zoom * fy);
		g2.drawLine(xApp, yApp, xAppEnd, yAppEnd);
		// Reset the stroke to a normal (not bold) one.
		g2.setStroke(new BasicStroke(1));
	}

	public void display(Graphics g, double x0, double y0, double zoom, int panelHeight){

		// Coordinates are expressed in the referential of the panel.
		int xApp = (int)(this.getX() * zoom + x0);
		int yApp = (int)(panelHeight - (this.getY() * zoom + y0));
		int apparentRadius = (int)(this.radius * zoom);

		// The disk that represents the particle.
		this.computeColor();
		g.setColor(this.color);
		g.fillOval(xApp - apparentRadius, yApp - apparentRadius, 2 * apparentRadius, 2 * apparentRadius);

		// Color for the central dot and the border
		if (this.selected){
			g.setColor(Color.blue);
		}else{
			g.setColor(this.color);
		}
		g.fillOval(xApp - apparentRadius / 10, yApp - apparentRadius / 10, apparentRadius / 5, apparentRadius / 5); // central dot
		// Border
		g.setColor(new Color(0, 0, 0, 128));
		g.drawOval(xApp - apparentRadius, yApp - apparentRadius, 2 * apparentRadius, 2 * apparentRadius); // border

		// g.setColor(Color.BLACK);
		// g.drawString(this.nbNeighbors + "", xApp + apparentRadius, yApp);
		// g.drawString(this.force + "", xApp + apparentRadius, yApp + apparentRadius);

		this.displayPreviousPositions(g, x0, y0, zoom, panelHeight);
		// this.displayForce(g, x0, y0, zoom, panelHeight);
	}

	public void displayForce(Graphics g, double x0, double y0, double zoom, int panelHeight){

		double scale = 0.0002 * zoom;

		int xApp = (int)(this.getX() * zoom + x0);
		int yApp = (int)(panelHeight - (this.getY() * zoom + y0));
		int dxApp = (int)(scale * this.force.getX());
		int dyApp = (int)(scale * this.force.getY());
		g.setColor(Color.ORANGE);
		g.drawLine(xApp, yApp, xApp + dxApp, yApp - dyApp);
	}

	public void displayPreviousPositions(Graphics g, double x0, double y0, double zoom, int panelHeight){

		g.setColor(Color.BLACK);
		int apparentRadius = 1;
		for(Vecteur v : this.positionList){
			int xApp = (int)(v.getX() * zoom + x0);
			int yApp = (int)(panelHeight - (v.getY() * zoom + y0));
			g.fillOval(xApp - apparentRadius, yApp - apparentRadius, 2 * apparentRadius + 1, 2 * apparentRadius + 1);
		}
	}

	public String toString(){
		return "p{ n°" + this.serialNumber + "\n   x=" + this.getX() + ",\n   y=" + this.getY() + ",\n   densite=" + this.density + "\n   vx="
				+ this.speed.getX() + "\n   vy=" + this.speed.getY() + "}";
	}

	/** Compute the speed with the forces applied on the particle. */
	public void computeSpeed(double dt, double gravite){

		this.speed.setY(this.speed.getY() + gravite * dt);
		this.speed = this.speed.sum(new Vecteur((dt / this.mass) * this.force.getX(), (dt / this.mass) * this.force.getY()));
	}

	public void resetSpeed(){
		this.speed = new Vecteur();
	}

	/** Move the particle with respect to its current speed. */
	public void move(double dt){

		if (this.position.getX() == Double.NaN || this.position.getY() == Double.NaN){
			System.out.println("NaN particle");
		}

		this.position = new Vecteur(this.position.getX() + this.speed.getX() * dt, this.position.getY() + this.speed.getY() * dt);
		// this.dampenSpeed(0.9999);

		// Update the list of the previous speeds.
		this.speedList.remove(0);
		this.speedList.add(this.speed.clone());

		// // Update the list of the previous positions.
		if (this.nbPrevPos > 0){
			this.positionList.remove(0);
			this.positionList.add(this.position.clone());
		}
	}

	/** Arbitrarily move the speed (independently from its actual speed). */
	public void move(double dx, double dy){
		this.position = this.position.sum(new Vecteur(dx, dy));
	}

	private void computeAverageSpeed(){
		double vx = 0;
		double vy = 0;
		Vecteur v;
		for(int i = 0; i < this.nMoy; i++){
			v = this.speedList.get(i);
			vx = vx + v.getX();
			vy = vy + v.getY();
		}
		if (this.vAvg == null){
			System.out.println("null tototototototo");
		}
		this.vAvg.setX(vx / this.nMoy);
		this.vAvg.setY(vy / this.nMoy);
	}

	/** Dampen the speed of the particle. */
	public void dampenSpeed(double f){
		this.speed = new Vecteur(f * this.speed.getX(), f * this.speed.getY());
	}

	/** Dampen the speed of the particle, with a specific factor on each axis. */
	public void dampenSpeed(double fX, double fY){
		this.speed = new Vecteur(fX * this.speed.getX(), fY * this.speed.getY());
	}

	/** Apply an external force on the particle. */
	public void receiveExternalForce(Vecteur f1){
		this.force = this.force.sum(f1);
		// System.out.println("Force: " + this.force);
	}

	/** Apply on this particle a force that comes from another particle. */
	public void receiveForces(Particle p){
		this.receivePressureForces(p);
		this.receiveViscosityForces(p);

		double k = Kernel.w(this.getDistance(p), this.radius);
		if (k > 0){
			if (this.serialNumber < p.serialNumber){
				this.incrementNbNeighbors();
				p.incrementNbNeighbors();
			}
		}
		// System.out.println("Force: " + this.force);
	}

	/** Receive a pressure force. This method modifies the values on the other particle as well. */
	private void receivePressureForces(Particle p){

		Vecteur diff = p.position.diff(this.position);

		Vecteur uab = p.position.diff(this.position).normer();
		double overlap = Math.max(0, (this.radius + p.radius) - p.getDistance(this));

		if (diff.estNul()){
			// Special case, arbitrary random modification.
			Random random = new Random();
			this.force = this.force.sum(new Vecteur(0 /* r.nextFloat()-0.5 */, random.nextDouble() - 0.5));
		}else if (Particle.rigidCollision && overlap > 0){

			System.out.println("overlap: " + overlap);

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
			if (relativeSpeed < 0){
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

			}else{
				// Particles are going away from each other, or at rest.
				// If the particles are already moving away, the resulting force is less intense or even zero.
				// Vector uy is aligned with the two particles.
				double df = overlap;
				Vecteur repulsionForce = uy.mult(df);
				this.speed.sum(repulsionForce);
				p.speed.diff(repulsionForce);
			}
		}else{

			// SPH MODEL

			Vecteur dF = uab.clone();
			double value = 0;
			// if (overlap > (this.radius + p.radius) / 2){
			// // Contact of the spheres
			// value = -overlap * 10;
			// }else if (overlap > (this.radius + p.radius) / 4){
			// // Each center touches the other sphere
			// value = -overlap * 10;
			// }
			// double ratio = overlap/this.radius;
			if (overlap > 0){
				value = -50 * (1 + 5 * overlap);
			}

			dF = dF.mult(value);
			this.force = this.force.sum(dF);
			p.force = p.force.diff(dF);
		}
	}

	private void receiveViscosityForces(Particle p){
		/*
		 * Vecteur diffV=p.v.diff(this.v); Vecteur diffPos=p.pos.diff(this.pos);
		 * 
		 * double lapl=Kernel.laplW(diffV, this.rayon);
		 * 
		 * Vecteur ajout=diffV.mult(lapl*this.visco*p.masse/p.densite);
		 * 
		 * this.f=this.f.somme(ajout);
		 */
	}

	/**
	 * Compute the color from the density.
	 * We use the color scale and interpolate its values.
	 */
	private void computeColor(){

		this.red = 0;
		this.green = 0;
		this.blue = 0;

		// Use a parameter of the particle to compute its color.
		// Version 1: density

		for(int i = 0; i < this.coloredPointList.size(); i++){
			this.incrementColor(this.coloredPointList.get(i).getColor(this.density));
		}

		this.applyColorCoefficientThreshold();

		this.color = new Color(this.red, this.green, this.blue, this.alpha);
	}

	/** No color coefficient should excess 255 or be negative. */
	private void applyColorCoefficientThreshold(){

		this.red = Math.min(255, Math.max(0, this.red));
		this.green = Math.min(255, Math.max(0, this.green));
		this.blue = Math.min(255, Math.max(0, this.blue));
	}

	/** Add another color to the previously computed one. */
	public void incrementColor(int rp, int gp, int bp){
		this.red = this.red + rp;
		this.green = this.green + gp;
		this.blue = this.blue + bp;
	}

	/** Add another color to the previously computed one. */
	private void incrementColor(Color c){
		this.red += c.getRed();
		this.blue += c.getBlue();
		this.green += c.getGreen();
	}

	/** Determine whether the particle contains a given point. */
	public boolean containsPoint(Vecteur p){
		return this.position.diff(p).norme() < this.radius;
	}

	public void increaseRadius(){
		this.radius *= dRadius;
		this.mass *= dRadius * dRadius;
		System.out.println("r+");
	}

	public void decreaseRadius(){
		this.radius *= 1 / dRadius;
		this.mass *= 1 / (dRadius * dRadius);
	}

	public int getSerialNumber(){
		return this.serialNumber;
	}
}
