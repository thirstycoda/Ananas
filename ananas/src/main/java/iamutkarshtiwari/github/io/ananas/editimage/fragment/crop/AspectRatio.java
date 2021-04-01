package iamutkarshtiwari.github.io.ananas.editimage.fragment.crop;

public class AspectRatio implements java.io.Serializable {
    private final String name;
    private final int x;
    private final int y;
    private final boolean isCircle;

    public AspectRatio(String name) {
        this(name, 0, 0);
    }

    public AspectRatio(String name, int x, int y) { this(name, x, y, false); }

    public AspectRatio(String name, int x, int y, boolean isCircle) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.isCircle = isCircle;
    }

    public String getName() {
        return name;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public boolean isFree() {
        return x == 0 && y == 0;
    }

    public boolean isFitImage() {
        return x == -1 && y == -1;
    }

    public boolean isCircle() {
        return isCircle;
    }
}
