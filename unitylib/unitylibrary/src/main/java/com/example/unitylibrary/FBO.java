package com.example.unitylibrary;

import android.opengl.GLES20;
import android.util.Log;

import com.example.unitylibrary.GLTexture2D;
import com.example.unitylibrary.Utils;

public class FBO {

    int mFBOID;
    private int mRBOID;
    int textureID;
    private int createTexture() {
        int[] textureIds = new int[1];
        //创建纹理
        GLES20.glGenTextures(1, textureIds, 0);
        if (textureIds[0] == 0) {
            return 0;
        }
        //绑定纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[0]);
        //环绕（超出纹理坐标范围）  （s==x t==y GL_REPEAT 重复）
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);
        //过滤（纹理像素映射到坐标点）  （缩小、放大：GL_LINEAR线性）
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        return textureIds[0];
    }
    public FBO(int width,int height) {

        int depthID;
        int[] temps = new int[1];
        // Render buffer
//        GLES20.glGenTextures(1, temps, 0);
//        depthID = temps[0];
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, temps[0]);
//        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_DEPTH_COMPONENT,
//                texture2D.getWidth(), texture2D.getHeight(),
//                0, GLES20.GL_DEPTH_COMPONENT, GLES20.GL_UNSIGNED_SHORT, null);
//        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
//        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
//        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
//        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        GLES20.glGenFramebuffers(1, temps, 0);
        mFBOID = temps[0];

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFBOID);
        textureID = createTexture();
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,textureID);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, textureID, 0);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height,
                0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
//        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_TEXTURE_2D, depthID, 0);
        //6. 检测是否绑定从成功
        if (GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)
                != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            Log.e("myyf", "glFramebufferTexture2D error");
        }
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    public void FBOBegin() {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFBOID);

        Utils.checkGlError("glBindBuffer GL_ARRAY_BUFFER 0");
    }

    public void FBOEnd() {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }
}

