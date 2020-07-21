package nars.gui3d;

import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import nars.entity.Item;
import nars.entity.TruthValue;

import java.util.HashMap;

public class Item3D {
    public boolean hasInit = false;
    public Item item;
    public ItemTYPE type;
    public String key;
    public Node geo;
    public HashMap<String, Float> valForHeight = new HashMap<>();

    public enum ItemTYPE {
        Concept, Task
    }
}
