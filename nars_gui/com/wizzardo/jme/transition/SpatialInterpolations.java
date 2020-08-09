package com.wizzardo.jme.transition;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;

public class SpatialInterpolations {
    public static Interpolation<Quaternion> rotateTo(Spatial s, Quaternion to) {
        return Interpolation.between(s.getLocalRotation().clone(), to);
    }

    public static Interpolation<Vector3f> translateTo(Spatial s, Vector3f to) {
        return Interpolation.between(s.getLocalTranslation().clone(), to);
    }

    public static Interpolation<Vector3f> scaleTo(Spatial s, Vector3f to) {
        return Interpolation.between(s.getLocalTranslation().clone(), to);
    }

    public static Interpolation<Vector3f> scaleTo(Spatial s, float to) {
        return Interpolation.between(s.getLocalScale().clone(), new Vector3f(to, to, to));
    }
}
