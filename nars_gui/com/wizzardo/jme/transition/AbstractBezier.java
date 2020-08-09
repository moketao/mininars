package com.wizzardo.jme.transition;

import com.jme3.math.FastMath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by wizzardo on 28/05/17.
 */
public abstract class AbstractBezier<T, B extends AbstractBezier> {

    protected List<T> points;
    protected float[] factorials;
    protected float length = -1;

    public AbstractBezier(List<T> points) {
        this.points = points;
    }

    public AbstractBezier() {
        this(new ArrayList<>(2));
    }

    public B append(T point) {
        points.add(point);
        length = -1;
        return (B) this;
    }

    public List<T> getPoints() {
        return points;
    }

    /**
     * @return n!/(i!*(n-i)!)
     */
    private static float factorial(int n, int i) {
        if (i == 0 | i == n) {
            return 1;
        }
        if (n - i > i) {
            float k = 1.0f;
            for (int j = n - i + 1; j <= n; j++) {
                k *= j;
                if (i > 1 & k % i == 0) {
                    k = k / i;
                    i--;
                }
            }
            while (i > 1) {
                k = k / i;
                i--;
            }
            return k;
        } else {
            float k = 1.0f;
            int g = n - i;
            for (int j = i + 1; j <= n; j++) {
                k *= j;
                if (g > 1 & k % g == 0) {
                    k = k / g;
                    g--;
                }
            }
            while (g > 1) {
                k = k / g;
                g--;
            }
            return k;
        }
    }

    /**
     * @param t [0,1]
     */
    public T getPoint(float t, float[] factorials) {
        return getPoint(t, factorials, createEmptyPoint());
    }

    /**
     * @param t [0,1]
     */
    public T getPoint(float t, float[] factorials, T into) {
        List<T> points = this.points;
        int n = points.size() - 1;
        float temp = (1.0f - t);
        for (int i = n; i >= 0; i--) {
            float k = FastMath.pow(temp, i) * FastMath.pow(t, n - i) * factorials[i];
            calculate(into, k, points.get(n - i));
        }
        return into;
    }

    protected abstract B create(List<T> points);

    protected abstract T createEmptyPoint();

    protected abstract void calculate(T result, float k, T point);

    protected abstract float distance(T point1, T point2);

    public B reverse() {
        ArrayList<T> points = new ArrayList<>(this.points);
        Collections.reverse(points);
        return create(points);
    }

    public B shift(T shift) {
        return map(point -> shiftPoint(point, shift));
    }

    public B turn90(T axis) {
        return map(point -> turn90(point, axis));
    }

    public B turn180(T axis) {
        return map(point -> turn180(point, axis));
    }

    public B turn270(T axis) {
        return map(point -> turn270(point, axis));
    }

    public B mirrorX(T axis) {
        return map(point -> mirrorX(point, axis));
    }

    public B mirrorY(T axis) {
        return map(point -> mirrorY(point, axis));
    }

    public B mirrorZ(T axis) {
        return map(point -> mirrorZ(point, axis));
    }

    protected B map(Mapper<T, T> mapper) {
        ArrayList<T> points = new ArrayList<>(this.points.size());
        for (T point : this.points) {
            points.add(mapper.map(point));
        }
        return create(points);
    }

    protected abstract T shiftPoint(T point, T shift);

    protected abstract T turn180(T point, T axis);

    protected abstract T turn90(T point, T axis);

    protected abstract T turn270(T point, T axis);

    protected abstract T mirrorX(T point, T axis);

    protected abstract T mirrorY(T point, T axis);

    protected abstract T mirrorZ(T point, T axis);

    /**
     * @param t [0,1]
     */
    public T getPoint(float t) {
        if (factorials == null || factorials.length != points.size())
            factorials = prepareFactorials();

        return getPoint(t, factorials);
    }

    /**
     * @param t [0,1]
     */
    public T getPoint(float t, T into) {
        if (factorials == null || factorials.length != points.size())
            factorials = prepareFactorials();

        reset(into);
        return getPoint(t, factorials, into);
    }

    protected abstract void reset(T into);

    protected float[] prepareFactorials() {
        int n = points.size() - 1;
        float[] floats = new float[n + 1];
        for (int i = 0; i <= n; i++) {
            floats[i] = factorial(n, i);
        }
        return floats;
    }

    public void clear() {
        points.clear();
        factorials = null;
        length = -1;
    }

    public float length() {
        if (length != -1)
            return length;

        float l = 0;
        T prev = getPoint(0);
        for (float i = 0; i < 1; i += 0.01) {
            T next = getPoint(i);
            l += distance(prev, next);
            prev = next;
        }
        length = l;
        return l;
    }

    public interface Mapper<A, B> {
        B map(A a);
    }
}
