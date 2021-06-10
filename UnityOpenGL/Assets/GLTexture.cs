using System;
using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class GLTexture : MonoBehaviour
{
    #if UNITY_ANDROID
    private AndroidJavaObject mGLTexCtrl;
    private AndroidJavaObject mFlutterApp;
    #endif
    private int mTextureId;
    private int mWidth;
    private int mHeight;
    private Vector3 right_pos;
    private Vector3 pos_zero;
    private float w,h;

    public MeshRenderer meshRenderer;
    public Material material;
    public Texture tex;
    private int haveStartFlutter = 0;

    private void Awake()
    {
        meshRenderer = GetComponent<MeshRenderer>();
        material = meshRenderer.material;
        tex = material.mainTexture;
        #if UNITY_ANDROID
        // 实例化com.example.unitylibrary.GLTexture类的对象
        mGLTexCtrl = new AndroidJavaObject("com.example.unitylibrary.GLTexture");
        // 初始化OpenGL
        mGLTexCtrl.Call("setupOpenGL");

        mFlutterApp = new AndroidJavaObject("com.example.unitylibrary.FlutterApp"); 
        
        #endif
    }

    void Start()
    {
        BindTexture();
        Vector3 pos = this.GetComponent<Transform>().position;
        float xSize=this.GetComponent<MeshRenderer>().bounds.size.x;
        float ySize = this.GetComponent<MeshRenderer>().bounds.size.y;

        
        Vector3 r_pos = new Vector3(pos.x + xSize/2,pos.y-ySize/2,pos.z);
        Vector3 pos_z = new Vector3(pos.x - xSize/2,pos.y+ySize/2,pos.z);
        
   
    
        right_pos = Camera.main.WorldToScreenPoint(r_pos);
        pos_zero = Camera.main.WorldToScreenPoint(pos_z);

        w = right_pos.x-pos_zero.x;
        h = pos_zero.y-right_pos.y;
        Debug.Log(right_pos);
        Debug.Log(pos_zero);
        Debug.Log("w:"+w+"&h:"+h);
        

    }

    void Update()
    {
        #if UNITY_ANDROID
        if(mGLTexCtrl.Call<bool>("isNeedUpdate"))
            mGLTexCtrl.Call("updateTexture");
        if (Input.touches.Length > 0){
            if(haveStartFlutter == 1){
            Debug.Log(Input.touches[0].position);
            double x = Input.touches[0].position.x;
            double y = Input.touches[0].position.y;
            x = x-pos_zero.x;
            y = y-pos_zero.y;
            y = -y;
            x = x/w;
            y = y/h;
            Debug.Log("x:"+x+"&y:"+y+"&type:"+Input.touches[0].phase);
            if(Input.touches[0].phase == TouchPhase.Began){
                mGLTexCtrl.Call("onTouchEvent",0,x,y);
            }else if(Input.touches[0].phase == TouchPhase.Moved){
                mGLTexCtrl.Call("onTouchEvent",1,x,y);

            }else if(Input.touches[0].phase == TouchPhase.Ended){
                mGLTexCtrl.Call("onTouchEvent",2,x,y);

            }
            }else{
                mFlutterApp.Call("startFlutter");
                haveStartFlutter = 1;
            }

        }
        #endif 
        
        if(Input.GetMouseButtonDown(1)){
            Debug.Log(Input.mousePosition);
        }
    }

    void BindTexture()
    {
        // 获取JavaPlugin生成的纹理ID
        #if UNITY_ANDROID
        mTextureId = mGLTexCtrl.Call<int>("getStreamTextureID");
        if (mTextureId == 0)
        {
            Debug.LogError("getStreamTextureID failed");
            return;
        }
        // Debug.Log("getStreamTextureID success");
        mWidth = mGLTexCtrl.Call<int>("getStreamTextureWidth");
        mHeight = mGLTexCtrl.Call<int>("getStreamTextureHeight");
        // 将纹理ID与当前GameObject绑定
        //GetComponent<MeshRenderer>().material.mainTexture = Texture2D.CreateExternalTexture
        //    (mWidth, mHeight, TextureFormat.ARGB32, false, false, (IntPtr)mTextureId);   
           // 更新纹理数据
        mGLTexCtrl.Call("updateTexture");     
        Texture2D texture = Texture2D.CreateExternalTexture(mWidth, mHeight, 
                    TextureFormat.RGB565, false, false, (IntPtr)mTextureId);
        texture.wrapMode = TextureWrapMode.Clamp;
        texture.filterMode = FilterMode.Bilinear;
        material.mainTexture = texture;
        #endif
     
    }
}