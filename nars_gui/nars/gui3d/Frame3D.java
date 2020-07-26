package nars.gui3d;

import com.jme3.math.Vector3f;
import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;


@Serializable
public class Frame3D extends AbstractMessage {

    /** 要驱动的动画目标对象 */
    public int itemHashCode;

    /** 记录当前 操作 类型 */
    public String opt;

    /** 动画的开始位置 */
    public Vector3f startPos = Vector3f.ZERO;

    /** 动画的结束位置 */
    public Vector3f endPos = Vector3f.ZERO;

    /** 开始大小 */
    public float startPosSize;

    /** 结束大小 */
    public float endPosSize;

}
