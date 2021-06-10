#extension GL_OES_EGL_image_external : require

precision mediump float;//精度 为float
varying vec2 v_texPo;//纹理位置  接收于vertex_shader
uniform samplerExternalOES sTexture;//纹理
void main() {
    gl_FragColor=texture2D(sTexture, v_texPo);
}