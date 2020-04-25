
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JPanel;

public class Menu extends JPanel {

    private ArrayList<JButton> buttonList; // For the automated treatment.

    private JButton boutonRectangle; // Pour des
    // rectangles
    // mobiles
    // indépendants des
    // carreaux

    private JButton boutonMur; // Pour des carreaux
    // fixes

    private JButton boutonParticulesCreation;
    private JButton boutonParticulesSel;

    private JButton boutonSelRectangles;

    private JButton boutonSource;
    // private JButton boutonDebitSource;
    private JButton boutonPuits;
    // private JButton boutonDebitPuits;
    private JButton boutonCarreauVide;
    private JButton boutonCarreauxSel;
    private JButton boutonSwitchVitesses;
    private JButton boutonRAZ;
    // private JTextField debitSources;
    // private JTextField debitPuits;
    private JButton boutonRayonPlus;
    private JButton boutonRayonMoins;

    /* Le panneau sur lequel le menu a une influence. */
    private GraphicPanel lePanneau;

    public Menu(GraphicPanel panneau) {
        super();

        // System.out.println("new Menu");
        this.lePanneau = panneau;

        this.boutonRectangle = new JButton("Rectangle");
        this.boutonSelRectangles = new JButton("Select Rectangles");

        this.boutonMur = new JButton("Mur");

        this.boutonParticulesCreation = new JButton("create P.");
        this.boutonParticulesSel = new JButton("Select. P.");

        this.boutonSource = new JButton("Source type A");
        // this.boutonDebitSource = new JButton("debit S=1.0");
        this.lePanneau.setSourceOutflow(0.05);

        this.boutonPuits = new JButton("Puits");
        // this.boutonDebitPuits = new JButton("debit P=1.0");
        this.lePanneau.setHoleInflow(0.1);

        this.boutonCarreauVide = new JButton("Vide");

        this.boutonCarreauxSel = new JButton("Sel. C");

        this.boutonSwitchVitesses = new JButton("vInst");

        this.boutonRAZ = new JButton("RAZ");

        this.boutonRayonPlus = new JButton("rad+");
        this.boutonRayonMoins = new JButton("rad-");
        // this.sourceFlow = new TitledTextField("Source flow");

        this.setLayout(new GridLayout(3, 5));

        this.add(this.boutonRectangle);
        this.add(this.boutonSelRectangles);
        this.add(this.boutonMur);
        this.add(this.boutonParticulesCreation);
        this.add(this.boutonParticulesSel);
        this.add(this.boutonSource);
        // this.add(this.boutonDebitSource);
        this.add(this.boutonPuits);
        // this.add(this.boutonDebitPuits);
        this.add(this.boutonCarreauVide);
        this.add(this.boutonCarreauxSel);
        this.add(this.boutonSwitchVitesses);
        this.add(this.boutonRAZ);
        // this.add(this.debitSources);
        // this.add(this.debitPuits);
        this.add(this.boutonRayonPlus);
        this.add(this.boutonRayonMoins);

        this.buttonList = new ArrayList<JButton>();
        this.buttonList.add(this.boutonCarreauVide);
        this.buttonList.add(this.boutonCarreauxSel);
        this.buttonList.add(this.boutonMur);
        this.buttonList.add(this.boutonParticulesCreation);
        this.buttonList.add(this.boutonParticulesSel);
        this.buttonList.add(this.boutonPuits);
        this.buttonList.add(this.boutonRayonMoins);
        this.buttonList.add(this.boutonRayonPlus);
        this.buttonList.add(this.boutonRAZ);
        this.buttonList.add(this.boutonRectangle);
        this.buttonList.add(this.boutonSelRectangles);
        this.buttonList.add(this.boutonSource);
        this.buttonList.add(this.boutonSwitchVitesses);
        this.reglerTailleTousBoutons();

        this.boutonRectangle.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // lePanneau.setOutilEnCours(Outil.CARREAU_RECTANGLE);
                lePanneau.setCurrentTool(Tool.RECTANGLE);
            }
        });

        this.boutonSelRectangles.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // lePanneau.setOutilEnCours(Outil.CARREAU_RECTANGLE);
                lePanneau.setCurrentTool(Tool.RECTANGLE_SELECTION);
            }
        });

        this.boutonMur.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                lePanneau.setCurrentTool(Tool.WALL_SQUARE);
            }
        });

        this.boutonParticulesCreation.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                lePanneau.setCurrentTool(Tool.PARTICLE_CREATION);
            }
        });
        this.boutonParticulesSel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                lePanneau.setCurrentTool(Tool.PARTICLE_SELECTION);
            }
        });
        this.boutonSource.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                lePanneau.setCurrentTool(Tool.SOURCE_SQUARE);
                lePanneau.toggleParticleType();
                boutonSource.setText("Source " + lePanneau.getParticleType());
            }
        });

        this.boutonPuits.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.out.println("Select puits.");
                lePanneau.setCurrentTool(Tool.HOLE_SQUARE);
            }
        });

        this.boutonCarreauVide.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                lePanneau.setCurrentTool(Tool.EMPTY_SQUARE);
            }
        });
        this.boutonCarreauxSel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                lePanneau.setCurrentTool(Tool.SELECTION_CARREAUX);
            }
        });
        this.boutonSwitchVitesses.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                /*
                 * Affichage des vitesses: on alterne entre vitesse instantanée
                 * et vitesse moyenne.
                 */
                if (boutonSwitchVitesses.getText() == "vInst") {
                    boutonSwitchVitesses.setText("vMoy");
                } else if (boutonSwitchVitesses.getText() == "vMoy") {
                    boutonSwitchVitesses.setText("v");
                } else {
                    boutonSwitchVitesses.setText("vInst");
                }
                lePanneau.setSpeedDisplay(boutonSwitchVitesses.getText());
            }
        });
        this.boutonRAZ.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.out.println("RAZ: TODO (avec demande de confirmation)");
            }
        });
        this.boutonRayonPlus.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                lePanneau.increaseParticleRadii();
            }
        });
        this.boutonRayonMoins.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                lePanneau.decreaseParticleRadii();
            }
        });

    }

    private void reglerTailleTousBoutons() {
        int largeur = 100;
        int hauteur = 50;
        Dimension dim = new Dimension(largeur, hauteur);

        for (JButton b : this.buttonList) {
            this.reglerTailleUnBouton(b, dim);
        }
    }

    private void reglerTailleUnBouton(JButton bouton, Dimension dim) {
        bouton.setMaximumSize(dim);
        bouton.setMinimumSize(dim);
        bouton.setPreferredSize(dim);
    }

    public void addKeyListener(KeyListener k) {

        for (JButton b : this.buttonList) {
            b.addKeyListener(k);
        }
    }
}
