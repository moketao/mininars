package nars.gui3d;
import java.util.ArrayList;
import java.util.HashMap;
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
}
