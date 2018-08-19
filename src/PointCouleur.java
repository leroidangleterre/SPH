import java.awt.Color;

/*
 * Cette classe sert à calculer la couleur des particules en fonction de leur densité.
 * Chaque PointCouleur est défini de la manière suivante:
 * -une densité de référence;
 * -un delta-densité;
 * -une couleur de référence, sous la forme d'un triplet (r, g, b).
 * 
 */

/**
 * This class is a too used to interpolate a color based on the value of a parameter.
 * It is defined with a central value, a width, and a reference color:
 * - when the parameter has this specified value, the reference color is returned;
 * - when the parameter is too far away from this center value, the returned color is black;
 * - between these parameter values, the result color is interpolated.
 */

public class PointCouleur{

	private double d0;
	private double dx;
	/*
	 * La moitié du support de la fonction;
	 * càd que la couleur sera (0, 0, 0) si la densité
	 * appartient à [0, d-dx] ou à [d+dx, 255];
	 * entre ces deux valeurs, la couleur varie linéairement
	 * entre (0, 0, 0) et (r, g, b).
	 */
	private int r0, g0, b0;

	public PointCouleur(double d, double dx, int r, int g, int b){
		this.d0 = d;
		this.dx = dx;
		this.r0 = r;
		this.g0 = g;
		this.b0 = b;
		/* ATTENTION!!! les coefficients r, g, b doivent être compris entre 0 et 255. */
	}

	public Color getColor(double value){

		double dx = value - this.d0;
		int r, g, b;

		if (value < this.d0 - this.dx || value > this.d0 + this.dx){
			r = 0;
			g = 0;
			b = 0;
		}else{
			r = (int)(((this.dx - Math.abs(dx)) * this.r0) / this.dx);
			g = (int)(((this.dx - Math.abs(dx)) * this.g0) / this.dx);
			b = (int)(((this.dx - Math.abs(dx)) * this.b0) / this.dx);
		}
		return new Color(r, g, b);
	}

	// public void appliquerCouleur(Particle p){
	//
	// /*
	// * Calculer les 3 composantes qu'on ajoute à la
	// * couleur de la particule.
	// */
	// int r, g, b;
	// double d = p.getDensity();
	//
	// /* Rouge. */
	// if (d < this.d0 - this.dx || d > this.d0 + this.dx){
	// r = 0;
	// g = 0;
	// b = 0;
	// }else{
	// r = (int)(((this.dx - Math.abs(d - this.d0)) * this.r0) / this.dx);
	// g = (int)(((this.dx - Math.abs(d - this.d0)) * this.g0) / this.dx);
	// b = (int)(((this.dx - Math.abs(d - this.d0)) * this.b0) / this.dx);
	//
	// }
	//
	// p.incrementColor(r, g, b);
	// }
}