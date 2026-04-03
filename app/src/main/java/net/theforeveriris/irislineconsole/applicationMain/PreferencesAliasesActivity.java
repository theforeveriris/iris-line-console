package net.theforeveriris.irislineconsole.applicationMain;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.theforeveriris.irislineconsole.R;
import net.theforeveriris.irislineconsole.dataStore.persistent.AliasEntry;
import net.theforeveriris.irislineconsole.dataStore.persistent.AliasPreferences;

import java.util.ArrayList;
import java.util.List;

public class PreferencesAliasesActivity extends BaseWindowActivity {
    private AliasListAdapter _aliasListAdapter;

    public PreferencesAliasesActivity() {
        super(R.layout.preferences_aliases_body, false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setHeaderFooterTexts("应用别名管理", null);
        this.setWindowBoundarySize(ROOT_WINDOW_FULL_WIDTH_IN_MOBILE, 2);

        this.changeBaseWindowElementSizeForAnimation(false);
        this.enableBaseWindowAnimation();

        Button addButton = findViewById(R.id.aliasListAddButton);
        addButton.setOnClickListener(v -> PreferencesAliasesActivity.this.startActivity(new Intent(PreferencesAliasesActivity.this, PreferencesEachAliasActivity.class))
        );

        this._aliasListAdapter = new AliasListAdapter(this, 0, new ArrayList<>());

        ListView aliasListView = findViewById(R.id.aliasList);
        aliasListView.setAdapter(this._aliasListAdapter);
        aliasListView.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(PreferencesAliasesActivity.this, PreferencesEachAliasActivity.class);
            intent.putExtra("alias_id", PreferencesAliasesActivity.this._aliasListAdapter.getItem(position).id);
            PreferencesAliasesActivity.this.startActivity(intent);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        this._aliasListAdapter.clear();
        this._aliasListAdapter.addAll(AliasPreferences.getInstance(this).getAllEntries());
        this._aliasListAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onStop() {
        super.onStop();
        this.finish();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        this.changeBaseWindowElementSizeForAnimation(true);
    }

    private static class AliasListAdapter extends ArrayAdapter<AliasEntry> {
        public AliasListAdapter(@NonNull Context context, int resource, @NonNull List<AliasEntry> objects) {
            super(context, resource, objects);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(this.getContext()).inflate(R.layout.alias_entry_view, parent, false);
            }
            ((TextView)convertView.findViewById(R.id.aliasNameOnEntryView)).setText(this.getItem(position).alias);
            ((TextView)convertView.findViewById(R.id.aliasPackageNameOnEntryView)).setText(this.getItem(position).packageName);

            return convertView;
        }
    }
}
