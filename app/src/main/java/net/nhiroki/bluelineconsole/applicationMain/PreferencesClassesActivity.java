package net.nhiroki.bluelineconsole.applicationMain;

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
import net.nhiroki.bluelineconsole.dataStore.persistent.ClassEntry;
import net.nhiroki.bluelineconsole.dataStore.persistent.ClassPreferences;

import java.util.ArrayList;
import java.util.List;

public class PreferencesClassesActivity extends BaseWindowActivity {
    private ClassListAdapter _classListAdapter;

    public PreferencesClassesActivity() {
        super(R.layout.preferences_classes_body, false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setHeaderFooterTexts(getString(R.string.preferences_title_for_header_and_footer_classes), null);
        this.setWindowBoundarySize(ROOT_WINDOW_FULL_WIDTH_IN_MOBILE, 2);

        this.changeBaseWindowElementSizeForAnimation(false);
        this.enableBaseWindowAnimation();

        Button addButton = findViewById(R.id.classListAddButton);
        addButton.setOnClickListener(v -> PreferencesClassesActivity.this.startActivity(new Intent(PreferencesClassesActivity.this, PreferencesEachClassActivity.class))
        );

        this._classListAdapter = new ClassListAdapter(this, 0, new ArrayList<>());

        ListView classListView = findViewById(R.id.classList);
        classListView.setAdapter(this._classListAdapter);
        classListView.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(PreferencesClassesActivity.this, PreferencesEachClassActivity.class);
            intent.putExtra("class_id", PreferencesClassesActivity.this._classListAdapter.getItem(position).id);
            PreferencesClassesActivity.this.startActivity(intent);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        this._classListAdapter.clear();
        this._classListAdapter.addAll(ClassPreferences.getInstance(this).getAllEntries());
        this._classListAdapter.notifyDataSetChanged();
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

    private static class ClassListAdapter extends ArrayAdapter<ClassEntry> {
        public ClassListAdapter(@NonNull Context context, int resource, @NonNull List<ClassEntry> objects) {
            super(context, resource, objects);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(this.getContext()).inflate(R.layout.class_entry_view, parent, false);
            }
            ((TextView)convertView.findViewById(R.id.classNameOnEntryView)).setText(this.getItem(position).name);
            ((TextView)convertView.findViewById(R.id.classDisplayNameOnEntryView)).setText(this.getItem(position).display_name);

            return convertView;
        }
    }
}
