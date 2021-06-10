#extension GL_OES_EGL_image_external : require

precision mediump float; //精度为float
varying vec2 vTextureCoord; //纹理位置,接收于vertex_shader
uniform samplerExternalOES sTexture; //加载摄像头流数据

void main() {
  gl_FragColor = texture2D(sTexture, vTextureCoord);
}