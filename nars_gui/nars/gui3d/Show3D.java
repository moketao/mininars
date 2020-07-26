package nars.gui3d;

import com.jme3.app.*;
import com.jme3.app.state.AppState;
import com.jme3.app.state.ConstantVerifierState;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.math.*;
import com.jme3.network.serializing.Serializer;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.control.BillboardControl;
import com.jme3.scene.shape.Line;
import com.jme3.system.AppSettings;
import com.simsilica.lemur.*;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.style.BaseStyles;
import nars.entity.*;
import nars.language.Inheritance;
import nars.language.Term;
import nars.main_nogui.ReasonerBatch;
import nars.storage.Memory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class Show3D extends SimpleApplication{
    public static final String INSERT_CONCEPT = "insert concept"; //生成概念/插入概念
    public static final String INSERT_TASK = "insert task"; //生成任务/插入任务
    public static final String DERIVED = "derived task"; //衍生任务/分解任务
    public static final String UPDATE_CONCEPT_Y = "update concept Y"; //更新概念的高度
    public static final String REMOVE = "remove"; //删除
    private static FlyCamAppState flyCamAppState;
    private ReasonerBatch reasoner;
    private Material matRed;
    private Material matGreen;
    private Material matBlue;
    private Material matDarkGray;
    private BitmapFont myFont;
    private HashMap<Integer, Item3D> map = new HashMap<>();
    private ArrayList<Frame3D> frameQueue = new ArrayList<>();
    private ArrayList<Frame3D> moveQueue = new ArrayList<>();
    private boolean showInfo = false;
    private Material matConcept;
    private Material matTask;
    private Material matConceptSml;

    private boolean online = true;
    private ArrayList<Item3D> willRemove = new ArrayList<>();
    private Memory memory;
    Label selLabel;
    private int updateTextDelay = 0;
    private FloatTxt3d floatTxt3d;

    FrameMgr frameMgr;
    Show3D(AppState... initialStates){
        super(initialStates);
    }
    static Show3D app;
    public static void init(ReasonerBatch reasoner) {
        flyCamAppState = new FlyCamAppState();
        app = new Show3D(new StatsAppState(), flyCamAppState ,new DebugKeysAppState(), new ConstantVerifierState());
        if(reasoner!=null){
            app.reasoner = reasoner;
            app.memory = reasoner.getMemory();
        }
        AppSettings setting= new AppSettings(true);
        setting.setResizable(true);
        setting.setTitle("3d Win");
        setting.setWidth(1024);
        setting.setAudioRenderer(null);
        setting.setVSync(false);
        app.setSettings(setting);
        app.showSettings = false;
        app.frameMgr = new FrameMgr();
        app.start();
    }
    public static void main(String[] params) {
        init(null);
    }
    public static Show3D inst() {
        return app;
    }

    @Override
    public void simpleInitApp() {
        initializeSerializable();
        initRes();
        init3D();
        initGUI();
        toggleInfo();
    }
    public void initializeSerializable() {
        Serializer.registerClass(Item3D.class);
        Serializer.registerClass(Frame3D.class);
        Serializer.registerClass(FrameMgr.class);
        Serializer.registerClass(Frame3D.class,new Frame3DSerializer());
    }
    void readFrameFromFile() throws IOException {
        app.frameMgr.read();
        setTips("已从"+FrameMgr.path+" 读取数据, len:"+app.frameMgr.timeLineFrames.size());
    }
    private void saveFramesToFile() throws IOException {
        app.frameMgr.save();
        setTips("已保存到"+FrameMgr.path+", len:"+app.frameMgr.timeLineFrames.size());
    }

    private void initRes() {
        MiniUtil.init(this.rootNode,this.assetManager);
        myFont = assetManager.loadFont("font/fontcn.fnt");
        matRed = MiniUtil.createMat(ColorRGBA.Red);
        matGreen = MiniUtil.createMat(ColorRGBA.Green);
        matBlue = MiniUtil.createMat(ColorRGBA.Blue);
        matDarkGray = MiniUtil.createMat(ColorRGBA.DarkGray);
        matConcept = MiniUtil.createPngMat("./node.png");
        matTask = MiniUtil.createPngMat("./task.png");
        matConceptSml = MiniUtil.createPngMat("./node_sml.png");
    }
    private void init3D() {
        app.settings.setTitle("3d win");
        flyCamAppState.getCamera().setDragToRotate(true);
        flyCamAppState.getCamera().setMoveSpeed(4.6f);
        flyCamAppState.getCamera().setZoomSpeed(12f);
        this.cam.setParallelProjection(false);
        this.cam.setFrustumPerspective(45f, 1f, 0.1f, 1000f);
        this.lostFocusBehavior = LostFocusBehavior.Disabled;
        resetCam();
        Line line = new Line(new Vector3f(0, 2.5f, 0.0f), new Vector3f(0f, 1.5f, 0f));
        Geometry geomLine = new Geometry("Line", line);
        geomLine.setMaterial(this.matRed);
        this.rootNode.attachChild(geomLine);
        MiniUtil.putArrow(Vector3f.ZERO, Vector3f.UNIT_X, matRed);
        MiniUtil.putArrow(Vector3f.ZERO, Vector3f.UNIT_Y, matGreen);
        MiniUtil.putArrow(Vector3f.ZERO, Vector3f.UNIT_Z, matBlue);
        MiniUtil.putGrid(new Vector3f(0F, 0.0F, 0.0F), matDarkGray);
        createTxt3d();
        createFloatTxt3d();

        inputManager.addMapping("clickItem3D", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addListener(actionListener, "clickItem3D");
    }
    private ActionListener actionListener = new ActionListener() {

        public void onAction(String binding, boolean keyPressed, float tpf) {
            if (binding.equals("clickItem3D") && !keyPressed) {
                CollisionResults results = new CollisionResults();
                Vector3f origin    = cam.getWorldCoordinates(inputManager.getCursorPosition(), 0.0f);
                Vector3f direction = cam.getWorldCoordinates(inputManager.getCursorPosition(), 0.3f);
                direction.subtractLocal(origin).normalizeLocal();
                Ray ray = new Ray(origin, direction);
                rootNode.collideWith(ray, results);
                if (results.size() > 0) {
                    CollisionResult closest = results.getClosestCollision();
                    Node parent = closest.getGeometry().getParent();
                    setTips(parent.getName());
                    floatTxt3d.setText(parent.getName(),parent);
                }
            }
        }
    };

    private void setTips(String str) {
        selLabel.setText(str);
        selLabel.center().move(settings.getWidth()*0.5f,28,0);
        updateTextDelay = 5;
    }

    private void resetCam() {
        System.out.println(cam.getLocation());
        System.out.println(inst().cam.getRotation());
        this.cam.setLocation(new Vector3f(3.009177f, 1.0300535f, 5.8067155f));
        this.cam.setRotation(new Quaternion(0.0094482275f, 0.97163385f, 0.039399598f, -0.2329937f));
    }

    private void createTxt3d() {
        BitmapText txt = MiniUtil.create3dtxt(myFont, "WASD移动摄像机\n拖拽左键可旋转摄像机");
        txt.setLocalTranslation(0,2f,-6f);
        this.rootNode.attachChild(txt);
    }
    private void createFloatTxt3d() {
        floatTxt3d = new FloatTxt3d("",myFont,new Vector3f(0,0.3f,0));
        this.rootNode.attachChild(floatTxt3d);
    }

    private void initGUI() {

        GuiGlobals.initialize(this);
        BaseStyles.loadGlassStyle();
        GuiGlobals.getInstance().getStyles().setDefaultStyle("glass");
        GuiGlobals.getInstance().getStyles().setDefault(myFont);
        Container myWindow = new Container();
        guiNode.attachChild(myWindow);
        myWindow.setLocalTranslation(1, settings.getHeight()-1, 0);
        Label txt = new Label("设置: ");
        txt.setFontSize(15);
        myWindow.addChild(txt);

        Button resetCamBtn = new Button("摄像机归位");
        resetCamBtn.setFontSize(20);
        myWindow.addChild(resetCamBtn);
        resetCamBtn.addClickCommands(source -> resetCam());

        Button showInfoBtn = new Button("显示/隐藏 OpenGL 信息");
        showInfoBtn.setFontSize(20);
        myWindow.addChild(showInfoBtn);
        showInfoBtn.addClickCommands(source -> {
            showInfo = !showInfo;
            toggleInfo();
        });

        Button saveBtn = new Button("保存到 frames.dat");
        saveBtn.setFontSize(20);
        myWindow.addChild(saveBtn);
        saveBtn.addClickCommands(source -> {
            try {
                saveFramesToFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        Button readBtn = new Button("读取 frames.dat");
        readBtn.setFontSize(20);
        myWindow.addChild(readBtn);
        readBtn.addClickCommands(source -> {
            try {
                readFrameFromFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        Button playBtn = new Button("播放 frames.dat");
        playBtn.setFontSize(20);
        myWindow.addChild(playBtn);
        playBtn.addClickCommands(source -> {
            play();
        });

        selLabel = new Label("当前未选中节点");
        selLabel.setFontSize(25);
        selLabel.setTextHAlignment(HAlignment.Center);
        selLabel.setTextVAlignment(VAlignment.Center);
        guiNode.attachChild(selLabel);
        updateTextDelay = 5;
    }

    private void toggleInfo() {
        setDisplayFps(showInfo);
        setDisplayStatView(showInfo);
    }



    @Override
    protected void destroyInput() {
        this.online = false;
        super.destroyInput();
    }

    public void append(String opt, Item item) {
        if(item==null) return;
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
        frameMgr.add(frame);
    }

    public <E extends Item> void remove(E overflowItem) {
        if(overflowItem==null)return;
        Item3D item3D = map.get(overflowItem.hashCode());
        if(item3D==null)return;
        willRemove.add(item3D);
        Frame3D frame3D = new Frame3D();
        frame3D.opt = REMOVE;
        frame3D.item3d = item3D;
        frameMgr.add(frame3D);
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
            item3D.geo = MiniUtil.create3dObject(opt+" "+item3D.key, getMat(concept));
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
            item3D.geo = MiniUtil.create3dObject(opt+" "+item3D.key,matTask);
            map.put(key,item3D);
        }
        Frame3D frame3D = new Frame3D();
        frame3D.item3d = item3D;
        frame3D.opt = opt;
        return frame3D;
    }

    private Material getMat(Concept concept) {
        if(concept.getTerm() instanceof Inheritance){
            return matConceptSml; // 系词在系统中有特殊地位,但在可视觉化中,可能应该弱化,暂时先给个小贴图.
        }
        return matConcept;
    }

    void addToRoot(Frame3D frame){
        frame.item3d.geo.setLocalTranslation(FastMath.nextRandomFloat()*2f-1f,0.2f,FastMath.nextRandomFloat()*3f-1.5f);
        BillboardControl billboardControl = new BillboardControl();
        billboardControl.setAlignment(BillboardControl.Alignment.Camera);
        frame.item3d.geo.addControl(billboardControl);
        this.rootNode.attachChild(frame.item3d.geo);
    }

    @Override
    public void simpleUpdate(float tpf) {
        if(updateTextDelay>0){
            updateTextDelay -= 1;
            if(updateTextDelay<=0){
                selLabel.center().move(settings.getWidth()*0.5f,28,0);
            }
        }
        while (frameQueue.size()>0){
            Frame3D frame = frameQueue.remove(frameQueue.size() - 1);
            if(frame!=null){
                addToRoot(frame);
            }
        }
        while (willRemove.size()>0){
            Item3D item3D = willRemove.remove(willRemove.size() - 1);
            if(item3D!=null){
                item3D.geo.removeFromParent();
            }
        }
        while (moveQueue.size()>0){
            Frame3D frame = moveQueue.remove(moveQueue.size() - 1);
            if(frame==null) continue;
            if(frame!=null && frame.item3d!=null && frame.item3d.geo!=null){
                frame.item3d.geo.setLocalTranslation(frame.endPos.x,frame.endPos.y,frame.endPos.z); // todo: 动画
            }
        }
        super.simpleUpdate(tpf);
    }

    public void move(Task task) {
        Sentence parentBelief = task.getParentBelief();
        Task parentTask = task.getParentTask();
        boolean isA = parentBelief.getContent() instanceof Inheritance;
        boolean isB = parentTask.getSentence().getContent() instanceof Inheritance;
        boolean isA2B = task.getSentence().getContent() instanceof Inheritance;
        if( isA && isB && isA2B){                                                                   // 针对下面的逻辑,举个例: 乌鸦是鸟, 鸟是动物
            Inheritance A = (Inheritance) parentBelief.getContent();
            moveOneConcept(A.getSubject(), A.getPredicate(),parentBelief.getTruth() );              // 鸟被推动一次

            Inheritance B = (Inheritance) parentTask.getSentence().getContent();
            moveOneConcept(B.getSubject(), B.getPredicate(),parentTask.getSentence().getTruth() );  // 动物被推动一次

            Inheritance B2 = (Inheritance) task.getSentence().getContent();
            moveOneConcept(B2.getSubject(), B2.getPredicate(),task.getSentence().getTruth() );      // 因为 task 推出 '乌鸦是动物', 所以动物的高度会被继续推高一次
        }
    }

    private void moveOneConcept(Term push,Term target, TruthValue truth) {
        Float baseY = 1f;
        Float pushPower;
        if(truth.getConfidence()<0.5){
            pushPower = baseY;                                      // 信心不足就不推了, 只给基础高度 1
        }else{
            pushPower = baseY + truth.getExpectation();             // 基础高度 + 经验高度
        }
        Concept concept = memory.getConcept(target);
        Item3D item3D2 = map.get(concept.hashCode());

        HashMap<String, Float> valForHeight = item3D2.valForHeight; // 推高的力量集 (来自其它 concept )
        String key = memory.getConcept(push).getKey();              // 推者的 key
        valForHeight.put(key,pushPower);                            // 记录当前信仰受到的推力

        Float sum = 0f;                                             // 推力汇总 , todo: 是否要用到 UtilityFunctions.aveGeo ?
        for(Float f : valForHeight.values())
        {
            sum+=f;                                                 // todo: 使用 FastMath.interpolateLinear 算偏移集,再平均, 或者 从上至下 树形分配位置.
        }
        Frame3D frame3D = new Frame3D();
        frame3D.item3d = item3D2;
        frame3D.opt = UPDATE_CONCEPT_Y;                             // 标注这次的操作是: 更新高度
        Vector3f posNow = frame3D.item3d.geo.getLocalTranslation(); // 当前位置
        frame3D.startPos.set(posNow);
        frame3D.endPos.set(posNow.x, sum, posNow.z);                // 将要移动到的位置
        frameMgr.add(frame3D);
        moveQueue.add(frame3D);
    }
    private void play() {
        int playIndex = 0;
        Float xInFrame = 0f;
        Frame3D frame3D = frameMgr.get(playIndex);
        Item3D item3D = map.get(frame3D.item3d);
        if(item3D==null){

        }
    }
}
