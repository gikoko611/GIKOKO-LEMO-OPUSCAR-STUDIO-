#!/usr/bin/env python3
"""G.I KO KO audio/subtitle post-production for rendered scene segments."""
import argparse, asyncio, json, os, subprocess, sys, time
from pathlib import Path

def run(cmd):
    subprocess.run(cmd, check=True)

def stamp(seconds):
    ms=max(0,int(seconds*1000)); h,ms=divmod(ms,3600000); m,ms=divmod(ms,60000); s,ms=divmod(ms,1000)
    return f"{h:02}:{m:02}:{s:02},{ms:03}"

async def make_tts(text, voice, target):
    import edge_tts
    last=None
    for attempt in range(3):
        try:
            await edge_tts.Communicate(text, voice).save(str(target))
            if target.exists() and target.stat().st_size>500: return
        except Exception as exc:
            last=exc
            await asyncio.sleep(2*(attempt+1))
    raise RuntimeError(f"Edge TTS failed for voice {voice}: {last}")

def main():
    ap=argparse.ArgumentParser()
    ap.add_argument("--project",required=True);ap.add_argument("--out",required=True)
    ap.add_argument("--duration",type=int,required=True);ap.add_argument("--work",required=True)
    args=ap.parse_args()
    project=json.loads(Path(args.project).read_text(encoding="utf-8"))
    out=Path(args.out);work=Path(args.work);work.mkdir(parents=True,exist_ok=True);out.parent.mkdir(parents=True,exist_ok=True)
    duration=args.duration; dialogue=project.get("dialogue") or []
    voice_mode=project.get("voice","No voice-over")
    music=bool(project.get("background_music",True));sfx=bool(project.get("sfx",True))
    subtitles=bool(project.get("myanmar_subtitles",True))
    srt=out.with_suffix(".srt");entries=[];speech=[]
    if dialogue:
        slot=duration/len(dialogue)
        for i,d in enumerate(dialogue):
            start=i*slot+0.15; end=min(duration,(i+1)*slot-0.1)
            en=(d.get("english") or "").strip(); my=(d.get("myanmar") or "").strip()
            if subtitles:
                caption=my or en
                if caption: entries.append((start,end,caption))
            if "English" in voice_mode or "Bilingual" in voice_mode:
                if en: speech.append((start,en,"en-US-GuyNeural"))
            if "Myanmar" in voice_mode or "Bilingual" in voice_mode:
                if my: speech.append((start+(0.0 if "English" not in voice_mode else min(1.2,slot*.25)),my,"my-MM-ThihaNeural"))
    if subtitles:
        with srt.open("w",encoding="utf-8") as f:
            for i,(a,b,text) in enumerate(entries,1):
                f.write(f"{i}\n{stamp(a)} --> {stamp(max(a+0.5,b))}\n{text}\n\n")
    else:
        srt=None

    voice_files=[]
    if speech:
        async def synth():
            for i,(start,text,voice) in enumerate(speech):
                target=work/f"voice_{i:03}.mp3"
                await make_tts(text,voice,target)
                voice_files.append((start,target))
        asyncio.run(synth())

    inputs=[];filters=[];mix=[]
    if music:
        # Original synthesized soft chord bed; no third-party music assets.
        music_expr="0.012*sin(2*PI*196*t)+0.010*sin(2*PI*246.94*t)+0.008*sin(2*PI*293.66*t)"
        run(["ffmpeg","-y","-loglevel","error","-f","lavfi","-i",f"aevalsrc={music_expr}:s=48000:d={duration}","-af",f"afade=t=in:st=0:d=2,afade=t=out:st={max(0,duration-3)}:d=3","-c:a","pcm_s16le",str(work/"music.wav")])
        inputs += ["-i",str(work/"music.wav")]
        filters.append("[0:a]volume=0.55[music]")
        mix.append("[music]")
        base_index=1
    else:
        base_index=0
    for i,(start,path) in enumerate(voice_files):
        inputs += ["-i",str(path)]
        idx=base_index+i
        delay=max(0,int(start*1000))
        filters.append(f"[{idx}:a]adelay={delay}|{delay},volume=1.0[v{i}]")
        mix.append(f"[v{i}]")
    if sfx:
        # Short original per-scene impact accents, generated from a low sine oscillator.
        sfx_path=work/"sfx.wav"
        expr=f"0.025*sin(2*PI*82*t)*if(lt(mod(t\\,{max(3,duration/max(1,len(project.get('scenes') or [1])))}),0.22),exp(-mod(t\\,{max(3,duration/max(1,len(project.get('scenes') or [1])))} )*12),0)"
        try:
            run(["ffmpeg","-y","-loglevel","error","-f","lavfi","-i",f"aevalsrc={expr}:s=48000:d={duration}","-c:a","pcm_s16le",str(sfx_path)])
            inputs += ["-i",str(sfx_path)]
            idx=base_index+len(voice_files)
            filters.append(f"[{idx}:a]volume=0.7[sfx]")
            mix.append("[sfx]")
        except subprocess.CalledProcessError:
            print("SFX generator skipped; continuing with music and voice.",file=sys.stderr)
    if not mix:
        run(["ffmpeg","-y","-loglevel","error","-f","lavfi","-i",f"anullsrc=r=48000:cl=stereo:d={duration}","-c:a","pcm_s16le",str(out)])
    else:
        if len(mix)==1: filters.append(f"{mix[0]}anull[out]")
        else: filters.append("".join(mix)+f"amix=inputs={len(mix)}:duration=longest:dropout_transition=0:normalize=0[out]")
        run(["ffmpeg","-y","-loglevel","error",*inputs,"-filter_complex",";".join(filters),"-map","[out]","-t",str(duration),"-ar","48000","-ac","2","-c:a","pcm_s16le",str(out)])
    print(json.dumps({"audio":str(out),"subtitle":str(srt) if srt else None,"voice_lines":len(voice_files),"subtitle_lines":len(entries)}))

if __name__=="__main__": main()
