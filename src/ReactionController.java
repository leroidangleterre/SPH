
import java.util.ArrayList;

/**
 * This class processes the reaction of any two particles. When the particles
 * are of a given type, they may react and become other types of particles.
 *
 * @author arthurmanoha
 */
class ReactionController {

    // The table of reaction
    // First two columns: the two reactants;
    // Third (and optional other) column(s): products of the reaction.
    private ArrayList<ArrayList<String>> reactions;

    public ReactionController() {
        reactions = new ArrayList<>();
        ArrayList newReac = new ArrayList<>();
        // typeA + typeB -> typeC
        newReac.add("typeA");
        newReac.add("typeB");
        newReac.add("typeC");
        reactions.add(newReac);
    }

    /**
     * Transform the particles according to the reaction equations, assuming the
     * two particles are in collision.
     *
     * @param p0 the first colliding particle
     * @param p1 the second colliding particle
     */
    public void processReaction(Particle p0, Particle p1) {
        if ((p0.getType().equals("typeA") && p1.getType().equals("typeB"))
                || (p0.getType().equals("typeB") && p1.getType().equals("typeA"))) {
            // A + B  ->  2C happens.
            p0.setType("typeC");
            p1.setType("typeC");
        }
    }

}
