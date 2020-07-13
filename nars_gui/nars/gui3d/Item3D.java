package nars.gui3d;

import com.jme3.scene.Geometry;
import nars.entity.Item;

public class Item3D {
    public boolean hasInit = false;
    public Item item;
    public ItemTYPE type;
    public String key;
    public Geometry geo;

    public enum ItemTYPE {
        Concept, Task
    }
}
