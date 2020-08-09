package com.wizzardo.jme.transition;

public class Transition<T> {

    protected final Change<T> change;
    protected final float duration;
    protected final Interpolation<T> interpolation;
    protected final EasingFunction easingFunction;

    protected float elapsedTime = 0;
    protected float delay = 0;
    protected OnTransitionStartedListener onStart;
    protected OnTransitionEndedListener onEnd;
    protected boolean reverse;

    public Transition(Change<T> change, float duration, Interpolation<T> interpolation, EasingFunction easingFunction) {
        this.change = change;
        this.duration = duration;
        this.interpolation = interpolation;
        this.easingFunction = easingFunction;
    }

    public void update(float tpf) {
        if (delay > 0) {
            delay -= tpf;
            if (delay < 0)
                tpf = -delay;
            else
                return;
        }

        if (reverse) {
            if (elapsedTime == duration)
                onStart();

            elapsedTime -= tpf;
            if (elapsedTime <= 0) {
                elapsedTime = 0;
                onEnd();
            }
        } else {
            if (elapsedTime == 0)
                onStart();

            elapsedTime += tpf;
            if (elapsedTime >= duration) {
                elapsedTime = duration;
                onEnd();
            }
        }
        change.update(interpolation.interpolate(easingFunction.ease(elapsedTime / duration)));
    }

    protected void onStart() {
        if (onStart != null)
            onStart.onStart();
    }

    protected void onEnd() {
        if (onEnd != null)
            onEnd.onEnd();
    }

    public Transition<T> onStart(OnTransitionStartedListener listener) {
        if (this.onStart == null)
            this.onStart = listener;
        else {
            OnTransitionStartedListener old = this.onStart;
            this.onStart = () -> {
                old.onStart();
                listener.onStart();
            };
        }
        return this;
    }

    public Transition<T> onEnd(OnTransitionEndedListener listener) {
        if (this.onEnd == null)
            this.onEnd = listener;
        else {
            OnTransitionEndedListener old = this.onEnd;
            this.onEnd = () -> {
                old.onEnd();
                listener.onEnd();
            };
        }
        return this;
    }

    public Transition<T> delay(float delay) {
        this.delay = delay;
        return this;
    }

    public Transition<T> reverse(boolean reverse) {
        this.reverse = reverse;
        return this;
    }
}
