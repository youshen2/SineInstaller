package moye.installer.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import moye.installer.R;
import moye.installer.listener.TouchScaleListener;

public class DialogActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialog);

        findViewById(R.id.ok_btn).setOnClickListener(view -> finish());
        findViewById(R.id.ok_btn).setOnTouchListener(new TouchScaleListener());

        Intent intent = this.getIntent();
        TextView title = findViewById(R.id.dialog_title);
        title.setText(intent.getStringExtra("title"));
        TextView content = findViewById(R.id.dialog_content);
        content.setText(intent.getStringExtra("content"));
    }

    @Override
    public void onBackPressed() {
    } //不可能让你用返回键退出的
}