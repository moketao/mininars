package nars.gui3d;

import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.font.Rectangle;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Node;
import com.jme3.scene.control.BillboardControl;

public class FloatTxt3d extends Node {
    private final BitmapText floatTxt;
    private Vector3f offset;

    public FloatTxt3d(String str, BitmapFont myFont, Vector3f offset){
        floatTxt = new BitmapText(myFont, false);
        this.offset = offset;
        floatTxt.setBox(new Rectangle(0.0F, 0.0F, 3.0F, 0.17F));
        floatTxt.setQueueBucket(RenderQueue.Bucket.Transparent);
        floatTxt.setText(str);
        floatTxt.setSize(0.17f);
        floatTxt.setAlignment(BitmapFont.Align.Center);
        this.attachChild(floatTxt);
        floatTxt.setLocalTranslation(-1.5f,0,0.2f);
        this.addControl(new BillboardControl());
    }
    public void setText(String str, Node target){
        floatTxt.setText(str);
        follow(target);
    }
    public void follow(Node target){
        Vector3f p = target.getLocalTranslation();
        setLocalTranslation(p.x+offset.x,p.y+offset.y,p.z+offset.z);
    }
}
