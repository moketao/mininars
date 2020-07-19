package nars.gui3d;

import com.jme3.app.*;
import com.jme3.app.state.AppState;
import com.jme3.app.state.ConstantVerifierState;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.font.Rectangle;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.control.BillboardControl;
import com.jme3.scene.debug.Arrow;
import com.jme3.scene.debug.Grid;
import com.jme3.scene.debug.WireBox;
import com.jme3.scene.shape.Line;
import com.jme3.scene.shape.Quad;
import com.jme3.system.AppSettings;
import com.jme3.texture.Texture;
import com.simsilica.lemur.*;
import com.simsilica.lemur.style.BaseStyles;
import nars.entity.Concept;
import nars.entity.Item;
import nars.entity.Task;

import java.util.ArrayList;
import java.util.HashMap;

public class Show3D extends SimpleApplication{
    public static final String INSERT_CONCEPT = "insert concept"; //生成概念/插入概念
    public static final String INSERT_TASK = "insert task"; //生成任务/插入任务
    public static final String DERIVED = "derived task"; //衍生任务/分解任务
    private static FlyCamAppState flyCamAppState;
    private Material mat;
    private String txtB = "WASD移动摄像机";
    private BitmapFont myFont;
    private HashMap<Integer, Item3D> map = new HashMap<>();
    private ArrayList<Frame3D> list = new ArrayList<>();
    private ArrayList<Frame3D> frameQueue = new ArrayList<>();
    private boolean showInfo = false;
    private Material matNode1;
    private float nodeWidth = 0.2f;
    private boolean online = true;
    private ArrayList<Item3D> willRemove = new ArrayList<>();

    Show3D(AppState... initialStates){
        super(initialStates);
    }
    static Show3D app;
    public static void main(String[] args) {
        flyCamAppState = new FlyCamAppState();
        app = new Show3D(new StatsAppState(), flyCamAppState ,new DebugKeysAppState(), new ConstantVerifierState());

        AppSettings setting= new AppSettings(true);
        setting.setResizable(true);
        setting.setTitle("3d Win");
        app.setSettings(setting);
        app.showSettings = false;

        app.start();
    }

    public static Show3D inst() {
        return app;
    }

    @Override
    public void simpleInitApp() {
        initRes();
        init3D();
        initGUI();
        toggleInfo();
    }

    private void initRes() {
        myFont = assetManager.loadFont("font/fontcn.fnt");
        mat = new Material(this.assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matNode1 = new Material(this.assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        Texture textureNode1 = this.assetManager.loadTexture("./node1.png");
        matNode1.setTexture("ColorMap",textureNode1);
        matNode1.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        mat.setColor("Color", ColorRGBA.Blue);
        mat.getAdditionalRenderState().setWireframe(true);
        mat.getAdditionalRenderState().setLineWidth(1.0F);
    }

    private void init3D() {
        app.settings.setTitle("3d win");
        flyCamAppState.getCamera().setDragToRotate(true);
        flyCamAppState.getCamera().setMoveSpeed(5);
        flyCamAppState.getCamera().setZoomSpeed(3.5f);
        this.lostFocusBehavior = LostFocusBehavior.Disabled;
        this.cam.setLocation(new Vector3f(5.5826545F, 3.6192513F, 8.016988F));
        this.cam.setRotation(new Quaternion(-0.04787097F, 0.9463123F, -0.16569641F, -0.27339742F));
        Line line = new Line(new Vector3f(2.0f, 2.0f, 2.0f), new Vector3f(0f, 0f, 0f));
        Geometry geomLine = new Geometry("Line", line);
        geomLine.setMaterial(this.mat);
        this.rootNode.attachChild(geomLine);
        this.putArrow(Vector3f.ZERO, Vector3f.UNIT_X, ColorRGBA.Red);
        this.putArrow(Vector3f.ZERO, Vector3f.UNIT_Y, ColorRGBA.Green);
        this.putArrow(Vector3f.ZERO, Vector3f.UNIT_Z, ColorRGBA.Blue);
        this.putGrid(new Vector3f(0F, 0.0F, 0.0F), ColorRGBA.DarkGray);
        createTxt3d();
    }

    private void createTxt3d() {
        BitmapText txt = new BitmapText(myFont, false);
        txt.setBox(new Rectangle(0.0F, 0.0F, 6.0F, 3.0F));
        txt.setQueueBucket(RenderQueue.Bucket.Transparent);
        txt.setSize(0.5F);
        txt.setText(this.txtB);
        this.rootNode.attachChild(txt);
    }

    private void initGUI() {

        GuiGlobals.initialize(this);
        BaseStyles.loadGlassStyle();
        GuiGlobals.getInstance().getStyles().setDefaultStyle("glass");
        GuiGlobals.getInstance().getStyles().setDefault(myFont);
        Container myWindow = new Container();
        guiNode.attachChild(myWindow);
        myWindow.setLocalTranslation(1, settings.getHeight()-1, 0);
        Label txt = new Label(" 设置 ");
        txt.setFontSize(15);
        myWindow.addChild(txt);
        Button clickMeBtn = new Button("显示/隐藏 OpenGL 信息");
        clickMeBtn.setFontSize(20);
        Button clickMe = myWindow.addChild(clickMeBtn);
        clickMe.addClickCommands(new Command<Button>() {
            @Override
            public void execute( Button source ) {
                showInfo = !showInfo;
                toggleInfo();
            }
        });
    }

    private void toggleInfo() {
        setDisplayFps(showInfo);
        setDisplayStatView(showInfo);
    }

    public Geometry putShape(Mesh shape, ColorRGBA color) {
        Geometry g = new Geometry("shape", shape);
        mat.setColor("Color", color);
        g.setMaterial(mat);
        this.rootNode.attachChild(g);
        return g;
    }

    public Geometry putArrow(Vector3f pos, Vector3f dir, ColorRGBA color) {
        Arrow arrow = new Arrow(dir);
        Geometry geometry = this.putShape(arrow, color);
        geometry.setLocalTranslation(pos);
        return geometry;
    }

    public Geometry putBox(Vector3f pos, float size, ColorRGBA color) {
        Geometry geometry = this.putShape(new WireBox(size, size, size), color);
        geometry.setLocalTranslation(pos);
        return geometry;
    }

    public Geometry putGrid(Vector3f pos, ColorRGBA color) {
        Geometry geometry = this.putShape(new Grid(6, 6, 0.2F), color);
        geometry.center().move(pos);
        return geometry;
    }

    @Override
    protected void destroyInput() {
        this.online = false;
        super.destroyInput();
    }

    public void append(String opt, Item item) {
        if(!online) return;
        Frame3D frame = null;
        if(item instanceof Task){
            Task task = (Task) item;
            frame = taskToFrame(opt,task);
        }else if (item instanceof Concept){
            Concept concept = (Concept) item;
            frame = conceptToFrame(opt,concept);
        }
        frameQueue.add(frame); //先加到等待队列,等线程有空了再处理(放到场景中)
    }

    public <E extends Item> void remove(E overflowItem) {
        Item3D item3D = map.get(overflowItem.hashCode());
        willRemove.add(item3D);
    }
    Item3D getItem3D(int key){
        Item3D item3D = map.get(key);
        if(item3D==null){
            item3D = new Item3D();
            map.put(key,item3D);
        }
        return item3D;
    }
    private Frame3D conceptToFrame(String opt, Concept concept) {
        int key = concept.hashCode();
        Item3D item3D = getItem3D(key);
        if(!item3D.hasInit){
            item3D.hasInit = true;
            item3D.type = Item3D.ItemTYPE.Concept;
            item3D.item = concept;
            item3D.key = concept.getKey();

            Quad quad = new Quad(nodeWidth,nodeWidth);
            Geometry g = new Geometry(opt+" "+item3D.key, quad);
            g.setMaterial(matNode1);
            g.setQueueBucket(RenderQueue.Bucket.Transparent);
            item3D.geo = g;
            map.put(key,item3D);
        }
        Frame3D frame3D = new Frame3D();
        frame3D.item3d = item3D;
        frame3D.opt = opt;
        return frame3D;
    }

    private Frame3D taskToFrame(String opt, Task task) {
        int key = task.hashCode();
        Item3D item3D = getItem3D(key);
        if(!item3D.hasInit){
            item3D.hasInit = true;
            item3D.type = Item3D.ItemTYPE.Concept;
            item3D.item = task;
            item3D.key = task.getKey();

            Quad quad = new Quad(nodeWidth,nodeWidth);
            Geometry g = new Geometry(opt+" "+item3D.key, quad);
            g.setMaterial(matNode1);
            g.setQueueBucket(RenderQueue.Bucket.Transparent);
            item3D.geo = g;
            map.put(key,item3D);
        }
        Frame3D frame3D = new Frame3D();
        frame3D.item3d = item3D;
        frame3D.opt = opt;
        return frame3D;
    }

    void addToRoot(Frame3D frame){
        frame.item3d.geo.setLocalTranslation(0,0,((Double) Math.random()).floatValue());
        frame.item3d.geo.addControl(new BillboardControl());
        this.rootNode.attachChild(frame.item3d.geo);
        list.add(frame);
        System.out.println("add: "+frame.item3d.geo.getName());
    }

    @Override
    public void simpleUpdate(float tpf) {
        while (frameQueue.size()>0){
            Frame3D frame = frameQueue.remove(frameQueue.size() - 1);
            addToRoot(frame);
        }
        while (willRemove.size()>0){
            Item3D item3D = willRemove.remove(willRemove.size() - 1);
            item3D.geo.removeFromParent();
        }
        super.simpleUpdate(tpf);
    }
}
