package cn.bavelee.pokeinstaller.apk;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.AndroidRuntimeException;

import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import cn.bavelee.pokeinstaller.ShellUtils;
import moye.installer.Activity.DialogActivity;

//你问问这部分代码的包怎么还是Poke的？绝对不是懒得改直接搬（）

public class APKCommander {

    private Context context;
    private Uri uri;
    private ApkInfo mApkInfo;
    private ICommanderCallback callback;
    private Handler handler;
    
    private SharedPreferences sharedPreferences;

    public APKCommander(Context context, Uri uri, ICommanderCallback commanderCallback) {
        this.context = context;
        this.uri = uri;
        this.callback = commanderCallback;
        this.sharedPreferences = context.getSharedPreferences("settings",context.MODE_PRIVATE); //Warning直接忽略，能用就行
        handler = new Handler(Looper.getMainLooper());
        new ParseApkTask().start();
    }

    public void startInstall() {
        new InstallApkTask().start();
    }

    public ApkInfo getApkInfo() {
        return mApkInfo;
    }

    private class InstallApkTask extends Thread {
        @Override
        public void run() {
            super.run();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onApkPreInstall(mApkInfo);
                }
            });
            String installCommand = "";
            switch(sharedPreferences.getInt("set_install_type",0)){ //判断一下安装方式
                case 1:
                    installCommand = "cp \"" + mApkInfo.getApkFile().getPath() + "\" \"/data/app/" + mApkInfo.getApkFile().getName() + "\"\nchmod 644 \"/data/app/" + mApkInfo.getApkFile().getName() + "\"\n"; //暴力复制方式的安装命令
                    break;
                default:
                    installCommand = "pm install -r \"" + mApkInfo.getApkFile().getPath() + "\"\n"; //正常的安装命令
                    break;
            }
            final int retCode = ShellUtils.execWithRoot(installCommand, new ShellUtils.Result() {
                @Override
                public void onStdout(final String text) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onInstallLog(mApkInfo, text);
                        }
                    });
                }

                @Override
                public void onStderr(final String text) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onInstallLog(mApkInfo, text);
                        }
                    });
                }

                @Override
                public void onCommand(String command) {

                }

                @Override
                public void onFinish(int resultCode) {

                }
            });
            if (retCode == 0 && mApkInfo.isFakePath())
                if(sharedPreferences.getBoolean("set_auto_delete",false) && mApkInfo.getApkFile().getPath().length() > 5 && !mApkInfo.getApkFile().getPath().isEmpty()) ShellUtils.execWithRoot("rm -f \"" + mApkInfo.getApkFile().getPath() + "\"\n"); //我知道有一定的危险性所以没加-r()
                handler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onApkInstalled(mApkInfo, retCode);
                }
            });
        }
    }

    private class ParseApkTask extends Thread {
        @Override
        public void run() {
            super.run();
            try {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onStartParseApk(uri);
                    }
                });
                mApkInfo = new ApkInfo();
                String apkSourcePath = ContentUriUtils.getPath(context, uri);
                if (apkSourcePath == null) {
                    mApkInfo.setFakePath(true);
                    File tempFile = new File(context.getExternalCacheDir(), System.currentTimeMillis() + ".apk");
                    try {
                        InputStream is = context.getContentResolver().openInputStream(uri);
                        if (is != null) {
                            OutputStream fos = new FileOutputStream(tempFile);
                            byte[] buf = new byte[4096 * 1024];
                            int ret;
                            while ((ret = is.read(buf)) != -1) {
                                fos.write(buf, 0, ret);
                                fos.flush();
                            }
                            fos.close();
                            is.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    mApkInfo.setApkFile(tempFile);
                } else {
                    mApkInfo.setApkFile(new File(apkSourcePath));
                }
                //读取apk的信息
                PackageManager pm = context.getPackageManager();
                PackageInfo pkgInfo = pm.getPackageArchiveInfo(mApkInfo.getApkFile().getPath(), PackageManager.GET_PERMISSIONS); //就是因为过时了才能在安卓4上用
                if (pkgInfo != null) {
                    pkgInfo.applicationInfo.sourceDir = mApkInfo.getApkFile().getPath();
                    pkgInfo.applicationInfo.publicSourceDir = mApkInfo.getApkFile().getPath();
                    mApkInfo.setAppName(pm.getApplicationLabel(pkgInfo.applicationInfo).toString());
                    mApkInfo.setPackageName(pkgInfo.applicationInfo.packageName);
                    mApkInfo.setVersionName(pkgInfo.versionName);
                    mApkInfo.setVersionCode(pkgInfo.versionCode); //管你过不过时，能用就行
                    mApkInfo.setIcon(pkgInfo.applicationInfo.loadIcon(pm));
                    try {
                        PackageInfo installedPkgInfo = pm.getPackageInfo(mApkInfo.getPackageName(), 0); //过时了才支持安卓4的...
                        mApkInfo.setInstalledVersionName(installedPkgInfo.versionName);
                        mApkInfo.setInstalledVersionCode(installedPkgInfo.versionCode);
                        mApkInfo.setHasInstalledApp(true);
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                        mApkInfo.setHasInstalledApp(false);
                    }
                    mApkInfo.setPermissions(pkgInfo.requestedPermissions);
                }
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onApkParsed(mApkInfo);
                    }
                });
            }catch (SecurityException e){
                //安卓4用mt管理器调用安装发现会抛出这个错误，uri没权限访问？而且只有安卓4有这个问题
                e.printStackTrace();
                Intent intent = new Intent(context, DialogActivity.class);
                intent.putExtra("title","提醒");
                intent.putExtra("content","没有对应的权限打开文件，请尝试更换文件管理器！");
                context.startActivity(intent);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onApkParsed(null);
                    }
                });
            } catch (Exception e) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onApkParsed(null);
                    }
                });
                e.printStackTrace();
                throw new AndroidRuntimeException(e);
            }
        }
    }
}
