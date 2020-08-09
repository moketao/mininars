package com.wizzardo.jme.transition;

import com.jme3.math.Vector2f;

public class Curves {

    static Bezier2f ease = easingOf(.25, .1, .25, 1);
    static Bezier2f easeIn = easingOf(.42, 0, 1, 1);
    static Bezier2f easeOut = easingOf(0, 0, .58, 1);
    static Bezier2f easeInOut = easingOf(.42, 0, .58, 1);

    static Bezier2f easeInSine = easingOf(0.47, 0, 0.745, 0.715);
    static Bezier2f easeInCubic = easingOf(0.55, 0.055, 0.675, 0.19);
    static Bezier2f easeInQuint = easingOf(0.755, 0.05, 0.855, 0.06);
    static Bezier2f easeInCirc = easingOf(0.6, 0.04, 0.98, 0.335);

    static Bezier2f easeOutSine = easingOf(0.39, 0.575, 0.565, 1);
    static Bezier2f easeOutCubic = easingOf(0.215, 0.61, 0.355, 1);
    static Bezier2f easeOutQuint = easingOf(0.23, 1, 0.32, 1);
    static Bezier2f easeOutCirc = easingOf(0.075, 0.82, 0.165, 1);

    static Bezier2f easeInOutSine = easingOf(0.445, 0.05, 0.55, 0.95);
    static Bezier2f easeInOutCubic = easingOf(0.645, 0.045, 0.355, 1);
    static Bezier2f easeInOutQuint = easingOf(0.86, 0, 0.07, 1);
    static Bezier2f easeInOutCirc = easingOf(0.785, 0.135, 0.15, 0.86);

    static Bezier2f easeInQuad = easingOf(0.55, 0.085, 0.68, 0.53);
    static Bezier2f easeInQuart = easingOf(0.895, 0.03, 0.685, 0.22);
    static Bezier2f easeInExpo = easingOf(0.95, 0.05, 0.795, 0.035);
    static Bezier2f easeInBack = easingOf(0.6, -0.28, 0.735, 0.045);

    static Bezier2f easeOutQuad = easingOf(0.25, 0.46, 0.45, 0.94);
    static Bezier2f easeOutQuart = easingOf(0.165, 0.84, 0.44, 1);
    static Bezier2f easeOutExpo = easingOf(0.19, 1, 0.22, 1);
    static Bezier2f easeOutBack = easingOf(0.175, 0.885, 0.32, 1.275);

    static Bezier2f easeInOutQuad = easingOf(0.455, 0.03, 0.515, 0.955);
    static Bezier2f easeInOutQuart = easingOf(0.77, 0, 0.175, 1);
    static Bezier2f easeInOutExpo = easingOf(1, 0, 0, 1);
    static Bezier2f easeInOutBack = easingOf(0.68, -0.55, 0.265, 1.55);

    public static Bezier2f easingOf(double x1, double y1, double x2, double y2) {
        return easingOf((float) x1, (float) y1, (float) x2, (float) y2);
    }

    public static Bezier2f easingOf(float x1, float y1, float x2, float y2) {
        return new Bezier2f(4)
                .append(new Vector2f(0, 0))
                .append(new Vector2f(x1, y1))
                .append(new Vector2f(x2, y2))
                .append(new Vector2f(1, 1));
    }
}
