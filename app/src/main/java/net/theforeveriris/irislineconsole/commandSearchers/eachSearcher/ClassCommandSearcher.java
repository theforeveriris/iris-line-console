package net.theforeveriris.irislineconsole.commandSearchers.eachSearcher;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import net.theforeveriris.irislineconsole.R;
import net.theforeveriris.irislineconsole.applicationMain.MainActivity;
import net.theforeveriris.irislineconsole.applicationMain.PreferencesClassesActivity;
import net.theforeveriris.irislineconsole.commands.applications.ApplicationDatabase;
import net.theforeveriris.irislineconsole.commandSearchers.lib.StringMatchStrategy;
import net.theforeveriris.irislineconsole.dataStore.cache.ApplicationInformation;
import net.theforeveriris.irislineconsole.dataStore.persistent.ClassEntry;
import net.theforeveriris.irislineconsole.dataStore.persistent.ClassPreferences;
import net.theforeveriris.irislineconsole.interfaces.CandidateEntry;
import net.theforeveriris.irislineconsole.interfaces.CommandSearcher;
import net.theforeveriris.irislineconsole.interfaces.EventLauncher;

import android.util.Pair;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClassCommandSearcher implements CommandSearcher {
    private static final String CONFIG_COMMAND_STRING = "config";
    private static final String CLASS_COMMAND_STRING = "class";
    private ApplicationDatabase applicationDatabase;

    @Override
    public void refresh(Context context) {
        this.applicationDatabase = new ApplicationDatabase(context);
    }

    @Override
    public void close() {
        if (this.applicationDatabase != null) {
            this.applicationDatabase.close();
        }
    }

    @Override
    public boolean isPrepared() {
        return this.applicationDatabase.isPrepared();
    }

    @Override
    public void waitUntilPrepared() {
        this.applicationDatabase.waitUntilPrepared();
    }

    @Override
    @NonNull
    public List<CandidateEntry> searchCandidateEntries(String query, Context context) {
        List<CandidateEntry> candidates = new ArrayList<>();

        // 1. Check for "config" command - add class configuration entry
        if (query.equalsIgnoreCase(CONFIG_COMMAND_STRING)) {
            candidates.add(new ClassConfigCandidateEntry());
        }

        // 2. Check for "class" command - show all classes
        if (query.equalsIgnoreCase(CLASS_COMMAND_STRING)) {
            List<ClassEntry> allClasses = ClassPreferences.getInstance(context).getAllEntries();
            for (ClassEntry classEntry : allClasses) {
                candidates.add(new ClassListCandidateEntry(classEntry));
            }
        }

        // 3. Check for "c_<class name>" - show apps in the class
        if (query.toLowerCase().startsWith("c_") && query.length() > 2) {
            String className = query.substring(2);
            ClassEntry classEntry = ClassPreferences.getInstance(context).getEntryByName(className);
            
            if (classEntry != null) {
                List<Pair<Integer, CandidateEntry>> appCandidates = new ArrayList<>();
                
                for (ApplicationInformation appInfo : applicationDatabase.getApplicationInformationList()) {
                    if (classEntry.appPackageNames.contains(appInfo.getPackageName())) {
                        final ApplicationInfo androidAppInfo = applicationDatabase.getAndroidApplicationInfo(appInfo.getPackageName());
                        final String appLabel = appInfo.getLabel();
                        appCandidates.add(new Pair<>(0, new ClassAppOpenCandidateEntry(context, appInfo, androidAppInfo, appLabel)));
                    }
                }
                
                Collections.sort(appCandidates, (o1, o2) -> o1.first.compareTo(o2.first));
                
                for (Pair<Integer, CandidateEntry> entry : appCandidates) {
                    candidates.add(entry.second);
                }
            }
        }

        return candidates;
    }

    private static class ClassConfigCandidateEntry implements CandidateEntry {
        @Override
        @NonNull
        public String getTitle() {
            return "class";
        }

        @Override
        public View getView(MainActivity mainActivity) {
            TextView packageNameView = new TextView(mainActivity);
            packageNameView.setText(mainActivity.getString(R.string.result_class_summary));
            packageNameView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return packageNameView;
        }

        @Override
        public boolean hasLongView() {
            return false;
        }

        @Override
        public EventLauncher getEventLauncher(Context context) {
            return activity -> activity.startActivityForResult(new Intent(activity, PreferencesClassesActivity.class), MainActivity.REQUEST_CODE_FOR_COMING_BACK);
        }

        @Override
        public Drawable getIcon(Context context) {
            return null;
        }

        @Override
        public boolean hasEvent() {
            return true;
        }

        @Override
        public boolean isSubItem() {
            return false;
        }

        @Override
        public boolean viewIsRecyclable() {
            return true;
        }
    }

    private static class ClassListCandidateEntry implements CandidateEntry {
        private final ClassEntry classEntry;

        ClassListCandidateEntry(ClassEntry classEntry) {
            this.classEntry = classEntry;
        }

        @Override
        @NonNull
        public String getTitle() {
            return classEntry.display_name;
        }

        @Override
        public View getView(MainActivity mainActivity) {
            TextView packageNameView = new TextView(mainActivity);
            packageNameView.setText(classEntry.name + " (" + classEntry.appPackageNames.size() + " apps)");
            packageNameView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return packageNameView;
        }

        @Override
        public boolean hasLongView() {
            return false;
        }

        @Override
        public EventLauncher getEventLauncher(Context context) {
            return null;
        }

        @Override
        public Drawable getIcon(Context context) {
            return null;
        }

        @Override
        public boolean hasEvent() {
            return false;
        }

        @Override
        public boolean isSubItem() {
            return false;
        }

        @Override
        public boolean viewIsRecyclable() {
            return true;
        }
    }

    private static class ClassAppOpenCandidateEntry implements CandidateEntry {
        private final ApplicationInformation applicationInformation;
        private final ApplicationInfo androidApplicationInfo;
        private final String title;
        private final boolean displayPackageName;

        ClassAppOpenCandidateEntry(Context context, ApplicationInformation applicationInformation, ApplicationInfo androidApplicationInfo, String appTitle) {
            this.applicationInformation = applicationInformation;
            this.androidApplicationInfo = androidApplicationInfo;
            this.title = appTitle;
            this.displayPackageName = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("pref_apps_show_package_name", false);
        }

        @Override
        @NonNull
        public String getTitle() {
            return title;
        }

        @Override
        public View getView(MainActivity mainActivity) {
            if(!displayPackageName) {
                return null;
            }

            String packageName = ClassAppOpenCandidateEntry.this.applicationInformation.getPackageName();
            TextView packageNameView = new TextView(mainActivity);
            packageNameView.setText(packageName);
            packageNameView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            return packageNameView;
        }

        @Override
        public boolean hasEvent() {
            return true;
        }

        @Override
        public EventLauncher getEventLauncher(final Context context) {
            return activity -> {
                String packageName = ClassAppOpenCandidateEntry.this.applicationInformation.getPackageName();
                Intent intent = activity.getPackageManager().getLaunchIntentForPackage(ClassAppOpenCandidateEntry.this.applicationInformation.getPackageName());
                if (packageName.equals(context.getPackageName())) {
                    activity.finishIfNotHome();
                    activity.startActivity(new Intent(activity, MainActivity.class));
                    return;
                }
                if (intent == null) {
                    Toast.makeText(activity, String.format(activity.getString(R.string.error_failure_not_found_opening_application_with_class), packageName), Toast.LENGTH_LONG).show();
                    return;
                }
                activity.startActivity(intent);
                activity.finishIfNotHome();
            };
        }

        @Override
        public boolean hasLongView() {
            return false;
        }

        @Override
        public Drawable getIcon(Context context) {
            return context.getPackageManager().getApplicationIcon(androidApplicationInfo);
        }

        @Override
        public boolean isSubItem() {
            return false;
        }

        @Override
        public boolean viewIsRecyclable() {
            return true;
        }
    }
}
