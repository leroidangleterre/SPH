/* Vecteur à deux composante. */

public class Vecteur {

    private double x, y;

    /* Construire u vecteur nul. */
    public Vecteur() {
        this.x = 0;
        this.y = 0;
    }

    /* Construire un vecteur avec des coordonnées spécifiées. */
    public Vecteur(double xParam, double yParam) {
        this.x = xParam;
        this.y = yParam;
    }

    public Vecteur(Vecteur v) {
        this.x = v.x;
        this.y = v.y;
    }

    /* Obtenir une copie indépendante de ce vecteur. */
    public Vecteur clone() {
        return new Vecteur(this.x, this.y);
    }

    /* Extraire les composantes. */
    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    /* Régler les composantes. */
    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    /* Comparer deux vecteurs. */
    public boolean equals(Vecteur v) {
        return this.x == v.x && this.y == v.y;
    }

    /* Savoir si un vecteur est nul. */
    public boolean estNul() {
        return this.x == 0 && this.y == 0;
    }

    /* Produit scalaire par un autre vecteur. */
    public double dot(Vecteur v) {
        return this.x * v.x + this.y * v.y;
    }

    /* Multiplication par un réel. */
    public Vecteur mult(double r) {
        return new Vecteur(r * this.x, r * this.y);
    }

    /*
	 * Somme.
	 * Renvoie un nouveau vecteur, résultat de l'opération this+v;
     */
    public Vecteur sum(Vecteur v) {
        return new Vecteur(this.x + v.x, this.y + v.y);
    }

    public void increment(double dx, double dy) {
        this.x += dx;
        this.y += dy;
    }

    /*
	 * Différence.
	 * Renvoie un nouveau vecteur, résultat de l'opération this - v;
     */
    public Vecteur diff(Vecteur v) {
        return new Vecteur(this.x - v.x, this.y - v.y);
    }

    /* Calculer la norme du vecteur. */
    public double norme() {
        return Math.sqrt(this.x * this.x + this.y * this.y);
    }

    /**
     * Construire un nouveau vecteur colineaire au vecteur initial, mais de
     * norme 1; renvoie un vecteur nul si le vecteur initial est nul.
     */
    public Vecteur normer() {
        double n = this.norme();
        if (n == 0) {
            return new Vecteur();
        } else {
            return new Vecteur(this.x / n, this.y / n);
        }
    }

    /*
	 * Obtenir un vecteur v orthogonal au vecteur initial u,
	 * tel que l'angle (u, v) soit direct (donc égal à +Pi/4).
     */
    public Vecteur getOrtho() {
        return new Vecteur(-this.y, +this.x);
    }

    public String toString() {
        return "{" + this.x + ", " + this.y + "}";
    }
}
