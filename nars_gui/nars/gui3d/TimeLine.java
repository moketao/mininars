package nars.gui3d;

import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Panel;
import com.simsilica.lemur.Slider;
import com.simsilica.lemur.core.GuiComponent;
import com.simsilica.lemur.event.*;

public class TimeLine extends Slider {
    public final Button thumb;
    public final Panel range;
    public Vector2f dragPos;
    public double startPercent;
    public final Button decrement;
    public final Button increment;

    TimeLine(){
        super();
        range = getRangePanel();
        range.setPreferredSize(new Vector3f(680f,50f,0f));

        thumb = getThumbButton();
        thumb.setText("");
        thumb.setSize(new Vector3f(8f,50f,0f));

        decrement = getDecrementButton();
        increment = getIncrementButton();
        decrement.setText("");
        increment.setText("");

        GuiComponent background = this.getBackground();
        Node bg = background.getGuiControl().getNode();
        bg.addControl(new MouseEventControl(new BgMouseHandler()));
    }

    protected class BgMouseHandler extends DefaultMouseListener {
        protected BgMouseHandler() {}
        public void mouseButtonEvent(MouseButtonEvent event, Spatial target, Spatial capture) {
            event.setConsumed();
            if (event.getButtonIndex() == 0) {
                if (event.isPressed()) {
                    dragPos = new Vector2f(event.getX(), event.getY());
                    startPercent = TimeLine.this.getModel().getPercent();
                } else {
                    dragPos = null;
                }
            }
        }

        public void mouseEntered(MouseMotionEvent event, Spatial target, Spatial capture) {
        }

        public void mouseExited(MouseMotionEvent event, Spatial target, Spatial capture) {
        }

        public void mouseMoved(MouseMotionEvent event, Spatial target, Spatial capture) {
            if(dragPos==null)
                return;

            int x = event.getX();
            float x0 = TimeLine.this.range.getWorldTranslation().x;
            float v = x - x0;
            float percent = v / TimeLine.this.range.getSize().x;
            TimeLine.this.getModel().setPercent(percent);
            event.setConsumed();
        }
    }

    private class BgDragger extends DefaultCursorListener {
        private Vector2f drag;
        private double startPercent;

        private BgDragger() {
            this.drag = null;
        }

        public void cursorButtonEvent(CursorButtonEvent event, Spatial target, Spatial capture) {
            if (event.getButtonIndex() == 0) {
                event.setConsumed();
                if (event.isPressed()) {
                    this.drag = new Vector2f(event.getX(), event.getY());
                    this.startPercent = TimeLine.this.getModel().getPercent();
                } else {
                    this.drag = null;
                }

            }
        }

        public void cursorMoved(CursorMotionEvent event, Spatial target, Spatial capture) {
            if (this.drag != null) {
                Vector3f v1 = null;
                Vector3f v2 = null;

                v1 = new Vector3f(TimeLine.this.thumb.getSize().x * 0.5F, 0.0F, 0.0F);
                v2 = v1.add(TimeLine.this.range.getSize().x - TimeLine.this.thumb.getSize().x * 0.5F, 0.0F, 0.0F);

                v1 = event.getRelativeViewCoordinates(TimeLine.this.range, v1);
                v2 = event.getRelativeViewCoordinates(TimeLine.this.range, v2);
                Vector3f dir = v2.subtract(v1);
                float length = dir.length();
                dir.multLocal(1.0F / length);
                Vector3f cursorDir = new Vector3f(event.getX() - this.drag.x, event.getY() - this.drag.y, 0.0F);
                float dot = cursorDir.dot(dir);
                float percent = dot / length;
                TimeLine.this.getModel().setPercent(this.startPercent + (double)percent);
                event.setConsumed();
            }
        }
    }
}
