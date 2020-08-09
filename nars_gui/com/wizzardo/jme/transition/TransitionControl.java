package com.wizzardo.jme.transition;

import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;

public class TransitionControl extends AbstractControl {

    protected final Transition transition;

    public TransitionControl(Transition transition) {
        this.transition = transition;
        transition.onEnd(() -> spatial.removeControl(this));
    }

    public <T> TransitionControl(Change<T> change, float duration, Interpolation<T> interpolation, EasingFunction easingFunction) {
        this(new Transition<>(change, duration, interpolation, easingFunction));
    }

    @Override
    protected void controlUpdate(float tpf) {
        transition.update(tpf);
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
    }
}
