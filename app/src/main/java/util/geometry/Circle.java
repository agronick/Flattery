package util.geometry;

import java.io.Serializable;

public final class Circle implements Serializable {

    private static final long serialVersionUID = 1L;

    public final Vector2 c;
    public final float r;

    public Circle(Vector2 c, float r) {
        if (!(r > 0)) throw new IllegalArgumentException("Radius must be positive");
        this.c = c;
        this.r = r;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((c == null) ? 0 : c.hashCode());
        long temp;
        temp = Double.doubleToLongBits(r);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Circle other = (Circle) obj;
        if (c == null) {
            if (other.c != null)
                return false;
        } else if (!c.equals(other.c))
            return false;
        return Double.doubleToLongBits(r) == Double.doubleToLongBits(other.r);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(c: " + c + ", r: " + r + ")";
    }

}