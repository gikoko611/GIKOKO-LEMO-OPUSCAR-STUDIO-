package com.gikoko.lemoopuscar;

import android.Manifest;
import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.Gravity;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyStore;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class MainActivity extends Activity {
    private static final String OWNER="gikoko611";
    private static final String REPO="GIKOKO-LEMO-OPUSCAR-STUDIO-";
    private static final String API="https://api.github.com/repos/"+OWNER+"/"+REPO;
    private static final String WORKFLOW="cloud-render.yml";
    private static final String KEY_ALIAS="GIKOKO_GITHUB_TOKEN_KEY";
    private static final String PREFS="studio_secure_prefs";
    private static final String TOKEN_KEY="encrypted_pat";
    private static final String RUN_KEY="last_run_id";
    private final Handler main=new Handler(Looper.getMainLooper());
    private EditText tokenInput,aiBaseInput,aiModelInput,aiKeyInput,titleInput,promptInput,characterInput,sceneInput,dialogueInput;
    private Spinner styleSpinner,durationSpinner,resolutionSpinner;
    private TextView status;
    private Button startButton,saveButton,checkButton,downloadButton;\n    private Spinner voiceSpinner;\n    private CheckBox musicCheck,sfxCheck,subtitleCheck;
    private String token;
    private boolean busy=false,pendingStart=false;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(8,11,18));
        getWindow().setNavigationBarColor(Color.rgb(8,11,18));
        token=loadToken();
        buildUi();
    }

    private TextView label(String s,int size){
        TextView v=new TextView(this);v.setText(s);v.setTextColor(Color.rgb(245,226,177));v.setTextSize(size);v.setPadding(0,14,0,7);return v;
    }
    private Spinner spinner(String[] values){
        Spinner s=new Spinner(this);ArrayAdapter<String>a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,values);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);s.setAdapter(a);return s;
    }
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);return b;}
    private void buildUi(){
        ScrollView scroll=new ScrollView(this);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(22,24,22,28);root.setBackgroundColor(Color.rgb(8,11,18));scroll.addView(root);
        TextView title=label("G.I KO KO",32);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);title.setGravity(Gravity.CENTER);root.addView(title);
        TextView sub=label("LEMO OPUSCAR PROFESSIONAL STUDIO",14);sub.setGravity(Gravity.CENTER);root.addView(sub);
        TextView credit=label("POWERED BY I KO KO",12);credit.setGravity(Gravity.CENTER);root.addView(credit);

        root.addView(label("GITHUB TOKEN — FIRST-TIME SETUP",16));
        TextView help=label("Create a Fine-grained personal access token for this repository only. Grant Actions: Read and write. Paste it below, then tap SAVE TOKEN. The token is encrypted with Android Keystore on this phone.",12);root.addView(help);
        tokenInput=new EditText(this);tokenInput.setSingleLine(true);tokenInput.setTextColor(Color.WHITE);tokenInput.setHintTextColor(Color.GRAY);tokenInput.setHint("github_pat_…");tokenInput.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);root.addView(tokenInput);
        saveButton=button(token==null?"SAVE TOKEN":"UPDATE TOKEN");root.addView(saveButton);saveButton.setOnClickListener(v->saveToken());
        Button clear=button("REMOVE SAVED TOKEN");root.addView(clear);clear.setOnClickListener(v->clearToken());

        root.addView(label("AI ASSISTANT — OPENAI-COMPATIBLE PROVIDER",16));
        aiBaseInput=edit("https://api.openai.com/v1","API Base URL",false);root.addView(aiBaseInput);
        aiModelInput=edit("gpt-4o-mini","Model ID",false);root.addView(aiModelInput);
        aiKeyInput=edit("","AI API Key (optional; encrypted on device)",true);root.addView(aiKeyInput);
        Button saveAi=button("SAVE AI PROVIDER");root.addView(saveAi);saveAi.setOnClickListener(v->saveAiProvider());
        Button assist=button("✨ AI STORY ASSIST");root.addView(assist);assist.setOnClickListener(v->assistStory());

        root.addView(label("STORY & TIMELINE ENGINE",18));
        titleInput=edit("The Golden Kingdom","Film Title",false);root.addView(titleInput);
        promptInput=edit("Write a funny adventure in an ancient Myanmar royal palace. Include an unexpected action-comedy twist.","Describe your film idea / Prompt",false);promptInput.setMinLines(3);root.addView(promptInput);
        characterInput=edit("Main character: a clever but clumsy royal guard. Keep costume, face, colors and personality consistent in every scene.","Character Bible — fixed appearance and personality",false);characterInput.setMinLines(3);root.addView(characterInput);
        sceneInput=edit("Palace courtyard — introduce the guard\nThrone room — a suspicious royal order\nPalace kitchen — comic chase\nRoyal hall — funny reveal","Scene Outline — one scene per line",false);sceneInput.setMinLines(4);root.addView(sceneInput);
        dialogueInput=edit("Guard: I have everything under control. | အားလုံးကို ကျွန်တော် ထိန်းချုပ်ထားပါတယ်။\nKing: Then why is the palace on fire? | ဒါဆို နန်းတော်က ဘာလို့ မီးလောင်နေတာလဲ။","Dialogue — English | Myanmar per line",false);dialogueInput.setMinLines(4);root.addView(dialogueInput);
        Button saveProject=button("SAVE PROJECT");root.addView(saveProject);saveProject.setOnClickListener(v->saveProject());

        root.addView(label("AUDIO & SUBTITLE ENGINE",18));
        voiceSpinner=spinner(new String[]{"No voice-over","English voice-over (provider required)","Myanmar voice-over (provider required)"});root.addView(voiceSpinner);
        musicCheck=new CheckBox(this);musicCheck.setText("Background Music");musicCheck.setTextColor(Color.WHITE);musicCheck.setChecked(true);root.addView(musicCheck);
        sfxCheck=new CheckBox(this);sfxCheck.setText("Sound Effects");sfxCheck.setTextColor(Color.WHITE);sfxCheck.setChecked(true);root.addView(sfxCheck);
        subtitleCheck=new CheckBox(this);subtitleCheck.setText("Myanmar Subtitles (SRT track)");subtitleCheck.setTextColor(Color.WHITE);subtitleCheck.setChecked(true);root.addView(subtitleCheck);

        root.addView(label("RENDER SETTINGS",18));
        root.addView(label("Film Style",14));styleSpinner=spinner(new String[]{"cartoon2d","cartoon3d","cinematic"});root.addView(styleSpinner);
        root.addView(label("Duration (minutes)",14));durationSpinner=spinner(new String[]{"1","5","10","30"});root.addView(durationSpinner);
        root.addView(label("Resolution",14));resolutionSpinner=spinner(new String[]{"480p","720p"});root.addView(resolutionSpinner);

        startButton=button("▶ START RENDER");root.addView(startButton);startButton.setOnClickListener(v->{
            if(token==null){setStatus("Please save your Fine-grained GitHub token first.");return;}
            if(ensureStoragePermission()){startRender();}else{pendingStart=true;requestStoragePermission();}
        });
        checkButton=button("CHECK RENDER STATUS");root.addView(checkButton);checkButton.setOnClickListener(v->checkLastRun());
        downloadButton=button("DOWNLOAD LATEST MP4");root.addView(downloadButton);downloadButton.setOnClickListener(v->downloadLastRun());
        status=label(token==null?"Status: Add your GitHub token to begin.":"Status: Ready — token encrypted on this device.",14);root.addView(status);
        root.addView(label("No GitHub Actions page is needed. The APK starts the workflow, checks status and downloads the MP4. Render time and GitHub usage limits apply. Artifacts expire after 7 days.",12));
        setContentView(scroll);\n        restoreProject();\n        aiBaseInput.setText(prefs().getString("ai_base","https://api.openai.com/v1"));\n        aiModelInput.setText(prefs().getString("ai_model","gpt-4o-mini"));
    }

    private EditText edit(String value,String hint,boolean secret){
        EditText e=new EditText(this);e.setText(value);e.setHint(hint);e.setTextColor(Color.WHITE);e.setHintTextColor(Color.GRAY);
        e.setSingleLine(!hint.contains("Prompt")&&!hint.contains("Bible")&&!hint.contains("Outline")&&!hint.contains("Dialogue"));
        e.setInputType(secret?(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD):(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES));
        e.setPadding(12,10,12,10);return e;
    }
    private void saveAiProvider(){
        String base=aiBaseInput.getText().toString().trim(),model=aiModelInput.getText().toString().trim(),key=aiKeyInput.getText().toString().trim();
        if(!base.startsWith("https://")||model.isEmpty()){setStatus("AI provider needs an HTTPS Base URL and Model ID.");return;}
        try{prefs().edit().putString("ai_base",base).putString("ai_model",model).apply();if(!key.isEmpty())prefs().edit().putString("ai_key_enc",encrypt(key)).apply();aiKeyInput.setText("");setStatus("AI provider settings saved. API key encrypted on device.");}
        catch(Exception e){setStatus("Could not encrypt AI key: "+e.getMessage());}
    }
    private JSONObject buildProject(){
        JSONObject p=new JSONObject();
        try{
            p.put("schema_version",1);p.put("title",titleInput.getText().toString().trim());
            p.put("prompt",promptInput.getText().toString().trim());p.put("character_bible",characterInput.getText().toString().trim());
            JSONArray scenes=new JSONArray();for(String line:sceneInput.getText().toString().split("\\n"))if(!line.trim().isEmpty())scenes.put(line.trim());p.put("scenes",scenes);
            JSONArray dialogue=new JSONArray();for(String line:dialogueInput.getText().toString().split("\\n"))if(!line.trim().isEmpty()){
                String[] parts=line.split("\\|",2);JSONObject d=new JSONObject();d.put("english",parts[0].trim());d.put("myanmar",parts.length>1?parts[1].trim():"");dialogue.put(d);
            }p.put("dialogue",dialogue);
            p.put("voice",voiceSpinner.getSelectedItem().toString());p.put("background_music",musicCheck.isChecked());p.put("sfx",sfxCheck.isChecked());p.put("myanmar_subtitles",subtitleCheck.isChecked());
            p.put("style",styleSpinner.getSelectedItem().toString());p.put("duration_minutes",durationSpinner.getSelectedItem().toString());p.put("resolution",resolutionSpinner.getSelectedItem().toString());
        }catch(Exception ignored){}return p;
    }
    private void saveProject(){
        JSONObject p=buildProject();String raw=p.toString();prefs().edit().putString("last_project",raw).apply();setStatus("Project saved on this phone ("+raw.length()+" characters).");
    }
    private void restoreProject(){
        String raw=prefs().getString("last_project",null);if(raw==null)return;
        try{JSONObject p=new JSONObject(raw);titleInput.setText(p.optString("title",""));promptInput.setText(p.optString("prompt",""));characterInput.setText(p.optString("character_bible",""));
            JSONArray a=p.optJSONArray("scenes");if(a!=null){StringBuilder b=new StringBuilder();for(int i=0;i<a.length();i++)b.append(a.getString(i)).append("\\n");sceneInput.setText(b.toString());}
            JSONArray ds=p.optJSONArray("dialogue");if(ds!=null){StringBuilder b=new StringBuilder();for(int i=0;i<ds.length();i++){JSONObject d=ds.getJSONObject(i);b.append(d.optString("english","")).append(" | ").append(d.optString("myanmar","")).append("\\n");}dialogueInput.setText(b.toString());}
            musicCheck.setChecked(p.optBoolean("background_music",true));sfxCheck.setChecked(p.optBoolean("sfx",true));subtitleCheck.setChecked(p.optBoolean("myanmar_subtitles",true));
        }catch(Exception ignored){}
    }
    private void assistStory(){
        if(busy)return;String prompt=promptInput.getText().toString().trim();if(prompt.isEmpty()){setStatus("Enter a film idea first.");return;}
        String key=null;try{String enc=prefs().getString("ai_key_enc",null);if(enc!=null)key=decrypt(enc);}catch(Exception ignored){}
        if(key==null||key.isEmpty()){offlineStoryAssist(prompt);return;}
        final String apiKey=key,base=prefs().getString("ai_base","https://api.openai.com/v1"),model=prefs().getString("ai_model","gpt-4o-mini");
        setBusy(true);setStatus("AI assistant is drafting the story, scenes and bilingual dialogue…");
        new Thread(()->{
            try{
                JSONObject body=new JSONObject();body.put("model",model);body.put("temperature",0.7);
                JSONArray msgs=new JSONArray();JSONObject sys=new JSONObject();sys.put("role","system");sys.put("content","You are a professional animation screenwriter. Return ONLY valid JSON with keys title, character_bible, scenes (array of 4-8 concise scene descriptions), dialogue (array of objects with english and myanmar). Preserve a single consistent character design. Keep dialogue concise and suitable for a short animated film.");
                JSONObject usr=new JSONObject();usr.put("role","user");usr.put("content","Create a storyboard for this idea. Keep it visually renderable as a simple 2D/3D/cinematic motion-graphics sequence. Idea: "+prompt);
                msgs.put(sys);msgs.put(usr);body.put("messages",msgs);
                ApiResponse response=api("POST",base.replaceAll("/+$","")+"/chat/completions",apiKey,body.toString());
                if(response.code<200||response.code>=300)throw new Exception("AI provider HTTP "+response.code+": "+response.body.substring(0,Math.min(500,response.body.length())));
                JSONObject root=new JSONObject(response.body);String content=root.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content").trim();
                content=content.replaceAll("^\\x60{3}json\\s*","").replaceAll("\\x60{3}\\s*$","");
                JSONObject story=new JSONObject(content);
                main.post(()->{
                    titleInput.setText(story.optString("title","Untitled Story"));characterInput.setText(story.optString("character_bible",""));
                    JSONArray sc=story.optJSONArray("scenes");StringBuilder sb=new StringBuilder();if(sc!=null)for(int i=0;i<sc.length();i++)sb.append(sc.optString(i)).append("\\n");sceneInput.setText(sb.toString());
                    JSONArray ds=story.optJSONArray("dialogue");StringBuilder db=new StringBuilder();if(ds!=null)for(int i=0;i<ds.length();i++){JSONObject d=ds.optJSONObject(i);db.append(d.optString("english","")).append(" | ").append(d.optString("myanmar","")).append("\\n");}dialogueInput.setText(db.toString());
                    setStatus("AI story draft ready. Review and edit every scene before rendering.");
                });
            }catch(Exception e){setStatus("AI assist error: "+e.getMessage());}
            finally{setBusy(false);}
        }).start();
    }
    private void offlineStoryAssist(String prompt){
        String[] ideas=prompt.split("[.!?。！？]+");StringBuilder scenes=new StringBuilder();int count=0;
        for(String idea:ideas){String v=idea.trim();if(v.isEmpty())continue;scenes.append("Scene ").append(++count).append(": ").append(v).append("\\n");if(count==6)break;}
        if(count==0)scenes.append("Scene 1: Introduce the main character\\nScene 2: A surprising problem appears\\nScene 3: Action and comic twist\\nScene 4: Happy ending");
        sceneInput.setText(scenes.toString());setStatus("Offline storyboard outline created. Add character details and bilingual dialogue; connect an AI provider for full AI writing.");
    }

    private void setStatus(String s){main.post(()->status.setText(s));}
    private void setBusy(boolean b){
        busy=b;main.post(()->{startButton.setEnabled(!b);checkButton.setEnabled(!b);downloadButton.setEnabled(!b);saveButton.setEnabled(!b);});
    }
    private boolean ensureStoragePermission(){
        if(Build.VERSION.SDK_INT>=23 && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED)return false;
        return true;
    }
    private void requestStoragePermission(){
        if(Build.VERSION.SDK_INT>=23)requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},701);
    }
    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] results){
        super.onRequestPermissionsResult(requestCode,permissions,results);
        if(requestCode==701){
            if(results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED&&pendingStart){pendingStart=false;startRender();}
            else{pendingStart=false;setStatus("Storage permission is required to save the MP4 in Downloads.");}
        }
    }

    private SharedPreferences prefs(){return getSharedPreferences(PREFS,MODE_PRIVATE);}
    private SecretKey getSecretKey() throws Exception{
        KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);
        if(ks.containsAlias(KEY_ALIAS))return ((KeyStore.SecretKeyEntry)ks.getEntry(KEY_ALIAS,null)).getSecretKey();
        KeyGenerator kg=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
        kg.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setRandomizedEncryptionRequired(true).build());
        return kg.generateKey();
    }
    private String encrypt(String plain) throws Exception{
        Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,getSecretKey());
        byte[] cipher=c.doFinal(plain.getBytes("UTF-8"));byte[] iv=c.getIV();byte[] all=new byte[iv.length+cipher.length];
        System.arraycopy(iv,0,all,0,iv.length);System.arraycopy(cipher,0,all,iv.length,cipher.length);
        return Base64.encodeToString(all,Base64.NO_WRAP);
    }
    private String decrypt(String encoded) throws Exception{
        byte[] all=Base64.decode(encoded,Base64.NO_WRAP);byte[] iv=new byte[12];byte[] cipher=new byte[all.length-12];
        System.arraycopy(all,0,iv,0,12);System.arraycopy(all,12,cipher,0,cipher.length);
        Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,getSecretKey(),new GCMParameterSpec(128,iv));
        return new String(c.doFinal(cipher),"UTF-8");
    }
    private String loadToken(){
        String e=prefs().getString(TOKEN_KEY,null);if(e==null)return null;
        try{return decrypt(e);}catch(Exception ex){prefs().edit().remove(TOKEN_KEY).apply();return null;}
    }
    private void saveToken(){
        String candidate=tokenInput.getText().toString().trim();
        if(candidate.length()<20){setStatus("Paste a valid Fine-grained token first.");return;}
        setBusy(true);setStatus("Validating token with GitHub…");
        new Thread(()->{
            try{
                ApiResponse r=api("GET",API+"/actions/workflows/"+WORKFLOW,candidate,null);
                if(r.code!=200)throw new Exception("GitHub token validation failed (HTTP "+r.code+"). Check token and repository permissions.");
                String encrypted=encrypt(candidate);prefs().edit().putString(TOKEN_KEY,encrypted).apply();token=candidate;
                main.post(()->{tokenInput.setText("");saveButton.setText("UPDATE TOKEN");setStatus("Token validated and encrypted in Android Keystore.");});
            }catch(Exception e){setStatus("Token error: "+e.getMessage());}
            finally{setBusy(false);}
        }).start();
    }
    private void clearToken(){
        token=null;prefs().edit().remove(TOKEN_KEY).apply();tokenInput.setText("");saveButton.setText("SAVE TOKEN");setStatus("Saved token removed from this app.");
    }

    private static class ApiResponse{int code;String body,location;ApiResponse(int c,String b,String l){code=c;body=b;location=l;}}
    private ApiResponse api(String method,String url,String bearer,String body)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
        c.setRequestMethod(method);c.setConnectTimeout(25000);c.setReadTimeout(40000);c.setInstanceFollowRedirects(false);
        c.setRequestProperty("Accept","application/vnd.github+json");
        c.setRequestProperty("X-GitHub-Api-Version","2022-11-28");
        c.setRequestProperty("Authorization","Bearer "+bearer);
        if(body!=null){c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json; charset=utf-8");byte[] data=body.getBytes("UTF-8");try(OutputStream o=c.getOutputStream()){o.write(data);}}
        int code=c.getResponseCode();InputStream in=(code>=200&&code<400)?c.getInputStream():c.getErrorStream();StringBuilder b=new StringBuilder();
        if(in!=null){try(BufferedReader br=new BufferedReader(new InputStreamReader(in,"UTF-8"))){String line;while((line=br.readLine())!=null)b.append(line);}}
        String location=c.getHeaderField("Location");c.disconnect();return new ApiResponse(code,b.toString(),location);
    }

    private void startRender(){
        if(busy)return;
        final String style=styleSpinner.getSelectedItem().toString();
        final String duration=durationSpinner.getSelectedItem().toString();
        final String resolution=resolutionSpinner.getSelectedItem().toString();
        final String requestId=UUID.randomUUID().toString().replace("-","").substring(0,12);
        setBusy(true);setStatus("Sending render request to GitHub Actions…");
        new Thread(()->{
            try{
                JSONObject inputs=new JSONObject();inputs.put("style",style);inputs.put("duration",duration);inputs.put("resolution",resolution);inputs.put("request_id",requestId);\n                String project=buildProject().toString();\n                if(project.length()>45000)throw new Exception("Project is too large. Shorten the prompt/dialogue and try again.");\n                inputs.put("project_json",project);
                JSONObject payload=new JSONObject();payload.put("ref","main");payload.put("inputs",inputs);
                ApiResponse dispatched=api("POST",API+"/actions/workflows/"+WORKFLOW+"/dispatches",token,payload.toString());
                if(dispatched.code!=204)throw new Exception("Workflow dispatch failed (HTTP "+dispatched.code+"): "+dispatched.body);
                setStatus("Render request accepted. Waiting for GitHub run… Request "+requestId);
                JSONObject run=null;
                for(int i=0;i<180;i++){
                    Thread.sleep(10000);
                    ApiResponse rr=api("GET",API+"/actions/workflows/"+WORKFLOW+"/runs?event=workflow_dispatch&per_page=30",token,null);
                    if(rr.code!=200)throw new Exception("Could not find workflow run (HTTP "+rr.code+"): "+rr.body);
                    JSONArray arr=new JSONObject(rr.body).optJSONArray("workflow_runs");
                    if(arr!=null)for(int j=0;j<arr.length();j++){
                        JSONObject item=arr.getJSONObject(j);
                        String title=item.optString("display_title",item.optString("name",""));
                        if(title.contains(requestId)){run=item;break;}
                    }
                    if(run!=null)break;
                    setStatus("Waiting for run registration… "+(i+1)*10+" seconds");
                }
                if(run==null)throw new Exception("GitHub accepted the request but the run ID was not found. Use CHECK RENDER STATUS later.");
                long runId=run.getLong("id");prefs().edit().putLong(RUN_KEY,runId).apply();
                setStatus("Cloud render queued. Run ID "+runId);
                JSONObject latest=run;
                for(int i=0;i<2160;i++){
                    ApiResponse sr=api("GET",API+"/actions/runs/"+runId,token,null);
                    if(sr.code!=200)throw new Exception("Status check failed (HTTP "+sr.code+"): "+sr.body);
                    latest=new JSONObject(sr.body);
                    String state=latest.optString("status","unknown");
                    String conclusion=latest.optString("conclusion","");
                    setStatus("Render "+state+" — "+style+" / "+duration+" min / "+resolution+"\nRun ID: "+runId);
                    if("completed".equals(state)){
                        if(!"success".equals(conclusion))throw new Exception("Render finished with result: "+conclusion+". Open the run logs to inspect the failure.");
                        setStatus("Render complete. Downloading MP4 artifact…");
                        downloadArtifact(runId);
                        return;
                    }
                    Thread.sleep(10000);
                }
                throw new Exception("Render is still running. Use CHECK RENDER STATUS and DOWNLOAD LATEST MP4 later.");
            }catch(Exception e){setStatus("Render error: "+e.getMessage());}
            finally{setBusy(false);}
        }).start();
    }

    private long lastRunId(){return prefs().getLong(RUN_KEY,-1);}
    private void checkLastRun(){
        if(token==null){setStatus("Save your GitHub token first.");return;}
        long id=lastRunId();if(id<=0){setStatus("No render run saved yet.");return;}
        if(busy)return;setBusy(true);
        new Thread(()->{
            try{
                ApiResponse r=api("GET",API+"/actions/runs/"+id,token,null);
                if(r.code!=200)throw new Exception("Status request failed: HTTP "+r.code+" "+r.body);
                JSONObject j=new JSONObject(r.body);
                setStatus("Render "+j.optString("status")+" / "+j.optString("conclusion","in progress")+"\nRun ID: "+id);
            }catch(Exception e){setStatus("Status error: "+e.getMessage());}
            finally{setBusy(false);}
        }).start();
    }
    private void downloadLastRun(){
        if(token==null){setStatus("Save your GitHub token first.");return;}
        long id=lastRunId();if(id<=0){setStatus("No render run saved yet.");return;}
        if(!ensureStoragePermission()){pendingStart=false;requestStoragePermission();setStatus("Allow storage permission, then tap DOWNLOAD LATEST MP4 again.");return;}
        if(busy)return;setBusy(true);
        new Thread(()->{try{downloadArtifact(id);}catch(Exception e){setStatus("Download error: "+e.getMessage());}finally{setBusy(false);}}).start();
    }

    private void downloadArtifact(long runId)throws Exception{
        ApiResponse ar=api("GET",API+"/actions/runs/"+runId+"/artifacts?per_page=30",token,null);
        if(ar.code!=200)throw new Exception("Artifact list failed (HTTP "+ar.code+"): "+ar.body);
        JSONArray artifacts=new JSONObject(ar.body).optJSONArray("artifacts");
        if(artifacts==null||artifacts.length()==0)throw new Exception("No MP4 artifact found for this run yet.");
        JSONObject chosen=null;
        for(int i=0;i<artifacts.length();i++){
            JSONObject a=artifacts.getJSONObject(i);
            if(a.optBoolean("expired",false))continue;
            if(a.optString("name","").startsWith("GI-KO-KO-")){chosen=a;break;}
        }
        if(chosen==null)throw new Exception("No available G.I KO KO MP4 artifact. It may have expired.");
        long artifactId=chosen.getLong("id");
        setStatus("Downloading artifact ZIP…");
        ApiResponse redirect=api("GET",API+"/actions/artifacts/"+artifactId+"/zip",token,null);
        if(redirect.code!=302||redirect.location==null)throw new Exception("Artifact download link failed (HTTP "+redirect.code+"): "+redirect.body);
        HttpURLConnection c=(HttpURLConnection)new URL(redirect.location).openConnection();
        c.setConnectTimeout(30000);c.setReadTimeout(0);c.setInstanceFollowRedirects(true);
        File zip=new File(getCacheDir(),"gikoko-render-"+runId+".zip");
        try(InputStream in=new BufferedInputStream(c.getInputStream());FileOutputStream out=new FileOutputStream(zip)){
            byte[] buf=new byte[64*1024];int n;long total=0;
            while((n=in.read(buf))!=-1){out.write(buf,0,n);total+=n;if(total%(5*1024*1024)<buf.length)setStatus("Downloading MP4 package… "+(total/(1024*1024))+" MB");}
        }finally{c.disconnect();}
        File downloads=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if(!downloads.exists()&&!downloads.mkdirs())throw new Exception("Could not create Downloads folder.");
        String saved=null;
        try(ZipInputStream zin=new ZipInputStream(new BufferedInputStream(new FileInputStream(zip)))){
            ZipEntry entry;
            while((entry=zin.getNextEntry())!=null){
                if(!entry.isDirectory()&&entry.getName().toLowerCase().endsWith(".mp4")){
                    String name=new File(entry.getName()).getName();
                    File target=new File(downloads,name);
                    try(FileOutputStream out=new FileOutputStream(target)){
                        byte[] buf=new byte[64*1024];int n;while((n=zin.read(buf))!=-1)out.write(buf,0,n);
                    }
                    saved=target.getAbsolutePath();break;
                }
            }
        }finally{zip.delete();}
        if(saved==null)throw new Exception("ZIP downloaded, but no MP4 file was inside it.");
        setStatus("SUCCESS — MP4 saved to Downloads.\n"+saved);
    }
}
