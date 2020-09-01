package nars.gui3d;

import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;
import com.jme3.scene.Node;
import nars.entity.Item;

import java.util.HashMap;

@Serializable
public class Item3D extends AbstractMessage {
    public boolean hasInit = false; // 是否已经创建了对应的 3d 对象
    public Node geo;
    public HashMap<String, Float> pushMap = new HashMap<>();
}
