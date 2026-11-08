package com.gikoko.lemoopuscar;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.view.View;
import android.widget.*;
import android.text.InputType;
import org.json.*;
import java.io.*;
import java.net.*;
import java.security.KeyStore;
import java.util.*;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** On-device provider manager. API keys are encrypted with Android Keystore. */
public class ProviderManager {
    public interface StoryCallback { void done(JSONObject story, String error, String provider); }
    private final Activity activity;
    private final SharedPreferences prefs;
    private static final String PREFS="studio_secure_prefs";
    private static final String KEY_ALIAS="GIKOKO_PROVIDER_KEY_V1";
    private static final String[] PROVIDERS={"Gemini","Groq","OpenRouter","Custom"};
    private static final String[] IDS={"gemini","groq","openrouter","custom"};
    private static final String[] BASES={
        "https://generativelanguage.googleapis.com/v1beta/openai",
        "https://api.groq.com/openai/v1",
        "https://openrouter.ai/api/v1",
        "https://api.example.com/v1"
    };

    public ProviderManager(Activity a){activity=a;prefs=a.getSharedPreferences(PREFS,Activity.MODE_PRIVATE);}

    private String id(String provider){for(int i=0;i<PROVIDERS.length;i++)if(PROVIDERS[i].equals(provider))return IDS[i];return "custom";}
    private String defaultBase(String provider){for(int i=0;i<PROVIDERS.length;i++)if(PROVIDERS[i].equals(provider))return BASES[i];return BASES[3];}
    private String key(String id,String field){return "provider_"+id+"_"+field;}
    private EditText field(LinearLayout box,String value,String hint,boolean secret){
        TextView label=new TextView(activity);label.setText(hint);label.setTextSize(12);label.setTextColor(0xfff5e2b1);box.addView(label);
        EditText e=new EditText(activity);e.setSingleLine(true);e.setTextColor(0xffffffff);e.setHintTextColor(0xff999999);e.setHint(hint);e.setText(value);
        if(secret)e.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        box.addView(e,new LinearLayout.LayoutParams(-1,-2));return e;
    }
    private CheckBox check(String text,boolean value,LinearLayout box){CheckBox c=new CheckBox(activity);c.setText(text);c.setTextColor(0xffffffff);c.setChecked(value);box.addView(c);return c;}
    private LinearLayout section(LinearLayout root,String title){
        TextView t=new TextView(activity);t.setText(title);t.setTextSize(16);t.setTextColor(0xffffd780);t.setPadding(0,16,0,5);root.addView(t);
        LinearLayout box=new LinearLayout(activity);box.setOrientation(LinearLayout.VERTICAL);root.addView(box);return box;
    }
    public void open(){
        ScrollView scroll=new ScrollView(activity);
        LinearLayout root=new LinearLayout(activity);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(22,12,22,20);root.setBackgroundColor(0xff080b12);scroll.addView(root);
        LinearLayout textBox=section(root,"TEXT AI PROVIDERS — Gemini / Groq / OpenRouter / Custom");
        Spinner primary=new Spinner(activity);ArrayAdapter<String> pa=new ArrayAdapter<>(activity,android.R.layout.simple_spinner_item,PROVIDERS);pa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);primary.setAdapter(pa);textBox.addView(primary);
        String savedPrimary=prefs.getString("provider_primary","Gemini");primary.setSelection(Math.max(0,Arrays.asList(PROVIDERS).indexOf(savedPrimary)));
        Spinner editProvider=new Spinner(activity);ArrayAdapter<String> ea=new ArrayAdapter<>(activity,android.R.layout.simple_spinner_item,PROVIDERS);ea.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);editProvider.setAdapter(ea);textBox.addView(editProvider);
        EditText base=field(textBox,defaultBase("Gemini"),"Provider Base URL",false);
        EditText model=field(textBox,"","Model ID",false);
        EditText apiKey=field(textBox,"","API Key (encrypted on device)",true);
        CheckBox freeOnly=check("Free-only model filter (OpenRouter/custom require explicit free marker; Gemini/Groq use their provider free-tier catalogs, subject to quota/model access)",true,textBox);
        CheckBox fallback=check("Enable automatic fallback to other configured providers",true,textBox);
        fallback.setChecked(prefs.getBoolean("provider_auto_fallback",true));
        TextView modelInfo=new TextView(activity);modelInfo.setTextColor(0xffcccccc);modelInfo.setTextSize(12);textBox.addView(modelInfo);
        editProvider.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            public void onNothingSelected(android.widget.AdapterView<?> p){}
            public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long row){
                String pid=IDS[pos];base.setText(prefs.getString(key(pid,"base"),BASES[pos]));
                model.setText(prefs.getString(key(pid,"model"),""));
                try{apiKey.setText("");String enc=prefs.getString(key(pid,"secret"),null);if(enc!=null)apiKey.setHint("Saved key exists — enter new key to replace");}catch(Exception ignored){}
                modelInfo.setText("Editing "+PROVIDERS[pos]+" • model discovery uses GET /models");
            }
        });
        Button discover=new Button(activity);discover.setText("DISCOVER MODELS");textBox.addView(discover);
        Button test=new Button(activity);test.setText("TEST CONNECTION");textBox.addView(test);
        Button save=new Button(activity);save.setText("SAVE THIS PROVIDER");textBox.addView(save);
        discover.setOnClickListener(v->{
            final String pid=IDS[editProvider.getSelectedItemPosition()],url=base.getText().toString().trim(),secret=readKey(pid,apiKey);
            final boolean applyFreeOnly=freeOnly.isChecked();
            modelInfo.setText("Loading models…");
            new Thread(()->{try{
                JSONObject response=request(url.replaceAll("/+$","")+"/models",secret,null);
                JSONArray data=response.optJSONArray("data");if(data==null)throw new Exception("Provider returned no data[] model list.");
                ArrayList<String> found=new ArrayList<>();
                for(int i=0;i<data.length();i++){
                    JSONObject item=data.optJSONObject(i);if(item==null)continue;
                    String mid=item.optString("id","");if(mid.isEmpty())continue;
                    if(applyFreeOnly&&!isFreeModel(pid,item,mid))continue;
                    found.add(mid);
                }
                activity.runOnUiThread(()->{
                    if(found.isEmpty()){modelInfo.setText("No models passed the Free-only filter. Turn it off to inspect the full catalog.");return;}
                    String[] choices=found.toArray(new String[0]);
                    new AlertDialog.Builder(activity).setTitle("Available models ("+choices.length+")").setItems(choices,(d,which)->{model.setText(choices[which]);modelInfo.setText("Selected: "+choices[which]);}).setNegativeButton("Close",null).show();
                });
            }catch(Exception e){activity.runOnUiThread(()->modelInfo.setText("Discovery failed: "+e.getMessage()));}}).start();
        });
        test.setOnClickListener(v->{
            String pid=IDS[editProvider.getSelectedItemPosition()],url=base.getText().toString().trim(),secret=readKey(pid,apiKey);
            modelInfo.setText("Testing provider…");
            new Thread(()->{try{request(url.replaceAll("/+$","")+"/models",secret,null);activity.runOnUiThread(()->modelInfo.setText("Connection OK — /models endpoint responded."));}
                catch(Exception e){activity.runOnUiThread(()->modelInfo.setText("Connection failed: "+e.getMessage()));}}).start();
        });
        save.setOnClickListener(v->{
            String pid=IDS[editProvider.getSelectedItemPosition()],url=base.getText().toString().trim(),mid=model.getText().toString().trim(),secret=apiKey.getText().toString().trim();
            if(!url.startsWith("https://")||mid.isEmpty()){modelInfo.setText("Enter HTTPS Base URL and Model ID first.");return;}
            try{
                SharedPreferences.Editor ed=prefs.edit().putString(key(pid,"base"),url).putString(key(pid,"model"),mid);
                if(!secret.isEmpty())ed.putString(key(pid,"secret"),encrypt(secret));
                ed.putString("provider_primary",primary.getSelectedItem().toString()).putBoolean("provider_auto_fallback",fallback.isChecked()).apply();
                apiKey.setText("");modelInfo.setText("Saved "+PROVIDERS[editProvider.getSelectedItemPosition()]+" configuration. Key encrypted.");
            }catch(Exception e){modelInfo.setText("Save failed: "+e.getMessage());}
        });

        LinearLayout media=section(root,"MEDIA PROVIDERS — independent settings");
        EditText imageBase=field(media,prefs.getString("media_image_base",""),"Image API Base URL",false);
        EditText imageModel=field(media,prefs.getString("media_image_model",""),"Image Model",false);
        EditText imageKey=field(media,"","Image API Key (encrypted)",true);
        EditText videoBase=field(media,prefs.getString("media_video_base",""),"Video API Base URL",false);
        EditText videoModel=field(media,prefs.getString("media_video_model",""),"Video Model",false);
        EditText videoKey=field(media,"","Video API Key (encrypted)",true);
        EditText audioBase=field(media,prefs.getString("media_audio_base",""),"Audio API Base URL",false);
        EditText audioModel=field(media,prefs.getString("media_audio_model",""),"Audio Model",false);
        EditText audioKey=field(media,"","Audio API Key (encrypted)",true);
        Button saveMedia=new Button(activity);saveMedia.setText("SAVE IMAGE / VIDEO / AUDIO PROVIDERS");media.addView(saveMedia);
        saveMedia.setOnClickListener(v->{
            try{SharedPreferences.Editor ed=prefs.edit();
                putMedia(ed,"image",imageBase,imageModel,imageKey);putMedia(ed,"video",videoBase,videoModel,videoKey);putMedia(ed,"audio",audioBase,audioModel,audioKey);
                ed.apply();imageKey.setText("");videoKey.setText("");audioKey.setText("");
                modelInfo.setText("Media provider settings saved. API keys encrypted on device.");
            }catch(Exception e){modelInfo.setText("Media settings error: "+e.getMessage());}
        });
        TextView note=new TextView(activity);note.setText("Note: Media provider settings are stored separately for future image/video/audio adapters. They do not change the current GitHub CPU renderer or create AI video unless a compatible media adapter is implemented.");note.setTextColor(0xffbbbbbb);note.setTextSize(12);note.setPadding(0,12,0,4);root.addView(note);
        new AlertDialog.Builder(activity).setTitle("G.I Ko Ko ViD Gen — Provider Control Center").setView(scroll).setPositiveButton("CLOSE",null).show();
    }
    private void putMedia(SharedPreferences.Editor ed,String type,EditText base,EditText model,EditText secret)throws Exception{
        ed.putString("media_"+type+"_base",base.getText().toString().trim()).putString("media_"+type+"_model",model.getText().toString().trim());
        String k=secret.getText().toString().trim();if(!k.isEmpty())ed.putString("media_"+type+"_secret",encrypt(k));
    }
    private String readKey(String pid,EditText input){
        String typed=input.getText().toString().trim();if(!typed.isEmpty())return typed;
        try{String e=prefs.getString(key(pid,"secret"),null);return e==null?"":decrypt(e);}catch(Exception ex){return "";}
    }
    private boolean isFreeModel(String provider,JSONObject item,String mid){
        if("gemini".equals(provider)||"groq".equals(provider))return true; // Provider-level free-tier APIs; quotas/model access vary.
        if(mid.toLowerCase(Locale.US).endsWith(":free"))return true;
        JSONObject pricing=item.optJSONObject("pricing");
        if(pricing!=null){
            double prompt=pricing.optDouble("prompt",-1),completion=pricing.optDouble("completion",-1);
            if(prompt==0.0&&completion==0.0)return true;
        }
        return false;
    }
    private JSONObject request(String url,String secret,String body)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
        c.setConnectTimeout(18000);c.setReadTimeout(35000);c.setRequestMethod(body==null?"GET":"POST");
        c.setRequestProperty("Accept","application/json");if(secret!=null&&!secret.isEmpty())c.setRequestProperty("Authorization","Bearer "+secret);
        if(body!=null){c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");try(OutputStream o=c.getOutputStream()){o.write(body.getBytes("UTF-8"));}}
        int code=c.getResponseCode();InputStream in=(code>=200&&code<300)?c.getInputStream():c.getErrorStream();StringBuilder b=new StringBuilder();
        if(in!=null)try(BufferedReader r=new BufferedReader(new InputStreamReader(in,"UTF-8"))){String line;while((line=r.readLine())!=null)b.append(line);}
        c.disconnect();if(code<200||code>=300)throw new Exception("HTTP "+code+" "+b.toString().substring(0,Math.min(250,b.length())));
        return new JSONObject(b.toString());
    }
    public void generateStory(String prompt,StoryCallback callback){
        new Thread(()->{
            String preferred=prefs.getString("provider_primary","Gemini");ArrayList<String> order=new ArrayList<>();order.add(preferred);
            if(prefs.getBoolean("provider_auto_fallback",true))for(String p:PROVIDERS)if(!p.equals(preferred))order.add(p);
            String last="No configured provider credentials.";
            for(String provider:order){
                String pid=id(provider),base=prefs.getString(key(pid,"base"),defaultBase(provider)),model=prefs.getString(key(pid,"model"),"");
                String secret="";try{String enc=prefs.getString(key(pid,"secret"),null);if(enc!=null)secret=decrypt(enc);}catch(Exception ignored){}
                if(model.isEmpty()||secret.isEmpty())continue;
                try{
                    JSONObject body=new JSONObject();body.put("model",model);body.put("temperature",0.7);
                    JSONArray messages=new JSONArray();JSONObject system=new JSONObject();system.put("role","system");
                    system.put("content","You are a professional animation screenwriter. Return ONLY valid JSON with keys title, character_bible, scenes (array of 8-30 concise scene descriptions), dialogue (array of 12-60 objects with english and myanmar), and animation_code (a JavaScript function expression string). animation_code must look like function(api){ api.scene({title:\"...\", description:\"...\", duration:12, background:\"#142b2a\", motion:\"bounce\"}); }. Call api.scene once per scene. Use only api.scene and plain values; never use imports, require, fetch, network, filesystem, process, eval, Function, timers or browser APIs. Title <=80 chars, description <=500 chars, background #RRGGBB, duration positive seconds, motion one of float, bounce, slide, pulse, still. Produce 8-30 scenes. Keep the same character design and story continuity.");
                    JSONObject user=new JSONObject();user.put("role","user");user.put("content","Create a complete bilingual long-form animated storyboard AND executable scene-plan code for the requested duration. animation_code must be a function expression string calling api.scene({...}) for every scene. Include enough distinct scenes and dialogue turns. Keep character design consistent. Idea: "+prompt);
                    messages.put(system);messages.put(user);body.put("messages",messages);
                    JSONObject response=request(base.replaceAll("/+$","")+"/chat/completions",secret,body.toString());
                    String content=response.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content").trim();
                    content=content.replaceAll("^\\x60{3}json\\s*","").replaceAll("\\x60{3}\\s*$","");
                    JSONObject story=new JSONObject(content);
                    activity.runOnUiThread(()->callback.done(story,null,provider));return;
                }catch(Exception e){last=provider+": "+e.getMessage();}
            }
            final String error=last;activity.runOnUiThread(()->callback.done(null,error,null));
        }).start();
    }
    private SecretKey secretKey()throws Exception{
        KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);
        if(ks.containsAlias(KEY_ALIAS))return ((KeyStore.SecretKeyEntry)ks.getEntry(KEY_ALIAS,null)).getSecretKey();
        KeyGenerator kg=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
        kg.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setRandomizedEncryptionRequired(true).build());
        return kg.generateKey();
    }
    private String encrypt(String value)throws Exception{
        Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,secretKey());byte[] encrypted=c.doFinal(value.getBytes("UTF-8"));
        byte[] iv=c.getIV(),all=new byte[iv.length+encrypted.length];System.arraycopy(iv,0,all,0,iv.length);System.arraycopy(encrypted,0,all,iv.length,encrypted.length);
        return Base64.encodeToString(all,Base64.NO_WRAP);
    }
    private String decrypt(String value)throws Exception{
        byte[] all=Base64.decode(value,Base64.NO_WRAP),iv=Arrays.copyOfRange(all,0,12),data=Arrays.copyOfRange(all,12,all.length);
        Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,secretKey(),new GCMParameterSpec(128,iv));return new String(c.doFinal(data),"UTF-8");
    }
}
