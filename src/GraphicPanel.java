
import java.awt.Color;
import java.awt.Graphics;
import javax.swing.JPanel;

public class GraphicPanel extends JPanel {

    private World terrain;

    private double x0, y0, zoom;

    private Tool currentTool;

    public GraphicPanel(World t) {
        this.terrain = t;
        this.x0 = 760;
        this.y0 = 408;
        this.zoom = 34.1;
    }

    public void paintComponent(Graphics g) {
        g.setColor(Color.white);
        g.fillRect(0, 0, this.getWidth(), this.getHeight());
        this.terrain.display(g, this.x0, this.y0, this.zoom, this.getHeight(), this.getWidth());
    }

    public void translateHorizontally(double dx) {
        this.x0 = this.x0 + dx;
        System.out.println("x0 = " + x0);
    }

    public void translateVertically(double dy) {
        this.y0 = this.y0 + dy;
        System.out.println("y0 = " + y0);
    }

    public void zoom(int xc, int yc, int rotation) {

        double factor;
        if (rotation > 0) {
            /* Zoom out. */
            factor = 1 / 1.1;
        } else {
            /* Zoom in. */
            factor = 1.1;
        }

        this.x0 = (this.x0 - (float) xc) * factor + (float) xc;
        this.y0 = (this.y0 - (float) (this.getHeight() - yc)) * factor + (float) (this.getHeight() - yc);

        this.zoom = this.zoom * factor;

        System.out.println("zoom = " + zoom);
    }

    /**
     * Action done when a left click occurs. Usually place or move stuff.
     */
    public void gestionClicGauche(double xClic, double yClic) {

        this.terrain.leftClickAction((xClic - this.x0) / this.zoom, (this.getHeight() - yClic - this.y0) / this.zoom);
    }

    /**
     * Action done when a right click occurs. Usually start panning view.
     */
    public void gestionClicDroit(double xClic, double yClic) {

        // this.terrain.rightClickAction((xClic - this.x0) / this.zoom, (this.getHeight() - yClic - this.y0) / this.zoom);
    }

    /**
     * Action done when the left button is release.
     */
    public void gestionDeclicGauche(double xClic, double yClic) {
        this.terrain.leftReleaseAction((xClic - this.x0) / this.zoom, (this.getHeight() - yClic - this.y0) / this.zoom, this.currentTool);
    }

    /**
     * Action done when the mouse moves.
     */
    public void processMouseMovement(double xClic, double yClic) {
        this.terrain.applyMouseMovement((xClic - this.x0) / this.zoom, (this.getHeight() - yClic - this.y0) / this.zoom);
    }

    public void setCurrentTool(Tool param) {
        this.currentTool = param;
    }

    public Tool getCurrentTool() {
        return this.currentTool;
    }

    /**
     * Automatically set the zoom and scroll to display all the terrain.
     */
    public void zoomAuto() {

        double ratio_ecran = (double) this.getHeight() / (double) this.getWidth();
        double ratio_terrain = this.terrain.getRatio();

        if (ratio_ecran < ratio_terrain) {
            // The screen is wider that the terrain: the terrain is displayed with spare room on the sides.
            this.zoom = this.getHeight() / this.terrain.getHeight();
        } else {
            // The screen is wider that the terrain: the terrain is displayed with spare room on the top and bottom.
            this.zoom = this.getWidth() / this.terrain.getWidth();
        }

        this.x0 = (this.getWidth() / 2) - ((this.terrain.getXMax() + this.terrain.getXMin()) / 2) * this.zoom;
        this.y0 = (this.getHeight() / 2) - ((this.terrain.getYMin() + this.terrain.getYMax()) / 2) * this.zoom;

        this.repaint();
    }

    public void setSourceOutflow(double outflow) {
        this.terrain.setSourceOutflow(outflow);
    }

    public double getSourceOutflow() {
        return this.terrain.getSourceOutflow();
    }

    public void setHoleInflow(double inflow) {
        this.terrain.setHoleInflow(inflow);
    }

    public double getHoleInflow() {
        return this.terrain.getHoleInflow();
    }

    /**
     * Set the speed display.
     */
    public void setSpeedDisplay(String vInst) {
        this.terrain.changerChoixVitesses(vInst);
        this.repaint();
    }

    public World getTerrain() {
        return this.terrain;
    }

    public void increaseParticleRadii() {
        System.out.println("r+");
        terrain.increaseParticleRadii();
    }

    public void decreaseParticleRadii() {
        System.out.println("r-");
        terrain.decreaseParticleRadii();
    }
}
