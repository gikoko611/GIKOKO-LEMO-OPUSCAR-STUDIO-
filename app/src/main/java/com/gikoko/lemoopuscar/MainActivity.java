package com.gikoko.lemoopuscar;
import android.app.Activity;
import android.os.Bundle;
import android.os.AsyncTask;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.*;
import android.content.Intent;
import android.net.Uri;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class MainActivity extends Activity {
    private static final String OWNER="gikoko611", REPO="GIKOKO-LEMO-OPUSCAR-STUDIO-";
    private static final String WORKFLOW_URL="https://github.com/"+OWNER+"/"+REPO+"/actions/workflows/cloud-render.yml";
    private static final String RUNS_API="https://api.github.com/repos/"+OWNER+"/"+REPO+"/actions/workflows/cloud-render.yml/runs?per_page=1";
    private Spinner styleSpinner,durationSpinner,resolutionSpinner;
    private TextView status;

    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(Color.rgb(8,11,18));getWindow().setNavigationBarColor(Color.rgb(8,11,18));buildUi();}
    private TextView label(String s,int size){TextView v=new TextView(this);v.setText(s);v.setTextColor(Color.rgb(245,226,177));v.setTextSize(size);v.setPadding(0,16,0,8);return v;}
    private Spinner spinner(String[] values){Spinner s=new Spinner(this);ArrayAdapter<String>a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,values);a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);s.setAdapter(a);return s;}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);return b;}
    private void buildUi(){
        ScrollView scroll=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(24,28,24,28);root.setBackgroundColor(Color.rgb(8,11,18));scroll.addView(root);
        TextView title=label("G.I KO KO",32);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);title.setGravity(Gravity.CENTER);root.addView(title);
        TextView sub=label("LEMO OPUSCAR PROFESSIONAL STUDIO",14);sub.setGravity(Gravity.CENTER);root.addView(sub);
        TextView credit=label("POWERED BY I KO KO",12);credit.setGravity(Gravity.CENTER);root.addView(credit);
        root.addView(label("1. FILM STYLE",16));styleSpinner=spinner(new String[]{"cartoon2d","cartoon3d","cinematic"});root.addView(styleSpinner);
        root.addView(label("2. DURATION (MINUTES)",16));durationSpinner=spinner(new String[]{"1","5","10","30"});root.addView(durationSpinner);
        root.addView(label("3. RESOLUTION",16));resolutionSpinner=spinner(new String[]{"480p","720p"});root.addView(resolutionSpinner);
        root.addView(label("Selections are a guide: choose the same options on the GitHub Run workflow page. Render runs in the cloud.",13));
        Button start=button("OPEN GITHUB CLOUD RENDER");root.addView(start);start.setOnClickListener(v->startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(WORKFLOW_URL))));
        Button check=button("CHECK LATEST RENDER STATUS");root.addView(check);check.setOnClickListener(v->new StatusTask().execute());
        status=label("Status: Ready. No render status checked yet.",14);root.addView(status);
        root.addView(label("No GitHub token is stored in this APK. 30-minute renders may take a long time and are subject to GitHub usage limits.",12));
        setContentView(scroll);
    }
    private class StatusTask extends AsyncTask<Void,Void,String>{
        protected void onPreExecute(){status.setText("Status: Checking GitHub Actions…");}
        protected String doInBackground(Void...x){
            HttpURLConnection c=null;
            try{
                c=(HttpURLConnection)new URL(RUNS_API).openConnection();c.setRequestMethod("GET");c.setConnectTimeout(12000);c.setReadTimeout(12000);
                c.setRequestProperty("Accept","application/vnd.github+json");c.setRequestProperty("X-GitHub-Api-Version","2022-11-28");
                int code=c.getResponseCode();if(code!=200)return "GitHub API HTTP "+code+". Check repository visibility or network.";
                BufferedReader r=new BufferedReader(new InputStreamReader(c.getInputStream(),"UTF-8"));StringBuilder b=new StringBuilder();String line;while((line=r.readLine())!=null)b.append(line);r.close();
                JSONArray runs=new JSONObject(b.toString()).optJSONArray("workflow_runs");if(runs==null||runs.length()==0)return "No render workflow runs found yet.";
                JSONObject run=runs.getJSONObject(0);String conclusion=run.isNull("conclusion")?"in progress":run.optString("conclusion");
                return run.optString("display_title","Cloud render")+"\nStatus: "+run.optString("status")+" / "+conclusion+"\n"+run.optString("html_url");
            }catch(Exception e){return "Could not check status: "+e.getMessage();}finally{if(c!=null)c.disconnect();}
        }
        protected void onPostExecute(String result){status.setText(result);}
    }
}
