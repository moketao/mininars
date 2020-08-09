package com.wizzardo.jme.transition;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

public interface Interpolation<T> {

    T interpolate(float percent);

    static Interpolation<Vector3f> between(Vector3f a, Vector3f b) {
        Bezier3f curve = new Bezier3f().append(a).append(b);
        Vector3f result = new Vector3f();
        return percent -> curve.getPoint(percent, result);
    }

    static Interpolation<ColorRGBA> between(ColorRGBA a, ColorRGBA b) {
        Quaternion from = new Quaternion(a.r, a.g, a.b, a.a);
        Quaternion to = new Quaternion(b.r, b.g, b.b, b.a);
        Quaternion temp = new Quaternion();
        ColorRGBA result = new ColorRGBA();
        return percent -> {
            temp.slerp(from, to, percent);
            return result.set(temp.getX(), temp.getY(), temp.getZ(), temp.getW());
        };
    }

    static Interpolation<Quaternion> between(Quaternion a, Quaternion b) {
        Quaternion result = new Quaternion();
        return percent -> result.slerp(a, b, percent);
    }
}
