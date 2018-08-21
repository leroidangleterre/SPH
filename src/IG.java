
import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.JFrame;

public class IG {

    private World world;
    private Menu menu;
    private GraphicPanel panel;
    private JFrame window;
    private int initWindowWidth, initWindowHeight;
    private MouseMotionListener mouseMotionListener;
    private MouseClickListener mouseClickListener;
    private MouseWheelMotionListener mouseWheelListener;
    private KeyBoardListener keyboardListener;
    private int xMouse, yMouse;
    private boolean movementOngoing;
    private boolean leftClickActive, mouseWheelClickActive, rightClickActive;

    private Timer evolutionTimer;
    private EvolutionTimerTask evolutionTimerTask;

    // Simulated time of one frame.
    private double worldPeriod;

    // Real-time duration of the timer ticks.
    private long timerPeriod;

    public IG(World wParam) {
        this.world = wParam;
        this.window = new JFrame();
        this.window.setVisible(true);
        this.window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.panel = new GraphicPanel(this.world);

        this.initWindowWidth = 1600;
        this.initWindowHeight = 1000;

        this.mouseMotionListener = new MouseMotionListener(this.panel);
        this.mouseClickListener = new MouseClickListener(this.panel);
        this.mouseWheelListener = new MouseWheelMotionListener(this.panel);
        this.keyboardListener = new KeyBoardListener(this.panel);
        this.panel.addMouseMotionListener(this.mouseMotionListener);
        this.panel.addMouseListener(this.mouseClickListener);
        this.panel.addMouseWheelListener(this.mouseWheelListener);
        this.panel.addKeyListener(this.keyboardListener);
        this.window.addKeyListener(this.keyboardListener);
        this.movementOngoing = false;
        this.leftClickActive = false;
        this.mouseWheelClickActive = false;
        this.rightClickActive = false;
        this.panel.setCurrentTool(Tool.RECTANGLE);
        this.window.setTitle("Rectangle");

        /* Menu. */
        this.menu = new Menu(this.panel);
        this.menu.addKeyListener(this.keyboardListener);

        this.window.setLayout(new BorderLayout());

        this.window.add(this.menu, BorderLayout.SOUTH);
        this.window.add(this.panel, BorderLayout.CENTER);

        // Tab key handling.
        this.window.setFocusTraversalKeysEnabled(false);

        this.window.setSize(initWindowWidth, initWindowHeight);

        Timer displayTimer = new Timer();
        TimerTask displayTask = new TimerTask() {

            @Override
            public void run() {
                window.repaint();
            }
        };
        int displayPeriod = 30;
        displayTimer.schedule(displayTask, 0, displayPeriod);

        this.timerPeriod = 15;
        this.worldPeriod = 0.0003;//0.0010;

        this.resetAndStartTimer(timerPeriod);
    }

    /**
     * Start the timer with the specified period.
     *
     * @param newPeriod the new period of the timer.
     */
    private void resetAndStartTimer(long newPeriod) {
        if (this.evolutionTimer != null) {
            this.evolutionTimer.cancel();
        }
        this.evolutionTimer = new Timer();
        this.evolutionTimerTask = new EvolutionTimerTask(this.world, this.worldPeriod);
        this.evolutionTimer.schedule(this.evolutionTimerTask, 0, this.timerPeriod);
    }

    /*
	 * La classe qui définit l'action à effectuer à chaque fois que le timer
	 * se réveille.
     */
    private class EvolutionTimerTask extends TimerTask {

        private World leTerrain;
        private double periode;
        private long[] tabEvolveTimes = new long[10];
        // private long[] tabRepaintTimes = new long[10];
        private int offset = 0;

        public EvolutionTimerTask(World leTerrain, double periode) {
            this.leTerrain = leTerrain;
            this.periode = periode;
        }

        public /* synchronized */ void run() {
            long dateBegin = System.currentTimeMillis();
            this.leTerrain.evoluerAuto(this.periode);

            double meanEvolveTime, sumEvolveTimes = 0;
            // double meanRepaintTime, sumRepaintTimes = 0;
            for (int i = 0; i < 10; i++) {
                sumEvolveTimes += tabEvolveTimes[i];
                // sumRepaintTimes += tabRepaintTimes[i];
            }
            meanEvolveTime = sumEvolveTimes / 10;
            // meanRepaintTime = sumRepaintTimes / 10;

            // System.out.println("Evolve: " + meanEvolveTime);
            offset++;
            if (offset >= 10) {
                offset = 0;
            }
            long dateEnd = System.currentTimeMillis();
            // System.out.println("compute: " + (dateEnd - dateBegin));
        }
    }

    /*
	 * La classe EcouteurMouvementSouris permet d'actualiser à chaque
	 * déplacement les informations que l'IG possède à propos de la position
	 * de la souris.
     */
    private class MouseMotionListener extends MouseAdapter {

        private GraphicPanel panneau;

        public MouseMotionListener(GraphicPanel p) {
            this.panneau = p;
        }

        @Override
        public void mouseMoved(MouseEvent event) {
            xMouse = event.getX();
            yMouse = event.getY();
            panneau.processMouseMovement(event.getX(), event.getY());
        }

        @Override
        public void mouseDragged(MouseEvent event) {

            if (movementOngoing == true) {
                int dx, dy;

                dx = event.getX() - xMouse;
                dy = -(event.getY() - yMouse);
                panneau.translateHorizontally(dx);
                panneau.translateVertically(dy);

                // System.out.print("MouseDragged without movement from (" + xMouse + ", " + yMouse + ")");
                xMouse = event.getX();
                yMouse = event.getY();
                // System.out.println(" to (" + xMouse + ", " + yMouse + ")");
            } else {
                panneau.processMouseMovement(event.getX(), event.getY());
            }
            panneau.repaint();
        }
    }

    private class MouseClickListener extends MouseAdapter {

        private GraphicPanel panneau;

        public MouseClickListener(GraphicPanel p) {
            this.panneau = p;
        }

        @Override
        public void mousePressed(MouseEvent event) {
            // Clic gauche:
            if (event.getButton() == 1) {
                leftClickActive = true;
                panneau.gestionClicGauche(event.getX(), event.getY());
            }
            // Clic molette:
            if (event.getButton() == 2) {
                mouseWheelClickActive = true;
                movementOngoing = true;
                panneau.gestionClicDroit(event.getX(), event.getY());
            } // Clic droit:
            else if (event.getButton() == 3) {
                rightClickActive = true;
                movementOngoing = true;
                panneau.gestionClicDroit(event.getX(), event.getY());
            }
            panneau.repaint();
        }

        @Override
        public void mouseReleased(MouseEvent event) {
            // Déclic gauche:
            if (event.getButton() == 1) {
                leftClickActive = false;
                panneau.gestionDeclicGauche(event.getX(), event.getY());
            }
            // Déclic molette:
            if (event.getButton() == 2) {
                mouseWheelClickActive = false;
                if (!rightClickActive) {
                    movementOngoing = false;
                }
            } // Déclic droit:
            else if (event.getButton() == 3) {
                rightClickActive = false;
                if (!mouseWheelClickActive) {
                    movementOngoing = false;
                }
            }
            panneau.repaint();
        }
    }

    private class MouseWheelMotionListener implements MouseWheelListener {

        private GraphicPanel panneau;

        public MouseWheelMotionListener(GraphicPanel p) {
            this.panneau = p;
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent ev) {
            panneau.zoom(ev.getX(), ev.getY(), ev.getWheelRotation());
            /*
			 * NB: le paramètre getWheelRotation est positif si on scrolle vers
			 * le haut.
             */
            panneau.repaint();
        }
    }

    private class KeyBoardListener implements KeyListener {

        private GraphicPanel panneau;

        public KeyBoardListener(GraphicPanel p) {
            this.panneau = p;
        }

        @Override
        public void keyPressed(KeyEvent e) {

            switch (e.getKeyCode()) {

            case KeyEvent.VK_A:
                world.selectEverything();
                break;

            case KeyEvent.VK_B:
                world.blockSpeeds();
                break;

            case KeyEvent.VK_S:
                this.panneau.setCurrentTool(Tool.PARTICLE_SELECTION);
                System.out.println("SELECTION_PARTICULES");
                window.setTitle("SELECTION_PARTICULES");
                break;
            case KeyEvent.VK_R:
                this.panneau.setCurrentTool(Tool.RECTANGLE);
                System.out.println("RECTANGLE");
                window.setTitle("RECTANGLE");
                break;
            case KeyEvent.VK_C:
                panneau.setCurrentTool(Tool.SOURCE_SQUARE);
                break;

            /* Switch gravity on or off. */
            case KeyEvent.VK_G:
                world.toggleGravity();
                break;

            case KeyEvent.VK_TAB:
                if (this.panneau.getCurrentTool() == Tool.PARTICLE_SELECTION) {
                    /* Création de carreaux. */
                    this.panneau.setCurrentTool(Tool.CREATION_CARREAUX);
                    System.out.println("CREATION_CARREAUX");
                    window.setTitle("CREATION_CARREAUX");
                }
                if (this.panneau.getCurrentTool() == Tool.SELECTION_CARREAUX) {
                    /* Sélection de carreaux. */
                    this.panneau.setCurrentTool(Tool.SELECTION_CARREAUX);
                    System.out.println("SELECTION_CARREAUX");
                    window.setTitle("SELECTION_CARREAUX");
                }
                if (this.panneau.getCurrentTool() == Tool.SELECTION_CARREAUX) {
                    /*
					 * Création de rectangle (qui interagissent avec les
					 * particules).
                     */ // TODO
                    this.panneau.setCurrentTool(Tool.RECTANGLE);
                    System.out.println("RECTANGLE");
                    window.setTitle("RECTANGLE");
                } else if (this.panneau.getCurrentTool() == Tool.RECTANGLE) {
                    /* Création de particules. */
                    this.panneau.setCurrentTool(Tool.PARTICLE_CREATION);
                    System.out.println("CREATION_PARTICULE");
                    window.setTitle("CREATION_PARTICULE");
                } else if (this.panneau.getCurrentTool() == Tool.PARTICLE_CREATION) {
                    /* Sélection de particules. */
                    this.panneau.setCurrentTool(Tool.PARTICLE_SELECTION);
                    System.out.println("SELECTION_PARTICULES");
                    window.setTitle("SELECTION_PARTICULES");
                }
                break;
            case KeyEvent.VK_CONTROL:
                world.increaseNbCtrlPressed();
                break;
            case KeyEvent.VK_DELETE:
                world.deleteSelection();
                this.panneau.repaint();
                break;
            case KeyEvent.VK_ENTER:
                /* Effectuer un pas d'exécution. */
                world.evoluerManuel(worldPeriod);
                break;
            case KeyEvent.VK_P:
            /* Play/pause. */
            case KeyEvent.VK_SPACE:
                /* Play/pause. */
                System.out.println("Play/Pause");
                world.switchPlayPause();
                break;
            case KeyEvent.VK_Z:
            // On ne break pas, la touche Z sert à faire le zoom
            // automatique.
            case KeyEvent.VK_NUMPAD0:
                this.panneau.zoomAuto();
                break;
            case KeyEvent.VK_ADD:
                timerPeriod += 100;
                System.out.println("timer period: " + timerPeriod + " ms;");
                resetAndStartTimer(timerPeriod);
                break;
            case KeyEvent.VK_SUBTRACT:
                timerPeriod = Math.max(timerPeriod - 100, 100);
                System.out.println("timer period: " + timerPeriod + " ms;");
                resetAndStartTimer(timerPeriod);
                break;
            case KeyEvent.VK_LEFT:
                world.rotateAllRectangles(0.1);

                break;
            case KeyEvent.VK_RIGHT:
                world.rotateAllRectangles(-0.1);
                break;
            default:
            /* Pas de changement. */
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            switch (e.getKeyCode()) {
            case KeyEvent.VK_CONTROL:
                world.decreaseNbCtrlPressed();
                break;
            default:
            /* Pas de changement. */
            }
        }

        @Override
        public void keyTyped(KeyEvent e) {

        }
    }
}
