package moye.installer.Activity;

import android.view.View;
import cn.bavelee.pokeinstaller.ShellUtils;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import moye.installer.Activity.DialogActivity;
import moye.installer.Activity.ExampleActivity;
import moye.installer.R;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MenuActivity extends BaseActivity{

    SharedPreferences sharedPreferences;
    SharedPreferences.Editor sharedPrefercences_editor;

    private boolean is_debug_version = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        sharedPreferences = getSharedPreferences("settings",MODE_PRIVATE);
        sharedPrefercences_editor = sharedPreferences.edit();

        ((EditText)findViewById(R.id.ui_scale_input)).setText(String.valueOf(sharedPreferences.getFloat("dpi",1.0F)));
        findViewById(R.id.preview).setOnClickListener(view -> {
            save();
            Intent intent = new Intent(this,ExampleActivity.class);
            startActivity(intent);
        });
        ((SwitchMaterial)findViewById(R.id.check_root)).setChecked(sharedPreferences.getBoolean("set_check_root",true));
        ((SwitchMaterial)findViewById(R.id.auto_delete)).setChecked(sharedPreferences.getBoolean("set_auto_delete",false));
        ((SwitchMaterial)findViewById(R.id.auto_do)).setChecked(sharedPreferences.getBoolean("set_auto_do",false));
        ((SwitchMaterial)findViewById(R.id.show_perm)).setChecked(sharedPreferences.getBoolean("set_show_perm",true));
        ((SwitchMaterial)findViewById(R.id.show_log)).setChecked(sharedPreferences.getBoolean("set_show_log",true));
        switch(sharedPreferences.getInt("set_install_type",0)){
            case 1:
                ((MaterialRadioButton)findViewById(R.id.type_copy)).setChecked(true);
                break;
            default:
                ((MaterialRadioButton)findViewById(R.id.type_pm)).setChecked(true);
                break;
        }
        findViewById(R.id.update_log).setOnClickListener(view -> {
            Intent intent = new Intent(this,DialogActivity.class);
            intent.putExtra("title","更新日志");
            intent.putExtra("content",getResources().getString(R.string.update_log));
            startActivity(intent);
        });
        if(!sharedPreferences.getBoolean("last_root_check",true)) findViewById(R.id.no_root).setVisibility(View.VISIBLE);
        if(!is_debug_version) findViewById(R.id.debug_tip).setVisibility(View.GONE);
        findViewById(R.id.no_root).setOnClickListener(view -> {
            sharedPrefercences_editor.putBoolean("last_root_check",true).commit();
            view.setVisibility(View.GONE);
            ShellUtils.exec("su\necho root\nexit\n", new ShellUtils.Result() {
                @Override
                public void onStdout(final String text) {}

                @Override
                public void onStderr(final String text) {
                    runOnUiThread(() -> {
                        Toast.makeText(MenuActivity.this, "授权失败", Toast.LENGTH_SHORT).show();
                        view.setVisibility(View.VISIBLE);
                    });
                    sharedPrefercences_editor.putBoolean("last_root_check",false).commit();
                }

                @Override
                public void onCommand(String command) {}

                @Override
                public void onFinish(int resultCode) {}
            },false);
        });
        findViewById(R.id.exit_btn).setOnClickListener(view -> finish());
        
        try{
            ((TextView)findViewById(R.id.app_version)).setText(getPackageManager().getPackageInfo(getPackageName(),0).versionName);
        }catch(PackageManager.NameNotFoundException e){
            e.printStackTrace();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        save();
    }
    
    private void save(){
        try{
            EditText uiScaleInput = findViewById(R.id.ui_scale_input);
            if(!uiScaleInput.getText().toString().isEmpty()) {
                float dpiTimes = Float.parseFloat(uiScaleInput.getText().toString());
                if (dpiTimes >= 0.3F && dpiTimes <= 3.0F) sharedPrefercences_editor.putFloat("dpi", dpiTimes).commit();
                else Toast.makeText(this, "页面大小超出范围", Toast.LENGTH_SHORT).show();
            }else Toast.makeText(this, "页面大小不能为空", Toast.LENGTH_SHORT).show();
            
            sharedPrefercences_editor.putBoolean("set_check_root",((SwitchMaterial)findViewById(R.id.check_root)).isChecked());
            sharedPrefercences_editor.putBoolean("set_auto_delete",((SwitchMaterial)findViewById(R.id.auto_delete)).isChecked());
            sharedPrefercences_editor.putBoolean("set_auto_do",((SwitchMaterial)findViewById(R.id.auto_do)).isChecked());
            sharedPrefercences_editor.putBoolean("set_show_perm",((SwitchMaterial)findViewById(R.id.show_perm)).isChecked());
            sharedPrefercences_editor.putBoolean("set_show_log",((SwitchMaterial)findViewById(R.id.show_log)).isChecked());
            if(((MaterialRadioButton)findViewById(R.id.type_pm)).isChecked()) sharedPrefercences_editor.putInt("set_install_type",0);
            else if(((MaterialRadioButton)findViewById(R.id.type_copy)).isChecked()) sharedPrefercences_editor.putInt("set_install_type",1);
            sharedPrefercences_editor.commit();
        }catch(Exception e){
            Toast.makeText(this, "数据存在异常", Toast.LENGTH_SHORT).show();
        }
    }
}
