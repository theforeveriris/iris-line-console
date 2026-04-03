package net.nhiroki.bluelineconsole.dataStore.persistent;

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

public class AliasPreferences {
    private static final String FOLDER_NAME = "lineconsole";
    private static final String FILE_NAME = "alias_preferences.json";
    private static AliasPreferences _singleton = null;
    private final Context context;
    private final Gson gson;
    private final Type listType;

    private AliasPreferences(Context context) {
        this.context = context.getApplicationContext();
        this.gson = new Gson();
        this.listType = new TypeToken<List<AliasEntry>>(){}.getType();
        ensureStorageDirectoryExists();
    }

    private void ensureStorageDirectoryExists() {
        File storageDir = new File(context.getExternalFilesDir(null), FOLDER_NAME);
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
    }

    public synchronized static AliasPreferences getInstance(Context context) {
        if (_singleton == null) {
            _singleton = new AliasPreferences(context.getApplicationContext());
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

    private List<AliasEntry> loadFromFile() {
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

    private void saveToFile(List<AliasEntry> entries) {
        File storageDir = new File(context.getExternalFilesDir(null), FOLDER_NAME);
        File file = new File(storageDir, FILE_NAME);
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(entries, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public long add(AliasEntry entry) {
        List<AliasEntry> entries = loadFromFile();
        
        int maxId = -1;
        for (AliasEntry e : entries) {
            if (e.id > maxId) {
                maxId = e.id;
            }
        }
        entry.id = maxId + 1;
        
        entries.add(entry);
        saveToFile(entries);
        return entry.id;
    }

    public List<AliasEntry> getAllEntries() {
        return loadFromFile();
    }

    public AliasEntry getEntryById(int id) {
        List<AliasEntry> entries = loadFromFile();
        for (AliasEntry entry : entries) {
            if (entry.id == id) {
                return entry;
            }
        }
        return null;
    }

    public AliasEntry getEntryByAlias(String alias) {
        List<AliasEntry> entries = loadFromFile();
        for (AliasEntry entry : entries) {
            if (entry.alias.equals(alias)) {
                return entry;
            }
        }
        return null;
    }

    public List<AliasEntry> getEntriesByPackageName(String packageName) {
        List<AliasEntry> entries = loadFromFile();
        List<AliasEntry> result = new ArrayList<>();
        for (AliasEntry entry : entries) {
            if (entry.packageName.equals(packageName)) {
                result.add(entry);
            }
        }
        return result;
    }

    public boolean aliasExists(String alias) {
        return getEntryByAlias(alias) != null;
    }

    public void deleteById(int id) {
        List<AliasEntry> entries = loadFromFile();
        entries.removeIf(entry -> entry.id == id);
        saveToFile(entries);
    }

    public void update(AliasEntry entry) {
        List<AliasEntry> entries = loadFromFile();
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).id == entry.id) {
                entries.set(i, entry);
                saveToFile(entries);
                return;
            }
        }
    }
}
