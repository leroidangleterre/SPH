// Smoothed Particle Hydrodynamics tool.

public class SPH {

    public static void main(String args[]) {

        System.out.println("Check kernel grad...");
        System.out.println("Square: debug wall collision");
        System.out.println("Add a flag at the end of each iteration so that the next one does not collide.");

        World terrain;
        IG ig;

        int nbLines = 30;
        int nbColumns = 30;
        double squareSize = 2.0;

        terrain = new World(nbLines, nbColumns, squareSize);

        ig = new IG(terrain);

    }
}
