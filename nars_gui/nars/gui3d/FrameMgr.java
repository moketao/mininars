package nars.gui3d;

import com.jme3.network.AbstractMessage;
import com.jme3.network.base.protocol.SerializerMessageProtocol;
import com.jme3.network.serializing.Serializable;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

@Serializable
public class FrameMgr extends AbstractMessage {
    public static String path = "frames.dat";
    public ArrayList<Frame3D> timeLineFrames = new ArrayList<>(); // 等待播放的帧信息集
    public void add(Frame3D frame3D){
        timeLineFrames.add(frame3D);
    }

    public void save() throws IOException {
        SerializerMessageProtocol protocol = new SerializerMessageProtocol();
        ByteBuffer buffer = ByteBuffer.allocate(6553800);
        ByteBuffer byteBuffer = protocol.toByteBuffer(this, buffer);
        File file = new File(path);
        if(!file.exists()) file.createNewFile();
        FileChannel wChannel = new FileOutputStream(file).getChannel();
        wChannel.write(byteBuffer);
        wChannel.close();
    }

    public void read() throws IOException {
        File file = new File(path);
        if(!file.exists())return;
        FileInputStream fileInputStream = new FileInputStream(file);
        byte[] bytes = fileInputStream.readAllBytes();
        fileInputStream.close();
        SerializerMessageProtocol protocol = new SerializerMessageProtocol();
        ByteBuffer wrap = ByteBuffer.wrap(bytes, 2, bytes.length-2);
        FrameMgr message = (FrameMgr) protocol.toMessage(wrap);
        this.timeLineFrames = message.timeLineFrames;
    }

    public Frame3D get(int playIndex) {
        return timeLineFrames.get(playIndex);
    }
}
