package com.wizzardo.jme.transition;

import com.jme3.math.FastMath;

public interface EasingFunction {
    float ease(float progress);

    float c4 = (2 * FastMath.PI) / 3;
    float c5 = (2 * FastMath.PI) / 4.5f;

    EasingFunction LINEAR = progress -> progress;

    EasingFunction EASE = of(Curves.ease);
    EasingFunction EASE_IN = of(Curves.easeIn);
    EasingFunction EASE_OUT = of(Curves.easeOut);
    EasingFunction EASE_IN_OUT = of(Curves.easeInOut);

    EasingFunction EASE_IN_SINE = of(Curves.easeInSine);
    EasingFunction EASE_IN_CUBIC = of(Curves.easeInCubic);
    EasingFunction EASE_IN_QUINT = of(Curves.easeInQuint);
    EasingFunction EASE_IN_CIRC = of(Curves.easeInCirc);

    EasingFunction EASE_OUT_SINE = of(Curves.easeOutSine);
    EasingFunction EASE_OUT_CUBIC = of(Curves.easeOutCubic);
    EasingFunction EASE_OUT_QUINT = of(Curves.easeOutQuint);
    EasingFunction EASE_OUT_CIRC = of(Curves.easeOutCirc);

    EasingFunction EASE_IN_OUT_SINE = of(Curves.easeInOutSine);
    EasingFunction EASE_IN_OUT_CUBIC = of(Curves.easeInOutCubic);
    EasingFunction EASE_IN_OUT_QUINT = of(Curves.easeInOutQuint);
    EasingFunction EASE_IN_OUT_CIRC = of(Curves.easeInOutCirc);

    EasingFunction EASE_IN_QUAD = of(Curves.easeInQuad);
    EasingFunction EASE_IN_QUART = of(Curves.easeInQuart);
    EasingFunction EASE_IN_EXPO = of(Curves.easeInExpo);
    EasingFunction EASE_IN_BACK = of(Curves.easeInBack);

    EasingFunction EASE_OUT_QUAD = of(Curves.easeOutQuad);
    EasingFunction EASE_OUT_QUART = of(Curves.easeOutQuart);
    EasingFunction EASE_OUT_EXPO = of(Curves.easeOutExpo);
    EasingFunction EASE_OUT_BACK = of(Curves.easeOutBack);

    EasingFunction EASE_IN_OUT_QUAD = of(Curves.easeInOutQuad);
    EasingFunction EASE_IN_OUT_QUART = of(Curves.easeInOutQuart);
    EasingFunction EASE_IN_OUT_EXPO = of(Curves.easeInOutExpo);
    EasingFunction EASE_IN_OUT_BACK = of(Curves.easeInOutBack);

    EasingFunction EASE_OUT_BOUNCE = progress -> {
        if (progress < (1f / 2.75f))
            return (7.5625f * progress * progress);
        else if (progress < (2f / 2.75f))
            return (7.5625f * (progress -= (1.5f / 2.75f)) * progress + 0.75f);
        else if (progress < (2.5 / 2.75))
            return (7.5625f * (progress -= (2.25f / 2.75f)) * progress + 0.9375f);
        else
            return (7.5625f * (progress -= (2.625f / 2.75f)) * progress + 0.984375f);
    };
    EasingFunction EASE_IN_BOUNCE = progress -> 1 - EASE_OUT_BOUNCE.ease(1 - progress);
    EasingFunction EASE_IN_OUT_BOUNCE = progress -> progress < 0.5 ? (1 - EASE_OUT_BOUNCE.ease(1 - 2 * progress)) / 2 : (1 + EASE_OUT_BOUNCE.ease(2 * progress - 1)) / 2;
    EasingFunction EASE_IN_ELASTIC = x -> x == 0 ? 0 : x == 1 ? 1 : -FastMath.pow(2, 10 * x - 10) * FastMath.sin((x * 10 - 10.75f) * c4);
    EasingFunction EASE_OUT_ELASTIC = x -> x == 0 ? 0 : x == 1 ? 1 : FastMath.pow(2, -10 * x) * FastMath.sin((x * 10 - 0.75f) * c4) + 1;
    EasingFunction EASE_IN_OUT_ELASTIC = x -> x == 0 ? 0 : x == 1 ? -(FastMath.pow(2, 20 * x - 10) * FastMath.sin((20 * x - 11.125f) * c5)) / 2 :
            FastMath.pow(2, -20 * x + 10) * FastMath.sin((20 * x - 11.125f) * c5) / 2 + 1;

    static EasingFunction of(Bezier2f curve) {
        return progress -> curve.getPoint(progress).y;
    }
}
