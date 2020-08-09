package com.wizzardo.jme.transition;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;

public class SpatialChanges {
    public static Change<Vector3f> location;

    public static Change<Quaternion> rotation(Spatial spatial) {
        return new Rotation(spatial);
    }

    public static Change<Vector3f> translation(Spatial spatial) {
        return new Translation(spatial);
    }

    public static Change<Vector3f> scale(Spatial spatial) {
        return new Scale(spatial);
    }

    public static abstract class AbstractSpatialChange<T> implements Change<T> {
        protected final Spatial spatial;

        public AbstractSpatialChange(Spatial spatial) {
            this.spatial = spatial;
        }
    }

    public static class Rotation extends AbstractSpatialChange<Quaternion> {
        public Rotation(Spatial spatial) {
            super(spatial);
        }

        @Override
        public void update(Quaternion quaternion) {
            spatial.setLocalRotation(quaternion);
        }
    }

    public static class Translation extends AbstractSpatialChange<Vector3f> {
        public Translation(Spatial spatial) {
            super(spatial);
        }

        @Override
        public void update(Vector3f vector3f) {
            spatial.setLocalTranslation(vector3f);
        }
    }

    public static class Scale extends AbstractSpatialChange<Vector3f> {
        public Scale(Spatial spatial) {
            super(spatial);
        }

        @Override
        public void update(Vector3f vector3f) {
            spatial.setLocalScale(vector3f);
        }
    }
}
