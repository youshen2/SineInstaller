package moye.installer.Activity;

import android.app.Activity;
import android.content.Context;
import moye.installer.SineInstaller;

public class BaseActivity extends Activity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(SineInstaller.changeContextDpi(newBase));
    }
}
