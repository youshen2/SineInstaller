package moye.installer.Activity;

import moye.installer.R;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class DialogActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialog);

        findViewById(R.id.ok_btn).setOnClickListener(view -> finish());

        Intent intent = this.getIntent();
        TextView title = (TextView) findViewById(R.id.dialog_title);
        title.setText(intent.getStringExtra("title"));
        TextView content = (TextView) findViewById(R.id.dialog_content);
        content.setText(intent.getStringExtra("content"));
    }

    @Override
    public void onBackPressed() {} //不可能让你用返回键退出的
}