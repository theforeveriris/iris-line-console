package net.theforeveriris.irislineconsole.applicationMain;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import net.theforeveriris.irislineconsole.R;
import net.theforeveriris.irislineconsole.applicationMain.lib.EditTextConfigurations;
import net.theforeveriris.irislineconsole.commands.applications.ApplicationDatabase;
import net.theforeveriris.irislineconsole.dataStore.cache.ApplicationInformation;
import net.theforeveriris.irislineconsole.dataStore.persistent.AliasEntry;
import net.theforeveriris.irislineconsole.dataStore.persistent.AliasPreferences;

import java.util.ArrayList;
import java.util.List;

public class PreferencesEachAliasActivity extends BaseWindowActivity {
    private static final int REQUEST_CODE_SELECT_APPLICATION = 1002;
    private static final String EXTRA_SELECTED_PACKAGE = "selected_package";
    private static final String EXTRA_ALIAS = "alias";
    
    private ApplicationDatabase _applicationDatabase;
    private String _selectedPackageName = "";
    private String _selectedAppLabel = "";

    public PreferencesEachAliasActivity() {
        super(R.layout.preferences_aliases_each_body, true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setWindowBoundarySize(ROOT_WINDOW_FULL_WIDTH_IN_MOBILE, 3);

        this.changeBaseWindowElementSizeForAnimation(false);
        this.enableBaseWindowAnimation();

        _applicationDatabase = new ApplicationDatabase(this);

        findViewById(R.id.alias_each_submit_button).setOnClickListener(v -> {
            saveAlias();
        });

        findViewById(R.id.alias_each_delete_button).setOnClickListener(v -> {
            Intent from_intent = this.getIntent();
            Integer aliasId = null;
            if (from_intent.hasExtra("alias_id")) {
                aliasId = from_intent.getIntExtra("alias_id", -1);
            }
            
            if (aliasId != null) {
                AliasPreferences.getInstance(PreferencesEachAliasActivity.this).deleteById(aliasId);
                PreferencesEachAliasActivity.this.finish();
            }
        });

        findViewById(R.id.alias_each_select_application).setOnClickListener(v -> {
            Intent intent = getIntent();
            intent.putExtra(EXTRA_ALIAS, ((EditText)findViewById(R.id.alias_each_alias)).getText().toString());
            setIntent(intent);
            
            List<String> selectedPackages = new ArrayList<>();
            if (_selectedPackageName != null && !_selectedPackageName.isEmpty()) {
                selectedPackages.add(_selectedPackageName);
            }
            Intent selectIntent = PreferencesSelectApplicationsActivity.createIntent(PreferencesEachAliasActivity.this, selectedPackages);
            startActivityForResult(selectIntent, REQUEST_CODE_SELECT_APPLICATION);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SELECT_APPLICATION && resultCode == RESULT_OK && data != null) {
            ArrayList<String> selectedPackages = data.getStringArrayListExtra(PreferencesSelectApplicationsActivity.RESULT_SELECTED_PACKAGES);
            if (selectedPackages != null && selectedPackages.size() > 0) {
                _selectedPackageName = selectedPackages.get(0);
                _selectedAppLabel = getAppLabel(_selectedPackageName);
                
                Intent intent = getIntent();
                intent.putExtra(EXTRA_SELECTED_PACKAGE, _selectedPackageName);
                setIntent(intent);
                
                updateSelectedAppText();
            }
        }
    }

    private String getAppLabel(String packageName) {
        for (ApplicationInformation appInfo : _applicationDatabase.getApplicationInformationList()) {
            if (appInfo.getPackageName().equals(packageName)) {
                return appInfo.getLabel();
            }
        }
        return packageName;
    }

    private void saveAlias() {
        Intent from_intent = this.getIntent();
        Integer aliasId = null;
        if (from_intent.hasExtra("alias_id")) {
            aliasId = from_intent.getIntExtra("alias_id", -1);
        }
        
        String aliasText = ((EditText)findViewById(R.id.alias_each_alias)).getText().toString().trim();
        
        // 验证输入
        if (aliasText.isEmpty()) {
            Toast.makeText(PreferencesEachAliasActivity.this, "请输入别名", Toast.LENGTH_LONG).show();
            return;
        }
        
        // 验证无效字符（只允许字母、数字和下划线）
        if (!aliasText.matches("^[a-zA-Z0-9_]+$")) {
            Toast.makeText(PreferencesEachAliasActivity.this, "别名只能包含字母、数字和下划线", Toast.LENGTH_LONG).show();
            return;
        }
        
        if (_selectedPackageName == null || _selectedPackageName.isEmpty()) {
            Toast.makeText(PreferencesEachAliasActivity.this, "请选择一个应用", Toast.LENGTH_LONG).show();
            return;
        }
        
        // 检查重复别名
        AliasEntry existingEntry = AliasPreferences.getInstance(this).getEntryByAlias(aliasText);
        if (existingEntry != null && (aliasId == null || existingEntry.id != aliasId)) {
            Toast.makeText(PreferencesEachAliasActivity.this, "该别名已存在", Toast.LENGTH_LONG).show();
            return;
        }
        
        AliasEntry entry = new AliasEntry();
        if (aliasId != null) {
            entry.id = aliasId;
        }
        entry.alias = aliasText;
        entry.packageName = _selectedPackageName;
        
        Toast.makeText(PreferencesEachAliasActivity.this, 
            "保存别名 " + entry.alias + " → " + _selectedAppLabel, 
            Toast.LENGTH_SHORT).show();

        if (aliasId == null) {
            AliasPreferences.getInstance(PreferencesEachAliasActivity.this).add(entry);
        } else {
            AliasPreferences.getInstance(PreferencesEachAliasActivity.this).update(entry);
        }
        PreferencesEachAliasActivity.this.finish();
    }

    @Override
    public void onResume() {
        super.onResume();
        initializeUI();
    }

    private void initializeUI() {
        Intent from_intent = this.getIntent();
        Integer aliasId = null;
        if (from_intent.hasExtra("alias_id")) {
            aliasId = from_intent.getIntExtra("alias_id", -1);
        }

        if (aliasId == null) {
            // 新增模式
            this.setHeaderFooterTexts("添加别名", null);
            ((Button)findViewById(R.id.alias_each_submit_button)).setText(R.string.button_add);
            findViewById(R.id.alias_each_delete_button).setVisibility(View.GONE);

            if (from_intent.hasExtra(EXTRA_ALIAS)) {
                ((EditText)findViewById(R.id.alias_each_alias)).setText(from_intent.getStringExtra(EXTRA_ALIAS));
            } else {
                ((EditText)findViewById(R.id.alias_each_alias)).setText("");
            }
            
            if (from_intent.hasExtra(EXTRA_SELECTED_PACKAGE)) {
                _selectedPackageName = from_intent.getStringExtra(EXTRA_SELECTED_PACKAGE);
                _selectedAppLabel = getAppLabel(_selectedPackageName);
            }

        } else {
            // 编辑模式
            final AliasEntry entry = AliasPreferences.getInstance(this).getEntryById(aliasId);
            if (entry == null) {
                finish();
                return;
            }

            this.setHeaderFooterTexts("编辑别名", null);
            ((Button)findViewById(R.id.alias_each_submit_button)).setText(R.string.button_update);
            findViewById(R.id.alias_each_delete_button).setVisibility(View.VISIBLE);

            if (from_intent.hasExtra(EXTRA_ALIAS)) {
                ((EditText)findViewById(R.id.alias_each_alias)).setText(from_intent.getStringExtra(EXTRA_ALIAS));
            } else {
                ((EditText)findViewById(R.id.alias_each_alias)).setText(entry.alias);
            }
            
            if (from_intent.hasExtra(EXTRA_SELECTED_PACKAGE)) {
                _selectedPackageName = from_intent.getStringExtra(EXTRA_SELECTED_PACKAGE);
            } else {
                _selectedPackageName = entry.packageName;
            }
            _selectedAppLabel = getAppLabel(_selectedPackageName);
        }

        updateSelectedAppText();

        final EditText aliasEditText = findViewById(R.id.alias_each_alias);
        EditTextConfigurations.applyCommandEditTextConfigurations(aliasEditText, this);
    }

    private void updateSelectedAppText() {
        TextView selectedAppText = findViewById(R.id.alias_each_selected_app_text);
        if (_selectedPackageName != null && !_selectedPackageName.isEmpty()) {
            selectedAppText.setText("已选择: " + _selectedAppLabel + " (" + _selectedPackageName + ")");
        } else {
            selectedAppText.setText("未选择应用");
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        this.changeBaseWindowElementSizeForAnimation(true);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (_applicationDatabase != null) {
            _applicationDatabase.close();
        }
        this.finish();
    }
}
