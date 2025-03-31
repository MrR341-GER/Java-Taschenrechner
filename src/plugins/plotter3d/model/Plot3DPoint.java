package plugins.plotter3d.model;

/**
 * Repräsentiert einen 3D-Punkt im Raum mit x-, y- und z-Koordinaten.
 * Wird im gesamten 3D-Darstellungssystem verwendet.
 */
public class Plot3DPoint {
    private double x, y, z;

    /**
     * Erzeugt einen neuen 3D-Punkt.
     * 
     * @param x x-Koordinate
     * @param y y-Koordinate
     * @param z z-Koordinate
     */
    public Plot3DPoint(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Erstellt eine Kopie dieses Punktes.
     * 
     * @return Ein neuer Punkt mit denselben Koordinaten.
     */
    public Plot3DPoint copy() {
        return new Plot3DPoint(x, y, z);
    }

    // Getter und Setter

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ", " + z + ")";
    }
}
