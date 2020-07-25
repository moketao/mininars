package nars.gui3d;

import com.jme3.math.Vector3f;

public class Frame3D {

    /** 要驱动的动画目标对象 */
    public Item3D item3d;

    /** 记录当前 操作 类型 */
    public String opt;

    /** 动画的开始位置 */
    public Vector3f startPos;

    /** 动画的结束位置 */
    public Vector3f endPos;

    /** 开始大小 */
    public float startPosSize;

    /** 结束大小 */
    public float endPosSize;
}
