package nars.gui3d;

import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.font.Rectangle;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.debug.Arrow;
import com.jme3.scene.debug.Grid;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;

public class MiniUtil {
    private static float nodeWidth = 0.5f;
    private static float taskWidth = 0.5f;
    private static RenderQueue.Bucket renderQueue = RenderQueue.Bucket.Transparent;;
    public static Node rootNode;
    private static AssetManager assetManager;

    public static void init(Node root,AssetManager assetManager){
        rootNode = root;
        MiniUtil.assetManager = assetManager;
    }
    public static BitmapText create3dtxt(BitmapFont myFont, String str) {
        BitmapText txt = new BitmapText(myFont, false);
        txt.setBox(new Rectangle(0.0F, 0.0F, 6.0F, 3.0F));
        txt.setQueueBucket(RenderQueue.Bucket.Transparent);
        txt.setSize(0.5F);
        txt.setText(str);
        return txt;
    }

    public static Geometry putShape(Mesh shape, Material mat) {
        Geometry g = new Geometry("shape", shape);
        g.setMaterial(mat);
        rootNode.attachChild(g);
        return g;
    }

    public static Geometry putArrow(Vector3f pos, Vector3f dir, Material mat) {
        Arrow arrow = new Arrow(dir);
        Geometry geometry = putShape(arrow, mat);
        geometry.setLocalTranslation(pos);
        return geometry;
    }

    public static Geometry putGrid(Vector3f pos, Material mat) {
        Geometry geometry = putShape(new Grid(16, 16, 0.2F), mat);
        geometry.center().move(pos);
        return geometry;
    }
    public static Material createMat( ColorRGBA color){
        Material m = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        m.setColor("Color", color);
        m.getAdditionalRenderState().setWireframe(true);
        m.getAdditionalRenderState().setLineWidth(1.0F);
        return m;
    }
    public static Material createPngMat(String pngPath){
        Material m = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        Texture textureNode = assetManager.loadTexture( pngPath );
        m.setTexture("ColorMap",textureNode);
        m.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        return m;
    }

    public static Node create3dObject(String obName, Material matTerm) {
        Quad quad = new Quad(nodeWidth,nodeWidth);
        Geometry g = new Geometry("g", quad);
        g.setMaterial(matTerm);
        g.setQueueBucket(renderQueue);
        Node pivot = new Node(obName);
        pivot.attachChild(g);
        g.setLocalTranslation(nodeWidth*-0.5f,nodeWidth*-0.5f,0);
        return pivot;
    }
}
