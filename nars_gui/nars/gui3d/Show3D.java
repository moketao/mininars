package nars.gui3d;

import com.jme3.app.*;
import com.jme3.app.state.AppState;
import com.jme3.app.state.ConstantVerifierState;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.effect.ParticleEmitter;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.*;
import com.jme3.network.serializing.Serializer;
import com.jme3.network.serializing.serializers.EnumSerializer;
import com.jme3.scene.Node;
import com.jme3.scene.control.BillboardControl;
import com.jme3.system.AppSettings;
import com.simsilica.lemur.*;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.style.BaseStyles;
import com.wizzardo.jme.transition.EasingFunction;
import com.wizzardo.jme.transition.SpatialChanges;
import com.wizzardo.jme.transition.SpatialInterpolations;
import com.wizzardo.jme.transition.TransitionControl;
import nars.entity.*;
import nars.language.Inheritance;
import nars.language.Term;
import nars.main_nogui.ReasonerBatch;
import nars.storage.Memory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class Show3D extends SimpleApplication{

    public  HashMap<String, Item3D> item3dMap       =   new HashMap<>();
    private HashMap<String, Item3D> itemMapForPlay  =   new HashMap<>();
    private ArrayList<Frame3D>      frameQueue      =   new ArrayList<>();
    private ArrayList<Frame3D>      moveQueue       =   new ArrayList<>();

    public static final String INSERT_CONCEPT = "Concept";      // 生成概念/插入概念
    public static final String INSERT_TASK = "BigTask";         // 生成任务/插入任务
    public static final String DERIVED = "Task";                // 衍生任务/分解任务
    public static final String PUSH_CONCEPT_Y = "PushConcept";  // 更新概念的高度
    public static final String REMOVE = "remove";               // 删除

    private static FlyCamAppState flyCamAppState;
    private Material matRed;
    private Material matGreen;
    private Material matBlue;
    private Material matDarkGray;
    private Material matLine2D;
    private BitmapFont myFont;
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
    private Container menu;
    private Container container2dTopRight;

    Show3D(AppState... initialStates){
        super(initialStates);
    }
    static Show3D app;
    public static void init(ReasonerBatch reasoner) {
        flyCamAppState = new FlyCamAppState();
        app = new Show3D(new StatsAppState(), flyCamAppState ,new DebugKeysAppState(), new ConstantVerifierState());
        if(reasoner!=null){
            app.memory = reasoner.getMemory();
        }
        AppSettings setting= new AppSettings(true);
        setting.setResizable(true);
        setting.setTitle("3d Win");
        setting.setWidth(1524);
        setting.setHeight(924);
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
        readFrameFromFile();
    }
    public void initializeSerializable() {
        Serializer.registerClass(Item3D.class);
        Serializer.registerClass(Frame3D.class);
        Serializer.registerClass(FrameMgr.class);
        Serializer.registerClass(Frame3D.class,new Frame3DSerializer());
        Serializer.registerClass(ItemTYPE.class,new EnumSerializer());
        Serializer.registerClass(MatType.class,new EnumSerializer());
    }

    void readFrameFromFile() {
        try {
            app.frameMgr.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
        setTips("已从"+FrameMgr.path+" 读取数据, len:"+app.frameMgr.timeLineFrames.size());
    }
    private void saveFramesToFile() {
        try {
            app.frameMgr.save();
        } catch (IOException e) {
            e.printStackTrace();
        }
        setTips("已保存到"+FrameMgr.path+", len:"+app.frameMgr.timeLineFrames.size());
    }

    private void initRes() {
        MiniUtil.init(this.rootNode,this.assetManager);
        myFont = assetManager.loadFont("font/fontCN.fnt");
        matRed = MiniUtil.createMat(ColorRGBA.Red);
        matGreen = MiniUtil.createMat(ColorRGBA.Green);
        matBlue = MiniUtil.createMat(ColorRGBA.Blue);
        matDarkGray = MiniUtil.createMat(ColorRGBA.DarkGray);
        matConcept = MiniUtil.createPngMat("./node.png");
        matTask = MiniUtil.createPngMat("./task.png");
        matLine2D = MiniUtil.createPngMat("./line_01.png",true);
        matLine2D.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.AlphaAdditive);
        matConceptSml = MiniUtil.createPngMat("./node_sml.png");
    }
    private void init3D() {
        app.settings.setTitle("3d win");
        flyCamAppState.getCamera().setDragToRotate(true);
        flyCamAppState.getCamera().setMoveSpeed(1.2f);
        flyCamAppState.getCamera().setZoomSpeed(12f);
        this.cam.setParallelProjection(false);
        this.cam.setFrustumPerspective(45f, 1f, 0.1f, 1000f);
        this.lostFocusBehavior = LostFocusBehavior.Disabled;
        resetCam();
        MiniUtil.putLine(0, 2.5f, 0.0f,0f, 1.5f, 0f,this.matDarkGray);
        Vector3f pos = new Vector3f(0, 0.01f, 0);
        MiniUtil.putArrow(pos, Vector3f.UNIT_X, matRed);
        MiniUtil.putArrow(pos, Vector3f.UNIT_Y, matGreen);
        MiniUtil.putArrow(pos, Vector3f.UNIT_Z, matBlue);
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
        txt.setLocalTranslation(0,2f,-16f);
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

        selLabel = new Label("当前未选中节点");
        selLabel.setFontSize(25);
        selLabel.setTextHAlignment(HAlignment.Center);
        selLabel.setTextVAlignment(VAlignment.Center);
        guiNode.attachChild(selLabel);
        updateTextDelay = 5;

        container2dTopRight = new Container();
        guiNode.attachChild(container2dTopRight);
        container2dTopRight.setLocalTranslation(300, settings.getHeight()-3, 0);

        TimeLine timeLine = new TimeLine();
        container2dTopRight.addChild(timeLine);

        menu = new Container();
        guiNode.attachChild(menu);
        menu.setLocalTranslation(1, settings.getHeight()-1, 0);

        Label txt = new Label("设置: ");
        txt.setFontSize(15);
        menu.addChild(txt);

        MiniUtil.Btn("摄像机归位",20, menu, source -> resetCam());
        MiniUtil.Btn("显示/隐藏 OpenGL 信息",20, menu, f -> {
            showInfo = !showInfo;
            toggleInfo();
        });
        MiniUtil.Btn("播放 测试 特效 eff",20, menu, f -> {
            ParticleEmitter effect = MiniUtil.createEffect("./flame.png", 4, 4, 3.8f);
            effect.emitAllParticles();
        });
        MiniUtil.Btn("保存到 frames.dat",20, menu, f -> {
            saveFramesToFile();
        });
        MiniUtil.Btn("读取 frames.dat",20, menu, f -> readFrameFromFile());
        MiniUtil.Btn("cam speed 0.2f",20, menu, f -> flyCamAppState.getCamera().setMoveSpeed(0.2f));
        MiniUtil.Btn("cam speed 1.2f",20, menu, f -> flyCamAppState.getCamera().setMoveSpeed(1.2f));
        MiniUtil.Btn("推理一帧 ",30, menu, f -> play());
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
        Frame3D frame = createFrameAnd3dObj(item,opt,null);
        frameQueue.add(frame); //先加到等待队列,等线程有空了再处理(放到场景中)
        frame.targetName = item.getKey();
        frameMgr.add(frame);
    }

    public <E extends Item> void remove(E overflowItem) {
        if(overflowItem==null)return;
        String key = overflowItem.getKey();
        Item3D item3D = item3dMap.get(key);
        if(item3D==null)return;
        willRemove.add(item3D);
        Frame3D frame3D = new Frame3D();
        frame3D.opt = REMOVE;
        frame3D.item3d = item3D;
        frame3D.targetName = key;
        frameMgr.add(frame3D);
    }
    Item3D getItem3D(String key){
        Item3D item3D = item3dMap.get(key);
        if(item3D==null){
            item3D = new Item3D();
            item3dMap.put(key,item3D);
        }
        return item3D;
    }
    private Frame3D createFrameAnd3dObj(Item item, String opt , Frame3D _frame3D) {
        boolean b = item instanceof Concept;
        Frame3D frame3D;
        Item3D item3D;
        String key;
        if(item == null){
            frame3D = _frame3D;
            item3D = new Item3D();
            key = _frame3D.targetName;
            item3dMap.put(key,item3D);
        }else{
            frame3D = new Frame3D();
            key = item.getKey();
            // todo: 检查 key 为 null 的情况.
            item3D = getItem3D(key);
        }
        frame3D.type = b?ItemTYPE.Concept:ItemTYPE.Task;
        setMat(item,frame3D);
        Material mat = setMat(item, frame3D);
        if(!item3D.hasInit){
            item3D.hasInit = true;
            frame3D.item = item;
            frame3D.key = item==null?frame3D.key:item.getKey();
            item3D.geo = MiniUtil.create3dObject(opt+" "+frame3D.key, mat);
            item3dMap.put(key,item3D);
        }
        frame3D.item3d = item3D;
        frame3D.opt = opt;
        return frame3D;
    }

    private Material setMat(Item item, Frame3D frame3D) {
        if(item==null){
            if(frame3D.mat==MatType.Concept){
                frame3D.mat = MatType.Concept;
                return matConcept;
            }else if (frame3D.mat==MatType.ConceptSml){
                frame3D.mat = MatType.ConceptSml;
                return matConceptSml;
            }else if (frame3D.mat==MatType.Task){
                frame3D.mat = MatType.Task;
                return matTask;
            }
            return null;
        }
        if(item instanceof Concept){
            frame3D.mat = MatType.Concept;
            if(((Concept) item).getTerm() instanceof Inheritance){
                frame3D.mat = MatType.ConceptSml;
                return matConceptSml; // 系词在系统中有特殊地位,但在可视觉化中,可能应该弱化,暂时先给个小贴图.
            }else {
                frame3D.mat = MatType.Concept;
                return matConcept;
            }
        }else {
            frame3D.mat = MatType.Task;
            return matTask;
        }
    }

    void addToRoot(Frame3D frame){
        Node geo = frame.item3d.geo;
        geo.setLocalTranslation(FastMath.nextRandomFloat()*2f-1f,0.2f,FastMath.nextRandomFloat()*3f-1.5f);
        BillboardControl billboardControl = new BillboardControl();
        billboardControl.setAlignment(BillboardControl.Alignment.Camera);
        geo.addControl(billboardControl);
        this.rootNode.attachChild(geo);

        geo.setLocalScale(0.1f);

        // 缩放动画
        geo.addControl(new TransitionControl(SpatialChanges.scale(geo), 1.65f, SpatialInterpolations.scaleTo(geo, 1f), EasingFunction.EASE_OUT_ELASTIC));
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
                Node geo = frame.item3d.geo;
                ConceptAnimationCtrl link = new ConceptAnimationCtrl(SpatialChanges.translation(geo), 0.35f, SpatialInterpolations.translateTo(geo, frame.endPos), EasingFunction.EASE_OUT_QUART, frame);

                update3DLink(frame);

                // 缓动
                geo.addControl(link);
            }
        }
        Link3dMgr.updateUV(tpf);
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
            moveOneConcept(B2.getSubject(), B2.getPredicate(),task.getSentence().getTruth() );      // 因为 task 推出 '乌鸦是动物', 所以 '动物' 的高度会被继续推高一次
        }
    }

    private void moveOneConcept(Term push,Term target, TruthValue truth) {
        Float baseY = 3f;
        Float pushPower;
        if(truth.getConfidence()<0.5){
            pushPower = baseY;                                      // 信心不足就不推了, 只给基础高度 1
        }else{
            pushPower = baseY + truth.getExpectation();             // 基础高度 + 经验高度
        }
        Item3D item3D2 = item3dMap.get(target.getName());

        HashMap<String, Float> valForHeight = item3D2.pushMap;      // 推高的力量集 (来自其它 concept )
        String key = push.getName();                                // 推者的 key
        valForHeight.put(key,pushPower);                            // 记录当前信仰受到的推力

        Float sum = 0f;                                             // 推力汇总 , todo: 是否要用到 UtilityFunctions.aveGeo ?
        for(Float f : valForHeight.values())
        {
            sum+=f;                                                 // todo: 使用 FastMath.interpolateLinear 算偏移集,再平均, 或者 从上至下 树形分配位置.
        }
        Frame3D frame3D = new Frame3D();
        frame3D.exp = truth.getExpectation();                       // 记录经验值 , 可用来展示神经突触 ( Link3D ) 的厚度/强度
        frame3D.item3d = item3D2;
        frame3D.opt = PUSH_CONCEPT_Y;                               // 标注这次的操作是: 更新高度
        Vector3f posNow = frame3D.item3d.geo.getLocalTranslation(); // 当前位置
        frame3D.startPos.set(posNow);
        frame3D.endPos.set(posNow.x, sum, posNow.z);                // 将要移动到的位置
        frameMgr.add(frame3D);
        frame3D.targetName = target.getName();
        frame3D.pushName = key;
        moveQueue.add(frame3D);

        frame3D.link3dKey = Link3dMgr.createKey(frame3D.pushName,frame3D.targetName);
    }

    public void update3DLink(Frame3D frame) {
        Link3D link = Link3dMgr.getLinkByLinKey(frame.link3dKey);
        Item3D item3DPush = item3dMap.get(frame.pushName);
        Item3D item3DTarget = item3dMap.get(frame.targetName);
        if( frame.exp > 0.5 && item3DTarget!=null && item3DPush!=null && item3DTarget.geo!=null && item3DPush.geo!=null){
            if (link==null){
                // 用经验值确定 link 的宽度
                System.out.println( "----------- ★ "+frame.pushName+" →→→ "+frame.targetName+": "+ frame.exp  );
                float w = FastMath.interpolateLinear(frame.exp, 0.01f, 0.3f);

                //创建 link
                link = new Link3D(matLine2D, item3DPush, item3DTarget, w);
                rootNode.attachChild(link);

                //记录 link
                Link3dMgr.putLinkByLinKey(frame.link3dKey,link);
                Link3dMgr.putLinkByItem(item3DTarget,link);
                Link3dMgr.putLinkByItem(item3DPush,link);
            }
            updateLinkAboutItem(item3DTarget);
            updateLinkAboutItem(item3DPush);
        }
    }

    private void updateLinkAboutItem(Item3D item) {
        ArrayList<Link3D> links = Link3dMgr.getLinkByItem(item);
        for (Link3D aLink : links) {
            Vector3f pos1 = item.geo.getLocalTranslation();
            if(aLink.pushItem.equals(item)){
                aLink.ctrl.setPoint(0,pos1);
            }
            if(aLink.targetItem.equals(item)){
                aLink.ctrl.setPoint(1,pos1);
            }
        }
    }

    private void log(String s){
        System.out.println(s);
    }
    int playIndex = -1;
    private void play() {
        playIndex++;
        //System.out.println("playIndex:"+playIndex);
        float xInFrame = 0f;
        Frame3D frame3D = frameMgr.get(playIndex);
        if(frame3D==null) {
            System.out.println("播放帧已用完 "+playIndex);
            return;
        }

        switch (frame3D.opt){
            case INSERT_TASK:
            case INSERT_CONCEPT:
            case DERIVED:{
                // 添加
                createFrameAnd3dObj(null, frame3D.opt, frame3D);
                addToRoot(frame3D);
                itemMapForPlay.put(frame3D.targetName,frame3D.item3d);
                break;
            }
            case REMOVE:{
                // 移除
                Item3D item3D = itemMapForPlay.get(frame3D.targetName);
                if(item3D!=null){
                    frame3D.item3d = item3D;
                    willRemove.add(frame3D.item3d);
                }else{
                    System.out.println("要移除的对象不存在");
                }
                break;
            }
            case PUSH_CONCEPT_Y:{
                // 升高
                frame3D.xInFrame = xInFrame;
                Item3D item3D = itemMapForPlay.get(frame3D.targetName);
                frame3D.item3d = item3D;
                moveQueue.add(frame3D);
                log(frame3D.pushName+" → "+frame3D.targetName);
                break;
            }
            default:{
                throw new Error("未处理的 opt ");
            }
        }
    }
}
