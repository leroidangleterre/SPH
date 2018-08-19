
/*
 * This class describes a rectangular object that collides with the particles.
 * Initially, it will be fixed and axis-aligned.
 */

import java.awt.Color;
import java.awt.Graphics;

public class Rectangle{

	private double xCenter, yCenter;
	private double l, h;
	private double ratio; // Height divided by width.
	private double elasticity;

	public Rectangle(double x, double y, double l, double h){
		this.xCenter = x;
		this.yCenter = y;
		this.l = l;
		this.h = h;
		this.elasticity = 0.01;
		if (this.l > 0){
			this.ratio = this.h / this.l;
		}else{
			this.ratio = 0;
		}
		// System.out.println("new Rectangle("+x+", "+y+", "+l+", "+h+");");
	}

	public void display(Graphics g, double x0, double y0, double zoom, int hauteurPanneau){

		int xApp = (int)(this.xCenter * zoom + x0);
		int yApp = (int)(hauteurPanneau - (this.yCenter * zoom + y0));
		int largeurApp = (int)(this.l * zoom);
		int hauteurApp = (int)(this.h * zoom);

		g.setColor(Color.gray);
		g.fillRect(xApp - largeurApp / 2, yApp - hauteurApp / 2, largeurApp, hauteurApp);
	}

	public boolean containsPoint(double x, double y){
		return (x > this.xCenter - this.l / 2 && x < this.xCenter + this.l / 2 && y > this.yCenter - this.h / 2 && y < this.yCenter + this.h / 2);
	}

	public void actOnParticle(Particle p){

		// System.out.println("Rectangle.agirSurParticule");

		/* On exprime les coordonnées de la particule dans le repère centré sur le centre du rectangle. */
		double xP = p.getX() - this.xCenter;
		double yP = p.getY() - this.yCenter;
		double vx = p.getVx();
		double vy = p.getVy();

		double profondeur; /*
							 * Cette valeur désigne la distance entre la particule et le bord du rectangle
							 * correspondant au quart contenant la particule.
							 */

		/*
		 * Le rectangle est virtuellement divisé en 4 secteurs (haut, bas, gauche et droite) par ses deux diagonales.
		 * 
		 * Le rectangle n'agit sur la particule que si cette dernière est incluse dans le rectangle.
		 * 
		 * Chaque secteur fait rebondir la particule si la composante adéquate de la vitesse de celle-ci
		 * n'a pas le bon sens.
		 * Par exemple, une particule située dans le quart haut et dont la vitesse verticale est négative
		 * sera affectée par le rebond; après ce rebond, sa vitesse verticale sera positive.
		 * D'autre part, on applique à la particule une force proportionnelle à la distance qui la sépare du bord.
		 */

		if (this.containsPoint(p.getX(), p.getY())){

			/* On compare x*ratio et y pour savoir dans quel quart la particule est située. */
			if (xP * this.ratio > Math.abs(yP)){
				/* Quart droit. */
				/* Rebond. */
				if (vx < 0){
					p.setVx(-vx);
				}
				/*
				 * Force: plus la particule est enfoncée profondément (càd loin du bord),
				 * plus elle sera repoussée violemment.
				 */
				profondeur = (this.l / 2 - xP);
				p.increaseVx(this.elasticity * profondeur);
			}else if (-xP * this.ratio > Math.abs(yP)){
				/* Quart gauche. */
				/* Rebond. */
				if (vx > 0){
					p.setVx(-vx);
				}
				/* Force. */
				profondeur = -(this.l / 2 - xP);
				p.increaseVx(this.elasticity * profondeur);
			}else if (yP > Math.abs(xP * this.ratio)){
				/* Quart haut. */
				/* Rebond. */
				if (vy < 0){
					p.setVy(-vy);
				}
				/* Force. */
				profondeur = (this.h / 2 - xP);
				p.increaseVy(this.elasticity * profondeur);
			}else{
				/* Quart bas. */
				if (vy > 0){
					p.setVy(-vy);
				}
				/* Force. */
				profondeur = -(this.h / 2 - xP);
				p.increaseVy(this.elasticity * profondeur);
			}
		}
	}
}