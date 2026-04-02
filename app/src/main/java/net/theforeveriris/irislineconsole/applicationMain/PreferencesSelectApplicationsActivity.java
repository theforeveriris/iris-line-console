package net.theforeveriris.irislineconsole.applicationMain;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.theforeveriris.irislineconsole.R;
import net.theforeveriris.irislineconsole.commands.applications.ApplicationDatabase;
import net.theforeveriris.irislineconsole.dataStore.cache.ApplicationInformation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PreferencesSelectApplicationsActivity extends BaseWindowActivity {
    private static final String EXTRA_SELECTED_PACKAGES = "selected_packages";
    public static final String RESULT_SELECTED_PACKAGES = "result_selected_packages";
    
    private ApplicationListAdapter _applicationListAdapter;
    private ApplicationDatabase _applicationDatabase;
    private Set<String> _selectedPackageNames;
    private final Object _lock = new Object();
    private List<ApplicationInfoWrapper> _allApplications;
    private EditText _searchEditText;
    private TextView _noResultsTextView;

    public PreferencesSelectApplicationsActivity() {
        super(R.layout.preferences_select_applications_body, false);
    }

    public static Intent createIntent(Context context, List<String> selectedPackages) {
        Intent intent = new Intent(context, PreferencesSelectApplicationsActivity.class);
        intent.putStringArrayListExtra(EXTRA_SELECTED_PACKAGES, new ArrayList<>(selectedPackages));
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setHeaderFooterTexts(getString(R.string.preferences_classes_select_applications), null);
        this.setWindowBoundarySize(ROOT_WINDOW_FULL_WIDTH_IN_MOBILE, 2);

        this.changeBaseWindowElementSizeForAnimation(false);
        this.enableBaseWindowAnimation();

        Intent fromIntent = getIntent();
        ArrayList<String> initialSelectedPackages = fromIntent.getStringArrayListExtra(EXTRA_SELECTED_PACKAGES);
        _selectedPackageNames = new HashSet<>(initialSelectedPackages != null ? initialSelectedPackages : new ArrayList<>());
        _allApplications = new ArrayList<>();

        this._applicationListAdapter = new ApplicationListAdapter(this, 0, new ArrayList<>());
        ListView applicationListView = findViewById(R.id.applicationList);
        applicationListView.setAdapter(this._applicationListAdapter);
        applicationListView.setOnItemClickListener((parent, view, position, id) -> {
            ApplicationInfoWrapper wrapper = _applicationListAdapter.getItem(position);
            if (wrapper != null) {
                synchronized (_lock) {
                    if (_selectedPackageNames.contains(wrapper.packageName)) {
                        _selectedPackageNames.remove(wrapper.packageName);
                    } else {
                        _selectedPackageNames.add(wrapper.packageName);
                    }
                }
                _applicationListAdapter.notifyDataSetChanged();
            }
        });

        Button doneButton = findViewById(R.id.applicationsDoneButton);
        doneButton.setOnClickListener(v -> {
            Intent resultIntent = new Intent();
            ArrayList<String> selectedPackages = new ArrayList<>(_selectedPackageNames);
            resultIntent.putStringArrayListExtra(RESULT_SELECTED_PACKAGES, selectedPackages);
            
            // Debug: 显示选择的应用数量
            Toast.makeText(PreferencesSelectApplicationsActivity.this, 
                "已选择 " + selectedPackages.size() + " 个应用", 
                Toast.LENGTH_SHORT).show();
                
            setResult(RESULT_OK, resultIntent);
            finish();
        });

        // 初始化搜索相关组件
        _searchEditText = findViewById(R.id.applicationSearchEditText);
        _noResultsTextView = findViewById(R.id.noResultsTextView);
        
        _searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                filterApplications(s.toString());
            }
        });

        _applicationDatabase = new ApplicationDatabase(this);
    }

    private void filterApplications(String query) {
        List<ApplicationInfoWrapper> filteredList = new ArrayList<>();
        String lowerQuery = query.toLowerCase();

        if (query.isEmpty()) {
            filteredList.addAll(_allApplications);
        } else {
            for (ApplicationInfoWrapper wrapper : _allApplications) {
                if (wrapper.label.toLowerCase().contains(lowerQuery) || 
                    wrapper.packageName.toLowerCase().contains(lowerQuery)) {
                    filteredList.add(wrapper);
                }
            }
        }

        _applicationListAdapter.clear();
        _applicationListAdapter.addAll(filteredList);
        _applicationListAdapter.notifyDataSetChanged();

        // 显示或隐藏无结果提示
        if (filteredList.isEmpty() && !query.isEmpty()) {
            _noResultsTextView.setVisibility(View.VISIBLE);
        } else {
            _noResultsTextView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        
        new Thread(() -> {
            _applicationDatabase.waitUntilPrepared();
            List<ApplicationInfoWrapper> wrappers = new ArrayList<>();
            
            for (ApplicationInformation appInfo : _applicationDatabase.getApplicationInformationList()) {
                ApplicationInfo androidAppInfo = _applicationDatabase.getAndroidApplicationInfo(appInfo.getPackageName());
                if (androidAppInfo != null) {
                    ApplicationInfoWrapper wrapper = new ApplicationInfoWrapper();
                    wrapper.packageName = appInfo.getPackageName();
                    wrapper.label = appInfo.getLabel();
                    wrapper.androidAppInfo = androidAppInfo;
                    wrappers.add(wrapper);
                }
            }
            
            Collections.sort(wrappers, (o1, o2) -> o1.label.compareToIgnoreCase(o2.label));
            
            runOnUiThread(() -> {
                _allApplications.clear();
                _allApplications.addAll(wrappers);
                filterApplications(_searchEditText.getText().toString());
            });
        }).start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (_applicationDatabase != null) {
            _applicationDatabase.close();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        this.changeBaseWindowElementSizeForAnimation(true);
    }

    private static class ApplicationInfoWrapper {
        String packageName;
        String label;
        ApplicationInfo androidAppInfo;
    }

    private class ApplicationListAdapter extends ArrayAdapter<ApplicationInfoWrapper> {
        public ApplicationListAdapter(@NonNull Context context, int resource, @NonNull List<ApplicationInfoWrapper> objects) {
            super(context, resource, objects);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(this.getContext()).inflate(R.layout.application_entry_view, parent, false);
            }
            
            ApplicationInfoWrapper wrapper = this.getItem(position);
            if (wrapper != null) {
                CheckBox checkBox = convertView.findViewById(R.id.applicationSelectedCheckBox);
                ImageView iconView = convertView.findViewById(R.id.applicationIconImageView);
                TextView labelView = convertView.findViewById(R.id.applicationLabelTextView);
                TextView packageNameView = convertView.findViewById(R.id.applicationPackageNameTextView);
                
                synchronized (_lock) {
                    checkBox.setChecked(_selectedPackageNames.contains(wrapper.packageName));
                }
                
                labelView.setText(wrapper.label);
                packageNameView.setText(wrapper.packageName);
                
                try {
                    Drawable icon = getContext().getPackageManager().getApplicationIcon(wrapper.androidAppInfo);
                    iconView.setImageDrawable(icon);
                } catch (Exception e) {
                    iconView.setImageDrawable(null);
                }
            }
            
            return convertView;
        }
    }
}
