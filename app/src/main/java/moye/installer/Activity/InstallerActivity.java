package moye.installer.Activity;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import moye.installer.R;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import cn.bavelee.pokeinstaller.ShellUtils;
import cn.bavelee.pokeinstaller.apk.APKCommander;
import cn.bavelee.pokeinstaller.apk.ApkInfo;
import cn.bavelee.pokeinstaller.apk.ICommanderCallback;

public class InstallerActivity extends BaseActivity implements ICommanderCallback {

    ImageView app_icon_view;
    TextView app_name_view;
    TextView app_packagename_view;
    TextView app_version_view;
    Button install_btn_view;
    Button cancel_btn_view;
    Button setting_btn_view;
    MaterialCardView log_view_card;
    
    TextView log_view;
    
    LinearProgressIndicator install_progress_view;

    APKCommander apkCommander;

    boolean is_installing = false;
    boolean is_install_finish = false;
    boolean install_result = true;
    
    private String install_log = "";

    SharedPreferences sharedPreferences;
    SharedPreferences.Editor sharedPreferences_editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_install);

        sharedPreferences = getSharedPreferences("settings",MODE_PRIVATE);
        sharedPreferences_editor = sharedPreferences.edit();
        
        sharedPreferences_editor.putBoolean("last_root_check",true).commit();
        
        requestPermission(); //申请额外的权限
        
        //检查root权限
        if(sharedPreferences.getBoolean("set_check_root",true)){ //是否开启
            ShellUtils.exec("su\necho root\nexit\n", new ShellUtils.Result() {
                @Override
                public void onStdout(final String text) {}

                @Override
                public void onStderr(final String text) {
                    runOnUiThread(() -> Toast.makeText(InstallerActivity.this, "好像没有授权root权限哦~", Toast.LENGTH_LONG).show());
                    install_log += text + "\n";
                    sharedPreferences_editor.putBoolean("last_root_check",false).commit();
                }

                @Override
                public void onCommand(String command) {}

                @Override
                public void onFinish(int resultCode) {}
            },false);
        }
        
        //赋值页面元素
        app_icon_view = findViewById(R.id.app_icon);
        app_name_view = findViewById(R.id.app_name);
        app_packagename_view = findViewById(R.id.app_packagename);
        app_version_view = findViewById(R.id.app_version);
        install_btn_view = findViewById(R.id.install_btn);

        install_progress_view = findViewById(R.id.install_progress);
        
        cancel_btn_view = findViewById(R.id.cancel_btn);
        cancel_btn_view.setOnClickListener(view -> finish());
        install_btn_view.setOnClickListener(view -> {
            if(is_install_finish && !is_installing){
                if(install_result){
                        //安装完成后的按钮逻辑
                        do_after_install();
                }else{
                    //那如果安装失败了....
                    Intent intent = new Intent(this,DialogActivity.class);
                    intent.putExtra("title","安装日志");
                    intent.putExtra("content",install_log);
                    startActivity(intent);
                }
            }else apkCommander.startInstall();
        });
        setting_btn_view = findViewById(R.id.menu_btn);
        setting_btn_view.setOnClickListener(view -> {
            Intent intent = new Intent(this,MenuActivity.class);
            startActivity(intent);
        });
        
        log_view = findViewById(R.id.log_view);
        log_view_card = findViewById(R.id.log_view_card);
        log_view_card.setOnLongClickListener(view -> {
            copy_content(install_log);
            return false;
        });

        if(getIntent().getData() == null) finish();

        //开始解析安装包
        install_btn_view.setEnabled(false);
        install_progress_view.setVisibility(View.GONE);
        apkCommander = new APKCommander(InstallerActivity.this,getIntent().getData(),InstallerActivity.this);
    }

    @Override
    public void onStartParseApk(Uri uri) {
        app_name_view.setText("解析中..");
        install_progress_view.setVisibility(View.VISIBLE);
    }

    @Override
    public void onApkParsed(ApkInfo apkInfo) {
        install_progress_view.setVisibility(View.GONE);
        //解析安装包
        if(apkInfo == null || TextUtils.isEmpty(apkInfo.getPackageName())) {
            //解析失败时
            app_name_view.setText("安装包解析失败");
            install_btn_view.setText("无法安装");
            Intent intent = new Intent(this,DialogActivity.class);
            intent.putExtra("title","安装包解析失败");
            intent.putExtra("content","该安装包可能损坏或不兼容您的安卓版本。");
            startActivity(intent);
        }
        else{
            //解析成功时
            install_btn_view.setEnabled(true);
            app_icon_view.setImageDrawable(apkInfo.getIcon());
            app_name_view.setText(apkInfo.getAppName());
            app_name_view.setOnLongClickListener(view -> {
                copy_content(apkInfo.getAppName());
                return false;
            });
            app_packagename_view.setText(apkInfo.getPackageName());
            app_packagename_view.setOnLongClickListener(view -> {
                copy_content(apkInfo.getPackageName());
                return false;
            });
            
            //如果已经安装
            if (apkInfo.isHasInstalledApp()) {
                app_version_view.setText(apkInfo.getInstalledVersion() + " -> " + apkInfo.getVersion());
                if(apkInfo.getVersionCode() < apkInfo.getInstalledVersionCode()) Toast.makeText(this, "即将降级安装", Toast.LENGTH_LONG).show();
            }
            else app_version_view.setText(apkInfo.getVersion());
            
            if(sharedPreferences.getBoolean("set_show_perm",true)){
                //显示应用权限
                if (apkInfo.getPermissions() != null && apkInfo.getPermissions().length > 0) {
                    String perm_content = "应用权限：\n";
                    for (String perm : apkInfo.getPermissions()) {
                        perm_content += perm + "\n";
                    }
                    log_view.setText(perm_content);
                    log_view_card.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    @Override
    public void onApkPreInstall(ApkInfo apkInfo) {
        //开始安装
        cancel_btn_view.setEnabled(false);
        install_btn_view.setEnabled(false);
        setting_btn_view.setEnabled(false);
        install_btn_view.setText("安装中..");
        is_installing = true;
        if(sharedPreferences.getBoolean("set_show_log",true)) log_view_card.setVisibility(View.VISIBLE);
        else log_view_card.setVisibility(View.GONE);
        install_progress_view.setVisibility(View.VISIBLE);
        log_view.setText("安装日志：");
    }

    @Override
    public void onApkInstalled(ApkInfo apkInfo, int resultCode) {
        //安装完成
        is_installing = false;
        is_install_finish = true;
        cancel_btn_view.setEnabled(true);
        setting_btn_view.setEnabled(true);
        install_progress_view.setVisibility(View.GONE);
        if(resultCode == 0){
            install_result = true;
            cancel_btn_view.setText("完成");
            install_btn_view.setEnabled(true);
            switch(sharedPreferences.getInt("set_install_type",0)){
                case 1:
                    install_btn_view.setText("重启");
                    Toast.makeText(this,"安装完成，需要重启",Toast.LENGTH_SHORT).show();
                    break;
                default:
                    install_btn_view.setText("打开");
                    Toast.makeText(this,"安装完成",Toast.LENGTH_SHORT).show();
                    break;
            }
            if(sharedPreferences.getBoolean("set_auto_do",false)) do_after_install();
        }else{
            String dialog_content = "请查看日志并检查root权限";
            if(install_log.contains("INSTALL_FAILED_INSUFFICIENT_STORAGE")) dialog_content = "存储空间不足";
            else if(install_log.contains("INSTALL_PARSE_FAILED_INCONSISTENT_CERTIFICATES")) dialog_content = "安装包签名与已安装应用的不一致";
            else if(install_log.contains("INSTALL_FAILED_VERSION_DOWNGRADE")) dialog_content = "降级安装失败";
            else if(install_log.contains("INSTALL_FAILED_CPU_ABI_INCOMPATIBLE")) dialog_content = "该安装包不支持本设备";
            else if(install_log.contains("INSTALL_FAILED_NEWER_SDK")) dialog_content = "您的安卓版本过高";
            else if(install_log.contains("INSTALL_FAILED_UPDATE_INCOMPATIBLE")) dialog_content = "您可能需要先卸载掉已安装的版本";
            else if(install_log.contains("INSTALL_FAILED_TEST_ONLY")) dialog_content = "该安装包为TestOnly的测试包，无法进行安装";
            else if(install_log.contains("not allowed by")) dialog_content = "请先解PM锁";
            install_failed(dialog_content);
        }
    }

    @Override
    public void onInstallLog(ApkInfo apkInfo, String logText) {
        install_log += logText + "\n";
        log_view.setText("安装日志：\n" + install_log + "\n\n（长按复制）");
    }

    @Override
    public void onBackPressed() {
        if(!is_installing) finish();
        else Toast.makeText(this,"请等待安装完成",Toast.LENGTH_SHORT).show();
    }
    
    public void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Toast.makeText(getApplicationContext(), "高版本安卓请授权权限后安装", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 2);
                finish();
            }
        }
    }
    
    private void copy_content(String content){
        ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE); 
        ClipData clipData = ClipData.newPlainText("label",content); 
        clipboardManager.setPrimaryClip(clipData); 
        Toast.makeText(this, "已尝试复制", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this,DialogActivity.class);
        intent.putExtra("title","提示");
        intent.putExtra("content","已尝试复制。部分手表(如小天才)阉割了剪贴板，无法复制内容");
        startActivity(intent);
    }
    
    private void do_after_install(){
        switch(sharedPreferences.getInt("set_install_type",0)){
            case 1:
                ShellUtils.execWithRoot("reboot\n");
                break;
            default:
                try{
                    Intent intent = getPackageManager().getLaunchIntentForPackage(apkCommander.getApkInfo().getPackageName());
                    startActivity(intent);
                    finish();
                }catch(ActivityNotFoundException e){
                    Toast.makeText(this, "没有入口活动", Toast.LENGTH_SHORT).show();
                    install_btn_view.setEnabled(false);
                    install_btn_view.setText("无法打开");
                }catch(Exception e){
                    Toast.makeText(this, "打开失败", Toast.LENGTH_SHORT).show();
                }
                break;
         }
    }
    
    private void install_failed(String reason){
        log_view_card.setVisibility(View.VISIBLE);
        install_result = false;
        install_btn_view.setText("安装失败");
        is_installing = false;
        is_install_finish = true;
        cancel_btn_view.setEnabled(true);
        setting_btn_view.setEnabled(true);
        Toast.makeText(this,"安装失败",Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this,DialogActivity.class);
        intent.putExtra("title","安装失败");
        intent.putExtra("content",reason);
        startActivity(intent);
    }
}