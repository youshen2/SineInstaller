package moye.installer.Activity;

import android.widget.ImageView;
import android.widget.TextView;
import moye.installer.R;

import android.os.Bundle;
import moye.installer.Activity.BaseActivity;

public class ExampleActivity extends BaseActivity {
    ImageView app_icon_view;
    TextView app_name_view;
    TextView app_packagename_view;
    TextView app_version_view;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_install); //复用了安装页面的布局
        
        app_icon_view = findViewById(R.id.app_icon);
        app_name_view = findViewById(R.id.app_name);
        app_packagename_view = findViewById(R.id.app_packagename);
        app_version_view = findViewById(R.id.app_version);
        
        app_icon_view.setImageDrawable(getResources().getDrawable(R.drawable.icon));
        app_name_view.setText("界面预览");
        app_packagename_view.setText("应用包名");
        app_version_view.setText("应用版本");
        
        findViewById(R.id.cancel_btn).setOnClickListener(view -> finish());
    }
}
