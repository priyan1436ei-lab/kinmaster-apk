package dev.priyan.cut;

import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.*;
import android.provider.OpenableColumns;
import android.view.*;
import android.widget.*;
import androidx.media3.common.*;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import androidx.media3.transformer.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

@android.annotation.SuppressLint("UnsafeOptInUsageError")
public final class MainActivity extends Activity {
    private final ArrayList<Clip> clips = new ArrayList<>();
    private final ArrayDeque<ArrayList<Clip>> undo = new ArrayDeque<>(), redo = new ArrayDeque<>();
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ExoPlayer player;
    private PlayerView preview;
    private LinearLayout timeline, root;
    private TextView status, count;
    private ProjectStore store;
    private int selected = -1;
    private boolean busy, destroyed;
    private Transformer transformer;
    private File exportFile;
    private ProgressDialog progress;
    private static final int IMPORT=10, SAVE_VIDEO=11;
    private static final int BG=0xff10151f, PANEL=0xff1d2635, MINT=0xff6ef3c5;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        store = new ProjectStore(this);
        player = new ExoPlayer.Builder(this).build();
        buildUi();
        try { clips.addAll(store.load()); } catch (Exception e) { message("Project restore failed: " + e.getMessage()); }
        selected = clips.isEmpty() ? -1 : 0; refresh();
        player.addListener(new Player.Listener() {
            @Override public void onPlayerError(PlaybackException error) { message("Preview failed. This media may be unsupported: " + error.getErrorCodeName()); }
        });
    }
    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density); }
    private TextView label(String value, int size, int color) {
        TextView t=new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color); t.setPadding(dp(12),dp(8),dp(12),dp(8)); return t;
    }
    private Button button(String name, Runnable action) {
        Button b=new Button(this); b.setText(name); b.setAllCaps(false); b.setTextColor(Color.WHITE); b.setTextSize(13);
        b.setOnClickListener(v -> { if (busy) { message("Finish or cancel the current operation first"); return; } action.run(); }); return b;
    }
    private void row(LinearLayout parent, String[] names, Runnable[] actions) {
        HorizontalScrollView scroll=new HorizontalScrollView(this); LinearLayout r=new LinearLayout(this);
        for(int i=0;i<names.length;i++) r.addView(button(names[i],actions[i])); scroll.addView(r); parent.addView(scroll);
    }
    private void buildUi() {
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(BG);
        root.setPadding(0,dp(8),0,dp(6));
        if (Build.VERSION.SDK_INT >= 30) root.setOnApplyWindowInsetsListener((v,insets) -> {
            android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()); v.setPadding(bars.left,bars.top,bars.right,bars.bottom); return insets;
        });
        TextView brand=label("◈  PRIYAN CUT   /   STUDIO",23,MINT); brand.setTypeface(null,Typeface.BOLD); root.addView(brand);
        root.addView(label("Make your next story.",13,0xffa2afc5));
        row(root,new String[]{"＋ Import videos","Undo","Redo","New project"},new Runnable[]{this::pick,this::undoEdit,this::redoEdit,this::newProject});
        preview=new PlayerView(this); preview.setPlayer(player); preview.setUseController(true); preview.setBackgroundColor(Color.BLACK);
        root.addView(preview,new LinearLayout.LayoutParams(-1,0,1));
        status=label("Import a video to begin",13,Color.WHITE); root.addView(status);
        count=label("TIMELINE · 0 clips",12,MINT); root.addView(count);
        HorizontalScrollView scroll=new HorizontalScrollView(this); timeline=new LinearLayout(this); scroll.addView(timeline); root.addView(scroll);
        root.addView(label("Select a clip to edit • preview shows the selected clip",11,0xffa2afc5));
        row(root,new String[]{"Trim","Split at playhead","Text","Rotate 90°","B&W","Mute"},new Runnable[]{this::trim,this::split,this::text,()->change(c->c.rotation=(c.rotation+90)%360),()->change(c->c.grayscale=!c.grayscale),()->change(c->c.muted=!c.muted)});
        row(root,new String[]{"← Move","Move →","Duplicate","Delete","▶ Export MP4","Save last render"},new Runnable[]{()->move(-1),()->move(1),this::duplicate,this::delete,this::exportOptions,this::saveLastRender});
        setContentView(root);
    }
    private void message(String s) { if (!destroyed) Toast.makeText(this,s,Toast.LENGTH_LONG).show(); }
    private Clip current() { return selected>=0 && selected<clips.size()?clips.get(selected):null; }
    private boolean requireClip() { if(current()==null) { message("Import and select a video first"); return false; } return true; }
    private ArrayList<Clip> snapshot() { ArrayList<Clip> result=new ArrayList<>(); for(Clip c:clips) result.add(c.copy()); return result; }
    private void checkpoint() { undo.push(snapshot()); if(undo.size()>50) undo.removeLast(); redo.clear(); }
    private void persist() { try { store.save(clips); } catch(Exception e) { message("Unable to save project: "+e.getMessage()); } }
    private void refresh() {
        timeline.removeAllViews(); long duration=0;
        for(int i=0;i<clips.size();i++) {
            Clip c=clips.get(i); duration+=c.lengthMs(); final int index=i;
            TextView tile=label((i+1)+"  "+c.name+"\n"+String.format(Locale.US,"%.2fs",c.lengthMs()/1000.0)+(c.muted?"  · muted":""),13,Color.WHITE);
            tile.setMaxWidth(dp(190)); GradientDrawable bg=new GradientDrawable(); bg.setColor(PANEL); bg.setCornerRadius(dp(10)); bg.setStroke(dp(2), i==selected?MINT:PANEL); tile.setBackground(bg);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(180),dp(74)); lp.setMargins(dp(5),dp(4),dp(5),dp(4)); timeline.addView(tile,lp);
            tile.setOnClickListener(v->{ if(!busy) { selected=index; refresh(); } });
        }
        count.setText(String.format(Locale.US,"TIMELINE · %d clips · %.2f seconds",clips.size(),duration/1000.0));
        Clip c=current(); player.stop(); player.clearMediaItems();
        if(c!=null) { player.setVideoEffects(EditPipeline.effects(c,720)); player.setVolume(c.muted?0:1); player.setMediaItem(EditPipeline.media(c)); player.prepare(); status.setText(getString(R.string.clip_status,c.name,c.rotation,c.grayscale?"  · B&W":"")); }
        else status.setText(R.string.import_hint);
    }
    private interface Mutation { void apply(Clip c); }
    private void change(Mutation mutation) { if(!requireClip())return; checkpoint(); mutation.apply(current()); persist(); refresh(); }
    private void undoEdit() { if(undo.isEmpty())return; redo.push(snapshot()); clips.clear(); clips.addAll(undo.pop()); selected=clips.isEmpty()?-1:0; persist(); refresh(); }
    private void redoEdit() { if(redo.isEmpty())return; undo.push(snapshot()); clips.clear(); clips.addAll(redo.pop()); selected=clips.isEmpty()?-1:0; persist(); refresh(); }
    private void newProject() { new AlertDialog.Builder(this).setTitle("Start a new project?").setMessage("The current edit will be cleared. You can undo this until the app closes.").setNegativeButton("Cancel",null).setPositiveButton("New",(d,w)->{checkpoint();clips.clear();selected=-1;persist();refresh();}).show(); }
    private void move(int delta) { if(!requireClip())return; int target=selected+delta; if(target<0||target>=clips.size())return; checkpoint(); Collections.swap(clips,selected,target);selected=target;persist();refresh(); }
    private void duplicate() { if(!requireClip())return;checkpoint();clips.add(selected+1,current().copy());selected++;persist();refresh(); }
    private void delete() { if(!requireClip())return;checkpoint();clips.remove(selected);selected=Math.min(selected,clips.size()-1);persist();refresh(); }
    private EditText input(String value, boolean numeric) { EditText e=new EditText(this);e.setText(value);if(numeric)e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);return e; }
    private void trim() {
        if(!requireClip())return; Clip c=current(); LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(20),0,dp(20),0);
        EditText start=input(String.format(Locale.US,"%.3f",c.inMs/1000.0),true), end=input(String.format(Locale.US,"%.3f",c.outMs/1000.0),true);
        box.addView(label("Source start (seconds)",14,Color.WHITE));box.addView(start);box.addView(label("Source end (seconds)",14,Color.WHITE));box.addView(end);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Trim video").setView(box).setNegativeButton("Cancel",null).setPositiveButton("Apply",null).create();
        dialog.setOnShowListener(v->dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(b->{try {
            double a=Double.parseDouble(start.getText().toString()), z=Double.parseDouble(end.getText().toString());
            if(!Double.isFinite(a)||!Double.isFinite(z))throw new IllegalArgumentException("Enter valid seconds");
            Clip trial=c.copy();trial.trim(Math.round(a*1000),Math.round(z*1000));checkpoint();c.trim(trial.inMs,trial.outMs);persist();refresh();dialog.dismiss();
        }catch(Exception e){message(e.getMessage());}}));dialog.show();
    }
    private void split() {
        if(!requireClip())return; long position=player.getCurrentPosition(); Clip trial=current().copy();
        try { trial.split(position); checkpoint(); Clip right=current().split(position);clips.add(selected+1,right);persist();refresh(); } catch(Exception e){message(e.getMessage());}
    }
    private void text() { if(!requireClip())return; EditText field=input(current().text,false);field.setHint("Title · empty removes it");field.setMaxLines(4);
        new AlertDialog.Builder(this).setTitle("Centered text overlay").setView(field).setNegativeButton("Cancel",null).setPositiveButton("Apply",(d,w)->{if(field.length()>200){message("Use 200 characters or fewer");return;}change(c->c.text=field.getText().toString());}).show(); }
    private void pick() { Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("video/*");i.addCategory(Intent.CATEGORY_OPENABLE);i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);startActivityForResult(i,IMPORT); }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data); if(result!=RESULT_OK||data==null)return;
        if(request==IMPORT) {
            ArrayList<Uri> uris=new ArrayList<>(); if(data.getClipData()!=null)for(int n=0;n<data.getClipData().getItemCount();n++)uris.add(data.getClipData().getItemAt(n).getUri());else if(data.getData()!=null)uris.add(data.getData());
            for(Uri u:uris)try{getContentResolver().takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(SecurityException e){message("Provider did not grant lasting media access");}
            busy=true;status.setText(R.string.reading_metadata);
            worker.execute(()->{
                ArrayList<Clip> added=new ArrayList<>();ArrayList<String> errors=new ArrayList<>();
                for(Uri u:uris){ MediaMetadataRetriever r=new MediaMetadataRetriever(); try {
                    r.setDataSource(this,u);String video=r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO);if(!"yes".equals(video))throw new IOException("Not a supported video");
                    long duration=Long.parseLong(r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
                    String name="Video";try(Cursor cursor=getContentResolver().query(u,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){if(cursor!=null&&cursor.moveToFirst())name=cursor.getString(0);}
                    Clip c=new Clip(u.toString(),name,duration);c.trim(0,duration);added.add(c);
                }catch(Exception e){errors.add(e.getMessage()==null?"Could not read video":e.getMessage());} finally { try {r.release();}catch(Exception ignored){} } }
                handler.post(()->{if(destroyed)return;busy=false;if(!added.isEmpty()){checkpoint();clips.addAll(added);selected=clips.size()-added.size();persist();}refresh();if(!errors.isEmpty())message("Some files were skipped: "+String.join("; ",errors));});
            });
        }else if(request==SAVE_VIDEO&&data.getData()!=null&&exportFile!=null)copyExport(data.getData());
    }
    private void exportOptions() { if(!requireClip())return;new AlertDialog.Builder(this).setTitle("Export resolution").setItems(new String[]{"720p · H.264 MP4","1080p · H.264 MP4"},(d,w)->startExport(w==0?720:1080)).setNegativeButton("Cancel",null).show(); }
    private void startExport(int height) {
        busy=true;player.pause();exportFile=new File(getCacheDir(),"render-"+System.currentTimeMillis()+".mp4");
        progress=new ProgressDialog(this);progress.setTitle("Rendering video");progress.setMessage("Starting encoder…");progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);progress.setMax(100);progress.setCancelable(false);
        progress.setButton(DialogInterface.BUTTON_NEGATIVE,"Cancel",(d,w)->cancelExport());progress.show();
        transformer=new Transformer.Builder(this).setVideoMimeType(MimeTypes.VIDEO_H264).setAudioMimeType(MimeTypes.AUDIO_AAC).addListener(new Transformer.Listener(){
            @Override public void onCompleted(Composition composition, ExportResult result) {
                busy=false;progress.dismiss();transformer=null;
                saveLastRender();
            }
            @Override public void onError(Composition composition,ExportResult result,ExportException e){busy=false;progress.dismiss();transformer=null;exportFile.delete();message("Export failed: "+e.getMessage()+". Try 720p or another source file.");}
        }).build();
        try { transformer.start(EditPipeline.composition(snapshot(),height),exportFile.getAbsolutePath());handler.post(poll); }
        catch(Exception e){cancelExport();message("Cannot start export: "+e.getMessage());}
    }
    private final Runnable poll=new Runnable(){public void run(){if(transformer==null||destroyed)return;ProgressHolder p=new ProgressHolder();if(transformer.getProgress(p)==Transformer.PROGRESS_STATE_AVAILABLE){progress.setMessage("Encoding on your device…");progress.setProgress(p.progress);}handler.postDelayed(this,400);}};
    private void cancelExport(){if(transformer!=null){transformer.cancel();transformer=null;}if(exportFile!=null)exportFile.delete();if(progress!=null)progress.dismiss();busy=false;}
    private void saveLastRender(){if(exportFile==null||!exportFile.exists()){message("Export a video first");return;}Intent save=new Intent(Intent.ACTION_CREATE_DOCUMENT);save.setType("video/mp4");save.addCategory(Intent.CATEGORY_OPENABLE);save.putExtra(Intent.EXTRA_TITLE,"PRIYAN-CUT-"+System.currentTimeMillis()+".mp4");startActivityForResult(save,SAVE_VIDEO);}
    private void copyExport(Uri destination){busy=true;status.setText(R.string.saving_mp4);File source=exportFile;worker.execute(()->{
        String error=null;try(InputStream in=new FileInputStream(source);OutputStream out=getContentResolver().openOutputStream(destination,"w")){if(out==null)throw new IOException("Destination unavailable");byte[] bytes=new byte[65536];int n;while((n=in.read(bytes))!=-1)out.write(bytes,0,n);}
        catch(Exception e){error=e.getMessage();}final String problem=error;handler.post(()->{if(destroyed)return;busy=false;status.setText(problem==null?"✓ MP4 saved successfully":"Save failed. Render retained in app cache.");message(problem==null?"Video saved":problem);if(problem==null)source.delete();});
    });}
    @Override protected void onStop(){super.onStop();if(player!=null)player.pause();persist();}
    @Override protected void onDestroy(){destroyed=true;handler.removeCallbacksAndMessages(null);cancelExport();player.release();worker.shutdown();super.onDestroy();}
}
