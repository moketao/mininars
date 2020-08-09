package com.wizzardo.jme.transition;

import com.jme3.math.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wizzardo on 28/05/17.
 */
public class Bezier3f extends AbstractBezier<Vector3f, Bezier3f> {

    public Bezier3f(List<Vector3f> points) {
        this.points = points;
    }

    public Bezier3f() {
        this(new ArrayList<>(2));
    }

    @Override
    protected Bezier3f create(List<Vector3f> points) {
        return new Bezier3f(points);
    }

    @Override
    protected Vector3f createEmptyPoint() {
        return new Vector3f();
    }

    @Override
    protected void calculate(Vector3f result, float k, Vector3f point) {
        result.x += k * point.x;
        result.y += k * point.y;
        result.z += k * point.z;
    }

    @Override
    protected void reset(Vector3f into) {
        into.x = 0;
        into.y = 0;
        into.z = 0;
    }

    @Override
    protected float distance(Vector3f point1, Vector3f point2) {
        return point1.distance(point2);
    }

    @Override
    protected Vector3f shiftPoint(Vector3f point, Vector3f shift) {
        return point.add(shift);
    }

    @Override
    protected Vector3f turn180(Vector3f point, Vector3f axis) {
        return new Vector3f(axis.x + (axis.x - point.x), axis.y + (axis.y - point.y), axis.z + (axis.z - point.z));
    }

    @Override
    protected Vector3f turn90(Vector3f point, Vector3f axis) {
        return new Vector3f(axis.x + (axis.z - point.z), point.y, axis.z - (axis.x - point.x));
    }

    @Override
    protected Vector3f turn270(Vector3f point, Vector3f axis) {
        return new Vector3f(axis.x - (axis.z - point.z), point.y, axis.z + (axis.x - point.x));
    }

    @Override
    protected Vector3f mirrorX(Vector3f point, Vector3f axis) {
        return new Vector3f(point.x, axis.y + (axis.y - point.y), axis.z + (axis.z - point.z));
    }

    @Override
    protected Vector3f mirrorY(Vector3f point, Vector3f axis) {
        return new Vector3f(axis.x + (axis.x - point.x), point.y, axis.z + (axis.z - point.z));
    }

    @Override
    protected Vector3f mirrorZ(Vector3f point, Vector3f axis) {
        return new Vector3f(axis.x + (axis.x - point.x), axis.y + (axis.y - point.y), point.z);
    }
}
