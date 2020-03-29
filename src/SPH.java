// Smoothed Particle Hydrodynamics tool.
public class SPH {

    public static void main(String args[]) {

        System.out.println("Check kernel grad...");
        System.out.println("Square: debug wall collision");
        System.out.println("Add a flag at the end of each iteration so that the next one does not collide.");

        World terrain;
        IG ig;

        int nbLines = 40;
        int nbColumns = 350;
        double squareSize = 1.0;

        terrain = new World(nbLines, nbColumns, squareSize);
//
//        terrain.createOneParticle(-2.01, 0.01);
//        terrain.createOneParticle(0.11, 0.01);

        ig = new IG(terrain);

    }
}
