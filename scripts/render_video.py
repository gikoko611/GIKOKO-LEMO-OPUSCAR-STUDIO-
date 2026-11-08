#!/usr/bin/env python3
"""Self-contained procedural video renderer for G.I KO KO LEMO OPUSCAR Studio."""
import argparse, json, subprocess, tempfile, textwrap
from pathlib import Path
from PIL import Image, ImageDraw

def run(cmd):
    print("+", " ".join(map(str, cmd)), flush=True)
    subprocess.run(cmd, check=True)

def font_path(pattern, fallback):
    try:
        result = subprocess.check_output(["fc-match", "-f", "%{file}", pattern], text=True).strip()
        if result and Path(result).exists(): return result
    except Exception: pass
    return fallback

def make_mascot(path):
    im=Image.new("RGBA",(420,520),(0,0,0,0)); d=ImageDraw.Draw(im)
    gold=(231,189,107,255); dark=(28,54,51,255)
    d.ellipse((65,465,355,510),fill=(8,15,22,95))
    d.rounded_rectangle((145,365,205,475),radius=22,fill=dark)
    d.rounded_rectangle((220,365,280,475),radius=22,fill=dark)
    d.rounded_rectangle((118,450,210,495),radius=18,fill=gold)
    d.rounded_rectangle((215,450,310,495),radius=18,fill=gold)
    d.rounded_rectangle((60,220,135,365),radius=30,fill=gold)
    d.rounded_rectangle((285,220,360,365),radius=30,fill=gold)
    d.rounded_rectangle((112,205,308,405),radius=55,fill=gold)
    d.line((210,230,210,395),fill=(116,77,32,255),width=8)
    d.ellipse((105,35,315,245),fill=(224,174,119,255))
    d.ellipse((160,120,180,142),fill=dark); d.ellipse((240,120,260,142),fill=dark)
    d.arc((170,135,250,205),start=10,end=170,fill=dark,width=8)
    d.polygon([(105,75),(130,5),(175,55),(210,0),(245,55),(290,8),(315,78)],fill=gold)
    d.rounded_rectangle((105,65,315,95),radius=10,fill=(179,125,48,255))
    d.ellipse((198,8,222,32),fill=(255,232,160,255)); im.save(path)

def write_text(work,name,value):
    p=work/(name+".txt"); p.write_text(str(value or "").strip(),encoding="utf-8"); return p

def esc(path):
    return str(path).replace("\\","\\\\").replace(":","\\:").replace("'","\\'")

def main():
    ap=argparse.ArgumentParser()
    ap.add_argument("--project",required=True); ap.add_argument("--duration",required=True,type=int)
    ap.add_argument("--offset",required=True,type=int); ap.add_argument("--resolution",default="480p")
    ap.add_argument("--out",required=True); ap.add_argument("--aspect-ratio",default="16:9"); ap.add_argument("--fps",type=int,default=24); a=ap.parse_args()
    p=json.loads(Path(a.project).read_text(encoding="utf-8"))
    plan=p.get("animation_plan") or []
    scenes=[(str(x.get("title","Scene"))+" — "+str(x.get("description","")))[:600] for x in plan] if plan else (p.get("scenes") or ["Opening","A surprising problem","Action and comic twist","Final reveal"])
    dialogue=p.get("dialogue") or []; total=max(1,int(p.get("duration_minutes",1))*60)
    duration=max(1,a.duration); offset=max(0,a.offset)
    base=720 if a.resolution=="720p" else 480
    dimensions={"16:9":(base*16//9,base),"9:16":(base,base*16//9),"1:1":(base,base),"4:5":(base*4//5,base)}
    width,height=dimensions.get(a.aspect_ratio,dimensions["16:9"])
    palettes={"cartoon2d":["0x142b2a","0x263c4b","0x49352a","0x263b32"],
              "cartoon3d":["0x172a36","0x28394e","0x4b3429","0x263d36"],
              "hybrid":["0x101827","0x264a52","0x45312c","0x1b2d3a"],
              "cinematic":["0x101827","0x1a2638","0x2b202e","0x162b35"]}
    style=str(p.get("style","cartoon2d")).lower(); palette=palettes.get(style,palettes["cartoon2d"])
    out=Path(a.out); out.parent.mkdir(parents=True,exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="gikoko-render-") as td:
        w=Path(td); mascot=w/"mascot.png"; make_mascot(mascot)
        regular=font_path("Noto Sans","/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf")
        myanmar=font_path("Noto Sans Myanmar",regular)
        scene_index=min(len(plan)-1,int((offset/max(1,total))*len(plan))) if plan else 0
        motion=str(plan[scene_index].get("motion","float")) if plan else "float"
        if motion=="bounce": mascot_y="H*0.38-h/2+22*abs(sin(2*PI*t/1.4))"
        elif motion=="slide": mascot_y="H*0.38-h/2";
        elif motion=="pulse": mascot_y="H*0.38-h/2+8*sin(2*PI*t/1.0)"
        elif motion=="still": mascot_y="H*0.38-h/2"
        else: mascot_y="H*0.38-h/2+10*sin(2*PI*t/2.5)"
        mascot_x="(W-w)/2+55*sin(2*PI*t/5)" if motion!="slide" else "(W-w)/2-70+140*min(1,t/4)"
        filters=[f"[0:v][1:v]overlay=x='{mascot_x}':y='{mascot_y}':shortest=1[base]"]
        chain="base"
        def draw(name,value,y,size,font=regular,color="0xfff0c2",box="0x000000@0.42",enable=None):
            nonlocal chain
            file=write_text(w,name,value); nxt=f"v{len(filters)}"
            part=(f"[{chain}]drawtext=fontfile='{esc(font)}':textfile='{esc(file)}':"
                  f"expansion=none:fontcolor={color}:fontsize={size}:x=(w-text_w)/2:y={y}:"
                  f"box=1:boxcolor={box}:boxborderw=12:line_spacing=7")
            if enable: part+=f":enable='{enable}'"
            filters.append(part+f"[{nxt}]"); chain=nxt
        draw("title",str(p.get("title") or "G.I KO KO STORY")[:70],"h*0.075",int(height*.075),color="0xffd780")
        draw("credit",str(p.get("watermark") or "POWERED BY I KO KO")[:100],"h*0.20",int(height*.027),color="0xffe8b0",box="0x000000@0.22")
        draw("character",textwrap.fill(str(p.get("character_bible") or "A consistent royal hero."),58)[:650],
             "h*0.77",int(height*.026),font=myanmar,enable="between(t,0,8)",box="0x000000@0.55")
        span=total/max(1,len(scenes))
        raw_durations=[max(1.0,float(x.get("duration",10))) for x in plan] if plan else [1.0]*len(scenes)
        duration_sum=sum(raw_durations) or 1.0
        cursor=0.0
        for i,scene in enumerate(scenes):
            span_i=total*raw_durations[i]/duration_sum
            start=cursor-offset; end=cursor+span_i-offset
            cursor+=span_i
            if end<0 or start>duration: continue
            en=f"between(t,{max(0,start):.3f},{min(duration,end):.3f})"
            draw(f"scene_{i}",f"SCENE {i+1} / {len(scenes)}","h*0.30",int(height*.043),color="0xffd780",enable=en,box="0x000000@0.32")
            draw(f"scene_text_{i}",textwrap.fill(str(scene),46)[:550],"h*0.66",int(height*.034),font=myanmar,enable=en)
        if dialogue:
            slot=total/len(dialogue)
            for i,item in enumerate(dialogue):
                start=i*slot-offset; end=(i+1)*slot-offset
                if end<0 or start>duration: continue
                caption="\n".join(x for x in (str(item.get("english") or "").strip(),str(item.get("myanmar") or "").strip()) if x)
                if caption:
                    en=f"between(t,{max(0,start):.3f},{min(duration,end):.3f})"
                    draw(f"dialogue_{i}",textwrap.fill(caption,54)[:500],"h*0.86",int(height*.027),font=myanmar,enable=en,box="0x000000@0.68")
        filters.append(f"[{chain}]drawbox=x=36:y=ih-20:w='(iw-72)*min(1,(t+{offset})/{total})':h=7:color=0xe9c46a@0.95:t=fill[vout]")
        if plan:
            scene_index=min(len(plan)-1,int((offset/max(1,total))*len(plan)))
            raw_bg=str(plan[scene_index].get("background","#142b2a"))
            bg="0x"+raw_bg.lstrip("#") if len(raw_bg)==7 and raw_bg.startswith("#") else palette[scene_index%len(palette)]
        else:
            bg=palette[(offset//max(1,int(span)))%len(palette)]
        run(["ffmpeg","-y","-hide_banner","-loglevel","error",
             "-f","lavfi","-i",f"color=c={bg}:s={width}x{height}:r={a.fps}:d={duration}",
             "-loop","1","-framerate","24","-i",str(mascot),
             "-filter_complex",";".join(filters),"-map","[vout]","-an","-t",str(duration),
             "-r",str(a.fps),"-c:v","libx264","-preset","ultrafast","-crf","27","-pix_fmt","yuv420p",
             "-movflags","+faststart","-threads","2",str(out)])
    if not out.exists() or out.stat().st_size<1000: raise SystemExit("No valid MP4 segment produced.")
    print(f"Rendered {out} ({out.stat().st_size} bytes)",flush=True)

if __name__=="__main__": main()
