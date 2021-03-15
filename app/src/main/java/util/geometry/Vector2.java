package util.geometry;

import java.io.Serializable;

import static java.lang.Math.atan2;
import static java.lang.Math.sqrt;

public final class Vector2 implements Serializable {

    public static final Vector2 NULL = new Vector2(0, 0);
    public static final Vector2 X = new Vector2(1, 0);
    public static final Vector2 Y = new Vector2(0, 1);
    private static final long serialVersionUID = 1L;
    public final float x;
    public final float y;

    public Vector2(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public Vector2 add(Vector2 a) {
        return new Vector2(x + a.x, y + a.y);
    }

    public Vector2 sub(Vector2 a) {
        return new Vector2(x - a.x, y - a.y);
    }

    public Vector2 neg() {
        return new Vector2(-x, -y);
    }

    public Vector2 scale(float a) {
        return new Vector2(a * x, a * y);
    }

    public float dot(Vector2 a) {
        return x * a.x + y * a.y;
    }

    public float modSquared() {
        return dot(this);
    }

    public float mod() {
        return (float) sqrt(modSquared());
    }

    public Vector2 normalize() {
        return scale(1 / mod());
    }

    public Vector2 rotPlus90() {
        return new Vector2(-y, x);
    }

    public Vector2 rotMinus90() {
        return new Vector2(y, -x);
    }

    public float angle() {
        return (float) atan2(y, x);
    }

    public Vector2 div(float a) {
        return new Vector2(x / a, y / a);
    }

    public Vector2 div(Vector2 a) {
        return new Vector2(x / a.x, y / a.y);
    }

    public Vector2 midpoint(Vector2 a) {
        return new Vector2(this.x + a.x, this.y + a.y).div(2f);
    }

    public float distance(Vector2 a) {
        return (float) Math.sqrt(Math.pow((a.x - this.x), 2) + Math.pow((a.y - this.y), 2));
    }

    /**
     * Returns the angle between the given two points
     */
    public double angleBetween(Vector2 a) {
        Vector2 diff = this.sub(a);
        double angle = Math.atan2(diff.y, diff.x);
        double degrees = 180 * angle / Math.PI;
        return ((360 + Math.round(degrees)) - 90) % 360;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Float.floatToIntBits(x);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Float.floatToIntBits(y);
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
        Vector2 other = (Vector2) obj;
        if (Float.floatToIntBits(x) != Float.floatToIntBits(other.x))
            return false;
        return Float.floatToIntBits(y) == Float.floatToIntBits(other.y);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + x + ", " + y + ")";
    }

}