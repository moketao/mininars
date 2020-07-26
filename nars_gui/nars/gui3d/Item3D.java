package nars.gui3d;

import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;
import com.jme3.scene.Node;
import nars.entity.Item;

import java.util.HashMap;

@Serializable
public class Item3D extends AbstractMessage {
    public boolean hasInit = false;
    public Node geo;
    public HashMap<String, Float> valForHeight = new HashMap<>();
}
