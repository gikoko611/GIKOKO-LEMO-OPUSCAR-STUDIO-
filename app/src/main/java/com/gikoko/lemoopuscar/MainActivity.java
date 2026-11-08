package com.gikoko.lemoopuscar;

import android.Manifest;
import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.content.ContentValues;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.View;
import android.widget.FrameLayout;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.Gravity;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
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
    private Spinner styleSpinner,durationSpinner,resolutionSpinner,aspectRatioSpinner,frameRateSpinner,renderModeSpinner,localDurationSpinner,themeSpinner;
    private EditText watermarkInput;
    private String watermarkLogoBase64="";
    private android.app.ProgressDialog generationDialog;
    private TextView renderCenterLog;
    private TextView status;
    private Button startButton,saveButton,checkButton,downloadButton,retryButton;
    private Spinner voiceSpinner;
    private CheckBox musicCheck,sfxCheck,subtitleCheck;
    private String token;
    private String animationCode="";
    private boolean busy=false,pendingStart=false;
    private int themeBg=0xff09091b,themeCard=0xff17152b,themeBorder=0xff7158b8,themeAccent=0xff8be9ff,themeText=0xfff1efff,themeMuted=0xffaaa6c8,themeButtonA=0xff6b3de6,themeButtonB=0xffd14aa8;
    private static final String DIAGNOSTIC_FILE="diagnostic.log";

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(8,11,18));
        getWindow().setNavigationBarColor(Color.rgb(8,11,18));
        token=loadToken();
        applyThemeFromName(prefs().getString("ui_theme","NEBULA PRISM"));
        installCrashLogger();
        buildUi();
    }

    private void applyThemeFromName(String name){
        if("OBSIDIAN LUX".equals(name)){themeBg=0xff080b10;themeCard=0xff171c24;themeBorder=0xffb38a4a;themeAccent=0xffe8c77a;themeText=0xfff4f0e6;themeMuted=0xffaaa18e;themeButtonA=0xff8b6a34;themeButtonB=0xffc8a85c;}
        else if("AURORA GLASS".equals(name)){themeBg=0xff07131d;themeCard=0xff102637;themeBorder=0xff36b8c6;themeAccent=0xff8fe7ff;themeText=0xffe9faff;themeMuted=0xff94b8c9;themeButtonA=0xff167aab;themeButtonB=0xff7157d8;}
        else {themeBg=0xff09091b;themeCard=0xff17152b;themeBorder=0xff7158b8;themeAccent=0xff8be9ff;themeText=0xfff1efff;themeMuted=0xffaaa6c8;themeButtonA=0xff6b3de6;themeButtonB=0xffd14aa8;}
    }
    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
    private android.graphics.drawable.GradientDrawable shape(int fill,int stroke,int radius){
        android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable();
        d.setColor(fill);d.setCornerRadius(dp(radius));if(stroke!=0)d.setStroke(dp(1),stroke);return d;
    }
    private TextView label(String s,int size){
        TextView v=new TextView(this);v.setText(s);v.setTextColor(themeText);v.setTextSize(size);
        v.setPadding(0,dp(8),0,dp(5));return v;
    }
    private Spinner spinner(String[] values){
        Spinner s=new Spinner(this);ArrayAdapter<String>a=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,values){
            @Override public View getView(int p,View c,android.view.ViewGroup parent){TextView t=(TextView)super.getView(p,c,parent);t.setTextColor(0xffe7fbff);t.setTextSize(14);t.setPadding(dp(10),dp(9),dp(10),dp(9));return t;}
            @Override public View getDropDownView(int p,View c,android.view.ViewGroup parent){TextView t=(TextView)super.getDropDownView(p,c,parent);t.setTextColor(0xffe7fbff);t.setBackgroundColor(0xff101d2a);t.setPadding(dp(14),dp(12),dp(14),dp(12));return t;}
        };
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);s.setAdapter(a);
        s.setBackground(shape(0xff101d2a,0xff245a70,12));return s;
    }
    private Button button(String s){
        Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(0xffefffff);
        b.setTextSize(13);b.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));
        b.setBackground(new android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{themeButtonA,themeButtonB}));
        b.setPadding(dp(12),dp(8),dp(12),dp(8));b.setElevation(dp(4));return b;
    }
    private LinearLayout column(){
        LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;
    }
    private LinearLayout card(LinearLayout parent){
        LinearLayout c=column();c.setPadding(dp(16),dp(13),dp(16),dp(15));
        c.setBackground(shape(themeCard,themeBorder,20));c.setElevation(dp(5));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(13);parent.addView(c,lp);return c;
    }
    private void heading(LinearLayout box,String text,String hint){
        TextView t=label(text,16);t.setTextColor(themeAccent);t.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));box.addView(t);
        if(hint!=null&&!hint.isEmpty()){TextView h=label(hint,11);h.setTextColor(0xff8daab8);h.setPadding(0,0,0,dp(7));box.addView(h);}
    }
    private void buildUi(){
        getWindow().setStatusBarColor(0xff07111b);getWindow().setNavigationBarColor(0xff07111b);
        FrameLayout shell=new FrameLayout(this);
        shell.setBackground(new android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
            new int[]{themeBg,themeCard,themeBg}));
        CyberGridView grid=new CyberGridView(this);shell.addView(grid,new FrameLayout.LayoutParams(-1,-1));
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);
        LinearLayout root=column();root.setPadding(dp(18),dp(14),dp(18),dp(30));scroll.addView(root);
        shell.addView(scroll);setContentView(shell);

        FrameLayout hero=new FrameLayout(this);hero.setBackground(new android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
            new int[]{themeButtonA,themeCard,themeBg}));
        hero.setElevation(dp(8));hero.setPadding(dp(18),dp(20),dp(18),dp(18));
        LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,dp(190));hp.bottomMargin=dp(16);root.addView(hero,hp);
        CyberGridView heroGrid=new CyberGridView(this);hero.addView(heroGrid,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout brand=column();brand.setGravity(Gravity.CENTER);FrameLayout.LayoutParams bp=new FrameLayout.LayoutParams(-1,-1);hero.addView(brand,bp);
        ImageView logo=new ImageView(this);logo.setImageResource(R.drawable.ic_launcher);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);logo.setElevation(dp(12));
        brand.addView(logo,new LinearLayout.LayoutParams(dp(88),dp(88)));
        android.animation.ValueAnimator pulse=android.animation.ValueAnimator.ofFloat(0.96f,1.045f);
        pulse.setDuration(1900);pulse.setRepeatCount(android.animation.ValueAnimator.INFINITE);pulse.setRepeatMode(android.animation.ValueAnimator.REVERSE);
        pulse.addUpdateListener(a->{float v=(Float)a.getAnimatedValue();logo.setScaleX(v);logo.setScaleY(v);logo.setRotationY((v-1f)*220f);});pulse.start();
        TextView appName=label("KO KO  •  ViD GEN",15);appName.setGravity(Gravity.CENTER);appName.setTextColor(0xffe9fbff);
        appName.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));brand.addView(appName);
        TextView credit=label("3D CREATIVE STUDIO   /   POWERED BY I KO KO",9);credit.setGravity(Gravity.CENTER);
        credit.setTextColor(0xff62e8d0);brand.addView(credit);

        LinearLayout quick=card(root);heading(quick,"STUDIO CONTROL","Your creative workspace");
        TextView intro=label("Create animated stories, generate scenes and render your video from one clean workspace.",12);
        intro.setTextColor(0xffa9c4d1);quick.addView(intro);
        LinearLayout quickRow=new LinearLayout(this);quickRow.setOrientation(LinearLayout.HORIZONTAL);quick.addView(quickRow);
        Button settings=button("⚙  SETTINGS");quickRow.addView(settings,new LinearLayout.LayoutParams(0,dp(48),1));
        Button assist=button("✦  AI STORY ASSIST");LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(0,dp(48),1);ap.leftMargin=dp(8);quickRow.addView(assist,ap);
        settings.setOnClickListener(v->openSettings());assist.setOnClickListener(v->assistStory());

        LinearLayout project=card(root);heading(project,"01  •  PROJECT BUILDER","Start with a title and a clear creative idea");
        titleInput=edit("The Golden Kingdom","Film title",false);project.addView(titleInput);
        promptInput=edit("Write a funny adventure in an ancient Myanmar royal palace. Include an unexpected action-comedy twist.","Describe your film idea / prompt",false);
        promptInput.setMinLines(3);project.addView(promptInput);
        characterInput=edit("Main character: a clever but clumsy royal guard. Keep costume, face, colors and personality consistent in every scene.","Character Bible — consistent look and personality",false);
        characterInput.setMinLines(3);project.addView(characterInput);
        sceneInput=edit("Palace courtyard — introduce the guard\nThrone room — a suspicious royal order\nPalace kitchen — comic chase\nRoyal hall — funny reveal","Scene outline — one scene per line",false);
        sceneInput.setMinLines(4);project.addView(sceneInput);
        dialogueInput=edit("Guard: I have everything under control. | အားလုံးကို ကျွန်တော် ထိန်းချုပ်ထားပါတယ်။\nKing: Then why is the palace on fire? | ဒါဆို နန်းတော်က ဘာလို့ မီးလောင်နေတာလဲ။","Dialogue — English | Myanmar per line",false);
        dialogueInput.setMinLines(4);project.addView(dialogueInput);
        LinearLayout config=card(root);heading(config,"VIDEO CONFIGURATION","Set duration, quality, framing and audio before generating");
        config.addView(label("VISUAL STYLE",11));styleSpinner=spinner(new String[]{"hybrid","cartoon2d","cartoon3d","cinematic"});config.addView(styleSpinner);
        config.addView(label("CLOUD FILM LENGTH (MINUTES)",11));durationSpinner=spinner(new String[]{"1","5","10","30"});config.addView(durationSpinner);
        config.addView(label("OUTPUT RESOLUTION",11));resolutionSpinner=spinner(new String[]{"480p","720p"});config.addView(resolutionSpinner);
        config.addView(label("VIDEO VIEW SIZE / ASPECT RATIO",11));aspectRatioSpinner=spinner(new String[]{"16:9","9:16","1:1","4:5"});config.addView(aspectRatioSpinner);
        config.addView(label("FRAME RATE",11));frameRateSpinner=spinner(new String[]{"24","30","60"});config.addView(frameRateSpinner);
        config.addView(label("VOICE-OVER",11));voiceSpinner=spinner(new String[]{"No voice-over","English voice-over","Myanmar voice-over","Bilingual voice-over"});config.addView(voiceSpinner);
        musicCheck=new CheckBox(this);musicCheck.setText("Background music");musicCheck.setTextColor(themeText);musicCheck.setChecked(true);config.addView(musicCheck);
        sfxCheck=new CheckBox(this);sfxCheck.setText("Sound effects");sfxCheck.setTextColor(themeText);sfxCheck.setChecked(true);config.addView(sfxCheck);
        subtitleCheck=new CheckBox(this);subtitleCheck.setText("Myanmar subtitle track");subtitleCheck.setTextColor(themeText);subtitleCheck.setChecked(true);config.addView(subtitleCheck);
        watermarkInput=edit("POWERED BY I KO KO","Watermark text",false);config.addView(label("WATERMARK / BRANDING",11));config.addView(watermarkInput);
        Button addLogo=button("＋  ADD MEDIA / WATERMARK LOGO");project.addView(addLogo);addLogo.setOnClickListener(v->pickWatermarkLogo());
        Button saveProject=button("▣  SAVE PROJECT");project.addView(saveProject);saveProject.setOnClickListener(v->saveProject());
        Button library=button("▤  PROJECT LIBRARY");project.addView(library);library.setOnClickListener(v->openProjectLibrary());

        LinearLayout render=card(root);heading(render,"02  •  RENDER STUDIO","Cloud rendering • MP4 output");
        startButton=button("▶   START VIDEO RENDER");render.addView(startButton);
        startButton.setOnClickListener(v->{if(ensureStoragePermission())startRender();else{pendingStart=true;requestStoragePermission();}});
        LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);rp.topMargin=dp(8);render.addView(actions,rp);
        retryButton=button("↻ RETRY");actions.addView(retryButton,new LinearLayout.LayoutParams(0,dp(46),1));retryButton.setOnClickListener(v->retryFailedRender());
        checkButton=button("◉ STATUS");LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,dp(46),1);cp.leftMargin=dp(7);actions.addView(checkButton,cp);checkButton.setOnClickListener(v->checkLastRun());
        downloadButton=button("↓ MP4");LinearLayout.LayoutParams dp2=new LinearLayout.LayoutParams(0,dp(46),1);dp2.leftMargin=dp(7);actions.addView(downloadButton,dp2);downloadButton.setOnClickListener(v->downloadLastRun());
        Button renderCenter=button("▣  OPEN RENDER CENTER / LOGS");LinearLayout.LayoutParams rclp=new LinearLayout.LayoutParams(-1,dp(46));rclp.topMargin=dp(8);render.addView(renderCenter,rclp);renderCenter.setOnClickListener(v->openRenderCenter());

        LinearLayout statusCard=card(root);heading(statusCard,"LIVE STATUS","Workflow and download updates");
        status=label(token==null?"●  READY  —  Add your GitHub token in Settings.":"●  READY  —  Secure token loaded.",12);
        status.setTextColor(0xff70f5c8);status.setMinLines(2);statusCard.addView(status);
        TextView foot=label("Designed for Android 10 • GitHub Actions build & render • 480p / 720p",10);
        foot.setTextColor(0xff7895a5);foot.setGravity(Gravity.CENTER);root.addView(foot);

        buildSettingsPanel();
        restoreProject();
    }
    private void buildSettingsPanel(){
        LinearLayout panel=column();panel.setPadding(dp(18),dp(8),dp(18),dp(24));
        LinearLayout appearance=card(panel);heading(appearance,"APPEARANCE / 3D THEMES","Switch the entire studio look");
        themeSpinner=spinner(new String[]{"NEBULA PRISM","OBSIDIAN LUX","AURORA GLASS"});appearance.addView(themeSpinner);
        setSpinnerValue(themeSpinner,prefs().getString("ui_theme","NEBULA PRISM"));
        themeSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){} public void onItemSelected(android.widget.AdapterView<?> p,View v,int position,long id){String selected=themeSpinner.getSelectedItem().toString();if(!selected.equals(prefs().getString("ui_theme","NEBULA PRISM"))){saveProject();prefs().edit().putString("ui_theme",selected).apply();applyThemeFromName(selected);if(settingsDialog!=null)settingsDialog.dismiss();settingsDialog=null;buildUi();}}});
        LinearLayout account=card(panel);heading(account,"SECURITY & GITHUB","Token is encrypted locally with Android Keystore");
        TextView help=label("Use a fine-grained token for this repository. Grant Actions read/write and Contents read/write for workflow dispatch and release access.",11);help.setTextColor(0xff9eb7c4);account.addView(help);
        tokenInput=edit("","GitHub fine-grained token",true);account.addView(tokenInput);
        saveButton=button(token==null?"SAVE TOKEN":"UPDATE TOKEN");account.addView(saveButton);saveButton.setOnClickListener(v->saveToken());
        Button clear=button("REMOVE SAVED TOKEN");account.addView(clear);clear.setOnClickListener(v->clearToken());

        LinearLayout ai=card(panel);heading(ai,"AI PROVIDER","OpenAI-compatible endpoint and model");
        aiBaseInput=edit("https://api.openai.com/v1","API base URL",false);ai.addView(aiBaseInput);
        aiModelInput=edit("gpt-4o-mini","Model ID",false);ai.addView(aiModelInput);
        aiKeyInput=edit("","AI API key — encrypted on device",true);ai.addView(aiKeyInput);
        Button saveAi=button("SAVE AI PROVIDER");ai.addView(saveAi);saveAi.setOnClickListener(v->saveAiProvider());
        Button providers=button("◎  PROVIDER MANAGER");ai.addView(providers);providers.setOnClickListener(v->new ProviderManager(this).open());

        LinearLayout output=card(panel);heading(output,"RENDER ENGINE","Choose on-device preview or full cloud production");
        TextView modeLabel=label("RENDER ENGINE",11);output.addView(modeLabel);
        renderModeSpinner=spinner(new String[]{"Cloud Render","Local Device Render"});output.addView(renderModeSpinner);
        setSpinnerValue(renderModeSpinner,prefs().getString("render_mode","Cloud Render"));
        renderModeSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){} public void onItemSelected(android.widget.AdapterView<?> p,View v,int position,long id){prefs().edit().putString("render_mode",renderModeSpinner.getSelectedItem().toString()).apply();}});
        localDurationSpinner=spinner(new String[]{"5","10","15"});output.addView(label("LOCAL CLIP DURATION (SECONDS) — MAX 30s",11));output.addView(localDurationSpinner);
        TextView localNote=label("Local mode exports a silent preview MP4. Use Cloud mode for full-length audio, voice and subtitle mastering.",10);localNote.setTextColor(themeMuted);output.addView(localNote);

        LinearLayout tools=card(panel);heading(tools,"MAINTENANCE","Logs and troubleshooting");
        Button diagnostics=button("⌁  DIAGNOSTIC CENTER  •  VIEW / COPY / EXPORT");tools.addView(diagnostics);
        diagnostics.setOnClickListener(v->showDiagnosticCenter());
        Button done=button("DONE");panel.addView(done);done.setOnClickListener(v->{if(settingsDialog!=null)settingsDialog.dismiss();});
        settingsPanel=panel;
        aiBaseInput.setText(prefs().getString("ai_base","https://api.openai.com/v1"));
        aiModelInput.setText(prefs().getString("ai_model","gpt-4o-mini"));
    }
    private LinearLayout settingsPanel;
    private android.app.AlertDialog settingsDialog;
    private void openSettings(){
        if(settingsDialog==null){
            ScrollView sc=new ScrollView(this);sc.setFillViewport(true);sc.addView(settingsPanel);
            settingsDialog=new android.app.AlertDialog.Builder(this).setTitle("G.I KO KO  /  SETTINGS").setView(sc).setNegativeButton("CLOSE",null).create();
            settingsDialog.setOnShowListener(d->{settingsDialog.getWindow().setBackgroundDrawable(shape(0xff081522,0xff1a6672,18));
                settingsDialog.getWindow().setLayout((int)(getResources().getDisplayMetrics().widthPixels*0.94),
                    (int)(getResources().getDisplayMetrics().heightPixels*0.88));});
        }
        settingsDialog.show();
        settingsDialog.getWindow().setBackgroundDrawable(shape(0xff081522,0xff1a6672,18));
        settingsDialog.getWindow().setLayout((int)(getResources().getDisplayMetrics().widthPixels*0.94),
            (int)(getResources().getDisplayMetrics().heightPixels*0.88));
    }
    private EditText edit(String value,String hint,boolean secret){
        EditText e=new EditText(this);e.setText(value);e.setHint(hint);e.setTextColor(0xffeffaff);e.setHintTextColor(0xff7693a2);
        e.setTextSize(14);e.setSingleLine(!hint.toLowerCase(Locale.US).contains("prompt")&&!hint.toLowerCase(Locale.US).contains("bible")&&!hint.toLowerCase(Locale.US).contains("outline")&&!hint.toLowerCase(Locale.US).contains("dialogue"));
        e.setInputType(secret?(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD):(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES));
        e.setPadding(dp(13),dp(11),dp(13),dp(11));e.setBackground(shape(themeBg,themeBorder,14));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(5);lp.bottomMargin=dp(8);e.setLayoutParams(lp);return e;
    }
    private static class CyberGridView extends View{
        private final android.graphics.Paint p=new android.graphics.Paint(3);
        private long start=System.currentTimeMillis();
        CyberGridView(Activity a){super(a);setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
        @Override protected void onDraw(android.graphics.Canvas c){
            super.onDraw(c);float w=getWidth(),h=getHeight();p.setStyle(android.graphics.Paint.Style.STROKE);p.setStrokeWidth(1f);p.setColor(0x2434e8d0);
            float step=dpStatic(getContext(),28);for(float x=0;x<w;x+=step)c.drawLine(x,0,x,h,p);for(float y=0;y<h;y+=step)c.drawLine(0,y,w,y,p);
            float phase=((System.currentTimeMillis()-start)%4200)/4200f;p.setStyle(android.graphics.Paint.Style.FILL);
            p.setColor(0x5535f4cb);for(int i=0;i<12;i++){float x=(i*83f+phase*w*0.35f)%Math.max(1,w);float y=(i*47f+phase*h*0.2f)%Math.max(1,h);c.drawCircle(x,y,1.6f,p);}
            p.setColor(0x1c20aaff);c.drawRect(0,phase*h, w,phase*h+dpStatic(getContext(),2),p);
            postInvalidateDelayed(90);
        }
        private static float dpStatic(android.content.Context c,float v){return v*c.getResources().getDisplayMetrics().density;}
    }
    private void saveAiProvider(){
        String base=aiBaseInput.getText().toString().trim(),model=aiModelInput.getText().toString().trim(),key=aiKeyInput.getText().toString().trim();
        if(!base.startsWith("https://")||model.isEmpty()){setStatus("AI provider needs an HTTPS Base URL and Model ID.");return;}
        try{prefs().edit().putString("ai_base",base).putString("ai_model",model).apply();if(!key.isEmpty())prefs().edit().putString("ai_key_enc",encrypt(key)).apply();aiKeyInput.setText("");setStatus("AI provider settings saved. API key encrypted on device.");}
        catch(Exception e){recordException("Save AI provider",e);setStatus("Could not encrypt AI key: "+e.getMessage());}
    }
    private JSONObject buildProject(){
        JSONObject p=new JSONObject();
        try{
            p.put("schema_version",1);p.put("title",titleInput.getText().toString().trim());
            p.put("prompt",promptInput.getText().toString().trim());p.put("character_bible",characterInput.getText().toString().trim());
            if(animationCode!=null&&!animationCode.trim().isEmpty())p.put("animation_code",animationCode);
            JSONArray scenes=new JSONArray();for(String line:sceneInput.getText().toString().split("\n"))if(!line.trim().isEmpty())scenes.put(line.trim());p.put("scenes",scenes);
            JSONArray dialogue=new JSONArray();for(String line:dialogueInput.getText().toString().split("\n"))if(!line.trim().isEmpty()){
                String[] parts=line.split("\\|",2);JSONObject d=new JSONObject();d.put("english",parts[0].trim());d.put("myanmar",parts.length>1?parts[1].trim():"");dialogue.put(d);
            }p.put("dialogue",dialogue);
            p.put("voice",voiceSpinner.getSelectedItem().toString());p.put("background_music",musicCheck.isChecked());p.put("sfx",sfxCheck.isChecked());p.put("myanmar_subtitles",subtitleCheck.isChecked());
            p.put("style",styleSpinner.getSelectedItem().toString());p.put("duration_minutes",durationSpinner.getSelectedItem().toString());p.put("resolution",resolutionSpinner.getSelectedItem().toString());
            p.put("aspect_ratio",aspectRatioSpinner==null?"16:9":aspectRatioSpinner.getSelectedItem().toString());
            p.put("fps",frameRateSpinner==null?"24":frameRateSpinner.getSelectedItem().toString());
            p.put("watermark",watermarkInput==null?"POWERED BY I KO KO":watermarkInput.getText().toString().trim());
            if(!watermarkLogoBase64.isEmpty())p.put("watermark_logo_base64",watermarkLogoBase64);
        }catch(Exception ignored){}return p;
    }

    private void openProjectLibrary(){
        try{
            JSONArray library=new JSONArray(prefs().getString("project_library","[]"));
            if(library.length()==0){new android.app.AlertDialog.Builder(this).setTitle("PROJECT LIBRARY").setMessage("No saved projects yet. Save a project to add it here.").setPositiveButton("OK",null).show();return;}
            String[] names=new String[library.length()];
            for(int i=0;i<library.length();i++){JSONObject item=library.optJSONObject(i);names[i]=item==null?"Untitled":item.optString("title","Untitled");}
            new android.app.AlertDialog.Builder(this).setTitle("OPEN SAVED PROJECT").setItems(names,(d,which)->{JSONObject item=library.optJSONObject(which);if(item!=null){JSONObject p=item.optJSONObject("project");if(p!=null){prefs().edit().putString("last_project",p.toString()).apply();buildUi();}}}).setNegativeButton("CLOSE",null).show();
        }catch(Exception e){setStatus("Project Library error: "+e.getMessage());}
    }
    private void pickWatermarkLogo(){
        Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("image/*");startActivityForResult(intent,702);
    }
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==702&&resultCode==RESULT_OK&&data!=null&&data.getData()!=null){
            try(InputStream in=getContentResolver().openInputStream(data.getData())){
                Bitmap source=BitmapFactory.decodeStream(in);if(source==null)throw new Exception("Unsupported image.");
                float scale=Math.min(1f,320f/Math.max(source.getWidth(),source.getHeight()));
                Bitmap scaled=Bitmap.createScaledBitmap(source,Math.max(1,(int)(source.getWidth()*scale)),Math.max(1,(int)(source.getHeight()*scale)),true);
                ByteArrayOutputStream bytes=new ByteArrayOutputStream();scaled.compress(Bitmap.CompressFormat.JPEG,58,bytes);
                watermarkLogoBase64=Base64.encodeToString(bytes.toByteArray(),Base64.NO_WRAP);
                prefs().edit().putString("watermark_logo_base64",watermarkLogoBase64).apply();
                if(source!=scaled)source.recycle();if(scaled!=source)scaled.recycle();
                setStatus("Watermark logo imported. It will be included in Cloud Render.");
            }catch(Exception e){recordException("Import watermark logo",e);setStatus("Logo import failed: "+e.getMessage());}
        }
    }
    @Override protected void onPause(){
        super.onPause();
        if(titleInput!=null){try{prefs().edit().putString("last_project",buildProject().toString()).putString("watermark_logo_base64",watermarkLogoBase64).apply();}catch(Exception ignored){}}
    }

    private void saveProject(){
        JSONObject p=buildProject();String raw=p.toString();
        try{
            JSONArray library=new JSONArray(prefs().getString("project_library","[]"));
            String title=p.optString("title","Untitled Project");boolean replaced=false;
            for(int i=0;i<library.length();i++){JSONObject item=library.optJSONObject(i);if(item!=null&&title.equals(item.optString("title"))){library.put(i,new JSONObject().put("title",title).put("project",p));replaced=true;break;}}
            if(!replaced)library.put(new JSONObject().put("title",title).put("project",p));
            while(library.length()>20)library.remove(0);
            prefs().edit().putString("project_library",library.toString()).putString("last_project",raw).putString("watermark_logo_base64",watermarkLogoBase64).apply();
        }catch(Exception e){recordException("Save project library",e);prefs().edit().putString("last_project",raw).apply();}
        setStatus("Project saved to this phone and Project Library ("+raw.length()+" characters).");
    }
    private void setSpinnerValue(Spinner spinner,String value){for(int i=0;i<spinner.getCount();i++)if(spinner.getItemAtPosition(i).toString().equals(value)){spinner.setSelection(i);return;}}
    private void restoreProject(){
        String raw=prefs().getString("last_project",null);if(raw==null)return;
        try{JSONObject p=new JSONObject(raw);titleInput.setText(p.optString("title",""));promptInput.setText(p.optString("prompt",""));characterInput.setText(p.optString("character_bible",""));
            JSONArray a=p.optJSONArray("scenes");if(a!=null){StringBuilder b=new StringBuilder();for(int i=0;i<a.length();i++)b.append(a.getString(i)).append("\n");sceneInput.setText(b.toString());}
            animationCode=p.optString("animation_code","");
            watermarkLogoBase64=p.optString("watermark_logo_base64",prefs().getString("watermark_logo_base64",""));
            JSONArray ds=p.optJSONArray("dialogue");if(ds!=null){StringBuilder b=new StringBuilder();for(int i=0;i<ds.length();i++){JSONObject d=ds.getJSONObject(i);b.append(d.optString("english","")).append(" | ").append(d.optString("myanmar","")).append("\n");}dialogueInput.setText(b.toString());}
            musicCheck.setChecked(p.optBoolean("background_music",true));sfxCheck.setChecked(p.optBoolean("sfx",true));subtitleCheck.setChecked(p.optBoolean("myanmar_subtitles",true));
            setSpinnerValue(styleSpinner,p.optString("style","cartoon2d"));setSpinnerValue(durationSpinner,p.optString("duration_minutes","1"));setSpinnerValue(resolutionSpinner,p.optString("resolution","480p"));setSpinnerValue(voiceSpinner,p.optString("voice","No voice-over"));
            if(aspectRatioSpinner!=null)setSpinnerValue(aspectRatioSpinner,p.optString("aspect_ratio","16:9"));
            if(frameRateSpinner!=null)setSpinnerValue(frameRateSpinner,p.optString("fps","24"));
            if(watermarkInput!=null)watermarkInput.setText(p.optString("watermark","POWERED BY I KO KO"));
        }catch(Exception ignored){}
    }
    private void assistStory(){
        if(busy)return;
        String prompt=promptInput.getText().toString().trim();
        if(prompt.isEmpty()){setStatus("Enter a film idea first.");return;}
        setBusy(true);setStatus("AI Director: analyzing prompt and building storyboard…");
        generationDialog=new android.app.ProgressDialog(this);generationDialog.setTitle("AI PROJECT GENERATION");generationDialog.setMessage("Connecting to selected AI provider…");generationDialog.setIndeterminate(true);generationDialog.setCancelable(false);generationDialog.show();
        String aiPrompt=prompt+"\nTarget video duration: "+durationSpinner.getSelectedItem().toString()+" minutes. Visual style: "+styleSpinner.getSelectedItem().toString()+". For a 30-minute film, create a detailed long-form story with 18-30 scenes and 30-60 bilingual dialogue turns. Keep the same character appearance and personality throughout.";
        new ProviderManager(this).generateStory(aiPrompt,(story,err,provider)->{
            if(err!=null){
                setBusy(false);if(generationDialog!=null){generationDialog.dismiss();generationDialog=null;}
                setStatus("AI provider chain failed: "+err+" — using offline outline helper.");
                offlineStoryAssist(prompt);
                return;
            }
            titleInput.setText(story.optString("title","Untitled Story"));
            characterInput.setText(story.optString("character_bible",""));
            animationCode=story.optString("animation_code","");
            JSONArray sc=story.optJSONArray("scenes");StringBuilder sb=new StringBuilder();
            if(sc!=null)for(int i=0;i<sc.length();i++)sb.append(sc.optString(i)).append("\n");
            sceneInput.setText(sb.toString());
            JSONArray ds=story.optJSONArray("dialogue");StringBuilder db=new StringBuilder();
            if(ds!=null)for(int i=0;i<ds.length();i++){JSONObject d=ds.optJSONObject(i);db.append(d.optString("english","")).append(" | ").append(d.optString("myanmar","")).append("\n");}
            dialogueInput.setText(db.toString());
            setBusy(false);if(generationDialog!=null){generationDialog.dismiss();generationDialog=null;}setStatus("AI story draft ready via "+provider+". Review and edit before rendering.");
        });
    }
    private void offlineStoryAssist(String prompt){
        String[] ideas=prompt.split("[.!?。！？]+");StringBuilder scenes=new StringBuilder();int count=0;
        for(String idea:ideas){String v=idea.trim();if(v.isEmpty())continue;scenes.append("Scene ").append(++count).append(": ").append(v).append("\n");if(count==6)break;}
        if(count==0)scenes.append("Scene 1: Introduce the main character\nScene 2: A surprising problem appears\nScene 3: Action and comic twist\nScene 4: Happy ending");
        sceneInput.setText(scenes.toString());setStatus("Offline storyboard outline created. Add character details and bilingual dialogue; connect an AI provider for full AI writing.");
    }

    private synchronized void appendDiagnostic(String message){
        try{
            String safe=message==null?"(empty)":message;
            if(token!=null&&!token.isEmpty())safe=safe.replace(token,"[REDACTED_GITHUB_TOKEN]");
            safe=safe.replaceAll("(?i)(Bearer\\s+)[A-Za-z0-9_\\-\\.]+","$1[REDACTED]");
            safe=safe.replaceAll("(?i)(github_pat_|ghp_|sk-|AIza)[A-Za-z0-9_\\-]{12,}","[REDACTED_SECRET]");
            File file=new File(getFilesDir(),DIAGNOSTIC_FILE);
            if(file.exists()&&file.length()>512*1024)file.delete();
            String stamp=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS",Locale.US).format(new Date());
            try(FileOutputStream out=new FileOutputStream(file,true)){
                out.write((stamp+"  "+safe+"\n").getBytes("UTF-8"));
            }
        }catch(Exception ignored){}
    }
    private void recordException(String context,Exception error){
        StringWriter sw=new StringWriter();error.printStackTrace(new PrintWriter(sw));
        appendDiagnostic("EXCEPTION ["+context+"] "+sw.toString());
    }
    private void installCrashLogger(){
        final Thread.UncaughtExceptionHandler previous=Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread,error)->{
            StringWriter sw=new StringWriter();error.printStackTrace(new PrintWriter(sw));
            appendDiagnostic("FATAL CRASH on "+thread.getName()+"\n"+sw.toString());
            if(previous!=null)previous.uncaughtException(thread,error);
        });
    }
    private String readDiagnosticLog(){
        File file=new File(getFilesDir(),DIAGNOSTIC_FILE);
        if(!file.exists())return "No diagnostic entries yet.";
        try(BufferedReader reader=new BufferedReader(new InputStreamReader(new FileInputStream(file),"UTF-8"))){
            StringBuilder out=new StringBuilder();String line;
            while((line=reader.readLine())!=null)out.append(line).append("\n");
            return out.toString();
        }catch(Exception e){return "Could not read log: "+e.getMessage();}
    }
    private void showDiagnosticCenter(){
        String log=readDiagnosticLog();
        android.widget.TextView content=new android.widget.TextView(this);
        content.setText(log);content.setTextColor(Color.WHITE);content.setTextSize(12);content.setTextIsSelectable(true);content.setPadding(18,12,18,12);
        ScrollView panel=new ScrollView(this);panel.setBackgroundColor(Color.rgb(8,11,18));panel.addView(content);
        android.app.AlertDialog dialog=new android.app.AlertDialog.Builder(this)
            .setTitle("G.I KO KO — DIAGNOSTIC CENTER")
            .setView(panel)
            .setPositiveButton("COPY LOG",(d,w)->{
                ClipboardManager clipboard=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
                clipboard.setPrimaryClip(ClipData.newPlainText("G.I Ko Ko ViD Gen diagnostic log",readDiagnosticLog()));
                Toast.makeText(this,"Diagnostic log copied.",Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("EXPORT / SHARE",(d,w)->{
                Intent share=new Intent(Intent.ACTION_SEND);share.setType("text/plain");
                share.putExtra(Intent.EXTRA_SUBJECT,"G.I Ko Ko ViD Gen Diagnostic Log");
                share.putExtra(Intent.EXTRA_TEXT,readDiagnosticLog());
                startActivity(Intent.createChooser(share,"Export diagnostic log"));
            })
            .setNeutralButton("CLEAR LOG",(d,w)->{
                File file=new File(getFilesDir(),DIAGNOSTIC_FILE);
                if(file.exists())file.delete();
                appendDiagnostic("Diagnostic log cleared by user.");
                Toast.makeText(this,"Diagnostic log cleared.",Toast.LENGTH_SHORT).show();
            }).create();
        dialog.show();
        dialog.getWindow().setLayout((int)(getResources().getDisplayMetrics().widthPixels*0.92),
            (int)(getResources().getDisplayMetrics().heightPixels*0.72));
    }
    private void openRenderCenter(){
        LinearLayout panel=column();panel.setPadding(dp(16),dp(12),dp(16),dp(12));
        TextView title=label("RENDER CENTER / LIVE LOGS",17);title.setTextColor(themeAccent);panel.addView(title);
        ProgressBar progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setIndeterminate(busy);panel.addView(progress,new LinearLayout.LayoutParams(-1,dp(8)));
        TextView note=label("Live job updates, stage messages and diagnostics. Cloud progress is reported from GitHub workflow steps; local progress is reported from encoded frames.",10);note.setTextColor(themeMuted);panel.addView(note);
        renderCenterLog=new TextView(this);renderCenterLog.setText(readDiagnosticLog());renderCenterLog.setTextColor(themeText);renderCenterLog.setTextSize(11);renderCenterLog.setTextIsSelectable(true);
        ScrollView scroll=new ScrollView(this);scroll.addView(renderCenterLog);panel.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        android.app.AlertDialog dialog=new android.app.AlertDialog.Builder(this).setView(panel).setPositiveButton("CLOSE",null).create();dialog.show();
        dialog.getWindow().setBackgroundDrawable(shape(themeBg,themeBorder,18));dialog.getWindow().setLayout((int)(getResources().getDisplayMetrics().widthPixels*.94),(int)(getResources().getDisplayMetrics().heightPixels*.82));
    }
    private void setStatus(String s){
        appendDiagnostic("STATUS: "+s);
        main.post(()->{if(status!=null)status.setText(s);if(renderCenterLog!=null)renderCenterLog.setText(readDiagnosticLog());});
    }
    private void setBusy(boolean b){
        busy=b;main.post(()->{if(startButton!=null)startButton.setEnabled(!b);if(checkButton!=null)checkButton.setEnabled(!b);if(downloadButton!=null)downloadButton.setEnabled(!b);if(saveButton!=null)saveButton.setEnabled(!b);if(retryButton!=null)retryButton.setEnabled(!b);});
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
            }catch(Exception e){recordException("Validate GitHub token",e);setStatus("Token error: "+e.getMessage());}
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
        String location=c.getHeaderField("Location");if(code>=400){String detail=b.toString();if(detail.length()>1800)detail=detail.substring(0,1800);appendDiagnostic("HTTP ERROR "+method+" "+url+" -> "+code+" "+detail);}c.disconnect();return new ApiResponse(code,b.toString(),location);
    }

    private void startRender(){
        if(busy)return;
        if(renderModeSpinner!=null && "Local Device Render".equals(renderModeSpinner.getSelectedItem().toString())){startLocalRender();return;}
        if(token==null){setStatus("Open Settings and add your GitHub token for Cloud Render.");return;}
        final String style=styleSpinner.getSelectedItem().toString();
        final String duration=durationSpinner.getSelectedItem().toString();
        final String resolution=resolutionSpinner.getSelectedItem().toString();
        final String requestId=UUID.randomUUID().toString().replace("-","").substring(0,12);
        prefs().edit().putString("last_request_id",requestId).apply();
        final String projectJson=buildProject().toString();
        JSONObject projectCheck=buildProject();
        if(projectJson.length()>45000){setStatus("Project is too large. Shorten the prompt/dialogue and try again.");return;}
        if(projectCheck.optJSONArray("scenes")==null||projectCheck.optJSONArray("scenes").length()==0){setStatus("Add at least one scene before rendering.");return;}
        prefs().edit().putString("last_project",projectJson).apply();
        setBusy(true);setStatus("Sending render request to GitHub Actions…");
        new Thread(()->{
            try{
                JSONObject inputs=new JSONObject();inputs.put("style",style);inputs.put("duration",duration);inputs.put("resolution",resolution);inputs.put("request_id",requestId);
                inputs.put("aspect_ratio",aspectRatioSpinner.getSelectedItem().toString());
                inputs.put("fps",frameRateSpinner.getSelectedItem().toString());
                inputs.put("project_json",projectJson);
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
                    setStatus("Render "+state+" — "+style+" / "+duration+" min / "+resolution+"\nRun ID: "+runId+(i%3==0?"\n"+getRunProgress(runId):""));
                    if("completed".equals(state)){
                        if(!"success".equals(conclusion))throw new Exception("Render finished with result: "+conclusion+". Open the run logs to inspect the failure.");
                        setStatus("Render complete. Downloading MP4 artifact…");
                        downloadArtifact(runId);
                        return;
                    }
                    Thread.sleep(10000);
                }
                throw new Exception("Render is still running. Use CHECK RENDER STATUS and DOWNLOAD LATEST MP4 later.");
            }catch(Exception e){recordException("Cloud render",e);setStatus("Render error: "+e.getMessage());}
            finally{setBusy(false);}
        }).start();
    }

    private void startLocalRender(){
        if(busy)return;
        final JSONObject project=buildProject();
        final int seconds=Integer.parseInt(localDurationSpinner.getSelectedItem().toString());
        final String resolution=resolutionSpinner.getSelectedItem().toString();
        final String ratio=aspectRatioSpinner.getSelectedItem().toString();
        final int fps=Integer.parseInt(frameRateSpinner.getSelectedItem().toString());
        saveProject();setBusy(true);setStatus("LOCAL RENDER — preparing on-device H.264 encoder…");
        new Thread(()->{File downloads=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);File target=new File(downloads,"GIKOKO-LOCAL-"+System.currentTimeMillis()+".mp4");try{
            LocalRenderEngine.render(project,target,seconds,resolution,ratio,fps,(done,total,stage)->{if(done%Math.max(1,fps*2)==0||done==total)setStatus(stage+" — "+(done*100/Math.max(1,total))+"% ("+done+"/"+total+" frames)");});
            setStatus("LOCAL RENDER COMPLETE — "+target.getAbsolutePath()+" ("+(target.length()/1024)+" KB). Silent preview MP4 saved to Downloads.");
        }catch(Exception e){recordException("Local video render",e);setStatus("Local render failed: "+e.getMessage()+" — try 480p / 24 FPS or use Cloud Render.");}finally{setBusy(false);}}).start();
    }

    private void retryFailedRender(){
        if(token==null){setStatus("Save your GitHub token first.");return;}
        long id=lastRunId();if(id<=0){setStatus("No previous render run to retry.");return;}
        if(busy)return;setBusy(true);setStatus("Requesting GitHub to retry failed render steps…");
        new Thread(()->{
            try{
                ApiResponse rr=api("POST",API+"/actions/runs/"+id+"/rerun-failed-jobs",token,null);
                if(rr.code!=201&&rr.code!=202)throw new Exception("GitHub retry request failed (HTTP "+rr.code+"): "+rr.body);
                setStatus("Retry accepted. Reusing the previous request and cached completed segments…");
                for(int i=0;i<2160;i++){
                    Thread.sleep(10000);
                    ApiResponse sr=api("GET",API+"/actions/runs/"+id,token,null);
                    if(sr.code!=200)throw new Exception("Retry status failed (HTTP "+sr.code+"): "+sr.body);
                    JSONObject run=new JSONObject(sr.body);String state=run.optString("status","unknown"),result=run.optString("conclusion","");
                    setStatus("Retry render "+state+" / "+result+"\nRun ID: "+id+(i%3==0?"\n"+getRunProgress(id):""));
                    if("completed".equals(state)){
                        if(!"success".equals(result))throw new Exception("Retry finished with result: "+result);
                        setStatus("Retry complete. Downloading MP4…");downloadArtifact(id);return;
                    }
                }
                setStatus("Retry is still running. Use CHECK RENDER STATUS when ready.");
            }catch(Exception e){recordException("Retry render",e);setStatus("Retry error: "+e.getMessage());}
            finally{setBusy(false);}
        }).start();
    }

    private long lastRunId(){return prefs().getLong(RUN_KEY,-1);}
    private String getRunProgress(long runId){
        try{
            ApiResponse r=api("GET",API+"/actions/runs/"+runId+"/jobs?per_page=30",token,null);
            if(r.code!=200)return "";
            JSONArray jobs=new JSONObject(r.body).optJSONArray("jobs");if(jobs==null)return "";
            int done=0,total=0;String current="";
            for(int i=0;i<jobs.length();i++){
                JSONArray steps=jobs.getJSONObject(i).optJSONArray("steps");if(steps==null)continue;
                for(int j=0;j<steps.length();j++){
                    JSONObject step=steps.getJSONObject(j);total++;
                    String state=step.optString("status","");
                    if("completed".equals(state))done++;
                    if("in_progress".equals(state))current=step.optString("name","");
                }
            }
            return "Steps "+done+"/"+total+(current.isEmpty()?"":" — "+current);
        }catch(Exception ignored){return "";}
    }
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
            }catch(Exception e){recordException("Check render status",e);setStatus("Status error: "+e.getMessage());}
            finally{setBusy(false);}
        }).start();
    }
    private void downloadLastRun(){
        if(token==null){setStatus("Save your GitHub token first.");return;}
        long id=lastRunId();if(id<=0){setStatus("No render run saved yet.");return;}
        if(!ensureStoragePermission()){pendingStart=false;requestStoragePermission();setStatus("Allow storage permission, then tap DOWNLOAD LATEST MP4 again.");return;}
        if(busy)return;setBusy(true);
        new Thread(()->{try{downloadArtifact(id);}catch(Exception e){recordException("Download MP4",e);setStatus("Download error: "+e.getMessage());}finally{setBusy(false);}}).start();
    }


    private void downloadReleaseMp4(long runId)throws Exception{
        ApiResponse release=api("GET",API+"/releases/tags/render-"+runId,token,null);
        if(release.code!=200)throw new Exception("Direct MP4 release not found (HTTP "+release.code+").");
        JSONArray assets=new JSONObject(release.body).optJSONArray("assets");
        if(assets==null)throw new Exception("Release has no downloadable assets.");
        JSONObject mp4=null;
        for(int i=0;i<assets.length();i++){
            JSONObject asset=assets.getJSONObject(i);
            if(asset.optString("name","").toLowerCase().endsWith(".mp4")){mp4=asset;break;}
        }
        if(mp4==null)throw new Exception("MP4 release asset not found.");
        long assetId=mp4.getLong("id");
        HttpURLConnection apiConn=(HttpURLConnection)new URL(API+"/releases/assets/"+assetId).openConnection();
        apiConn.setRequestMethod("GET");
        apiConn.setConnectTimeout(30000);apiConn.setReadTimeout(40000);apiConn.setInstanceFollowRedirects(false);
        apiConn.setRequestProperty("Accept","application/octet-stream");
        apiConn.setRequestProperty("Authorization","Bearer "+token);
        apiConn.setRequestProperty("X-GitHub-Api-Version","2022-11-28");
        int code=apiConn.getResponseCode();
        String location=apiConn.getHeaderField("Location");
        apiConn.disconnect();
        if((code!=302&&code!=301)||location==null)throw new Exception("MP4 direct link request failed (HTTP "+code+").");
        HttpURLConnection fileConn=(HttpURLConnection)new URL(location).openConnection();
        fileConn.setConnectTimeout(30000);fileConn.setReadTimeout(0);fileConn.setInstanceFollowRedirects(true);
        File downloads=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if(!downloads.exists()&&!downloads.mkdirs())throw new Exception("Could not create Downloads folder.");
        String name=new File(mp4.optString("name","GIKOKO-RENDER-"+runId+".mp4")).getName();
        File target=new File(downloads,name);
        setStatus("Downloading MP4 directly…");
        try(InputStream in=new BufferedInputStream(fileConn.getInputStream());FileOutputStream out=new FileOutputStream(target)){
            byte[] buf=new byte[64*1024];int n;long total=0;
            while((n=in.read(buf))!=-1){out.write(buf,0,n);total+=n;if(total%(5*1024*1024)<buf.length)setStatus("Downloading MP4… "+(total/(1024*1024))+" MB");}
        }finally{fileConn.disconnect();}
        if(!target.exists()||target.length()==0)throw new Exception("Direct MP4 download returned an empty file.");
        setStatus("SUCCESS — MP4 downloaded directly to Downloads.\n"+target.getAbsolutePath());
    }

    private void downloadArtifact(long runId)throws Exception{
        try{
            downloadReleaseMp4(runId);
            return;
        }catch(Exception releaseError){
            setStatus("Direct MP4 link unavailable; using ZIP artifact fallback…");
        }
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