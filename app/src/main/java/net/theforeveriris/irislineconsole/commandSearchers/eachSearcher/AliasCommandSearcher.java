package net.theforeveriris.irislineconsole.commandSearchers.eachSearcher;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
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
import net.theforeveriris.irislineconsole.applicationMain.PreferencesAliasesActivity;
import net.theforeveriris.irislineconsole.commands.applications.ApplicationDatabase;
import net.theforeveriris.irislineconsole.dataStore.cache.ApplicationInformation;
import net.theforeveriris.irislineconsole.dataStore.persistent.AliasEntry;
import net.theforeveriris.irislineconsole.dataStore.persistent.AliasPreferences;
import net.theforeveriris.irislineconsole.interfaces.CandidateEntry;
import net.theforeveriris.irislineconsole.interfaces.CommandSearcher;
import net.theforeveriris.irislineconsole.interfaces.EventLauncher;

import java.util.ArrayList;
import java.util.List;

public class AliasCommandSearcher implements CommandSearcher {
    private static final String CONFIG_COMMAND_STRING = "config";
    private static final String ALIAS_COMMAND_STRING = "alias";
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

        // 1. Check for "config" command - add alias configuration entry
        if (query.equalsIgnoreCase(CONFIG_COMMAND_STRING)) {
            candidates.add(new AliasConfigCandidateEntry());
        }

        // 2. Check for "alias" command - show all aliases and config
        if (query.equalsIgnoreCase(ALIAS_COMMAND_STRING)) {
            candidates.add(new AliasConfigCandidateEntry());
            List<AliasEntry> allAliases = AliasPreferences.getInstance(context).getAllEntries();
            for (AliasEntry aliasEntry : allAliases) {
                candidates.add(new AliasListCandidateEntry(aliasEntry, getAppLabel(aliasEntry.packageName)));
            }
        }

        // 3. Check if query matches any alias - launch the corresponding app
        AliasEntry matchedAlias = AliasPreferences.getInstance(context).getEntryByAlias(query);
        if (matchedAlias != null) {
            ApplicationInformation appInfo = findApplicationByPackageName(matchedAlias.packageName);
            if (appInfo != null) {
                ApplicationInfo androidAppInfo = applicationDatabase.getAndroidApplicationInfo(appInfo.getPackageName());
                String appLabel = appInfo.getLabel();
                candidates.add(new AliasAppOpenCandidateEntry(context, appInfo, androidAppInfo, appLabel, matchedAlias.alias));
            }
        }

        return candidates;
    }

    private ApplicationInformation findApplicationByPackageName(String packageName) {
        for (ApplicationInformation appInfo : applicationDatabase.getApplicationInformationList()) {
            if (appInfo.getPackageName().equals(packageName)) {
                return appInfo;
            }
        }
        return null;
    }

    private String getAppLabel(String packageName) {
        for (ApplicationInformation appInfo : applicationDatabase.getApplicationInformationList()) {
            if (appInfo.getPackageName().equals(packageName)) {
                return appInfo.getLabel();
            }
        }
        return packageName;
    }

    private static class AliasConfigCandidateEntry implements CandidateEntry {
        @Override
        @NonNull
        public String getTitle() {
            return "alias";
        }

        @Override
        public View getView(MainActivity mainActivity) {
            TextView packageNameView = new TextView(mainActivity);
            packageNameView.setText("应用别名管理");
            packageNameView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return packageNameView;
        }

        @Override
        public boolean hasLongView() {
            return false;
        }

        @Override
        public EventLauncher getEventLauncher(Context context) {
            return activity -> activity.startActivityForResult(new Intent(activity, PreferencesAliasesActivity.class), MainActivity.REQUEST_CODE_FOR_COMING_BACK);
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

    private static class AliasListCandidateEntry implements CandidateEntry {
        private final AliasEntry aliasEntry;
        private final String appLabel;

        AliasListCandidateEntry(AliasEntry aliasEntry, String appLabel) {
            this.aliasEntry = aliasEntry;
            this.appLabel = appLabel;
        }

        @Override
        @NonNull
        public String getTitle() {
            return aliasEntry.alias;
        }

        @Override
        public View getView(MainActivity mainActivity) {
            TextView packageNameView = new TextView(mainActivity);
            packageNameView.setText(appLabel + " (" + aliasEntry.packageName + ")");
            packageNameView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return packageNameView;
        }

        @Override
        public boolean hasLongView() {
            return false;
        }

        @Override
        public EventLauncher getEventLauncher(Context context) {
            return activity -> {
                if (activity instanceof MainActivity) {
                    ((MainActivity) activity).changeInputText(aliasEntry.alias);
                }
            };
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

    private static class AliasAppOpenCandidateEntry implements CandidateEntry {
        private final ApplicationInformation applicationInformation;
        private final ApplicationInfo androidApplicationInfo;
        private final String title;
        private final String alias;
        private final boolean displayPackageName;

        AliasAppOpenCandidateEntry(Context context, ApplicationInformation applicationInformation, ApplicationInfo androidApplicationInfo, String appTitle, String alias) {
            this.applicationInformation = applicationInformation;
            this.androidApplicationInfo = androidApplicationInfo;
            this.title = appTitle;
            this.alias = alias;
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

            String packageName = AliasAppOpenCandidateEntry.this.applicationInformation.getPackageName();
            TextView packageNameView = new TextView(mainActivity);
            packageNameView.setText(packageName + " (别名: " + alias + ")");
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
                String packageName = AliasAppOpenCandidateEntry.this.applicationInformation.getPackageName();
                Intent intent = activity.getPackageManager().getLaunchIntentForPackage(AliasAppOpenCandidateEntry.this.applicationInformation.getPackageName());
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
