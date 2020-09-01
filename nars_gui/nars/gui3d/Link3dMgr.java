package nars.gui3d;
import com.jme3.math.Vector2f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Link3dMgr {
    public static HashMap<Item3D, ArrayList<Link3D>> item2link = new HashMap<>();
    public static HashMap<String, Link3D> linkMap = new HashMap<>();

    public static String createKey(String a, String b) {
        int i = a.compareTo(b);
        String link3dKey = i>0 ? (a+"---"+b) : (b+"---"+a);
        return link3dKey;
    }
    public static Link3D getLinkByLinKey(String link3dKey) {
        return linkMap.get(link3dKey);
    }
    public static void putLinkByLinKey(String link3dKey, Link3D link) {
        linkMap.put(link3dKey,link);
    }
    public static void putLinkByItem(Item3D item, Link3D link) {
        ArrayList<Link3D> lineControls = item2link.get(item);
        if(lineControls==null){
            lineControls = new ArrayList<>();
            item2link.put(item,lineControls);
        }
        if(lineControls.contains(link)){
            return;
        }
        lineControls.add(link);
    }
    public static ArrayList<Link3D> getLinkByItem(Item3D item) {
        ArrayList<Link3D> lineControls = item2link.get(item);
        if(lineControls==null){
            lineControls = new ArrayList<>();
        }
        return lineControls;
    }

    public static void updateUV(float tpf) {
        for (Map.Entry<String, Link3D> e : linkMap.entrySet()) {
            Link3D link = e.getValue();
            Mesh mesh = link.ctrl.mesh;
            VertexBuffer uvs = mesh.getBuffer(VertexBuffer.Type.TexCoord);
            float dif = 0.003f;
            Vector2f[] texCoord= new Vector2f[uvs.getNumElements()];
            for(int i = 0; i<uvs.getNumElements(); i++) {
                float u=(Float)uvs.getElementComponent(i, 0);
                float v=(Float)uvs.getElementComponent(i, 1);
                texCoord[i]= new Vector2f(u -dif ,v);
            }
            mesh.clearBuffer(VertexBuffer.Type.TexCoord);
            mesh.setBuffer(VertexBuffer.Type.TexCoord, 2, BufferUtils.createFloatBuffer(texCoord));
        }
    }
}
