package net.theforeveriris.irislineconsole.applicationMain;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import net.theforeveriris.irislineconsole.R;
import net.theforeveriris.irislineconsole.applicationMain.lib.EditTextConfigurations;
import net.theforeveriris.irislineconsole.dataStore.persistent.ClassEntry;
import net.theforeveriris.irislineconsole.dataStore.persistent.ClassPreferences;

import java.util.ArrayList;
import java.util.List;

public class PreferencesEachClassActivity extends BaseWindowActivity {
    private static final int REQUEST_CODE_SELECT_APPLICATIONS = 1001;
    private static final String EXTRA_SELECTED_APP_PACKAGES = "selected_app_packages";
    private static final String EXTRA_CLASS_NAME = "class_name";
    private static final String EXTRA_DISPLAY_NAME = "display_name";

    public PreferencesEachClassActivity() {
        super(R.layout.preferences_classes_each_body, true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setWindowBoundarySize(ROOT_WINDOW_FULL_WIDTH_IN_MOBILE, 3);

        this.changeBaseWindowElementSizeForAnimation(false);
        this.enableBaseWindowAnimation();

        findViewById(R.id.class_each_submit_button).setOnClickListener(v -> {
            saveClass();
        });

        findViewById(R.id.class_each_delete_button).setOnClickListener(v -> {
            Intent from_intent = this.getIntent();
            Integer classId = null;
            if (from_intent.hasExtra("class_id")) {
                classId = from_intent.getIntExtra("class_id", -1);
            }
            
            if (classId != null) {
                ClassPreferences.getInstance(PreferencesEachClassActivity.this).deleteById(classId);
                PreferencesEachClassActivity.this.finish();
            }
        });

        findViewById(R.id.class_each_select_applications).setOnClickListener(v -> {
            Intent intent = getIntent();
            intent.putExtra(EXTRA_CLASS_NAME, ((EditText)findViewById(R.id.class_each_name)).getText().toString());
            intent.putExtra(EXTRA_DISPLAY_NAME, ((EditText)findViewById(R.id.class_each_display_name)).getText().toString());
            setIntent(intent);
            
            List<String> selectedAppPackages = getCurrentSelectedAppPackages();
            Intent selectIntent = PreferencesSelectApplicationsActivity.createIntent(PreferencesEachClassActivity.this, selectedAppPackages);
            startActivityForResult(selectIntent, REQUEST_CODE_SELECT_APPLICATIONS);
        });
    }

    private List<String> getCurrentSelectedAppPackages() {
        Intent from_intent = this.getIntent();
        if (from_intent.hasExtra(EXTRA_SELECTED_APP_PACKAGES)) {
            return from_intent.getStringArrayListExtra(EXTRA_SELECTED_APP_PACKAGES);
        }
        return new ArrayList<>();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SELECT_APPLICATIONS && resultCode == RESULT_OK && data != null) {
            ArrayList<String> selectedPackages = data.getStringArrayListExtra(PreferencesSelectApplicationsActivity.RESULT_SELECTED_PACKAGES);
            if (selectedPackages != null) {
                Intent intent = getIntent();
                intent.putStringArrayListExtra(EXTRA_SELECTED_APP_PACKAGES, selectedPackages);
                setIntent(intent);
                
                // Debug: 显示收到的应用数量
                Toast.makeText(PreferencesEachClassActivity.this, 
                    "收到 " + selectedPackages.size() + " 个应用", 
                    Toast.LENGTH_SHORT).show();
                
                // Re-initialize UI to reflect the new state
                initializeUI();
            }
        }
    }

    private void saveClass() {
        Intent from_intent = this.getIntent();
        Integer classId = null;
        if (from_intent.hasExtra("class_id")) {
            classId = from_intent.getIntExtra("class_id", -1);
        }
        
        ClassEntry entry = new ClassEntry();
        if (classId != null) {
            entry.id = classId;
        }
        entry.name = ((EditText)findViewById(R.id.class_each_name)).getText().toString();
        entry.display_name = ((EditText)findViewById(R.id.class_each_display_name)).getText().toString();
        List<String> selectedApps = getCurrentSelectedAppPackages();
        entry.appPackageNames = new ArrayList<>(selectedApps);
        
        // Debug: 显示保存的应用数量
        Toast.makeText(PreferencesEachClassActivity.this, 
            "保存 " + entry.name + "，包含 " + selectedApps.size() + " 个应用", 
            Toast.LENGTH_LONG).show();

        if (entry.name.isEmpty()) {
            Toast.makeText(PreferencesEachClassActivity.this, R.string.error_invalid_command_name, Toast.LENGTH_LONG).show();
            return;
        }
        if (entry.display_name.isEmpty()) {
            Toast.makeText(PreferencesEachClassActivity.this, R.string.error_empty_display_name, Toast.LENGTH_LONG).show();
            return;
        }

        if (classId == null) {
            ClassPreferences.getInstance(PreferencesEachClassActivity.this).add(entry);
        } else {
            ClassPreferences.getInstance(PreferencesEachClassActivity.this).update(entry);
        }
        PreferencesEachClassActivity.this.finish();
    }

    @Override
    public void onResume() {
        super.onResume();
        initializeUI();
    }

    private void initializeUI() {
        Intent from_intent = this.getIntent();
        Integer classId = null;
        if (from_intent.hasExtra("class_id")) {
            classId = from_intent.getIntExtra("class_id", -1);
        }

        if (classId == null) {
            // new
            this.setHeaderFooterTexts(this.getString(R.string.preferences_title_for_header_and_footer_add_class), null);
            ((Button)findViewById(R.id.class_each_submit_button)).setText(R.string.button_add);
            findViewById(R.id.class_each_delete_button).setVisibility(View.GONE);

            if (from_intent.hasExtra(EXTRA_CLASS_NAME)) {
                ((EditText)findViewById(R.id.class_each_name)).setText(from_intent.getStringExtra(EXTRA_CLASS_NAME));
            } else {
                ((EditText)findViewById(R.id.class_each_name)).setText("");
            }
            
            if (from_intent.hasExtra(EXTRA_DISPLAY_NAME)) {
                ((EditText)findViewById(R.id.class_each_display_name)).setText(from_intent.getStringExtra(EXTRA_DISPLAY_NAME));
            } else {
                ((EditText)findViewById(R.id.class_each_display_name)).setText("");
            }

        } else {
            final ClassEntry entry = ClassPreferences.getInstance(this).getEntryById(classId);
            if (entry == null) {
                finish();
                return;
            }

            this.setHeaderFooterTexts(this.getString(R.string.preferences_title_for_header_and_footer_edit_class), null);
            ((Button)findViewById(R.id.class_each_submit_button)).setText(R.string.button_update);
            findViewById(R.id.class_each_delete_button).setVisibility(View.VISIBLE);

            if (from_intent.hasExtra(EXTRA_CLASS_NAME)) {
                ((EditText)findViewById(R.id.class_each_name)).setText(from_intent.getStringExtra(EXTRA_CLASS_NAME));
            } else {
                ((EditText)findViewById(R.id.class_each_name)).setText(entry.name);
            }
            
            if (from_intent.hasExtra(EXTRA_DISPLAY_NAME)) {
                ((EditText)findViewById(R.id.class_each_display_name)).setText(from_intent.getStringExtra(EXTRA_DISPLAY_NAME));
            } else {
                ((EditText)findViewById(R.id.class_each_display_name)).setText(entry.display_name);
            }
            
            if (!from_intent.hasExtra(EXTRA_SELECTED_APP_PACKAGES)) {
                Intent intent = getIntent();
                intent.putStringArrayListExtra(EXTRA_SELECTED_APP_PACKAGES, new ArrayList<>(entry.appPackageNames));
                setIntent(intent);
            }
        }

        final EditText classNameEditText = findViewById(R.id.class_each_name);
        EditTextConfigurations.applyCommandEditTextConfigurations(classNameEditText, this);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        this.changeBaseWindowElementSizeForAnimation(true);
    }

    @Override
    protected void onStop() {
        super.onStop();
        this.finish();
    }
}
