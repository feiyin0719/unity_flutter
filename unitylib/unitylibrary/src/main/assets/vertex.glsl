attribute vec4 aPosition;
attribute mediump vec2 aTextureCoord;
varying mediump vec2 vTextureCoord;
uniform mat4 uMVPMatrix;

void main() {
  gl_Position = uMVPMatrix * aPosition;
  vTextureCoord = aTextureCoord.xy;
}