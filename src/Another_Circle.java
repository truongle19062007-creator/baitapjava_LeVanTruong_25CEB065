public class Another_Circle {
    private double radius;

    public Another_Circle() {
        radius = 1.0;
    }
    public Another_Circle(double radius) {
        this.radius = radius;
    }
    public double getRadius() {
        return radius;
    }
    public void setRadius(double radius) {
        this.radius = radius;
    }
    public double getArea() {
        return radius * radius * Math.PI;
    }
    public double getCircumference() {
        return 2 * radius * Math.PI;
    }

    @Override
    public String toString() {
        return "Another_Circle{" +
                "radius=" + radius +
                '}';
    }
}
