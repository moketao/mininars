package nars.gui3d;

import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import nars.entity.Item;

public class Item3D {
    public boolean hasInit = false;
    public Item item;
    public ItemTYPE type;
    public String key;
    public Node geo;

    public enum ItemTYPE {
        Concept, Task
    }
}
