package nars.gui3d;

import com.jme3.app.Application;
import com.jme3.app.FlyCamAppState;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.input.KeyInput;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.input.FunctionId;
import com.simsilica.lemur.input.InputMapper;

public class CameraToggleState extends AbstractAppState {
    public static final FunctionId F_CAMERA_TOGGLE = new FunctionId("Camera Toggle");
    private InputMapper inputMapper;
    private AppStateManager stateManager;

    public void initialize(AppStateManager stateManager, Application app) {
        this.stateManager = stateManager;
        super.initialize(stateManager, app);

        inputMapper = GuiGlobals.getInstance().getInputMapper();

        inputMapper.map(F_CAMERA_TOGGLE, KeyInput.KEY_SPACE);
        inputMapper.addDelegate(F_CAMERA_TOGGLE, this, "toggleCamera");
    }
    public void toggleCamera() {
        FlyCamAppState state = stateManager.getState(FlyCamAppState.class);
        state.setEnabled(!state.isEnabled());
    }
    public void cleanup() {
    }
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if(enabled){

        }else{
            inputMapper.removeDelegate(F_CAMERA_TOGGLE, this, "toggleCamera");
        }
    }
}
