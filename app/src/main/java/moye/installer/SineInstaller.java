package moye.installer;
import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.util.DisplayMetrics;

public class SineInstaller extends Application {
    public static Context changeContextDpi(Context old){
        float dpiTimes = old.getSharedPreferences("settings",MODE_PRIVATE).getFloat("dpi", 1.0F);
        if(dpiTimes == 1.0F) return old;
        try{
            DisplayMetrics displayMetrics = old.getResources().getDisplayMetrics();
            Configuration configuration = old.getResources().getConfiguration();
            configuration.densityDpi = (int)(displayMetrics.densityDpi * dpiTimes);
            return old.createConfigurationContext(configuration);
        }catch (Exception e){
            e.printStackTrace();
            return old;
        }
    }

}
