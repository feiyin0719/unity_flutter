package com.example.unitylibrary;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;

import com.unity3d.player.UnityPlayer;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GLTexture {
    public static GLTexture instance = null;
    private static final String TAG = "GLTexture";

    private int mTextureID = 0;
    private int mTextureWidth = 0;
    private int mTextureHeight = 0;

    SurfaceTexture mCameraInputSurface;
    SurfaceTexture mOutputSurfaceTexture;
    int mOutputTex[];

    private ByteBuffer mBuffer;

    private volatile EGLContext mSharedEglContext;
    private volatile EGLConfig mSharedEglConfig;

    private EGLDisplay mEGLDisplay;
    private EGLContext mEglContext;
    private EGLSurface mEglSurface;
    private FBO fbo;
    private float[] mMVPMatrix = new float[16];
    GLTextureOES glTextureOES;
    Surface surface;
    SurfaceTexture surfaceTexture;

    private boolean needUpdate = true;

    // 创建单线程池，用于处理OpenGL纹理
    private final ExecutorService mRenderThread = Executors.newSingleThreadExecutor();
    // 使用Unity线程Looper的Handler，用于执行Java层的OpenGL操作
    private Handler mUnityRenderHandler;

    public int getStreamTextureWidth() {
        //Log.d(TAG,"mTextureWidth = "+ mTextureWidth);
        return mTextureWidth;
    }

    public int getStreamTextureHeight() {
        //Log.d(TAG,"mTextureHeight = "+ mTextureHeight);
        return mTextureHeight;
    }

    public int getStreamTextureID() {
        Log.d(TAG, "getStreamTextureID sucess = " + mTextureID);
        return mTextureID;
    }

    public Context context;

    public GLTexture() {
        this(UnityPlayer.currentActivity);
    }

    public GLTexture(Context context) {
        instance = this;
        this.context = context;
    }

    private void glLogE(String msg) {
        Log.e(TAG, msg + ", err=" + GLES20.glGetError());
    }

    public boolean isNeedUpdate() {
        return needUpdate;
    }

    public void setNeedUpdate(boolean needUpdate) {
        this.needUpdate = needUpdate;
    }

    // 被unity调用
    public void setupOpenGL() {
        Log.d(TAG, "setupOpenGL called by Unity ");

        // 注意：该调用一定是从Unity绘制线程发起
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        mUnityRenderHandler = new Handler(Looper.myLooper());

        // Unity获取EGLContext
        mSharedEglContext = EGL14.eglGetCurrentContext();
        if (mSharedEglContext == EGL14.EGL_NO_CONTEXT) {
            glLogE("eglGetCurrentContext failed");
            return;
        }
        glLogE("eglGetCurrentContext success");

        EGLDisplay sharedEglDisplay = EGL14.eglGetCurrentDisplay();
        if (sharedEglDisplay == EGL14.EGL_NO_DISPLAY) {
            glLogE("sharedEglDisplay failed");
            return;
        }
        glLogE("sharedEglDisplay success");

        // 获取Unity绘制线程的EGLConfig
        int[] numEglConfigs = new int[1];
        EGLConfig[] eglConfigs = new EGLConfig[1];
        if (!EGL14.eglGetConfigs(sharedEglDisplay, eglConfigs, 0, eglConfigs.length,
                numEglConfigs, 0)) {
            glLogE("eglGetConfigs failed");
            return;
        }
        mSharedEglConfig = eglConfigs[0];
        mRenderThread.execute(new Runnable() {
            @Override
            public void run() {
                // 初始化OpenGL环境
                initOpenGL();
                // 生成OpenGL纹理ID
//                int textures[] = new int[1];
//                GLES20.glGenTextures(1, textures, 0);
//                if (textures[0] == 0) {
//                    glLogE("glGenTextures failed");
//                    return;
//                } else {
//                    glLogE("glGenTextures success");
//                }
                mTextureWidth = 2140;
                mTextureHeight = 1080;
                glTextureOES = new GLTextureOES(context, mTextureWidth, mTextureHeight);
                surfaceTexture = new SurfaceTexture(glTextureOES.getTextureID());


                surfaceTexture.setDefaultBufferSize(mTextureWidth, mTextureHeight);
                surface = new Surface(surfaceTexture);
                surfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
                    @Override
                    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                        needUpdate = true;
                    }
                });
                fbo = new FBO(mTextureWidth, mTextureHeight);
                mTextureID = fbo.textureID;

                mBuffer = ByteBuffer.allocate(mTextureWidth * mTextureHeight * 4);


            }
        });


    }

    private void initOpenGL() {
        mEGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (mEGLDisplay == EGL14.EGL_NO_DISPLAY) {
            glLogE("eglGetDisplay failed");
            return;
        }
        glLogE("eglGetDisplay success");

        int[] version = new int[2];
        if (!EGL14.eglInitialize(mEGLDisplay, version, 0, version, 1)) {
            mEGLDisplay = null;
            glLogE("eglInitialize failed");
            return;
        }
        glLogE("eglInitialize success");

        int[] eglContextAttribList = new int[]{
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, // 该值需与Unity绘制线程使用的一致
                EGL14.EGL_NONE
        };
        // 创建Java线程的EGLContext时，将Unity线程的EGLContext和EGLConfig作为参数传递给eglCreateContext，
        // 从而实现两个线程共享EGLContext
        mEglContext = EGL14.eglCreateContext(mEGLDisplay, mSharedEglConfig, mSharedEglContext,
                eglContextAttribList, 0);
        if (mEglContext == EGL14.EGL_NO_CONTEXT) {
            glLogE("eglCreateContext failed");
            return;
        }
        glLogE("eglCreateContext success");

        int[] surfaceAttribList = {
                EGL14.EGL_WIDTH, mTextureWidth,
                EGL14.EGL_HEIGHT, mTextureHeight,
                EGL14.EGL_NONE
        };
        // Java线程不进行实际绘制，因此创建PbufferSurface而非WindowSurface
        // 创建Java线程的EGLSurface时，将Unity线程的EGLConfig作为参数传递给eglCreatePbufferSurface
        mEglSurface = EGL14.eglCreatePbufferSurface(mEGLDisplay, mSharedEglConfig, surfaceAttribList, 0);
        if (mEglSurface == EGL14.EGL_NO_SURFACE) {
            glLogE("eglCreatePbufferSurface failed");
            return;
        }
        glLogE("eglCreatePbufferSurface success");

        if (!EGL14.eglMakeCurrent(mEGLDisplay, mEglSurface, mEglSurface, mEglContext)) {
            glLogE("eglMakeCurrent failed");
            return;
        }
        glLogE("eglMakeCurrent success");

        GLES20.glFlush();
    }

    public void updateTexture() {
        //Log.d(TAG,"updateTexture called by unity");
        mRenderThread.execute(new Runnable() {
            @Override
            public void run() {
//                final Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.app_icon);
//                if(bitmap == null)
//                    Log.d(TAG,"bitmap decode faild" + bitmap);
//                else
//                    Log.d(TAG,"bitmap decode success" + bitmap);
//                mUnityRenderHandler.post(new Runnable() {
//                    @Override
//                    public void run() {
                if (!needUpdate) return;
                needUpdate = false;
                surfaceTexture.updateTexImage();
                Matrix.setIdentityM(mMVPMatrix, 0);
                fbo.FBOBegin();
                GLES20.glViewport(0, 0, mTextureWidth, mTextureHeight);
                glTextureOES.draw(mMVPMatrix);

//                GLES20.glReadPixels(0, 0, mTextureWidth, mTextureHeight, GLES20.GL_RGBA,
//                        GLES20.GL_UNSIGNED_BYTE, mBuffer);
//                Bitmap bitmap1 = Bitmap.createBitmap(mTextureWidth, mTextureHeight, Bitmap.Config.ARGB_8888);
//                bitmap1.copyPixelsFromBuffer(mBuffer);
//                saveBitmap(context, bitmap1, "test");
//                mBuffer.clear();
                fbo.FBOEnd();


//                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureID);
//                GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
//                GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
//                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
//                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
//                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
//                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
//                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
//                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
//                bitmap.recycle();
//                    }
//                });
            }
        });
    }

    public static void saveBitmap(Context context, final Bitmap b, String name) {


        String path = context.getExternalCacheDir().getPath();
        long dataTake = System.currentTimeMillis();
        final String jpegName = path + "/" + name + ".jpg";
        try {
            FileOutputStream fout = new FileOutputStream(jpegName);
            BufferedOutputStream bos = new BufferedOutputStream(fout);
            b.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            bos.flush();
            bos.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


    }

    public void destroy() {
        mRenderThread.shutdownNow();
    }

    public Surface getSurface() {
        return surface;
    }

    private FlutterSurface flutterSurface;

    public void attachFlutterSurface(FlutterSurface surface) {
        flutterSurface = surface;
    }

    public void onTouchEvent(final int type,final double x,final double y) {
        UnityPlayer.currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (flutterSurface != null)
                    flutterSurface.onTouchEvent(type, x, y);
            }
        });

    }
}