#!/usr/bin/env python3
"""G.I KO KO audio/subtitle post-production with Gemini TTS primary and Edge TTS fallback."""
import argparse, asyncio, base64, json, os, subprocess, sys, time
from pathlib import Path
from urllib.request import Request, urlopen
from urllib.error import HTTPError, URLError

def run(cmd):
    subprocess.run(cmd, check=True)

def stamp(seconds):
    ms=max(0,int(seconds*1000)); h,ms=divmod(ms,3600000); m,ms=divmod(ms,60000); s,ms=divmod(ms,1000)
    return f"{h:02}:{m:02}:{s:02},{ms:03}"

def gemini_tts(text, voice, target):
    api_key=os.environ.get("GEMINI_API_KEY","").strip()
    if not api_key:
        raise RuntimeError("GEMINI_API_KEY is not configured")
    model=os.environ.get("GEMINI_TTS_MODEL","gemini-3.8-flash-tts").strip()
    style=("Speak naturally and clearly. Use fluent Burmese pronunciation." if voice=="Kore"
           else "Speak natural, clear American English with expressive but controlled delivery.")
    payload={
        "model":model,
        "input":[{
            "type":"user_input",
            "content":[{
                "type":"text",
                "text":text,
                "annotations":[{"type":"speech_metadata","style":style}]
            }]
        }],
        "response_format":{"type":"audio"},
        "generation_config":{"speech_config":[{"voice":voice}]}
    }
    request=Request(
        "https://generativelanguage.googleapis.com/v1beta/interactions",
        data=json.dumps(payload,ensure_ascii=False).encode("utf-8"),
        headers={"Content-Type":"application/json","x-goog-api-key":api_key},
        method="POST"
    )
    with urlopen(request,timeout=120) as response:
        result=json.loads(response.read().decode("utf-8"))
    audio=(result.get("output_audio") or {}).get("data")
    if not audio:
        raise RuntimeError("Gemini TTS response did not contain output_audio.data")
    audio_bytes=base64.b64decode(audio,validate=True)
    if len(audio_bytes)<1000 or not audio_bytes.startswith(b"RIFF"):
        raise RuntimeError("Gemini TTS returned an invalid or unexpectedly short WAV file")
    target.write_bytes(audio_bytes)

async def edge_tts_fallback(text, voice, target):
    import edge_tts
    edge_voice="my-MM-ThihaNeural" if voice=="Kore" else "en-US-GuyNeural"
    mp3_target=target.with_suffix(".mp3")
    await edge_tts.Communicate(text,edge_voice).save(str(mp3_target))
    if not mp3_target.exists() or mp3_target.stat().st_size<500:
        raise RuntimeError("Edge TTS returned an empty audio file")
    return mp3_target

async def make_tts(text, voice, target):
    provider=os.environ.get("TTS_PROVIDER","edge").strip().lower()
    use_gemini=provider in ("auto","gemini") and bool(os.environ.get("GEMINI_API_KEY","").strip())
    if provider=="gemini" and not os.environ.get("GEMINI_API_KEY","").strip():
        raise RuntimeError("TTS_PROVIDER=gemini but GEMINI_API_KEY is missing")
    last=None
    if use_gemini:
        for attempt in range(3):
            try:
                gemini_tts(text,voice,target)
                print(f"TTS_PROVIDER=Gemini model={os.environ.get('GEMINI_TTS_MODEL','gemini-3.8-flash-tts')} voice={voice}")
                return target
            except Exception as exc:
                last=exc
                print(f"Gemini TTS attempt {attempt+1}/3 failed: {type(exc).__name__}: {exc}",file=sys.stderr)
                await asyncio.sleep(2*(attempt+1))
        if provider=="gemini":
            raise RuntimeError(f"Gemini TTS failed after 3 attempts: {last}")
        print("Gemini TTS unavailable; falling back to Edge TTS.",file=sys.stderr)
    try:
        result=await edge_tts_fallback(text,voice,target)
        print(f"TTS_PROVIDER=Edge fallback voice={voice}")
        return result
    except Exception as exc:
        if last:
            raise RuntimeError(f"Gemini TTS failed ({last}); Edge TTS fallback failed ({exc})") from exc
        raise

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
    subtitles=bool(project.get("bilingual_subtitles",project.get("myanmar_subtitles",True)))
    srt=out.with_suffix(".srt");entries=[];speech=[]
    if dialogue:
        slot=duration/len(dialogue)
        for i,d in enumerate(dialogue):
            start=i*slot+0.15; end=min(duration,(i+1)*slot-0.1)
            en=(d.get("english") or "").strip(); my=(d.get("myanmar") or "").strip()
            if subtitles:
                caption="\n".join(part for part in (en,my) if part)
                if caption: entries.append((start,end,caption))
            if "English" in voice_mode or "Bilingual" in voice_mode:
                if en: speech.append((start,en,"Puck"))
            if "Myanmar" in voice_mode or "Bilingual" in voice_mode:
                if my: speech.append((start+(min(1.5,slot*.35) if "Bilingual" in voice_mode else 0.0),my,"Kore"))
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
                target=work/f"voice_{i:03}.wav"
                actual=await make_tts(text,voice,target)
                voice_files.append((start,actual))
        asyncio.run(synth())

    inputs=[];filters=[];mix=[]
    if music:
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
        sfx_path=work/"sfx.wav"
        period=max(3,duration/max(1,len(project.get("scenes") or [1])))
        expr=f"0.025*sin(2*PI*82*t)*exp(-mod(t\\,{period})*6)"
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
