package nars.gui3d;

import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;

public class Link3D extends Node {

    public final LineControl ctrl;
    public Item3D pushItem;
    public Item3D targetItem;

    public Link3D(Material mat , Item3D pushItem, Item3D targetItem, float lineWidth){
        this.pushItem = pushItem;
        this.targetItem = targetItem;
        Vector3f startPos = pushItem.geo.getLocalTranslation();
        Vector3f endPos = targetItem.geo.getLocalTranslation();
        Geometry geo = new Geometry("lineGeo",new Quad(1,1));
        geo.setMaterial(mat);
        LineControl lineControl = new LineControl(new LineControl.Algo2CamPosBBNormalized(), true);
        geo.addControl(lineControl);
        Vector3f v0 = new Vector3f(); v0.set(startPos);
        Vector3f v1 = new Vector3f(); v1.set(endPos);
        MiniUtil.toNormalColor(geo);
        lineControl.addPoint(v0,lineWidth);
        lineControl.addPoint(v1,lineWidth);
        ctrl = lineControl;
        this.attachChild(geo);
        MiniUtil.setQueue(this);
    }
}
