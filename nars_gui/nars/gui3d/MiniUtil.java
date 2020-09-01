package nars.gui3d;

import com.jme3.asset.AssetManager;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.font.Rectangle;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.*;
import com.jme3.scene.debug.Arrow;
import com.jme3.scene.debug.Grid;
import com.jme3.scene.shape.Line;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Command;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.HAlignment;

public class MiniUtil {
    static float[] colorArray = new float[4*4];
    private static float nodeWidth = 0.5f;
    public static Node rootNode;
    private static AssetManager assetManager;

    public static void init(Node root,AssetManager assetManager){
        rootNode = root;
        MiniUtil.assetManager = assetManager;

        int colorIndex = 0;

        for(int i = 0; i < 4; i++){
            colorArray[colorIndex++]= 1f;
            colorArray[colorIndex++]= 1f;
            colorArray[colorIndex++]= 1f;
            colorArray[colorIndex++]= 1f;
        }
    }
    public static BitmapText create3dtxt(BitmapFont myFont, String str) {
        BitmapText txt = new BitmapText(myFont, false);
        txt.setBox(new Rectangle(0.0F, 0.0F, 6.0F, 3.0F));
        setQueue(txt);
        txt.setSize(0.5F);
        txt.setText(str);
        return txt;
    }

    public static void setQueue(Spatial ob) {
        ob.setQueueBucket(RenderQueue.Bucket.Transparent);
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
        return createPngMat(pngPath,false);
    }
    public static Material createPngMat(String pngPath,boolean repeatUV){
        Material m = new Material(assetManager, "Common/MatDefs/Misc/Particle.j3md");
        Texture textureNode = assetManager.loadTexture( pngPath );
        if( repeatUV ){
            textureNode.setWrap(Texture.WrapMode.Repeat);
        }
        m.setTexture("Texture", textureNode);
        m.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        return m;
    }

    public static Node create3dObject(String obName, Material mat) {
        Quad quad = new Quad(nodeWidth,nodeWidth);
        Geometry g = new Geometry("g", quad);
        g.setMaterial(mat);
        toNormalColor(g);
        Node pivot = new Node(obName);
        pivot.attachChild(g);
        g.setLocalTranslation(nodeWidth*-0.5f,nodeWidth*-0.5f,0);
        setQueue(pivot);
        return pivot;
    }

    public static void putLine(float x1, float y1, float z1, float x2, float y2 , float z2, Material mat) {
        Line line = new Line(new Vector3f(x1, y1, z1), new Vector3f(x2, y2, z2));
        Geometry geomLine = new Geometry("Line", line);
        geomLine.setMaterial(mat);
        rootNode.attachChild(geomLine);
    }
    public static ParticleEmitter createEffect(String imgFileName , int imgsX , int imgsY, float liftTime) {
        ParticleEmitter effect;
        effect = new ParticleEmitter("Flame", ParticleMesh.Type.Triangle, 1);
        effect.setStartColor(new ColorRGBA(1f, 1f, 1f, 1f));
        effect.setEndColor(new ColorRGBA(.4f, .22f, .12f, 0f));
        effect.setStartSize(1f);
        effect.setEndSize(1f);
        effect.setParticlesPerSec(0);
        effect.setGravity(0, 0, 0);
        effect.setLowLife(liftTime);
        effect.setHighLife(liftTime);
        effect.setImagesX(imgsX);
        effect.setImagesY(imgsY);
        Material pngMat = MiniUtil.createPngMat(imgFileName);
        effect.setMaterial(pngMat);
        Node node = new Node();
        node.attachChild(effect);
        MiniUtil.setQueue(node);
        rootNode.attachChild(node);
        return effect;
    }

    /** 修正粒子材质默认为红色的问题 */
    public static void toNormalColor(Geometry geo) {
        geo.getMesh().setBuffer(VertexBuffer.Type.Color, 4, colorArray);
    }

    /** 创建按钮 */
    public static void Btn(String title, int fontSize, Container container2dTopLeft , Command command) {
        Button playBtn = new Button(title);
        //playBtn.setTextHAlignment(HAlignment.Center);
        playBtn.setFontSize(fontSize);
        container2dTopLeft.addChild(playBtn);
        playBtn.addClickCommands(command);
    }
}
