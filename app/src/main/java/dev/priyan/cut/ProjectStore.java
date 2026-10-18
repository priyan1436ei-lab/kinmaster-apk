package dev.priyan.cut;

import android.content.Context;
import android.util.AtomicFile;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

final class ProjectStore {
    private final AtomicFile file;
    ProjectStore(Context context) { file = new AtomicFile(new File(context.getFilesDir(), "project.json")); }
    void save(List<Clip> clips) throws Exception {
        JSONArray a = new JSONArray();
        for (Clip c : clips) a.put(new JSONObject().put("uri", c.uri).put("name", c.name).put("duration", c.durationMs)
            .put("in", c.inMs).put("out", c.outMs).put("rotation", c.rotation).put("muted", c.muted).put("gray", c.grayscale).put("text", c.text));
        byte[] bytes = new JSONObject().put("schema", 1).put("clips", a).toString(2).getBytes(StandardCharsets.UTF_8);
        FileOutputStream stream = file.startWrite();
        try { stream.write(bytes); file.finishWrite(stream); } catch (Exception e) { file.failWrite(stream); throw e; }
    }
    ArrayList<Clip> load() throws Exception {
        ArrayList<Clip> clips = new ArrayList<>();
        if (!file.getBaseFile().exists()) return clips;
        try (InputStream stream = file.openRead(); ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192]; int n; while ((n = stream.read(buffer)) != -1) bytes.write(buffer,0,n);
            JSONObject root = new JSONObject(bytes.toString("UTF-8"));
            if (root.getInt("schema") != 1) throw new IOException("Unsupported project version");
            JSONArray a = root.getJSONArray("clips");
            for (int i=0; i<a.length(); i++) {
                JSONObject o=a.getJSONObject(i); Clip c=new Clip(o.getString("uri"),o.getString("name"),o.getLong("duration"));
                c.trim(o.getLong("in"),o.getLong("out")); c.rotation=o.getInt("rotation"); c.muted=o.getBoolean("muted"); c.grayscale=o.getBoolean("gray"); c.text=o.getString("text"); clips.add(c);
            }
        }
        return clips;
    }
}
