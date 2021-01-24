/* Cette énumération définit les noms des différents outils disponibles. */

public enum Tool {
    PARTICLE_SELECTION,
    SELECTION_CARREAUX,
    RECTANGLE,
    RECTANGLE_SELECTION,
    PARTICLE_CREATION,
    CREATION_CARREAUX,
    SOURCE_SQUARE,
    HOLE_SQUARE,
    EMPTY_SQUARE,
    WALL_SQUARE,
    CARREAU_RECTANGLE
};

/*
   SELECTION permet de tracer un cadre rectangulaire et de sélectionner tous
   les éléments qui seront situés dans ce cadre.

   RECTANGLE permet de tracer un objet rectangulaire mobile en translation et en rotation,
   qui interagira avec les autres rectangles et avec les particules.

    RECTANGLE_SELECTION permet de sélectionner puis de déplacer les rectangles.

   MUR permet de créer un carreau fixe qui repousse les particules mais qui n'évolue jamais.

   PARTICULE permet de placer une particule unique en cas de clic-déclic,
   ou un ensemble de particules en cas de clic-drag-déclic.

   SOURCE permet de remplacer les carreaux standard par des carreaux source:
   à chaque étape de calcul, un carreau de ce type va d'une part créer un
   certain nombre de particules, et d'autre part prendre en charge toutes les
   particules exactement comme un carreau standard.

   PUITS permet de remplacer les carreaux standard par des carreaux puits:
   il est caractérisé par une quantité n.

   Si n est nul, rien ne se passe.

   Si n est positif, sa valeur est ajoutée à un compteur et cela fait générer
   une particule à chaque fois que l'on peut décrémenter ce compteur.
   Exemple:
   - si n vaut 3, le carreau absorbe 3 particules à chaque étape.
   - si n vaut 3.5, le carreau absorbe alternativement 3 et 4 sur deux étapes.

   si n est négatif strictement, le carreau absorbe systématiquement toute
   particule qui y pénètre.

   CARREAUVIDE permet d'obtenir un carreau qui n'est ni une source ni un puits.
   -> c'est utile pour effacer une source ou un puits;
   -> cela n'a pas d'effet sur les carreaux vides.
 */
