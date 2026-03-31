package net.theforeveriris.irislineconsole.dataStore.persistent;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ClassPreferences {
    private static final String FOLDER_NAME = "lineconsole";
    private static final String FILE_NAME = "class_preferences.json";
    private static ClassPreferences _singleton = null;
    private final Context context;
    private final Gson gson;
    private final Type listType;

    private ClassPreferences(Context context) {
        this.context = context.getApplicationContext();
        this.gson = new Gson();
        this.listType = new TypeToken<List<ClassEntry>>(){}.getType();
        // 确保存储目录存在
        ensureStorageDirectoryExists();
    }

    private void ensureStorageDirectoryExists() {
        File storageDir = new File(context.getExternalFilesDir(null), FOLDER_NAME);
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
    }

    public synchronized static ClassPreferences getInstance(Context context) {
        if (_singleton == null) {
            _singleton = new ClassPreferences(context.getApplicationContext());
        }
        return _singleton;
    }

    public static void destroyFilesForCleanTest(Context context) {
        if (_singleton != null) {
            _singleton = null;
        }
        File storageDir = new File(context.getApplicationContext().getExternalFilesDir(null), FOLDER_NAME);
        File file = new File(storageDir, FILE_NAME);
        file.delete();
    }

    public void close() {
        _singleton = null;
    }

    private List<ClassEntry> loadFromFile() {
        File storageDir = new File(context.getExternalFilesDir(null), FOLDER_NAME);
        File file = new File(storageDir, FILE_NAME);
        if (!file.exists()) {
            return new ArrayList<>();
        }

        try (FileReader reader = new FileReader(file)) {
            return gson.fromJson(reader, listType);
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private void saveToFile(List<ClassEntry> entries) {
        File storageDir = new File(context.getExternalFilesDir(null), FOLDER_NAME);
        File file = new File(storageDir, FILE_NAME);
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(entries, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public long add(ClassEntry entry) {
        List<ClassEntry> entries = loadFromFile();
        
        // Generate new ID
        int maxId = -1;
        for (ClassEntry e : entries) {
            if (e.id > maxId) {
                maxId = e.id;
            }
        }
        entry.id = maxId + 1;
        
        entries.add(entry);
        saveToFile(entries);
        return entry.id;
    }

    public List<ClassEntry> getAllEntries() {
        return loadFromFile();
    }

    public ClassEntry getEntryById(int id) {
        List<ClassEntry> entries = loadFromFile();
        for (ClassEntry entry : entries) {
            if (entry.id == id) {
                return entry;
            }
        }
        return null;
    }

    public ClassEntry getEntryByName(String name) {
        List<ClassEntry> entries = loadFromFile();
        for (ClassEntry entry : entries) {
            if (entry.name.equals(name)) {
                return entry;
            }
        }
        return null;
    }

    public void deleteById(int id) {
        List<ClassEntry> entries = loadFromFile();
        entries.removeIf(entry -> entry.id == id);
        saveToFile(entries);
    }

    public void update(ClassEntry entry) {
        List<ClassEntry> entries = loadFromFile();
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).id == entry.id) {
                entries.set(i, entry);
                saveToFile(entries);
                return;
            }
        }
    }
}
