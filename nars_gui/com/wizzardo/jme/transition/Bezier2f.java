package com.wizzardo.jme.transition;

import com.jme3.math.Vector2f;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wizzardo on 28/05/17.
 */
public class Bezier2f extends AbstractBezier<Vector2f, Bezier2f> {

    public Bezier2f(List<Vector2f> points) {
        super(points);
    }

    public Bezier2f() {
        this(2);
    }

    public Bezier2f(int size) {
        this(new ArrayList<>(size));
    }

    @Override
    protected Vector2f createEmptyPoint() {
        return new Vector2f();
    }

    @Override
    protected void calculate(Vector2f result, float k, Vector2f point) {
        result.x += k * point.x;
        result.y += k * point.y;
    }

    @Override
    protected void reset(Vector2f into) {
        into.x = 0;
        into.y = 0;
    }

    @Override
    protected float distance(Vector2f point1, Vector2f point2) {
        return point1.distance(point2);
    }

    @Override
    protected Bezier2f create(List<Vector2f> points) {
        return new Bezier2f(points);
    }

    @Override
    protected Vector2f shiftPoint(Vector2f point, Vector2f shift) {
        return point.add(shift);
    }

    @Override
    protected Vector2f turn180(Vector2f point, Vector2f axis) {
        return new Vector2f(axis.x + (axis.x - point.x), axis.y + (axis.y - point.y));
    }

    @Override
    protected Vector2f turn90(Vector2f point, Vector2f axis) {
        return new Vector2f(axis.x + (axis.y - point.y), axis.y - (axis.x - point.x));
    }

    @Override
    protected Vector2f turn270(Vector2f point, Vector2f axis) {
        return new Vector2f(axis.x - (axis.y - point.y), axis.y + (axis.x - point.x));
    }

    @Override
    protected Vector2f mirrorX(Vector2f point, Vector2f axis) {
        return new Vector2f(point.x, axis.y + (axis.y - point.y));
    }

    @Override
    protected Vector2f mirrorY(Vector2f point, Vector2f axis) {
        return new Vector2f(axis.x + (axis.x - point.x), point.y);
    }

    @Override
    protected Vector2f mirrorZ(Vector2f point, Vector2f axis) {
        return new Vector2f(point);
    }
}
