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
    private EditText tokenInput,aiBaseInput,aiModelInput,aiKeyInput,titleInput,promptInput,characterInput,sceneInput,dialogueInput,durationInput;
    private Spinner styleSpinner,resolutionSpinner,aspectRatioSpinner,frameRateSpinner,renderModeSpinner,localDurationSpinner,themeSpinner,geminiModelSpinner;
    private EditText watermarkInput;
    private String watermarkLogoBase64="";
    private android.app.ProgressDialog generationDialog;
    private TextView renderCenterLog,chatTranscript;
    private TextView status;
    private Button startButton,saveButton,checkButton,downloadButton,retryButton;
    private Spinner voiceSpinner;
    private CheckBox musicCheck,sfxCheck,subtitleCheck;
    private String token;
    private String animationCode="";
    private String characterReferenceBase64="";
    private String characterMotion="float";
    private String directorAttachmentText="";
    private TextView directorAttachmentLabel;
    private TextView characterReferenceStatus;
    private ImageView characterReferencePreview;
    private boolean busy=false,pendingStart=false;
    private boolean facebookContentMode=true;
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
        ((android.graphics.drawable.GradientDrawable)b.getBackground()).setCornerRadius(dp(9));
        b.setPadding(dp(12),dp(8),dp(12),dp(8));b.setElevation(dp(4));return b;
    }
    private LinearLayout column(){
        LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;
    }
    private LinearLayout card(LinearLayout parent){
        LinearLayout c=column();c.setPadding(dp(16),dp(13),dp(16),dp(15));
        c.setBackground(shape(themeCard,themeBorder,14));c.setElevation(dp(5));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(13);parent.addView(c,lp);return c;
    }
    private void heading(LinearLayout box,String text,String hint){
        TextView t=label(text,16);t.setTextColor(themeAccent);t.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));box.addView(t);
        if(hint!=null&&!hint.isEmpty()){TextView h=label(hint,11);h.setTextColor(0xff8daab8);h.setPadding(0,0,0,dp(7));box.addView(h);}
    }

    private void buildUi(){
        getWindow().setStatusBarColor(0xff07111b);getWindow().setNavigationBarColor(0xff07111b);
        FrameLayout shell=new FrameLayout(this);
        shell.setBackground(new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TL_BR,new int[]{0xff07111b,0xff0b1d2a,0xff07111b}));
        shell.addView(new StudioAtmosphereView(this),new FrameLayout.LayoutParams(-1,-1));
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);
        LinearLayout root=column();root.setPadding(dp(16),dp(12),dp(16),dp(28));scroll.addView(root);shell.addView(scroll);setContentView(shell);

        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);top.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams tlp=new LinearLayout.LayoutParams(-1,dp(54));tlp.bottomMargin=dp(14);root.addView(top,tlp);
        ImageView logo=new ImageView(this);logo.setImageResource(R.drawable.ic_launcher);logo.setScaleType(ImageView.ScaleType.FIT_CENTER);top.addView(logo,new LinearLayout.LayoutParams(dp(46),dp(46)));
        LinearLayout brand=column();LinearLayout.LayoutParams blp=new LinearLayout.LayoutParams(0,-2,1);blp.leftMargin=dp(10);top.addView(brand,blp);
        TextView name=label("G.I KO KO  •  ViD GEN",16);name.setTextColor(0xfff2fbff);name.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));brand.addView(name);
        TextView sub=label("CREATIVE PRODUCTION STUDIO  /  POWERED BY I KO KO",9);sub.setTextColor(0xff54e5c2);brand.addView(sub);
        Button settings=button("⚙");top.addView(settings,new LinearLayout.LayoutParams(dp(52),dp(46)));settings.setOnClickListener(v->openSettings());

        LinearLayout hero=card(root);hero.setBackground(new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TL_BR,new int[]{0xff123c4b,0xff10283a,0xff101a2c}));
        heading(hero,"YOUR STORY. YOUR STUDIO.","Plan a story, direct the scenes, and export a finished MP4.");
        TextView cloud=label("●  CLOUD RENDER ACTIVE     •     480p / 720p",10);cloud.setTextColor(0xff5cf2c5);hero.addView(cloud);

        LinearLayout chat=card(root);
        LinearLayout chatTitle=new LinearLayout(this);chatTitle.setGravity(Gravity.CENTER_VERTICAL);chatTitle.setOrientation(LinearLayout.HORIZONTAL);chat.addView(chatTitle,new LinearLayout.LayoutParams(-1,-2));
        TextView geminiMark=label("✦",23);geminiMark.setTextColor(0xff75e7ff);geminiMark.setGravity(Gravity.CENTER);geminiMark.setBackground(shape(0xff123849,0xff35bfc8,9));chatTitle.addView(geminiMark,new LinearLayout.LayoutParams(dp(34),dp(34)));
        LinearLayout chatHeading=column();LinearLayout.LayoutParams chlp=new LinearLayout.LayoutParams(0,-2,1);chlp.leftMargin=dp(9);chatTitle.addView(chatHeading,chlp);
        TextView chatName=label("AI DIRECTOR CHAT",15);chatName.setTextColor(themeAccent);chatName.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));chatHeading.addView(chatName);
        TextView chatHint=label("Gemini-powered creative direction",10);chatHint.setTextColor(themeMuted);chatHeading.addView(chatHint);
        TextView chatIndex=label("01",11);chatIndex.setTextColor(0xff7debd5);chatTitle.addView(chatIndex);
        TextView chatDescription=label("Describe your film naturally. AI prepares the storyboard; cloud rendering creates the MP4.",10);chatDescription.setTextColor(themeMuted);chat.addView(chatDescription);
        ScrollView transcriptScroll=new ScrollView(this);transcriptScroll.setFillViewport(false);transcriptScroll.setVerticalScrollBarEnabled(true);transcriptScroll.setBackground(shape(0xff0a1722,0xff1d5360,10));
        chatTranscript=new TextView(this);chatTranscript.setTextColor(0xffd8eaf0);chatTranscript.setTextSize(13);chatTranscript.setLineSpacing(dp(3),1f);chatTranscript.setPadding(dp(13),dp(12),dp(13),dp(12));chatTranscript.setTextIsSelectable(true);
        chatTranscript.setText(prefs().getString("chat_history","G.I KO KO AI DIRECTOR\nTell me what kind of story you want to make."));
        transcriptScroll.addView(chatTranscript,new ScrollView.LayoutParams(-1,-2));chat.addView(transcriptScroll,new LinearLayout.LayoutParams(-1,dp(220)));
        TextView ql=label("QUICK IDEAS",10);ql.setTextColor(0xff6fe9d0);chat.addView(ql);
        LinearLayout quick=new LinearLayout(this);quick.setOrientation(LinearLayout.HORIZONTAL);chat.addView(quick);
        String[] ideas={"Myanmar comedy","Action adventure","Historical cartoon"};
        for(String idea:ideas){Button q=button(idea);q.setTextSize(10);LinearLayout.LayoutParams qp=new LinearLayout.LayoutParams(0,dp(42),1);qp.rightMargin=dp(5);quick.addView(q,qp);q.setOnClickListener(v->promptInput.setText("Create a "+idea.toLowerCase(Locale.US)+" story with consistent characters, exciting scenes and a memorable ending."));}
        promptInput=edit("","Message your AI Director…",false);promptInput.setMinLines(3);promptInput.setMaxLines(6);promptInput.setBackground(shape(0xff0a1722,0xff247e7b,10));chat.addView(promptInput);
        LinearLayout sendRow=new LinearLayout(this);sendRow.setGravity(Gravity.CENTER_VERTICAL);sendRow.setOrientation(LinearLayout.HORIZONTAL);LinearLayout.LayoutParams srp=new LinearLayout.LayoutParams(-1,dp(48));srp.topMargin=dp(6);chat.addView(sendRow,srp);
        Button attach=button("＋");attach.setTextSize(22);sendRow.addView(attach,new LinearLayout.LayoutParams(dp(48),-1));attach.setOnClickListener(v->openDirectorAttachmentPicker());
        Button send=button("➤  SEND TO AI DIRECTOR");LinearLayout.LayoutParams sendLp=new LinearLayout.LayoutParams(0,-1,1);sendLp.leftMargin=dp(8);sendRow.addView(send,sendLp);
        directorAttachmentLabel=label("No attachment",9);directorAttachmentLabel.setTextColor(themeMuted);chat.addView(directorAttachmentLabel);
        send.setOnClickListener(v->{String m=promptInput.getText().toString().trim();if(m.isEmpty()&&directorAttachmentText.isEmpty()&&characterReferenceBase64.isEmpty()){setStatus("Write a command or attach a file first.");return;}if(!directorAttachmentText.isEmpty())m+=(m.isEmpty()?"":"\n\n")+"ATTACHED FILE CONTENT:\n"+directorAttachmentText;if(!characterReferenceBase64.isEmpty())m+=(m.isEmpty()?"":"\n\n")+"An image reference is attached to the current project context.";promptInput.setText(m);appendChatMessage("YOU",m);sendToMasterAi();});

        LinearLayout tools=card(root);heading(tools,"STUDIO TOOLKIT","10 focused tools for story prep, finishing and cloud delivery.");
        addToolRow(tools,R.drawable.ic_tool_templates,"QUICK CREATIVE WIZARD","Choices, examples, image reference and one-tap actions",()->openCreativeWorkbench(),R.drawable.ic_tool_character,"CHARACTER LAB","Build a consistent character bible",()->openCharacterBuilder());
        addToolRow(tools,R.drawable.ic_tool_templates,"STARTER TEMPLATES","10 ready-to-edit film blueprints",()->openTemplateGallery(),R.drawable.ic_tool_character,"CHARACTER REFERENCE","Upload and preview your own image",()->openCharacterReferenceLab());
        addToolRow(tools,R.drawable.ic_tool_dialogue,"DIALOGUE DESK","Edit English + Myanmar lines",()->openDialogueBuilder(),R.drawable.ic_tool_subtitles,"SUBTITLE PREVIEW","Preview bilingual captions",()->previewSubtitles());
        addToolRow(tools,R.drawable.ic_tool_audio,"AUDIO LAB","Voice, music and sound effects",()->openAudioLab(),R.drawable.ic_tool_cloud,"CLOUD QUEUE","Check render status and logs",()->{checkLastRun();openRenderCenter();});
        addToolRow(tools,R.drawable.ic_tool_projects,"PROJECT LIBRARY","Open saved projects and drafts",()->openProjectLibrary(),R.drawable.ic_tool_brand,"BRAND KIT","Logo and watermark controls",()->openBrandKit());
        addToolRow(tools,R.drawable.ic_tool_ratio,"FORMAT PRESETS","Portrait, landscape and square",()->openFormatPresets(),R.drawable.ic_tool_diagnostics,"DIAGNOSTICS","Copy or export technical logs",()->showDiagnosticCenter());
        addToolRow(tools,R.drawable.ic_tool_templates,"FACEBOOK CONTENT","Mixed Reels & 1–3 min videos",()->openFacebookContentSystem(),R.drawable.ic_tool_audio,"BILINGUAL VOICE","English + Myanmar narration",()->openAudioLab());
        addToolRow(tools,R.drawable.ic_tool_reference,"REFERENCE LAB","Upload your own character image",()->openCharacterReferenceLab(),R.drawable.ic_tool_motion,"MOTION PRESETS","Float, bounce, slide, pulse or still",()->openCharacterReferenceLab());

        LinearLayout project=card(root);heading(project,"02  /  PROJECT DETAILS","Keep the title, character identity and scene continuity in one place.");
        titleInput=edit("The Golden Kingdom","Project title",false);project.addView(titleInput);
        characterInput=edit("Main character: a clever but clumsy royal guard. Keep costume, face, colors and personality consistent in every scene.","Character Bible — consistent look and personality",false);characterInput.setMinLines(3);project.addView(characterInput);
        sceneInput=edit("Palace courtyard — introduce the guard\nThrone room — a suspicious royal order\nPalace kitchen — comic chase\nRoyal hall — funny reveal","Scene outline — one scene per line",false);sceneInput.setMinLines(4);project.addView(sceneInput);
        dialogueInput=edit("Guard: I have everything under control. | အားလုံးကို ကျွန်တော် ထိန်းချုပ်ထားပါတယ်။\nKing: Then why is the palace on fire? | ဒါဆို နန်းတော်က ဘာလို့ မီးလောင်နေတာလဲ။","Dialogue — English | Myanmar per line",false);dialogueInput.setMinLines(4);project.addView(dialogueInput);
        LinearLayout pa=new LinearLayout(this);pa.setOrientation(LinearLayout.HORIZONTAL);project.addView(pa);
        Button save=button("▣  SAVE DRAFT");pa.addView(save,new LinearLayout.LayoutParams(0,dp(46),1));save.setOnClickListener(v->saveProject());
        Button library=button("▤  PROJECTS");LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(46),1);lp.leftMargin=dp(8);pa.addView(library,lp);library.setOnClickListener(v->openProjectLibrary());

        LinearLayout config=card(root);heading(config,"03  /  PRODUCTION SETTINGS","Choose output format, quality, voice and finishing options.");
        config.addView(label("VISUAL STYLE",11));styleSpinner=spinner(new String[]{"hybrid","cartoon2d","cartoon3d","cinematic"});config.addView(styleSpinner);
        config.addView(label("VIDEO LENGTH  •  MINUTES (TYPE YOUR OWN)",11));durationInput=edit("2","Enter whole minutes (1–180)",false);durationInput.setInputType(InputType.TYPE_CLASS_NUMBER);config.addView(durationInput);
        config.addView(label("OUTPUT QUALITY",11));resolutionSpinner=spinner(new String[]{"480p","720p"});config.addView(resolutionSpinner);
        config.addView(label("ASPECT RATIO",11));aspectRatioSpinner=spinner(new String[]{"16:9","9:16","1:1","4:5"});config.addView(aspectRatioSpinner);
        config.addView(label("FRAME RATE",11));frameRateSpinner=spinner(new String[]{"24","30","60"});config.addView(frameRateSpinner);
        config.addView(label("VOICE-OVER",11));voiceSpinner=spinner(new String[]{"No voice-over","English voice-over","Myanmar voice-over","Bilingual voice-over"});config.addView(voiceSpinner);
        setSpinnerValue(aspectRatioSpinner,"9:16");setSpinnerValue(voiceSpinner,"Bilingual voice-over");
        musicCheck=new CheckBox(this);musicCheck.setText("Background music");musicCheck.setTextColor(themeText);musicCheck.setChecked(true);config.addView(musicCheck);
        sfxCheck=new CheckBox(this);sfxCheck.setText("Sound effects");sfxCheck.setTextColor(themeText);sfxCheck.setChecked(true);config.addView(sfxCheck);
        subtitleCheck=new CheckBox(this);subtitleCheck.setText("Bilingual subtitles  •  English + Myanmar");subtitleCheck.setTextColor(themeText);subtitleCheck.setChecked(true);config.addView(subtitleCheck);
        watermarkInput=edit("POWERED BY I KO KO","Watermark text",false);config.addView(watermarkInput);
        Button addLogo=button("＋  ADD BRAND LOGO");config.addView(addLogo);addLogo.setOnClickListener(v->pickWatermarkLogo());

        LinearLayout render=card(root);heading(render,"04  /  CLOUD RENDER","GitHub Actions renders your project and exports a downloadable MP4.");
        startButton=button("▶   START CLOUD RENDER");render.addView(startButton,new LinearLayout.LayoutParams(-1,dp(54)));
        startButton.setOnClickListener(v->{if(ensureStoragePermission())startRender();else{pendingStart=true;requestStoragePermission();}});
        LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);LinearLayout.LayoutParams al=new LinearLayout.LayoutParams(-1,-2);al.topMargin=dp(9);render.addView(actions,al);
        retryButton=button("↻ RETRY");actions.addView(retryButton,new LinearLayout.LayoutParams(0,dp(46),1));retryButton.setOnClickListener(v->retryFailedRender());
        checkButton=button("◉ STATUS");LinearLayout.LayoutParams cl=new LinearLayout.LayoutParams(0,dp(46),1);cl.leftMargin=dp(7);actions.addView(checkButton,cl);checkButton.setOnClickListener(v->checkLastRun());
        downloadButton=button("↓ MP4");LinearLayout.LayoutParams dl=new LinearLayout.LayoutParams(0,dp(46),1);dl.leftMargin=dp(7);actions.addView(downloadButton,dl);downloadButton.setOnClickListener(v->downloadLastRun());
        Button center=button("▣  RENDER CENTER  /  LOGS");LinearLayout.LayoutParams cpl=new LinearLayout.LayoutParams(-1,dp(46));cpl.topMargin=dp(8);render.addView(center,cpl);center.setOnClickListener(v->openRenderCenter());

        LinearLayout sc=card(root);heading(sc,"SYSTEM STATUS","Cloud queue, render progress and export messages.");
        status=label(token==null?"●  SETUP REQUIRED  —  Add your GitHub token in Settings.":"●  READY  —  GitHub token secured on this device.",12);status.setTextColor(0xff70f5c8);status.setMinLines(2);sc.addView(status);
        TextView foot=label("G.I KO KO ViD Gen  •  ANDROID 10+  •  CLOUD MP4 EXPORT",9);foot.setTextColor(0xff6e91a0);foot.setGravity(Gravity.CENTER);root.addView(foot);
        buildSettingsPanel();restoreProject();
    }
    private void appendChatMessage(String who,String message){
        try{String old=prefs().getString("chat_history","G.I KO KO AI DIRECTOR");String next=old+"\n\n"+who+"\n"+message;if(next.length()>9000)next=next.substring(next.length()-9000);prefs().edit().putString("chat_history",next).apply();if(chatTranscript!=null){chatTranscript.setText(next);chatTranscript.post(()->{if(chatTranscript.getParent() instanceof ScrollView)((ScrollView)chatTranscript.getParent()).fullScroll(View.FOCUS_DOWN);});}}catch(Exception ignored){}
    }
    private void openDirectorAttachmentPicker(){new android.app.AlertDialog.Builder(this).setTitle("ATTACH TO AI DIRECTOR").setItems(new String[]{"＋  Image / photo reference","▤  Text / document file"},(d,w)->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType(w==0?"image/*":"*/*");startActivityForResult(i,w==0?703:704);}).setNegativeButton("CANCEL",null).show();}

    private String[] geminiFreeModels(){
        return new String[]{"gemini-3.8-flash","gemini-3.6-flash","gemini-2.5-flash","gemini-2.5-flash-lite"};
    }
    private String getSelectedGeminiModel(){
        if(geminiModelSpinner!=null&&geminiModelSpinner.getSelectedItem()!=null)return geminiModelSpinner.getSelectedItem().toString();
        return prefs().getString("gemini_model","gemini-3.8-flash");
    }
    private void buildSettingsPanel(){
        LinearLayout panel=column();panel.setPadding(dp(18),dp(8),dp(18),dp(24));
        LinearLayout ai=card(panel);heading(ai,"GEMINI AI  /  FREE MODEL","Choose the Gemini API model used by AI Director and Master AI. Only free-tier model IDs are listed; availability and quotas depend on your Google AI Studio project.");
        geminiModelSpinner=spinner(geminiFreeModels());
        setSpinnerValue(geminiModelSpinner,prefs().getString("gemini_model","gemini-3.8-flash"));
        ai.addView(geminiModelSpinner);
        TextView secure=label("API KEY: GitHub Actions repository secret  •  GEMINI_API_KEY. Never stored in this APK.",10);secure.setTextColor(0xff63e8c5);ai.addView(secure);
        Button saveGemini=button("SAVE GEMINI MODEL");ai.addView(saveGemini);
        saveGemini.setOnClickListener(v->{String selected=getSelectedGeminiModel();prefs().edit().putString("gemini_model",selected).apply();setStatus("Gemini model saved: "+selected+". API key remains in GitHub Secrets.");});
        LinearLayout appearance=card(panel);heading(appearance,"APPEARANCE","Choose the studio colour theme.");
        themeSpinner=spinner(new String[]{"NEBULA PRISM","OBSIDIAN LUX","AURORA GLASS"});appearance.addView(themeSpinner);setSpinnerValue(themeSpinner,prefs().getString("ui_theme","NEBULA PRISM"));
        themeSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){} public void onItemSelected(android.widget.AdapterView<?> p,View v,int position,long id){String selected=themeSpinner.getSelectedItem().toString();if(!selected.equals(prefs().getString("ui_theme","NEBULA PRISM"))){saveProject();prefs().edit().putString("ui_theme",selected).apply();applyThemeFromName(selected);if(settingsDialog!=null)settingsDialog.dismiss();settingsDialog=null;buildUi();}}});
        LinearLayout account=card(panel);heading(account,"GITHUB CLOUD ACCESS","Your fine-grained token is encrypted with Android Keystore.");
        TextView help=label("Required for Cloud Render, status, retry and MP4 download. Grant Actions read/write and Contents read/write for this repository.",11);help.setTextColor(0xff9eb7c4);account.addView(help);
        tokenInput=edit("","GitHub fine-grained token",true);account.addView(tokenInput);
        saveButton=button(token==null?"SAVE GITHUB TOKEN":"UPDATE GITHUB TOKEN");account.addView(saveButton);saveButton.setOnClickListener(v->saveToken());
        Button clear=button("REMOVE SAVED TOKEN");account.addView(clear);clear.setOnClickListener(v->clearToken());
        LinearLayout tools=card(panel);heading(tools,"SUPPORT & DIAGNOSTICS","Review logs when a cloud render fails.");
        Button diagnostics=button("⌁  DIAGNOSTIC CENTER  •  VIEW / COPY / EXPORT");tools.addView(diagnostics);diagnostics.setOnClickListener(v->showDiagnosticCenter());
        TextView hidden=label("Advanced provider configuration and local rendering are temporarily hidden. Existing saved provider settings are preserved.",10);hidden.setTextColor(themeMuted);tools.addView(hidden);
        Button done=button("DONE");panel.addView(done);done.setOnClickListener(v->{if(settingsDialog!=null)settingsDialog.dismiss();});settingsPanel=panel;
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
        e.setTextSize(14);e.setSingleLine(!hint.toLowerCase(Locale.US).contains("prompt")&&!hint.toLowerCase(Locale.US).contains("director")&&!hint.toLowerCase(Locale.US).contains("bible")&&!hint.toLowerCase(Locale.US).contains("outline")&&!hint.toLowerCase(Locale.US).contains("dialogue"));
        e.setInputType(secret?(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD):(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES));
        e.setPadding(dp(13),dp(11),dp(13),dp(11));e.setBackground(shape(themeBg,themeBorder,14));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(5);lp.bottomMargin=dp(8);e.setLayoutParams(lp);return e;
    }
    private static class StudioAtmosphereView extends View{
        private final android.graphics.Paint paint=new android.graphics.Paint(3);private final long started=System.currentTimeMillis();
        StudioAtmosphereView(Activity a){super(a);setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
        @Override protected void onDraw(android.graphics.Canvas canvas){super.onDraw(canvas);float w=getWidth(),h=getHeight(),t=(System.currentTimeMillis()-started)/1000f;
            // 1) Drifting aurora glow.
            for(int i=0;i<3;i++){float x=(float)(w*(0.18+0.32*i)+Math.sin(t*0.32+i)*w*0.12),y=(float)(h*(0.22+0.27*i)+Math.cos(t*0.25+i)*h*0.08),r=Math.max(w,h)*0.34f;paint.setShader(new android.graphics.RadialGradient(x,y,r,new int[]{i==1?0x2631d9c8:0x222b8fff,0x002b8fff},null,android.graphics.Shader.TileMode.CLAMP));canvas.drawCircle(x,y,r,paint);}paint.setShader(null);
            // 2) Floating particles.
            paint.setStyle(android.graphics.Paint.Style.FILL);for(int i=0;i<26;i++){float x=(i*71f+(t*(9+i%4)*5))%(w+30)-15,y=(i*113f+(float)Math.sin(t*0.55+i)*18)%(h+30)-15;paint.setColor(i%3==0?0x7762f5d2:0x5556bfff);canvas.drawCircle(x,y,1.2f+(i%3)*0.45f,paint);}
            // 3) Rotating orbital arc.
            paint.setStyle(android.graphics.Paint.Style.STROKE);paint.setStrokeWidth(1.15f);paint.setColor(0x263ee8dc);float cx=w*0.82f,cy=h*0.18f,r=dpStatic(getContext(),42);canvas.save();canvas.rotate(t*8,cx,cy);canvas.drawArc(cx-r,cy-r,cx+r,cy+r,18,235,false,paint);canvas.restore();paint.setStyle(android.graphics.Paint.Style.FILL);postInvalidateDelayed(70);
        }
        private static float dpStatic(android.content.Context c,float v){return v*c.getResources().getDisplayMetrics().density;}
    }
    private void saveAiProvider(){
        String base=aiBaseInput.getText().toString().trim(),model=aiModelInput.getText().toString().trim(),key=aiKeyInput.getText().toString().trim();
        if(!base.startsWith("https://")||model.isEmpty()){setStatus("AI provider needs an HTTPS Base URL and Model ID.");return;}
        try{prefs().edit().putString("ai_base",base).putString("ai_model",model).apply();if(!key.isEmpty())prefs().edit().putString("ai_key_enc",encrypt(key)).apply();aiKeyInput.setText("");setStatus("AI provider settings saved. API key encrypted on device.");}
        catch(Exception e){recordException("Save AI provider",e);setStatus("Could not encrypt AI key: "+e.getMessage());}
    }
    private String getDurationMinutes(){
        String raw=durationInput==null?"2":durationInput.getText().toString().trim();
        if(!raw.matches("[0-9]{1,3}"))throw new IllegalArgumentException("Enter a whole number of minutes from 1 to 180.");
        int minutes=Integer.parseInt(raw);
        if(minutes<1||minutes>180)throw new IllegalArgumentException("Video duration must be between 1 and 180 minutes.");
        return Integer.toString(minutes);
    }

    private JSONObject buildProject(){
        JSONObject p=new JSONObject();
        try{
            p.put("schema_version",1);p.put("title",titleInput.getText().toString().trim());
            p.put("platform","facebook");p.put("content_mode",facebookContentMode?"mixed":"standard");p.put("facebook_content_mode",facebookContentMode);p.put("free_first",true);
            p.put("prompt",promptInput.getText().toString().trim());p.put("character_bible",characterInput.getText().toString().trim());
            p.put("character_reference_base64",characterReferenceBase64);
            p.put("character_reference_mime","image/webp");
            p.put("character_motion",characterMotion);
            if(animationCode!=null&&!animationCode.trim().isEmpty())p.put("animation_code",animationCode);
            JSONArray scenes=new JSONArray();for(String line:sceneInput.getText().toString().split("\n"))if(!line.trim().isEmpty())scenes.put(line.trim());p.put("scenes",scenes);
            JSONArray dialogue=new JSONArray();for(String line:dialogueInput.getText().toString().split("\n"))if(!line.trim().isEmpty()){
                String[] parts=line.split("\\|",2);JSONObject d=new JSONObject();d.put("english",parts[0].trim());d.put("myanmar",parts.length>1?parts[1].trim():"");dialogue.put(d);
            }p.put("dialogue",dialogue);
            p.put("voice",voiceSpinner.getSelectedItem().toString());p.put("background_music",musicCheck.isChecked());p.put("sfx",sfxCheck.isChecked());p.put("bilingual_subtitles",subtitleCheck.isChecked());p.put("myanmar_subtitles",subtitleCheck.isChecked());
            p.put("style",styleSpinner.getSelectedItem().toString());p.put("duration_minutes",getDurationMinutes());p.put("resolution",resolutionSpinner.getSelectedItem().toString());
            p.put("aspect_ratio",aspectRatioSpinner==null?"16:9":aspectRatioSpinner.getSelectedItem().toString());
            p.put("fps",frameRateSpinner==null?"24":frameRateSpinner.getSelectedItem().toString());
            p.put("watermark",watermarkInput==null?"POWERED BY I KO KO":watermarkInput.getText().toString().trim());
            if(!watermarkLogoBase64.isEmpty())p.put("watermark_logo_base64",watermarkLogoBase64);
        }catch(Exception ignored){}return p;
    }


    private void addToolRow(LinearLayout parent,int iconA,String titleA,String descA,Runnable actionA,int iconB,String titleB,String descB,Runnable actionB){
        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);rp.topMargin=dp(5);rp.bottomMargin=dp(5);parent.addView(row,rp);
        row.addView(toolTile(iconA,titleA,descA,actionA),new LinearLayout.LayoutParams(0,dp(94),1));
        View gap=new View(this);row.addView(gap,new LinearLayout.LayoutParams(dp(8),1));
        row.addView(toolTile(iconB,titleB,descB,actionB),new LinearLayout.LayoutParams(0,dp(94),1));
    }
    private View toolTile(int icon,String title,String description,Runnable action){
        LinearLayout tile=new LinearLayout(this);tile.setGravity(Gravity.CENTER_VERTICAL);tile.setOrientation(LinearLayout.HORIZONTAL);tile.setPadding(dp(9),dp(8),dp(7),dp(8));tile.setBackground(shape(0xff0b1c29,0xff205064,10));tile.setMinimumHeight(dp(88));tile.setClickable(true);tile.setFocusable(true);
        ImageView image=new ImageView(this);image.setImageResource(icon);image.setScaleType(ImageView.ScaleType.FIT_CENTER);tile.addView(image,new LinearLayout.LayoutParams(dp(34),dp(34)));
        LinearLayout copy=column();LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,-2,1);cp.leftMargin=dp(7);tile.addView(copy,cp);
        TextView t=label(title,10);t.setTextColor(0xff72e9d0);t.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));copy.addView(t);
        TextView d=label(description,9);d.setTextColor(0xffa6bdc9);d.setMaxLines(2);copy.addView(d);
        tile.setOnClickListener(v->{try{action.run();}catch(Exception e){setStatus("Tool error: "+e.getMessage());}});return tile;
    }
    private void openCreativeWorkbench(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
        LinearLayout panel=column();panel.setPadding(dp(14),dp(8),dp(14),dp(12));scroll.addView(panel);
        TextView intro=label("BUILD A VIDEO WITHOUT WRITING A LONG PROMPT\nChoose a format, pick a story direction, add a reference image if you want, then preview or execute.",12);
        intro.setTextColor(0xffb8d5df);panel.addView(intro);
        LinearLayout presets=card(panel);heading(presets,"01  /  START WITH AN EXAMPLE","Tap an example to prefill the creative brief.");
        String[] examples={"Myanmar palace comedy","3D cartoon adventure","Cinematic product promo","Myanmar folk tale","Movie recap narration","Motivational Reel"};
        String[] briefs={
            "A funny historical story in an ancient Myanmar palace. A clumsy royal guard causes a chain of visual gags, a playful chase and a clever ending.",
            "A colourful 3D cartoon adventure with a brave young hero, a mysterious forest, teamwork, exciting but family-friendly action and a happy ending.",
            "A premium product showcase with cinematic lighting, macro detail shots, clear feature highlights and a polished brand ending.",
            "A Myanmar-inspired folk tale with a magical atmosphere, a kind hero, a meaningful lesson and beautiful village scenery.",
            "Create an original chronological movie recap narration from my notes. Explain the premise, key turning points and ending in fresh wording.",
            "An uplifting vertical short with a relatable challenge, a practical message, emotional progress and a hopeful final scene."
        };
        LinearLayout exampleGrid=new LinearLayout(this);exampleGrid.setOrientation(LinearLayout.VERTICAL);presets.addView(exampleGrid);
        final int[] chosenExample={0};
        TextView selectedExample=label("Selected: "+examples[0],11);selectedExample.setTextColor(0xff61e8c7);presets.addView(selectedExample);
        for(int i=0;i<examples.length;i+=2){
            LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);exampleGrid.addView(row);
            for(int j=i;j<Math.min(i+2,examples.length);j++){
                final int index=j;Button b=button(examples[j]);b.setTextSize(10);
                LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,dp(46),1);bp.rightMargin=dp(5);row.addView(b,bp);
                b.setOnClickListener(v->{chosenExample[0]=index;selectedExample.setText("Selected: "+examples[index]);});
            }
        }
        LinearLayout choices=card(panel);heading(choices,"02  /  CREATIVE CONTROLS","Use quick choices instead of typing every production detail.");
        choices.addView(label("CONTENT TYPE",10));Spinner type=spinner(new String[]{"Comedy / Entertainment","Action Adventure","Historical / Myanmar Culture","Folk Tale / Moral Story","Movie Recap / Story Summary","Product Promotion","Travel Documentary","Motivational / Education","Music Visualizer"});choices.addView(type);
        choices.addView(label("VISUAL STYLE",10));Spinner visual=spinner(new String[]{"cartoon3d","cartoon2d","cinematic","hybrid"});choices.addView(visual);setSpinnerValue(visual,styleSpinner.getSelectedItem().toString());
        choices.addView(label("PLATFORM / FRAME",10));Spinner format=spinner(new String[]{"9:16  •  Reels / Shorts / TikTok","16:9  •  YouTube","1:1  •  Square Feed","4:5  •  Portrait Feed"});choices.addView(format);
        choices.addView(label("VIDEO DURATION",10));Spinner length=spinner(new String[]{"1 minute","2 minutes","3 minutes","5 minutes","10 minutes","15 minutes","30 minutes","Custom — keep current value"});choices.addView(length);setSpinnerValue(length,"2 minutes");
        choices.addView(label("NARRATION",10));Spinner voice=spinner(new String[]{"Bilingual voice-over","Myanmar voice-over","English voice-over","No voice-over"});choices.addView(voice);
        choices.addView(label("EXPORT QUALITY",10));Spinner quality=spinner(new String[]{"720p","480p"});choices.addView(quality);setSpinnerValue(quality,resolutionSpinner.getSelectedItem().toString());
        LinearLayout flags=new LinearLayout(this);flags.setOrientation(LinearLayout.VERTICAL);choices.addView(flags);
        CheckBox captions=new CheckBox(this);captions.setText("English + Myanmar subtitles");captions.setTextColor(themeText);captions.setChecked(true);flags.addView(captions);
        CheckBox music=new CheckBox(this);music.setText("Background music");music.setTextColor(themeText);music.setChecked(true);flags.addView(music);
        CheckBox sfx=new CheckBox(this);sfx.setText("Sound effects");sfx.setTextColor(themeText);sfx.setChecked(true);flags.addView(sfx);
        LinearLayout ref=card(panel);heading(ref,"03  /  IMAGE REFERENCE","Optional: upload a character, product or visual reference. The image is attached to the project.");
        ImageView preview=new ImageView(this);preview.setScaleType(ImageView.ScaleType.FIT_CENTER);preview.setBackground(shape(0xff071923,0xff205064,14));
        LinearLayout.LayoutParams imageLp=new LinearLayout.LayoutParams(-1,dp(132));imageLp.bottomMargin=dp(7);ref.addView(preview,imageLp);
        if(!characterReferenceBase64.isEmpty()){try{byte[] data=Base64.decode(characterReferenceBase64,Base64.DEFAULT);preview.setImageBitmap(BitmapFactory.decodeByteArray(data,0,data.length));}catch(Exception ignored){}}
        TextView refStatus=label(characterReferenceBase64.isEmpty()?"No image selected — optional":"Reference image ready",10);refStatus.setTextColor(0xff62edc7);ref.addView(refStatus);
        Button upload=button("＋  CHOOSE IMAGE / CHARACTER REFERENCE");ref.addView(upload);
        upload.setOnClickListener(v->{new android.app.AlertDialog.Builder(this).setTitle("ADD VISUAL REFERENCE").setItems(new String[]{"Upload an image","Open Character Reference Lab"},(d,w)->{if(w==0){Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("image/*");startActivityForResult(intent,703);}else openCharacterReferenceLab();}).setNegativeButton("CANCEL",null).show();});
        LinearLayout previewCard=card(panel);heading(previewCard,"04  /  PREVIEW YOUR BRIEF","Review the choices before sending anything to AI or starting a render.");
        TextView briefPreview=label("",11);briefPreview.setTextColor(0xffd7edf4);briefPreview.setPadding(dp(12),dp(12),dp(12),dp(12));briefPreview.setBackground(shape(0xff081923,0xff205064,12));previewCard.addView(briefPreview);
        Runnable refresh=()->{
            String d=length.getSelectedItem().toString();
            String summary="TYPE: "+type.getSelectedItem()+"\nSTYLE: "+visual.getSelectedItem()+"\nFORMAT: "+format.getSelectedItem()+"\nLENGTH: "+d+"\nVOICE: "+voice.getSelectedItem()+"\nQUALITY: "+quality.getSelectedItem()+"\nSUBTITLES: "+(captions.isChecked()?"EN + MY":"Off")+"\nMUSIC: "+(music.isChecked()?"On":"Off")+"  •  SFX: "+(sfx.isChecked()?"On":"Off")+"\nIMAGE: "+(characterReferenceBase64.isEmpty()?"Not attached":"Attached");
            briefPreview.setText(summary);
        };
        type.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){refresh.run();}public void onNothingSelected(android.widget.AdapterView<?> p){}});
        visual.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){refresh.run();}public void onNothingSelected(android.widget.AdapterView<?> p){}});
        format.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){refresh.run();}public void onNothingSelected(android.widget.AdapterView<?> p){}});
        length.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){refresh.run();}public void onNothingSelected(android.widget.AdapterView<?> p){}});
        voice.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){refresh.run();}public void onNothingSelected(android.widget.AdapterView<?> p){}});
        quality.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){refresh.run();}public void onNothingSelected(android.widget.AdapterView<?> p){}});
        captions.setOnCheckedChangeListener((b,c)->refresh.run());music.setOnCheckedChangeListener((b,c)->refresh.run());sfx.setOnCheckedChangeListener((b,c)->refresh.run());
        Button updatePreview=button("↻  UPDATE PREVIEW");previewCard.addView(updatePreview);updatePreview.setOnClickListener(v->refresh.run());refresh.run();
        final android.app.AlertDialog dialog=new android.app.AlertDialog.Builder(this).setTitle("G.I KO KO  /  CREATIVE WORKBENCH").setView(scroll).setPositiveButton("APPLY TO PROJECT",null).setNeutralButton("ASK AI DIRECTOR",null).setNegativeButton("CLOSE",null).create();
        dialog.setOnShowListener(x->{
            dialog.getButton(-1).setOnClickListener(v->{
                applyCreativeWorkbench(examples[chosenExample[0]],briefs[chosenExample[0]],type,visual,format,length,voice,quality,captions,music,sfx);
                dialog.dismiss();
            });
            dialog.getButton(-3).setOnClickListener(v->{
                applyCreativeWorkbench(examples[chosenExample[0]],briefs[chosenExample[0]],type,visual,format,length,voice,quality,captions,music,sfx);
                dialog.dismiss();sendToMasterAi();
            });
        });
        dialog.show();
        refresh.run();
        dialog.getButton(-2).setText("CLOSE");
        Button execute=button("▶  APPLY SETTINGS + EXECUTE CLOUD RENDER");
        LinearLayout.LayoutParams execLp=new LinearLayout.LayoutParams(-1,dp(50));execLp.topMargin=dp(8);previewCard.addView(execute,execLp);
        execute.setOnClickListener(v->{
            applyCreativeWorkbench(examples[chosenExample[0]],briefs[chosenExample[0]],type,visual,format,length,voice,quality,captions,music,sfx);
            dialog.dismiss();saveProject();startRender();
        });
    }

    private void applyCreativeWorkbench(String example,String brief,Spinner type,Spinner visual,Spinner format,Spinner length,Spinner voice,Spinner quality,CheckBox captions,CheckBox music,CheckBox sfx){
        try{
            String contentType=type.getSelectedItem().toString();
            String selectedStyle=visual.getSelectedItem().toString();
            String ratio=format.getSelectedItem().toString().split("  •")[0].trim();
            String duration=length.getSelectedItem().toString().replace(" minute","").replace("s","");
            if(duration.startsWith("Custom"))duration=durationInput.getText().toString().trim();
            if(!duration.matches("[0-9]{1,3}"))duration="2";
            titleInput.setText(example.toUpperCase(Locale.US));
            promptInput.setText(brief+"\n\nContent type: "+contentType+". Visual style: "+selectedStyle+". Target format: "+ratio+". Create a clear opening, coherent scenes, consistent characters, engaging pacing and a satisfying ending. Use original narration and avoid unsupported factual claims.");
            setSpinnerValue(styleSpinner,selectedStyle);setSpinnerValue(aspectRatioSpinner,ratio);
            durationInput.setText(duration);setSpinnerValue(voiceSpinner,voice.getSelectedItem().toString());
            setSpinnerValue(resolutionSpinner,quality.getSelectedItem().toString());
            musicCheck.setChecked(music.isChecked());sfxCheck.setChecked(sfx.isChecked());subtitleCheck.setChecked(captions.isChecked());
            if(sceneInput.getText().toString().trim().isEmpty())sceneInput.setText("Opening — introduce the story and main character\nDevelopment — reveal the goal and first challenge\nTurning point — raise the stakes and change the plan\nFinale — resolve the story with a memorable ending");
            saveProject();setStatus("Creative Workbench settings applied. Review the project and execute when ready.");
        }catch(Exception e){recordException("Apply Creative Workbench",e);setStatus("Could not apply workbench settings: "+e.getMessage());}
    }

    private String[][] starterTemplates(){
        return new String[][]{
            {"MYANMAR PALACE COMEDY","A fast-paced historical comedy inside an ancient Myanmar royal palace. Include a clever but clumsy royal guard, a proud king, visual gags, a chase and a warm comic twist.","A loyal royal guard in emerald-and-gold armour, same face and costume in every scene; clever, nervous and kind.","Palace gate — the guard makes a grand entrance\nThrone room — the king gives a strange order\nRoyal kitchen — a runaway feast causes chaos\nPalace courtyard — the guard saves the day by accident","Guard: I have everything under control. | အားလုံးကို ကျွန်တော် ထိန်းချုပ်ထားပါတယ်။\nKing: Then why is the royal goose wearing my crown? | ဒါဆို တော်ဝင်ငန်းက ဘာလို့ ကျွန်တော့်သရဖူကို ဆောင်းထားတာလဲ။","cartoon3d","5"},
            {"MYANMAR FOLK TALE","Retell a Myanmar-inspired folk tale with a moral lesson, magical atmosphere, kind humour and family-friendly scenes. Keep all character designs consistent.","A brave village child with a simple traditional outfit, warm expressive face and a small woven shoulder bag.","Village morning — introduce the hero\nForest path — discover a mysterious clue\nRiverbank — meet a magical helper\nVillage festival — resolve the problem and share the lesson","Child: I will help you. | ကူညီပေးပါမယ်။\nSpirit: Kindness is stronger than fear. | ကြင်နာမှုက ကြောက်ရွံ့မှုထက် ပိုအားကောင်းတယ်။","cartoon2d","5"},
            {"ACTION ADVENTURE","Create a cinematic action-adventure short with a clear mission, escalating obstacles, safe non-graphic action and a satisfying final reveal.","A young explorer in a navy utility jacket, orange backpack and short dark hair; maintain the same look throughout.","Mission briefing — reveal the objective\nJungle trail — first obstacle\nAncient bridge — high-stakes escape\nHidden chamber — final discovery","Explorer: We have one chance. | ကျွန်တော်တို့မှာ အခွင့်အရေးတစ်ခုပဲ ရှိတယ်။\nPartner: Then let's make it count. | ဒါဆို အဲဒီအခွင့်အရေးကို အကောင်းဆုံးအသုံးချကြစို့။","cinematic","5"},
            {"RURAL MYSTERY","Build a gentle suspense mystery in a Myanmar village. Use clues, atmosphere and clever deduction without horror or graphic violence.","A curious village investigator wearing a light blue shirt, dark longyi and carrying a small notebook.","Quiet village lane — a strange sound at night\nTea shop — collect witness clues\nOld monastery path — discover a footprint\nVillage pond — reveal the harmless truth","Investigator: Every clue matters. | သဲလွန်စတိုင်းက အရေးကြီးတယ်။\nWitness: I heard it near the pond. | ရေကန်နားက အသံကို ကြားခဲ့တာပါ။","hybrid","5"},
            {"KIDS ANIMAL FABLE","Create a colourful family-friendly animal story about teamwork, honesty and friendship. Use expressive characters and gentle comedy.","A small golden-brown rabbit with a green scarf; keep its proportions, scarf and facial features consistent.","Meadow — introduce the rabbit\nForest — meet a shy turtle\nHill path — solve a teamwork challenge\nSunny clearing — celebrate friendship","Rabbit: We can do it together. | အတူတူလုပ်ရင် လုပ်နိုင်ပါတယ်။\nTurtle: Slow steps still move forward. | ဖြည်းဖြည်းသွားလည်း ရှေ့ကိုရောက်ပါတယ်။","cartoon2d","5"},
            {"TRAVEL MINI DOCUMENTARY","Create a short travel documentary about a beautiful Myanmar destination. Use respectful factual narration, scenic establishing shots and concise captions; do not invent specific facts.","A friendly travel host wearing a neutral outdoor jacket and carrying a compact camera.","Opening aerial-style establishing scene\nLocal street and daily life\nFood and craft close-ups\nGolden-hour scenic closing","Host: Let's explore this beautiful place. | ဒီလှပတဲ့နေရာကို အတူလေ့လာကြရအောင်။\nHost: Every journey has a story. | ခရီးစဉ်တိုင်းမှာ ဇာတ်လမ်းတစ်ပုဒ်ရှိပါတယ်။","cinematic","5"},
            {"PRODUCT SHOWCASE","Create a polished product promo with premium lighting, clean close-ups, feature callouts and a clear final call to action. Avoid unsupported product claims.","A clean premium product hero shot with consistent shape, colours, material and logo placement across all scenes.","Hero reveal — premium opening\nDetail macro — highlight craftsmanship\nFeature scene — demonstrate one benefit\nClosing frame — brand and call to action","Narrator: Designed for your everyday. | နေ့စဉ်အသုံးပြုမှုအတွက် ဒီဇိုင်းထုတ်ထားသည်။\nNarrator: Discover the difference. | ကွာခြားချက်ကို ကိုယ်တိုင်ရှာဖွေကြည့်ပါ။","cinematic","1"},
            {"MOTIVATIONAL SHORT","Create an uplifting motivational short with a clear emotional arc, practical message and hopeful ending. Keep the voice calm and authentic.","A young creator in a simple charcoal hoodie, consistent appearance and natural expressions.","Quiet beginning — face a challenge\nSmall action — take the first step\nSetback — learn and adapt\nSunrise ending — show progress","Narrator: Start before you feel ready. | အဆင်သင့်ဖြစ်မယ်လို့ မစောင့်ဘဲ စတင်လိုက်ပါ။\nNarrator: Small steps build big change. | ခြေလှမ်းသေးသေးလေးတွေက အပြောင်းအလဲကြီးကို တည်ဆောက်ပေးတယ်။","hybrid","1"},
            {"MUSIC VIDEO VISUALIZER","Create a rhythmic music-video visual sequence with beat-synced transitions, striking colours and a consistent central performer. Do not reproduce copyrighted lyrics.","A solo performer in a black-and-teal stage outfit, same hairstyle and costume throughout.","Opening silhouette — reveal performer\nVerse mood — atmospheric close-ups\nChorus energy — dynamic stage lights\nFinal beat — memorable closing pose","Performer: Feel the rhythm tonight. | ဒီညရဲ့ စည်းချက်ကို ခံစားလိုက်ပါ။\nPerformer: Let the moment shine. | ဒီအချိန်လေးကို တောက်ပစေပါ။","cartoon3d","1"},
            {"MOVIE RECAP NARRATION","Create a concise, original recap-style narration from user-provided plot notes. Summarize events in your own words, preserve chronology and avoid reproducing dialogue from the source film.","A neutral cinematic narrator; use consistent visual motifs and avoid depicting real actors as exact copies.","Opening — introduce the premise\nSetup — explain the key characters\nTurning point — describe the central conflict\nClimax and ending — summarize the resolution","Narrator: Everything changes after one unexpected decision. | မမျှော်လင့်ထားတဲ့ ဆုံးဖြတ်ချက်တစ်ခုနောက်မှာ အရာအားလုံး ပြောင်းလဲသွားတယ်။\nNarrator: Now the truth comes to light. | အခုတော့ အမှန်တရား ပေါ်လာပါပြီ။","cinematic","5"}
        };
    }
    private void openTemplateGallery(){
        String[][] templates=starterTemplates();String[] names=new String[templates.length];
        for(int i=0;i<templates.length;i++)names[i]="✦  "+templates[i][0];
        new android.app.AlertDialog.Builder(this).setTitle("STARTER TEMPLATE LIBRARY").setItems(names,(d,which)->{
            String[] t=templates[which];promptInput.setText(t[1]);titleInput.setText(t[0]);characterInput.setText(t[2]);sceneInput.setText(t[3]);dialogueInput.setText(t[4]);
            setSpinnerValue(styleSpinner,t[5]);if(durationInput!=null)durationInput.setText(t[6]);setStatus("Template loaded: "+t[0]+". Edit any section before cloud generation.");saveProject();
        }).setNegativeButton("CLOSE",null).show();
    }
    private void openCharacterReferenceLab(){
        LinearLayout panel=column();panel.setPadding(dp(14),dp(8),dp(14),dp(8));
        TextView note=label("Upload a character image. Transparent-background PNG/WebP works best. Gemini describes its visible design; the free FFmpeg engine animates the image layer. No Omni or Veo API is used.",11);
        note.setTextColor(themeMuted);panel.addView(note);
        characterReferencePreview=new ImageView(this);characterReferencePreview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        characterReferencePreview.setBackground(shape(0xff071923,0xff205064,14));
        if(!characterReferenceBase64.isEmpty()){try{byte[] previewBytes=Base64.decode(characterReferenceBase64,Base64.DEFAULT);characterReferencePreview.setImageBitmap(BitmapFactory.decodeByteArray(previewBytes,0,previewBytes.length));}catch(Exception ignored){}}
        LinearLayout.LayoutParams previewLp=new LinearLayout.LayoutParams(-1,dp(140));previewLp.bottomMargin=dp(8);panel.addView(characterReferencePreview,previewLp);
        characterReferenceStatus=label(characterReferenceBase64.isEmpty()?"No reference image selected.":"Reference image ready for cloud animation.",11);
        characterReferenceStatus.setTextColor(0xff62edc7);panel.addView(characterReferenceStatus);
        Button upload=button("＋  UPLOAD CHARACTER IMAGE");panel.addView(upload);
        upload.setOnClickListener(v->{Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("image/*");startActivityForResult(intent,703);});
        TextView motionLabel=label("ANIMATION MOTION",11);motionLabel.setTextColor(themeAccent);panel.addView(motionLabel);
        Spinner motion=spinner(new String[]{"float","bounce","slide","pulse","still"});panel.addView(motion);setSpinnerValue(motion,characterMotion);
        new android.app.AlertDialog.Builder(this).setTitle("CHARACTER REFERENCE LAB").setView(panel)
            .setPositiveButton("APPLY",(d,w)->{characterMotion=motion.getSelectedItem().toString();saveProject();setStatus("Character reference settings saved. Gemini analysis and free image animation will run during Cloud Render.");})
            .setNegativeButton("CLOSE",null).show();
    }
    private void openCharacterBuilder(){
        EditText input=edit(characterInput.getText().toString(),"Describe the character's appearance, outfit, colours, personality and continuity rules.",false);input.setMinLines(5);
        new android.app.AlertDialog.Builder(this).setTitle("CHARACTER CONTINUITY LAB").setMessage("Keep the same face, body proportions, outfit, palette and personality in every scene.").setView(input).setPositiveButton("APPLY",(d,w)->{characterInput.setText(input.getText().toString());saveProject();}).setNegativeButton("CANCEL",null).show();
    }
    private void openDialogueBuilder(){
        EditText input=edit(dialogueInput.getText().toString(),"English line | Myanmar translation",false);input.setMinLines(7);
        new android.app.AlertDialog.Builder(this).setTitle("BILINGUAL DIALOGUE DESK").setMessage("One dialogue turn per line. Format: English sentence | မြန်မာဘာသာပြန်").setView(input).setPositiveButton("APPLY",(d,w)->{dialogueInput.setText(input.getText().toString());saveProject();}).setNegativeButton("CANCEL",null).show();
    }
    private void previewSubtitles(){
        StringBuilder preview=new StringBuilder();String[] lines=dialogueInput.getText().toString().split("\\n");
        for(String line:lines){String[] p=line.split("\\|",2);if(p.length>0&&!p[0].trim().isEmpty())preview.append(p[0].trim()).append("\n");if(p.length>1&&!p[1].trim().isEmpty())preview.append(p[1].trim()).append("\n");preview.append("\n");}
        TextView view=label(preview.length()==0?"No dialogue yet. Add English and Myanmar lines in Dialogue Desk.":preview.toString(),14);view.setTextColor(0xffeffaff);view.setPadding(dp(14),dp(14),dp(14),dp(14));view.setBackground(shape(0xff071923,0xff1d6471,14));
        ScrollView scroll=new ScrollView(this);scroll.addView(view);
        new android.app.AlertDialog.Builder(this).setTitle("SUBTITLE PREVIEW  /  EN + MY").setView(scroll).setPositiveButton("DONE",null).show();
    }
    private void openAudioLab(){
        LinearLayout panel=column();panel.setPadding(dp(14),dp(8),dp(14),dp(8));
        TextView voiceLabel=label("VOICE-OVER PRESET",11);voiceLabel.setTextColor(themeAccent);panel.addView(voiceLabel);
        Spinner voices=spinner(new String[]{"No voice-over","English voice-over","Myanmar voice-over","Bilingual voice-over"});panel.addView(voices);setSpinnerValue(voices,voiceSpinner.getSelectedItem().toString());
        CheckBox music=new CheckBox(this);music.setText("Background music");music.setTextColor(themeText);music.setChecked(musicCheck.isChecked());panel.addView(music);
        CheckBox sfx=new CheckBox(this);sfx.setText("Sound effects");sfx.setTextColor(themeText);sfx.setChecked(sfxCheck.isChecked());panel.addView(sfx);
        new android.app.AlertDialog.Builder(this).setTitle("AUDIO LAB").setMessage("Choose narration and finishing layers for the cloud mix.").setView(panel)
            .setPositiveButton("APPLY",(d,w)->{setSpinnerValue(voiceSpinner,voices.getSelectedItem().toString());musicCheck.setChecked(music.isChecked());sfxCheck.setChecked(sfx.isChecked());saveProject();})
            .setNegativeButton("CANCEL",null).show();
    }
    private void openBrandKit(){
        EditText watermark=edit(watermarkInput.getText().toString(),"Watermark text",false);
        new android.app.AlertDialog.Builder(this).setTitle("BRAND KIT").setMessage("Add your own logo and a consistent studio watermark.").setView(watermark)
            .setPositiveButton("SAVE TEXT",(d,w)->{watermarkInput.setText(watermark.getText().toString());saveProject();})
            .setNeutralButton("IMPORT LOGO",(d,w)->pickWatermarkLogo()).setNegativeButton("CANCEL",null).show();
    }
    private void openFormatPresets(){
        String[] formats={"Landscape 16:9 — YouTube","Portrait 9:16 — Reels / Shorts / TikTok","Square 1:1 — Feed","Portrait 4:5 — Social feed"};
        String[] ratios={"16:9","9:16","1:1","4:5"};
        new android.app.AlertDialog.Builder(this).setTitle("FORMAT PRESETS").setItems(formats,(d,w)->{setSpinnerValue(aspectRatioSpinner,ratios[w]);setStatus("Output format set to "+ratios[w]+". Save draft or render when ready.");}).setNegativeButton("CLOSE",null).show();
    }

    private void openFacebookContentSystem(){
        LinearLayout panel=column();panel.setPadding(dp(14),dp(8),dp(14),dp(8));
        TextView summary=label("FACEBOOK CREATOR MODE\n\nMixed original content: comedy, Myanmar history & culture, storytelling, moral lessons, education, action/adventure and motivation. The AI Director chooses the best mix for each idea.\n\nFree-first production: Gemini Free Tier for story/visual analysis when available; offline FFmpeg animation and Edge TTS for video/audio. No Omni/Veo video API and no paid TTS by default.",12);
        summary.setTextColor(themeText);panel.addView(summary);
        CheckBox enabled=new CheckBox(this);enabled.setText("Enable Facebook mixed-content brief");enabled.setTextColor(themeText);enabled.setChecked(facebookContentMode);panel.addView(enabled);
        new android.app.AlertDialog.Builder(this).setTitle("FACEBOOK CONTENT SYSTEM").setView(panel)
            .setPositiveButton("APPLY",(d,w)->{
                facebookContentMode=enabled.isChecked();
                if(durationInput!=null&&durationInput.getText().toString().trim().isEmpty())durationInput.setText("2");setSpinnerValue(aspectRatioSpinner,"9:16");
                setSpinnerValue(voiceSpinner,"Bilingual voice-over");
                subtitleCheck.setChecked(true);musicCheck.setChecked(true);sfxCheck.setChecked(true);
                saveProject();setStatus("Facebook Creator Mode applied: mixed content, custom minutes (1–180), 9:16 Reels, bilingual voice and subtitles. Type your preferred whole-minute duration.");
            }).setNegativeButton("CANCEL",null).show();
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
        if(resultCode!=RESULT_OK||data==null||data.getData()==null)return;
        if(requestCode==704){
            try(InputStream in=getContentResolver().openInputStream(data.getData())){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1){if(out.size()+n>24000)throw new Exception("Text attachment exceeds 24 KB. Choose a smaller file.");out.write(buffer,0,n);}directorAttachmentText=new String(out.toByteArray(),"UTF-8");String fileName=data.getData().getLastPathSegment();if(directorAttachmentLabel!=null)directorAttachmentLabel.setText("Attached: "+(fileName==null?"document":fileName));setStatus("Text/document attached to AI Director.");}catch(Exception e){setStatus("Attachment failed: "+e.getMessage());}
        }else if(requestCode==702){
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
        }else if(requestCode==703){
            Bitmap source=null,scaled=null;
            try{
                BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;
                try(InputStream boundsIn=getContentResolver().openInputStream(data.getData())){BitmapFactory.decodeStream(boundsIn,null,bounds);}
                int sample=1;while(Math.max(bounds.outWidth/sample,bounds.outHeight/sample)>1024)sample*=2;
                BitmapFactory.Options decodeOptions=new BitmapFactory.Options();decodeOptions.inSampleSize=sample;
                try(InputStream imageIn=getContentResolver().openInputStream(data.getData())){source=BitmapFactory.decodeStream(imageIn,null,decodeOptions);}
                if(source==null)throw new Exception("Unsupported image.");
                byte[] compressed=null;
                int[] limits={512,448,384,320,256};
                int[] qualities={62,54,46};
                for(int limit:limits){
                    float scale=Math.min(1f,(float)limit/Math.max(source.getWidth(),source.getHeight()));
                    scaled=Bitmap.createScaledBitmap(source,Math.max(1,(int)(source.getWidth()*scale)),Math.max(1,(int)(source.getHeight()*scale)),true);
                    for(int quality:qualities){
                        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
                        scaled.compress(Bitmap.CompressFormat.WEBP,quality,bytes);
                        if(bytes.size()<=19500){compressed=bytes.toByteArray();break;}
                    }
                    if(compressed!=null)break;
                    if(scaled!=source){scaled.recycle();scaled=null;}
                }
                if(compressed==null)throw new Exception("Image is too detailed for the safe project upload limit. Try a simpler character PNG/WebP.");
                characterReferenceBase64=Base64.encodeToString(compressed,Base64.NO_WRAP);
                prefs().edit().putString("character_reference_base64",characterReferenceBase64).apply();
                if(characterReferenceStatus!=null)characterReferenceStatus.setText("Reference image ready ("+(compressed.length/1024)+" KB compressed).");
                if(characterReferencePreview!=null)characterReferencePreview.setImageBitmap(BitmapFactory.decodeByteArray(compressed,0,compressed.length));
                setStatus("Character reference imported. Gemini visual analysis + free cloud animation are enabled for the next render.");
                saveProject();
            }catch(Exception e){recordException("Import character reference",e);setStatus("Character image import failed: "+e.getMessage());}
            finally{if(source!=null&&!source.isRecycled())source.recycle();if(scaled!=null&&scaled!=source&&!scaled.isRecycled())scaled.recycle();}
        }
    }
    @Override protected void onPause(){
        super.onPause();
        if(titleInput!=null){try{prefs().edit().putString("last_project",buildProject().toString()).putString("watermark_logo_base64",watermarkLogoBase64).putString("character_reference_base64",characterReferenceBase64).apply();}catch(Exception ignored){}}
    }

    private void saveProject(){
        JSONObject p=buildProject();String raw=p.toString();
        try{
            JSONArray library=new JSONArray(prefs().getString("project_library","[]"));
            String title=p.optString("title","Untitled Project");boolean replaced=false;
            for(int i=0;i<library.length();i++){JSONObject item=library.optJSONObject(i);if(item!=null&&title.equals(item.optString("title"))){library.put(i,new JSONObject().put("title",title).put("project",p));replaced=true;break;}}
            if(!replaced)library.put(new JSONObject().put("title",title).put("project",p));
            while(library.length()>20)library.remove(0);
            prefs().edit().putString("project_library",library.toString()).putString("last_project",raw).putString("watermark_logo_base64",watermarkLogoBase64).putString("character_reference_base64",characterReferenceBase64).apply();
        }catch(Exception e){recordException("Save project library",e);prefs().edit().putString("last_project",raw).apply();}
        setStatus("Project saved to this phone and Project Library ("+raw.length()+" characters).");
    }
    private void setSpinnerValue(Spinner spinner,String value){for(int i=0;i<spinner.getCount();i++)if(spinner.getItemAtPosition(i).toString().equals(value)){spinner.setSelection(i);return;}}
    private void restoreProject(){
        String raw=prefs().getString("last_project",null);if(raw==null)return;
        try{JSONObject p=new JSONObject(raw);titleInput.setText(p.optString("title",""));promptInput.setText(p.optString("prompt",""));characterInput.setText(p.optString("character_bible",""));
            JSONArray a=p.optJSONArray("scenes");if(a!=null){StringBuilder b=new StringBuilder();for(int i=0;i<a.length();i++)b.append(a.getString(i)).append("\n");sceneInput.setText(b.toString());}
            animationCode=p.optString("animation_code","");
            characterReferenceBase64=p.optString("character_reference_base64",prefs().getString("character_reference_base64",""));
            characterMotion=p.optString("character_motion","float");
            watermarkLogoBase64=p.optString("watermark_logo_base64",prefs().getString("watermark_logo_base64",""));
            JSONArray ds=p.optJSONArray("dialogue");if(ds!=null){StringBuilder b=new StringBuilder();for(int i=0;i<ds.length();i++){JSONObject d=ds.getJSONObject(i);b.append(d.optString("english","")).append(" | ").append(d.optString("myanmar","")).append("\n");}dialogueInput.setText(b.toString());}
            musicCheck.setChecked(p.optBoolean("background_music",true));sfxCheck.setChecked(p.optBoolean("sfx",true));subtitleCheck.setChecked(p.optBoolean("bilingual_subtitles",p.optBoolean("myanmar_subtitles",true)));
            facebookContentMode=p.optBoolean("facebook_content_mode",p.optString("content_mode","mixed").equals("mixed"));setSpinnerValue(styleSpinner,p.optString("style","hybrid"));if(durationInput!=null)durationInput.setText(p.optString("duration_minutes","2"));setSpinnerValue(resolutionSpinner,p.optString("resolution","480p"));setSpinnerValue(voiceSpinner,p.optString("voice","No voice-over"));
            if(aspectRatioSpinner!=null)setSpinnerValue(aspectRatioSpinner,p.optString("aspect_ratio","16:9"));
            if(frameRateSpinner!=null)setSpinnerValue(frameRateSpinner,p.optString("fps","24"));
            if(watermarkInput!=null)watermarkInput.setText(p.optString("watermark","POWERED BY I KO KO"));
        }catch(Exception ignored){}
    }

    private void assistStory(){
        if(busy)return;
        final String userPrompt=promptInput.getText().toString().trim();
        final String durationForPrompt;
        try{durationForPrompt=getDurationMinutes();}catch(IllegalArgumentException e){setStatus(e.getMessage());return;}
        final String prompt=facebookContentMode ? userPrompt+"\n\nFACEBOOK MIXED-CONTENT PRODUCTION BRIEF: Create an original Facebook-ready episode combining suitable elements from comedy, Myanmar history and culture, storytelling, moral lessons, education, action/adventure and motivation. Select only the categories that fit the idea; do not force every category into one episode. Hook viewers in the first 3 seconds, use a clear beginning, escalating middle and satisfying ending, short scene beats, strong visual actions, and natural bilingual English + Myanmar dialogue. Keep character appearance consistent. Target exactly about "+durationForPrompt+" minutes, with enough scenes and dialogue to fill the selected duration. Avoid copyrighted characters, copied scripts, unlicensed music and misleading factual claims." : userPrompt;
        if(prompt.isEmpty()){setStatus("Enter a film idea first.");return;}
        if(token==null){
            new ProviderManager(this).generateStory(prompt,(story,err,provider)->{
                if(err!=null){appendChatMessage("AI DIRECTOR","No cloud GitHub token or saved custom provider is available. A basic offline outline was created.");offlineStoryAssist(prompt);return;}
                applyStoryDraft(story,"device provider: "+provider);
            });
            return;
        }
        final String requestId=UUID.randomUUID().toString().replace("-","").substring(0,12);
        final String duration;try{duration=getDurationMinutes();}catch(IllegalArgumentException e){setStatus(e.getMessage());return;}
        final String style=styleSpinner.getSelectedItem().toString();
        final String geminiModel=getSelectedGeminiModel();
        setBusy(true);setStatus("AI Director is preparing your storyboard with Gemini Cloud…");
        generationDialog=new android.app.ProgressDialog(this);
        generationDialog.setTitle("G.I KO KO  /  AI DIRECTOR");
        generationDialog.setMessage("Sending your prompt to Gemini through GitHub Actions…");
        generationDialog.setIndeterminate(true);generationDialog.setCancelable(false);generationDialog.show();
        new Thread(()->{
            try{
                JSONObject inputs=new JSONObject();
                inputs.put("request_id",requestId);inputs.put("prompt",prompt);inputs.put("duration",duration);inputs.put("style",style);inputs.put("gemini_model",geminiModel);
                JSONObject payload=new JSONObject();payload.put("ref","main");payload.put("inputs",inputs);
                ApiResponse dispatch=api("POST",API+"/actions/workflows/story-assist.yml/dispatches",token,payload.toString());
                if(dispatch.code!=204)throw new Exception("Gemini story workflow dispatch failed (HTTP "+dispatch.code+"): "+dispatch.body);
                JSONObject run=null;
                for(int i=0;i<90;i++){
                    Thread.sleep(5000);
                    ApiResponse rr=api("GET",API+"/actions/workflows/story-assist.yml/runs?event=workflow_dispatch&per_page=30",token,null);
                    if(rr.code!=200)throw new Exception("Could not find AI Director run (HTTP "+rr.code+"): "+rr.body);
                    JSONArray runs=new JSONObject(rr.body).optJSONArray("workflow_runs");
                    if(runs!=null)for(int j=0;j<runs.length();j++){
                        JSONObject item=runs.getJSONObject(j);
                        String display=item.optString("display_title",item.optString("name",""));
                        if(display.contains(requestId)){run=item;break;}
                    }
                    if(run!=null)break;
                }
                if(run==null)throw new Exception("Gemini workflow was accepted, but its run ID was not found. Check GitHub Actions.");
                long runId=run.getLong("id");
                JSONObject latest=run;
                for(int i=0;i<180;i++){
                    ApiResponse rr=api("GET",API+"/actions/runs/"+runId,token,null);
                    if(rr.code!=200)throw new Exception("AI Director status failed (HTTP "+rr.code+").");
                    latest=new JSONObject(rr.body);
                    if("completed".equals(latest.optString("status"))){
                        if(!"success".equals(latest.optString("conclusion")))throw new Exception("Gemini story workflow ended: "+latest.optString("conclusion")+". Check the Actions log and GEMINI_API_KEY secret.");
                        break;
                    }
                    Thread.sleep(4000);
                }
                if(!"completed".equals(latest.optString("status")))throw new Exception("AI Director is still running. Try again shortly.");
                ApiResponse ar=api("GET",API+"/actions/runs/"+runId+"/artifacts?per_page=20",token,null);
                if(ar.code!=200)throw new Exception("Story artifact list failed (HTTP "+ar.code+").");
                JSONArray artifacts=new JSONObject(ar.body).optJSONArray("artifacts");
                JSONObject chosen=null;
                if(artifacts!=null)for(int i=0;i<artifacts.length();i++){JSONObject item=artifacts.getJSONObject(i);if(!item.optBoolean("expired",false)&&item.optString("name","").equals("GI-KO-KO-STORY-"+requestId)){chosen=item;break;}}
                if(chosen==null)throw new Exception("Gemini finished but the story.json artifact is missing.");
                ApiResponse redir=api("GET",API+"/actions/artifacts/"+chosen.getLong("id")+"/zip",token,null);
                if(redir.code!=302||redir.location==null)throw new Exception("Could not download the AI Director story result (HTTP "+redir.code+").");
                HttpURLConnection conn=(HttpURLConnection)new URL(redir.location).openConnection();
                conn.setConnectTimeout(30000);conn.setReadTimeout(60000);conn.setInstanceFollowRedirects(true);
                String storyText=null;
                try(ZipInputStream zin=new ZipInputStream(new BufferedInputStream(conn.getInputStream()))){
                    ZipEntry entry;while((entry=zin.getNextEntry())!=null){
                        if(!entry.isDirectory()&&entry.getName().toLowerCase(Locale.US).endsWith("story.json")){
                            ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;
                            while((n=zin.read(buf))!=-1)out.write(buf,0,n);
                            storyText=out.toString("UTF-8");break;
                        }
                    }
                }finally{conn.disconnect();}
                if(storyText==null)throw new Exception("Downloaded story package did not contain story.json.");
                JSONObject story=new JSONObject(storyText);
                main.post(()->applyStoryDraft(story,"Gemini Cloud"));
            }catch(Exception e){
                recordException("Gemini Cloud AI Director",e);
                main.post(()->{appendChatMessage("AI DIRECTOR","Cloud story generation failed: "+e.getMessage());setStatus("AI Director error: "+e.getMessage());if(generationDialog!=null){generationDialog.dismiss();generationDialog=null;}setBusy(false);});
            }
        }).start();
    }

    // Gemini Master AI: secure GitHub Actions proxy keeps GEMINI_API_KEY out of the APK.
    private void sendToMasterAi(){
        if(busy)return;
        if(token==null){appendChatMessage("GEMINI MASTER AI","Please add your GitHub fine-grained token in Settings. Gemini API access is provided securely by the existing GEMINI_API_KEY Actions secret.");setStatus("GitHub token required for Gemini Master AI. Your Gemini key stays in GitHub Secrets.");return;}
        final String prompt=promptInput.getText().toString().trim();
        if(prompt.isEmpty())return;
        final String requestId=UUID.randomUUID().toString().replace("-","").substring(0,12);
        final String projectJson=buildProject().toString();
        final String geminiModel=getSelectedGeminiModel();
        if(projectJson.length()>45000){setStatus("Project context is too large for Gemini. Shorten the project notes and try again.");return;}
        setBusy(true);setStatus("Gemini Master AI is reviewing your command and current project…");
        generationDialog=new android.app.ProgressDialog(this);generationDialog.setTitle("G.I KO KO  /  GEMINI MASTER AI");generationDialog.setMessage("Securely sending the command to Gemini through GitHub Actions…");generationDialog.setIndeterminate(true);generationDialog.setCancelable(false);generationDialog.show();
        new Thread(()->{
            try{
                JSONObject inputs=new JSONObject();inputs.put("request_id",requestId);inputs.put("prompt",prompt);inputs.put("project_json",projectJson);inputs.put("gemini_model",geminiModel);
                JSONObject payload=new JSONObject();payload.put("ref","main");payload.put("inputs",inputs);
                ApiResponse dispatched=api("POST",API+"/actions/workflows/studio-agent.yml/dispatches",token,payload.toString());
                if(dispatched.code!=204)throw new Exception("Gemini Master AI dispatch failed (HTTP "+dispatched.code+"): "+dispatched.body);
                JSONObject run=null;
                for(int i=0;i<90;i++){
                    Thread.sleep(5000);
                    ApiResponse rr=api("GET",API+"/actions/workflows/studio-agent.yml/runs?event=workflow_dispatch&per_page=30",token,null);
                    if(rr.code!=200)throw new Exception("Could not find Gemini agent run (HTTP "+rr.code+"): "+rr.body);
                    JSONArray runs=new JSONObject(rr.body).optJSONArray("workflow_runs");
                    if(runs!=null)for(int j=0;j<runs.length();j++){JSONObject item=runs.getJSONObject(j);String title=item.optString("display_title",item.optString("name",""));if(title.contains(requestId)){run=item;break;}}
                    if(run!=null)break;
                }
                if(run==null)throw new Exception("Gemini accepted the request but the agent run ID was not found. Check GitHub Actions.");
                long runId=run.getLong("id");JSONObject latest=run;
                for(int i=0;i<180;i++){
                    ApiResponse sr=api("GET",API+"/actions/runs/"+runId,token,null);
                    if(sr.code!=200)throw new Exception("Gemini agent status failed (HTTP "+sr.code+").");
                    latest=new JSONObject(sr.body);
                    if("completed".equals(latest.optString("status"))){if(!"success".equals(latest.optString("conclusion")))throw new Exception("Gemini agent workflow ended: "+latest.optString("conclusion")+". Check Actions logs and the GEMINI_API_KEY secret.");break;}
                    Thread.sleep(4000);
                }
                if(!"completed".equals(latest.optString("status")))throw new Exception("Gemini is still processing. Please try again shortly.");
                ApiResponse ar=api("GET",API+"/actions/runs/"+runId+"/artifacts?per_page=20",token,null);
                if(ar.code!=200)throw new Exception("Gemini agent artifact list failed (HTTP "+ar.code+").");
                JSONArray artifacts=new JSONObject(ar.body).optJSONArray("artifacts");JSONObject chosen=null;
                if(artifacts!=null)for(int i=0;i<artifacts.length();i++){JSONObject item=artifacts.getJSONObject(i);if(!item.optBoolean("expired",false)&&item.optString("name","").equals("GI-KO-KO-AGENT-"+requestId)){chosen=item;break;}}
                if(chosen==null)throw new Exception("Gemini completed but agent.json artifact is missing.");
                ApiResponse redir=api("GET",API+"/actions/artifacts/"+chosen.getLong("id")+"/zip",token,null);
                if(redir.code!=302||redir.location==null)throw new Exception("Could not download Gemini agent result (HTTP "+redir.code+").");
                HttpURLConnection conn=(HttpURLConnection)new URL(redir.location).openConnection();conn.setConnectTimeout(30000);conn.setReadTimeout(60000);conn.setInstanceFollowRedirects(true);
                String resultText=null;
                try(ZipInputStream zin=new ZipInputStream(new BufferedInputStream(conn.getInputStream()))){ZipEntry entry;while((entry=zin.getNextEntry())!=null){if(!entry.isDirectory()&&entry.getName().toLowerCase(Locale.US).endsWith("agent.json")){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;while((n=zin.read(buf))!=-1)out.write(buf,0,n);resultText=out.toString("UTF-8");break;}}}finally{conn.disconnect();}
                if(resultText==null)throw new Exception("Downloaded Gemini result did not contain agent.json.");
                JSONObject result=new JSONObject(resultText);main.post(()->applyMasterAiResult(result));
            }catch(Exception e){recordException("Gemini Master AI",e);main.post(()->{appendChatMessage("GEMINI MASTER AI","Request failed: "+e.getMessage());setStatus("Gemini Master AI error: "+e.getMessage());if(generationDialog!=null){generationDialog.dismiss();generationDialog=null;}setBusy(false);});}
        }).start();
    }

    private void applyMasterAiResult(JSONObject result){
        try{
            JSONObject u=result.optJSONObject("project_updates");boolean changed=false;
            if(u!=null){
                if(u.has("title")){titleInput.setText(u.optString("title",""));changed=true;}
                if(u.has("character_bible")){characterInput.setText(u.optString("character_bible",""));changed=true;}
                if(u.has("scenes")){JSONArray a=u.optJSONArray("scenes");if(a!=null){StringBuilder b=new StringBuilder();for(int i=0;i<a.length();i++)b.append(a.optString(i)).append("\n");sceneInput.setText(b.toString());changed=true;}}
                if(u.has("dialogue")){JSONArray a=u.optJSONArray("dialogue");if(a!=null){StringBuilder b=new StringBuilder();for(int i=0;i<a.length();i++){JSONObject d=a.optJSONObject(i);if(d!=null)b.append(d.optString("english","")).append(" | ").append(d.optString("myanmar","")).append("\n");}dialogueInput.setText(b.toString());changed=true;}}
                if(u.has("duration_minutes")&&durationInput!=null){int n=u.optInt("duration_minutes",0);if(n>=1&&n<=180){durationInput.setText(Integer.toString(n));changed=true;}}
                if(u.has("style")){String v=u.optString("style","");if(v.matches("hybrid|cartoon2d|cartoon3d|cinematic")){setSpinnerValue(styleSpinner,v);changed=true;}}
                if(u.has("resolution")){String v=u.optString("resolution","");if(v.matches("480p|720p")){setSpinnerValue(resolutionSpinner,v);changed=true;}}
                if(u.has("aspect_ratio")){String v=u.optString("aspect_ratio","");if(v.matches("16:9|9:16|1:1|4:5")){setSpinnerValue(aspectRatioSpinner,v);changed=true;}}
                if(u.has("fps")){String v=u.optString("fps","");if(v.matches("24|30|60")){setSpinnerValue(frameRateSpinner,v);changed=true;}}
                if(u.has("voice")){String v=u.optString("voice","");if(v.matches("No voice-over|English voice-over|Myanmar voice-over|Bilingual voice-over")){setSpinnerValue(voiceSpinner,v);changed=true;}}
                if(u.has("background_music")){musicCheck.setChecked(u.optBoolean("background_music"));changed=true;}
                if(u.has("sfx")){sfxCheck.setChecked(u.optBoolean("sfx"));changed=true;}
                if(u.has("bilingual_subtitles")){subtitleCheck.setChecked(u.optBoolean("bilingual_subtitles"));changed=true;}
            }
            String reply=result.optString("reply","Gemini has processed your request.");
            if(changed)reply+="\n\nProject settings updated. Review the fields before rendering.";
            appendChatMessage("GEMINI MASTER AI",reply);
            if(generationDialog!=null){generationDialog.dismiss();generationDialog=null;}setBusy(false);
            String action=result.optString("action","chat");executeMasterAiAction(action,result.optString("confirmation_message",""));
        }catch(Exception e){recordException("Apply Gemini agent result",e);if(generationDialog!=null){generationDialog.dismiss();generationDialog=null;}setBusy(false);setStatus("Could not apply Gemini response: "+e.getMessage());}
    }

    private void executeMasterAiAction(String action,String confirmation){
        switch(action){
            case "open_character_reference_lab": openCharacterReferenceLab();break;
            case "open_character_builder": openCharacterBuilder();break;
            case "open_dialogue_desk": openDialogueBuilder();break;
            case "open_subtitle_preview": previewSubtitles();break;
            case "open_audio_lab": openAudioLab();break;
            case "open_project_library": openProjectLibrary();break;
            case "open_brand_kit": openBrandKit();break;
            case "open_format_presets": openFormatPresets();break;
            case "open_facebook_content": openFacebookContentSystem();break;
            case "open_template_gallery": openTemplateGallery();break;
            case "open_render_center": openRenderCenter();break;
            case "open_diagnostics": showDiagnosticCenter();break;
            case "open_settings": openSettings();break;
            case "save_project": saveProject();setStatus("Project saved from Gemini command.");break;
            case "check_render_status": checkLastRun();break;
            case "retry_render": new android.app.AlertDialog.Builder(this).setTitle("GEMINI REQUEST — RETRY RENDER").setMessage("Retry the failed GitHub render job now?").setPositiveButton("RETRY",(d,w)->retryFailedRender()).setNegativeButton("CANCEL",null).show();break;
            case "download_latest_mp4": downloadLastRun();break;
            case "start_render": new android.app.AlertDialog.Builder(this).setTitle("CONFIRM CLOUD RENDER").setMessage(confirmation.isEmpty()?"Gemini is ready to start a Cloud Render using the current project settings. Continue?":confirmation).setPositiveButton("START RENDER",(d,w)->{if(ensureStoragePermission())startRender();else{pendingStart=true;requestStoragePermission();}}).setNegativeButton("CANCEL",null).show();break;
            default: setStatus("Gemini Master AI is ready. No app action was requested.");break;
        }
    }
    private void applyStoryDraft(JSONObject story,String provider){
        titleInput.setText(story.optString("title","Untitled Story"));
        characterInput.setText(story.optString("character_bible",""));
        animationCode=story.optString("animation_code","");
        JSONArray scenes=story.optJSONArray("scenes");StringBuilder sceneText=new StringBuilder();
        if(scenes!=null)for(int i=0;i<scenes.length();i++)sceneText.append(scenes.optString(i)).append("\n");
        sceneInput.setText(sceneText.toString());
        JSONArray dialogue=story.optJSONArray("dialogue");StringBuilder dialogueText=new StringBuilder();
        if(dialogue!=null)for(int i=0;i<dialogue.length();i++){JSONObject d=dialogue.optJSONObject(i);if(d!=null)dialogueText.append(d.optString("english","")).append(" | ").append(d.optString("myanmar","")).append("\n");}
        dialogueInput.setText(dialogueText.toString());
        appendChatMessage("AI DIRECTOR","Storyboard ready via "+provider+". "+(scenes==null?0:scenes.length())+" scenes and "+(dialogue==null?0:dialogue.length())+" dialogue turns. Review the draft and start Cloud Render when ready.");
        if(generationDialog!=null){generationDialog.dismiss();generationDialog=null;}
        setBusy(false);setStatus("AI story draft ready via "+provider+". Review and edit before rendering.");
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
        final String duration;try{duration=getDurationMinutes();}catch(IllegalArgumentException e){setStatus(e.getMessage());return;}
        final String resolution=resolutionSpinner.getSelectedItem().toString();
        final String requestId=UUID.randomUUID().toString().replace("-","").substring(0,12);
        final String geminiModel=getSelectedGeminiModel();
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
                inputs.put("project_json",projectJson);inputs.put("gemini_model",geminiModel);
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
        if(!"480p".equals(resolution)||fps>24){setStatus("For stable Local Render on low-memory phones, select 480p and 24 FPS. Use Cloud Render for 720p or 30/60 FPS.");return;}
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