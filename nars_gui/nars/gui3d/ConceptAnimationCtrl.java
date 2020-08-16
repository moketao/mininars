package nars.gui3d;

import com.wizzardo.jme.transition.Change;
import com.wizzardo.jme.transition.EasingFunction;
import com.wizzardo.jme.transition.Interpolation;
import com.wizzardo.jme.transition.TransitionControl;

public class ConceptAnimationCtrl extends TransitionControl {
    private Frame3D frame;

    public <T> ConceptAnimationCtrl(Change<T> change, float duration, Interpolation<T> interpolation, EasingFunction easingFunction , Frame3D frame) {
        super(change, duration, interpolation, easingFunction);
        this.frame = frame;
    }
    @Override
    protected void controlUpdate(float tpf) {
        super.controlUpdate(tpf);
        Show3D.app.update3DLink(frame);
    }
}
