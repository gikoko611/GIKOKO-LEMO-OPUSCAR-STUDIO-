package com.gikoko.lemoopuscar;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/** Lightweight on-device silent MP4 renderer. Designed for short previews on low-memory phones. */
public final class LocalRenderEngine {
    public interface Progress { void onProgress(int completed, int total, String stage); }
    private LocalRenderEngine() {}

    public static void render(JSONObject project, File output, int seconds, String resolution,
                              String ratio, int fps, Progress progress) throws Exception {
        if (seconds < 1 || seconds > 15) throw new IllegalArgumentException("Local render supports up to 15 seconds per clip on this low-memory device.");
        int base = "720p".equals(resolution) ? 720 : 480;
        int width, height;
        if ("9:16".equals(ratio)) { width=base; height=base*16/9; }
        else if ("1:1".equals(ratio)) { width=base; height=base; }
        else if ("4:5".equals(ratio)) { width=base*4/5; height=base; }
        else { width=base*16/9; height=base; }
        width=(width+1)&~1; height=(height+1)&~1;
        int frames=seconds*fps;
        MediaCodec codec=null; MediaMuxer muxer=null; Bitmap bitmap=null;
        boolean muxerStarted=false, inputDone=false, outputDone=false;
        int track=-1;
        try {
            String mime="video/avc";
            MediaCodecInfo selected=null; int color=0;
            MediaCodecList list=new MediaCodecList(MediaCodecList.REGULAR_CODECS);
            for (MediaCodecInfo info:list.getCodecInfos()) {
                if (!info.isEncoder()) continue;
                for (String type:info.getSupportedTypes()) if (mime.equalsIgnoreCase(type)) {
                    MediaCodecInfo.CodecCapabilities caps=info.getCapabilitiesForType(type);
                    for (int candidate:new int[]{MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar,
                            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar}) {
                        for (int supported:caps.colorFormats) if (supported==candidate) { selected=info;color=candidate;break; }
                        if (selected!=null) break;
                    }
                }
                if (selected!=null) break;
            }
            if (selected==null) throw new IllegalStateException("This phone's H.264 encoder does not expose a compatible YUV420 input format.");
            codec=MediaCodec.createByCodecName(selected.getName());
            MediaFormat format=MediaFormat.createVideoFormat(mime,width,height);
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT,color);
            format.setInteger(MediaFormat.KEY_BIT_RATE, "720p".equals(resolution)?2400000:1100000);
            format.setInteger(MediaFormat.KEY_FRAME_RATE,fps);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,2);
            codec.configure(format,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);
            codec.start();
            if (!output.getParentFile().exists()) output.getParentFile().mkdirs();
            muxer=new MediaMuxer(output.getAbsolutePath(),MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
            Canvas canvas=new Canvas(bitmap);
            Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
            MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();
            JSONArray scenes=project.optJSONArray("scenes");
            JSONArray dialogue=project.optJSONArray("dialogue");
            String title=project.optString("title","G.I KO KO ViD GEN");
            String watermark=project.optString("watermark","POWERED BY I KO KO");
            long timeout=10000;
            int frame=0; int[] pixelBuffer=new int[width*height]; byte[] yuvBuffer=new byte[width*height*3/2];
            while (!outputDone) {
                if (!inputDone) {
                    int inputIndex=codec.dequeueInputBuffer(timeout);
                    if (inputIndex>=0) {
                        ByteBuffer input=codec.getInputBuffer(inputIndex);
                        if (input==null) throw new IllegalStateException("Video encoder input buffer unavailable.");
                        if (frame<frames) {
                            input.clear();
                            drawFrame(canvas,paint,width,height,frame,frames,fps,title,watermark,scenes,dialogue);
                            writeYuv420(bitmap,input,width,height,color==MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar,pixelBuffer,yuvBuffer);
                            codec.queueInputBuffer(inputIndex,0,input.position(),(long)frame*1000000L/fps,0);
                            frame++;
                            if (progress!=null && (frame%Math.max(1,fps)==0 || frame==frames))
                                progress.onProgress(frame,frames,"Encoding local video");
                        } else {
                            codec.queueInputBuffer(inputIndex,0,0,(long)frame*1000000L/fps,MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            inputDone=true;
                        }
                    }
                }
                int outputIndex=codec.dequeueOutputBuffer(info,timeout);
                if (outputIndex==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (muxerStarted) throw new IllegalStateException("Encoder output format changed twice.");
                    track=muxer.addTrack(codec.getOutputFormat()); muxer.start(); muxerStarted=true;
                } else if (outputIndex>=0) {
                    ByteBuffer encoded=codec.getOutputBuffer(outputIndex);
                    if (encoded!=null && info.size>0 && muxerStarted && (info.flags&MediaCodec.BUFFER_FLAG_CODEC_CONFIG)==0) {
                        encoded.position(info.offset); encoded.limit(info.offset+info.size);
                        muxer.writeSampleData(track,encoded,info);
                    }
                    if ((info.flags&MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0) outputDone=true;
                    codec.releaseOutputBuffer(outputIndex,false);
                }
            }
            if (!muxerStarted) throw new IllegalStateException("Local encoder did not produce an MP4 track.");
        } finally {
            if (bitmap!=null) bitmap.recycle();
            if (codec!=null) { try { codec.stop(); } catch(Exception ignored) {} try { codec.release(); } catch(Exception ignored) {} }
            if (muxer!=null) { try { if(muxerStarted)muxer.stop(); } catch(Exception ignored) {} try { muxer.release(); } catch(Exception ignored) {} }
        }
        if (!output.exists() || output.length()<1024) throw new IllegalStateException("Local MP4 output is empty or incomplete.");
    }

    private static void drawFrame(Canvas c, Paint p, int w, int h, int frame, int total, int fps,
                                  String title, String watermark, JSONArray scenes, JSONArray dialogue) {
        float t=frame/(float)Math.max(1,total);
        int bg=Color.rgb(12+(int)(18*t),18+(int)(10*t),38+(int)(22*t));
        c.drawColor(bg);
        p.setShader(new android.graphics.LinearGradient(0,0,w,h,0xff192b52,0xff311d45,android.graphics.Shader.TileMode.CLAMP));
        c.drawRect(0,0,w,h,p); p.setShader(null);
        p.setColor(0x5539d9ff); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(Math.max(2,w/300f));
        c.drawRoundRect(new RectF(w*.035f,h*.035f,w*.965f,h*.965f),w*.035f,w*.035f,p); p.setStyle(Paint.Style.FILL);
        p.setColor(0xfff4d58d); p.setTextAlign(Paint.Align.CENTER); p.setTypeface(android.graphics.Typeface.create("sans-serif",1));
        p.setTextSize(w*.055f); c.drawText(fit(title,p,w*.84f),w/2f,h*.16f,p);
        int sceneIndex=0; String scene="Opening scene";
        if(scenes!=null&&scenes.length()>0){sceneIndex=Math.min(scenes.length()-1,(int)(t*scenes.length()));scene=scenes.optString(sceneIndex,scene);}
        p.setColor(Color.WHITE);p.setTextSize(w*.038f);c.drawText("SCENE "+(sceneIndex+1)+" / "+Math.max(1,scenes==null?1:scenes.length()),w/2f,h*.29f,p);
        p.setTextSize(w*.043f);drawWrapped(c,p,scene,w/2f,h*.42f,w*.78f,w*.065f);
        float pulse=(float)Math.sin(frame*0.12f);
        p.setColor(0xff43d6d0);c.drawCircle(w/2f,h*.66f+ pulse*h*.012f,w*.075f,p);
        p.setColor(0xff0b1424);c.drawCircle(w/2f-w*.025f,h*.65f,w*.009f,p);c.drawCircle(w/2f+w*.025f,h*.65f,w*.009f,p);
        String caption="";
        if(dialogue!=null&&dialogue.length()>0){int di=Math.min(dialogue.length()-1,(int)(t*dialogue.length()));JSONObject d=dialogue.optJSONObject(di);if(d!=null)caption=d.optString("myanmar",d.optString("english",""));}
        if(!caption.isEmpty()){p.setColor(0xdd07111f);c.drawRoundRect(new RectF(w*.08f,h*.79f,w*.92f,h*.91f),14,14,p);p.setColor(Color.WHITE);p.setTextSize(w*.031f);drawWrapped(c,p,caption,w/2f,h*.84f,w*.76f,w*.048f);}
        p.setColor(0xffd8c28a);p.setTextSize(w*.025f);c.drawText(watermark,w/2f,h*.96f,p);
        p.setColor(0xff46d9d1);c.drawRect(w*.08f,h*.975f,w*.08f+(w*.84f*(frame+1)/total),h*.982f,p);
    }
    private static String fit(String s,Paint p,float max){String v=s==null?"":s;while(v.length()>3&&p.measureText(v)>max)v=v.substring(0,v.length()-2)+"…";return v;}
    private static void drawWrapped(Canvas c,Paint p,String text,float x,float y,float max,float lineH){
        String[] words=(text==null?"":text).split("\\s+");String line="";int row=0;
        for(String word:words){String next=line.isEmpty()?word:line+" "+word;if(p.measureText(next)>max&&!line.isEmpty()){c.drawText(line,x,y+row++*lineH,p);line=word;if(row>=3)break;}else line=next;}
        if(!line.isEmpty()&&row<3)c.drawText(line,x,y+row*lineH,p);
    }
    private static void writeYuv420(Bitmap bitmap,ByteBuffer out,int width,int height,boolean semiPlanar,int[] pixels,byte[] yuv){
        bitmap.getPixels(pixels,0,width,0,0,width,height);
        int frameSize=width*height;int yIndex=0,uvIndex=frameSize;
        int uIndex=frameSize,vIndex=frameSize+frameSize/4;
        for(int j=0;j<height;j++)for(int i=0;i<width;i++){
            int rgb=pixels[j*width+i],r=(rgb>>16)&255,g=(rgb>>8)&255,b=rgb&255;
            int y=((66*r+129*g+25*b+128)>>8)+16;
            int u=((-38*r-74*g+112*b+128)>>8)+128;
            int v=((112*r-94*g-18*b+128)>>8)+128;
            yuv[yIndex++]=(byte)Math.max(0,Math.min(255,y));
            if((j&1)==0&&(i&1)==0){
                if(semiPlanar){yuv[uvIndex++]=(byte)Math.max(0,Math.min(255,u));yuv[uvIndex++]=(byte)Math.max(0,Math.min(255,v));}
                else {yuv[uIndex++]=(byte)Math.max(0,Math.min(255,u));yuv[vIndex++]=(byte)Math.max(0,Math.min(255,v));}
            }
        }
        out.put(yuv);
    }
}