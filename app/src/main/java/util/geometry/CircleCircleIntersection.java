package util.geometry;

import static java.lang.Math.max;
import static java.lang.Math.sqrt;

// References:
// http://paulbourke.net/geometry/2circle/
// http://mathworld.wolfram.com/Circle-CircleIntersection.html

public final class CircleCircleIntersection {

    public final Circle c1;
    public final Circle c2;
    // Results valid for all intersections:
    public final Type type;
    public final float distanceC1cC2c;
    // Results valid for eccentric circles:
    public final Vector2 radicalPoint;
    public final float distanceC1cRadicalLine;
    public final float distanceC2cRadicalLine;
    public final Vector2 versorC1cC2c;
    public final Vector2 versorRadicalLine;
    // Results valid for tangent circles:
    public final Vector2 intersectionPoint;
    // Results valid for overlapping circles:
    public final Vector2 intersectionPoint1;
    public final Vector2 intersectionPoint2;
    public final float distanceRadicalPointIntersectionPoints;

    public CircleCircleIntersection(Circle c1, Circle c2) {
        this.c1 = c1;
        this.c2 = c2;

        // Vector going from c1 center to c2 center:
        Vector2 vectorC1cC2c = c2.c.sub(c1.c);
        // Distance between circle centers:
        distanceC1cC2c = vectorC1cC2c.mod();

        // If circles are concentric there is no radical line, handle the case here:
        if (distanceC1cC2c == 0) {
            if (c1.r == c2.r) type = Type.COINCIDENT;
            else type = Type.CONCENTRIC_CONTAINED;
            radicalPoint = null;
            distanceC1cRadicalLine = 0;
            distanceC2cRadicalLine = 0;
            versorC1cC2c = null;
            versorRadicalLine = null;
            intersectionPoint = null;
            intersectionPoint1 = null;
            intersectionPoint2 = null;
            distanceRadicalPointIntersectionPoints = 0;
            return;
        }

        // Type determination is simpler if done later.
		/* else
		{
			float rs = c1.r + c2.r;
			if (distanceC1cC2c >= rs) {
				if (distanceC1cC2c == rs) type = Type.EXTERNALLY_TANGENT;
				else type = Type.SEPARATE;
			}
			else
			{
				float rd = abs(c1.r - c2.r);
				if (distanceC1cC2c <= rd) {
					if (distanceC1cC2c == rd) type = Type.INTERNALLY_TANGENT;
					else type = Type.ECCENTRIC_CONTAINED;
				}
				else type = Type.OVERLAPPING;
			}
		} */

        // Direction versor from c1 center to c2 center:
        versorC1cC2c = vectorC1cC2c.scale(1 / distanceC1cC2c);
        // Signed distances from circle centers to radical line (the direction to the other center is positive):
        distanceC1cRadicalLine = (sq(distanceC1cC2c) + sq(c1.r) - sq(c2.r)) / (2 * distanceC1cC2c);
        distanceC2cRadicalLine = distanceC1cC2c - distanceC1cRadicalLine;
        // Intersection between line connecting circle centers and radical line:
        radicalPoint = c1.c.add(versorC1cC2c.scale(distanceC1cRadicalLine));
        // Direction versor of radical line (points to the left if looking from c1 center to c2 center):
        versorRadicalLine = versorC1cC2c.rotPlus90();

        // If type had been determined before:
        //switch (type.getIntersectionPointCount()) { ... }

        // Square of distance between radical point and either intersection point, if circles are overlapping:
        float sqH = sq(c1.r) - sq(distanceC1cRadicalLine);
        if (sqH > 0) {
            type = Type.OVERLAPPING;
            intersectionPoint = null;
            distanceRadicalPointIntersectionPoints = (float) sqrt(sqH);
            intersectionPoint1 = radicalPoint.add(versorRadicalLine.scale(distanceRadicalPointIntersectionPoints));
            intersectionPoint2 = radicalPoint.add(versorRadicalLine.scale(-distanceRadicalPointIntersectionPoints));
        } else {
            boolean external = distanceC1cC2c > max(c1.r, c2.r);
            if (sqH == 0) {
                type = external ? Type.EXTERNALLY_TANGENT : Type.INTERNALLY_TANGENT;
                intersectionPoint = radicalPoint;
            } else {
                type = external ? Type.SEPARATE : Type.ECCENTRIC_CONTAINED;
                intersectionPoint = null;
            }
            intersectionPoint1 = null;
            intersectionPoint2 = null;
            distanceRadicalPointIntersectionPoints = 0;
        }
    }

    // Valid for noncoincident circles:
    public Vector2[] getIntersectionPoints() {
        switch (type.getIntersectionPointCount()) {
            case 0:
                return new Vector2[]{};
            case 1:
                return new Vector2[]{intersectionPoint};
            case 2:
                return new Vector2[]{intersectionPoint1, intersectionPoint2};
            default:
                throw new IllegalStateException("Coincident circles");
        }
    }

    private float sq(float a) {
        return a * a;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
                + "(c1: " + c1
                + ", c2: " + c2
                + ", type: " + type
                + ", distanceC1cC2c: " + distanceC1cC2c
                + ", radicalPoint: " + radicalPoint
                + ", distanceC1cRadicalLine: " + distanceC1cRadicalLine
                + ", distanceC2cRadicalLine: " + distanceC2cRadicalLine
                + ", versorC1cC2c: " + versorC1cC2c
                + ", versorRadicalLine: " + versorRadicalLine
                + ", intersectionPoint: " + intersectionPoint
                + ", intersectionPoint1: " + intersectionPoint1
                + ", intersectionPoint2: " + intersectionPoint2
                + ", distanceRadicalPointIntersectionPoints: "
                + distanceRadicalPointIntersectionPoints
                + ")";
    }

    public enum Type {

        COINCIDENT(-1),
        CONCENTRIC_CONTAINED(0),
        ECCENTRIC_CONTAINED(0),
        INTERNALLY_TANGENT(1),
        OVERLAPPING(2),
        EXTERNALLY_TANGENT(1),
        SEPARATE(0);

        private final int n;

        Type(int n) {
            this.n = n;
        }

        public int getIntersectionPointCount() {
            // Returns -1 if count is infinite (coincident circles).
            return n;
        }

        public boolean isConcentric() {
            return this == COINCIDENT || this == CONCENTRIC_CONTAINED;
        }

        public boolean isContained() {
            return this == CONCENTRIC_CONTAINED || this == ECCENTRIC_CONTAINED;
        }

        public boolean isTangent() {
            return n == 1;
        }

        public boolean isDisjoint() {
            return n == 0;
        }

    }

}