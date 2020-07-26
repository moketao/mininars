package nars.gui3d;

import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;
import com.jme3.scene.Node;

import java.util.HashMap;

@Serializable
public class Item3D extends AbstractMessage {
    public boolean hasInit = false;
    public ItemTYPE type;
    public String key;
    public int geoHashCode;
    public HashMap<String, Float> valForHeight = new HashMap<>();

    public enum ItemTYPE {
        Concept, Task
    }
}
