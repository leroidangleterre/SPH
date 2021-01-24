
public class Kernel {

    public static enum KernelTypes {

        LINEAR, CUBIC_SPLINE, GAUSSIAN
    };

    public static KernelTypes currentType = KernelTypes.CUBIC_SPLINE;

    public static double w(double r, double h) {

        double value = 0;

        double q = r / h;

        switch (currentType) {
            case LINEAR:
                if (q <= 1) {
                    value = 1 - q;
                }
                break;
            case CUBIC_SPLINE:

                // Cubic spline kernel, using http://pysph.readthedocs.io/en/latest/reference/kernels.html
                double sigma = 10 / (7 * Math.PI * h * h);

                if (q < 1) {
                    value = sigma * (1 - 1.5 * q * q * (1 - 0.5 * q));
                } else if (q < 2) {
                    value = sigma * 0.25 * (2 - q) * (2 - q) * (2 - q);
                } else {
                    value = 0;
                }
                break;
            default:
                break;
        }
        return value;

    }

    public static Vecteur gradW(double x, double y, double h) {

        double norm = Math.sqrt(x * x + y * y);
        Vecteur result = null;
        switch (currentType) {
            case LINEAR:
                if (norm > 0) {
                    result = new Vecteur(-x / norm, -y / norm);
                } else {
                    result = new Vecteur();
                }
                break;

            case CUBIC_SPLINE:
                // See http://www2.mpia-hd.mpg.de/~dullemon/lectures/fluiddynamics08/chap_10_sph.pdf
                double r = Math.sqrt(x * x + y * y);
                double q = r / h;
                double value;
                double fact = 10 * Math.PI / 7;
                if (q < 1) {
                    value = -3 * q * (1 - 0.75 * q) * h / r;
                } else if (q < 2) {
                    value = -0.75 * (2 - q) * (2 - q) * h / r;
                } else {
                    value = 0;
                }
                result = new Vecteur(x, y).mult(value * fact);
                break;
            default:
                break;
        }
        return result;

    }

    /* Laplacien de cette fonction. */
    public static double laplW(Vecteur v, double h) {

        double x = v.getX();
        double y = v.getY();
        double r = v.norme();

        /* Le laplacien est défini comme la somme des dérivées secondes de W par rapport à chacune des deux variables x et y. */
        double d2Wdx2 = (1 / (h * h)) * (h + 2 * x - 1 / r);
        double d2Wdy2 = (1 / (h * h)) * (h + 2 * y - 1 / r);

        return d2Wdx2 + d2Wdy2;
    }
}
