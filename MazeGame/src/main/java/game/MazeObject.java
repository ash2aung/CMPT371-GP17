package game;

import java.util.Objects;

/**
 * Represents some entity in the maze, interactable objects (Cat, Cheese, Mouse) will all inherit from
 *      this class. This class will distinguish between wall and path objects using the passable field.
 * member col: this object's current column in the maze
 * member row: this object's current row in the maze
 * member passable: states whether the object can be passed through (Cat, Cheese, Path) or not (Wall)
 * member visible: states whether the object is visible to the player (within or was within range of the mouse)
 */
public class MazeObject {
    protected int col;
    protected int row;
    protected boolean passable = true;
    protected boolean visible = false;

    /**
     * Only used for Paths and Walls
     * Creates an object with only one parameter: passable. For Walls, it's false. For Paths, it's tru
     * The column and row do not need to be saved as neither need to be used for anything for Paths or Walls
     * @param passable States whether the object can be passed through (Path) or not
     */
    public MazeObject(boolean passable) {
        this.passable = passable;
    }

    /**
     * Only used for Paths and Walls after Cat moves off a square
     * Creates an object with 2 parameters: passable (Wall=true, Path=false) and visible (if it was visible to the
     *                                      player before the Cat moved onto that square or not)
     * The column and row do not need to be saved as neither need to be used for anything for Paths or Walls
     * @param passable Whether the player can pass through the object
     * @param visible Whether the object is visible to the player
     */
    public MazeObject(boolean passable, boolean visible) {
        this.passable = passable;
        this.visible = visible;
    }

    /**
     * Constructor to initialize the Walls and Paths in the maze
     * @param col The current object's column
     * @param row The current object's row
     * @param passable Whether the player can pass through this object
     */
    public MazeObject(int col, int row, boolean passable) {
        this.col = col;
        this.row = row;
        this.passable = passable;
    }

    /**
     * Constructor called on by the subclasses (Cat, Cheese, and Mouse)
     * @param col The column of this object
     * @param row The row of this object
     * @param passable Whether this object can be passed through
     * @param visible Whether this object is visible to the player
     */
    public MazeObject(int col, int row, boolean passable, boolean visible) {
        this.col = col;
        this.row = row;
        this.passable = passable;
        this.visible = visible;
    }


    /**
     * Returns this object's column
     * @return col: this object's column
     */
    public int getCol() {
        return this.col;
    }

    /**
     * Returns this object's row
     * @return row: this object's row
     */
    public int getRow() {
        return this.row;
    }

    /**
     * Sets this object's column
     * Used when a new Cheese needs to be placed, when the player (mouse) moves, or when a Cat moves
     * @param col the column of this object's new position
     */
    public void setCol(int col) {
        this.col = col;
    }

    /**
     * Sets this object's row
     * Used when a new Cheese needs to be placed, when the player (mouse) moves, or when a Cat moves
     * @param row the row of this object's new position
     */
    public void setRow(int row) {
        this.row = row;
    }

    /**
     * Gets the passable state of (whether the Mouse can move through) this object
     * @return passable: Whether the Mouse can move into the same space as this object
     */
    public boolean isPassable() {
        return passable;
    }

    /**
     * Sets this object to visible
     * Used when Mouse moves and visibility needs to be updated
     */
    public void setVisible() {
        this.visible = true;
    }

    /**
     * Gets whether this object is visible to the player
     * @return visible: whether this object is visible to the player or not
     */
    public boolean isVisible() {
        return this.visible;
    }


    /**
     * Compares MazeObjects based on fields
     * @param o The object being compared with.
     * @return If the objects have the same field values.
     */
    @Override
    public boolean equals(Object o) {
        if(o == null) {
            return false;
        }
        if(o == this) {
            return true;
        }
        if(!(o instanceof MazeObject)) {
            return false;
        }
        MazeObject mazeObject = (MazeObject) o;

        return this.row == mazeObject.row
                && this.col == mazeObject.col
                && this.passable == mazeObject.passable
                && this.visible == mazeObject.visible;
    }

    /**
     * Creates the hashcode of MazeObjects based on fields.
     * @return Hashcode for object.
     */
    @Override
    public int hashCode() {
        return Objects.hash(this.col, this.row, this.passable, this.visible);
    }

    /**
     * Sets this object to passable by the player
     * Effectively changes this object from a Wall to a Path or vice versa
     * @param passable: Whether the Mouse can pass through this object (Path) or not (Wall)
     */
    public void setPassable(boolean passable) {
        this.passable = passable;
    }
}