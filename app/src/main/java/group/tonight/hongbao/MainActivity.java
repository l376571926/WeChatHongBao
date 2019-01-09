package group.tonight.hongbao;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.FileCallback;
import com.lzy.okgo.callback.StringCallback;
import com.lzy.okgo.model.Progress;
import com.lzy.okgo.model.Response;
import com.lzy.okgo.request.base.Request;
import com.socks.library.KLog;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private TextView mVersiontTextView;
    private String mVersionName;
    private int mVersionCode;
    private CheckBox mStatusCheckBox;
    private TextView mOpenButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mStatusCheckBox = (CheckBox) findViewById(R.id.status);
        mVersiontTextView = (TextView) findViewById(R.id.version);
        mOpenButton = (TextView) findViewById(R.id.setting);
        findViewById(R.id.setting).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            }
        });

        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            mVersionName = packageInfo.versionName;
            mVersionCode = packageInfo.versionCode;
            mVersiontTextView.setText("当前版本：" + mVersionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        int accessibilityEnabled = 0;
        try {
            accessibilityEnabled = Settings.Secure.getInt(getApplicationContext().getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        boolean opened = false;
        if (accessibilityEnabled == 1) {
            //如果手机中开启了一些APP的辅助功能，settingValue的值为：APP1包名/APP1继承AccessibilityService类全名: APP2包名/APP2继承AccessibilityService类全名
            //com.kingroot.kinguser/com.kingroot.common.utils.system.monitor.top.TopAppMonitorAccessibilityService:group.tonight.hongbao/group.tonight.hongbao.HongBaoAccessibilityService
            String settingValue = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            KLog.e(settingValue);
            String[] split = settingValue.split(":");
            KLog.e(Arrays.toString(split));
            for (String value : split) {
                String[] pkgAndService = value.split("/");
                String packageName = pkgAndService[0];
                String serviceClassName = pkgAndService[1];
                KLog.e("包名：" + packageName + "，服务名：" + serviceClassName);
                if (packageName.equals(getPackageName())) {
                    opened = true;
                    break;
                }
            }
        }
        if (opened) {//服务已开启
            mStatusCheckBox.setChecked(true);
            mOpenButton.setText("已开启");
        } else {//服务未开启
            mStatusCheckBox.setChecked(false);
            mOpenButton.setText("开启红包辅助");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_share) {
            ImageView imageView = new ImageView(this);
            imageView.setImageResource(R.mipmap.ic_launcher);
            imageView.setAdjustViewBounds(true);
            FrameLayout frameLayout = new FrameLayout(this);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
            frameLayout.addView(imageView, params);
            Glide.with(imageView)
                    .load("http://qr.liantu.com/api.php?text=" + "https://fir.im/zr2t")
                    .apply(new RequestOptions().placeholder(R.mipmap.ic_launcher).override(300, 300))
                    .into(imageView);
            new AlertDialog.Builder(this)
                    .setIcon(R.mipmap.ic_launcher)
                    .setTitle("下载")
                    .setMessage("App下载二维码")
                    .setView(frameLayout)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    }).show();
        } else if (item.getItemId() == R.id.actioin_update) {
            OkGo.<String>get("https://download.fir.im/zr2t")
                    .execute(new StringCallback() {
                        @Override
                        public void onSuccess(Response<String> response) {
                            String json = response.body();
                            try {
                                JSONObject jsonObject = new JSONObject(json);
                                JSONObject app = jsonObject.getJSONObject("app");
                                String id = app.getString("id");
                                String download_token = app.getString("token");

                                JSONObject master = app.getJSONObject("releases").getJSONObject("master");
                                final String release_id = master.getString("id");
                                final String version = master.getString("version");
                                final int build = master.getInt("build");
                                final String changelog = master.getString("changelog");
                                final long fsize = master.getLong("fsize");
                                final long created_at = master.getLong("created_at");

                                OkGo.<String>post("https://download.fir.im/apps/" + id + "/install")
                                        .params("download_token", download_token)
                                        .params("release_id", release_id)
                                        .execute(new StringCallback() {
                                            @Override
                                            public void onSuccess(Response<String> response) {
                                                String json = response.body();
                                                try {
                                                    JSONObject jsonObject1 = new JSONObject(json);
                                                    final String apkUrl = jsonObject1.getString("url");
                                                    KLog.e("apk下载地址：" + apkUrl);

                                                    final UpdateBean updateBean = new UpdateBean();
                                                    updateBean.setId(release_id);
                                                    updateBean.setVersion(version);
                                                    updateBean.setBuild(build);
                                                    updateBean.setChangelog(changelog);
                                                    updateBean.setFsize(fsize);
                                                    updateBean.setCreated_at(created_at);
                                                    updateBean.setApkUrl(apkUrl);

                                                    KLog.e(updateBean.toString());

                                                    if (mVersionName.equals(version) && mVersionCode == build) {
                                                        Toast.makeText(MainActivity.this, "已是最新版本", Toast.LENGTH_SHORT).show();
                                                        return;
                                                    }

                                                    DecimalFormat format = new DecimalFormat("0.00");
                                                    double fileSize = fsize / 1024.0f / 1024.f;

                                                    StringBuilder builder = new StringBuilder();
                                                    builder.append("版本号：")
                                                            .append(build)
                                                            .append("\n")
                                                            .append("版本名称：")
                                                            .append(version)
                                                            .append("\n")
                                                            .append("Apk大小：")
                                                            .append(format.format(fileSize) + "MB")
                                                            .append("\n")
                                                            .append("发布时间：")
                                                            .append("\n")
                                                            .append(DateFormat.getDateTimeInstance().format(new Date(created_at * 1000)))
                                                            .append("\n")
                                                            .append("\n")
                                                            .append("更新内容：")
                                                            .append("\n")
                                                            .append(changelog);

                                                    new AlertDialog.Builder(MainActivity.this)
                                                            .setIcon(R.mipmap.ic_launcher)
                                                            .setTitle("发现新版本")
                                                            .setMessage(builder.toString())
                                                            .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                                                @Override
                                                                public void onClick(DialogInterface dialog, int which) {

                                                                }
                                                            })
                                                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                                                @Override
                                                                public void onClick(DialogInterface dialog, int which) {
                                                                    final ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
                                                                    progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);

                                                                    OkGo.<File>get(apkUrl)
                                                                            .execute(new FileCallback() {
                                                                                @Override
                                                                                public void onStart(Request<File, ? extends Request> request) {
                                                                                    super.onStart(request);
                                                                                    progressDialog.show();
                                                                                }

                                                                                @Override
                                                                                public void onFinish() {
                                                                                    super.onFinish();
                                                                                    progressDialog.dismiss();
                                                                                }

                                                                                @Override
                                                                                public void downloadProgress(Progress progress) {
                                                                                    super.downloadProgress(progress);
                                                                                    float fraction = progress.fraction;

                                                                                }

                                                                                @Override
                                                                                public void onSuccess(Response<File> response) {
                                                                                    File body = response.body();
                                                                                    KLog.e(body.getPath());
                                                                                    installApk(MainActivity.this, body.getPath());
                                                                                }
                                                                            });
                                                                }
                                                            })
                                                            .show();
                                                } catch (JSONException e) {
                                                    e.printStackTrace();
                                                    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        });

                            } catch (JSONException e) {
                                e.printStackTrace();
                                Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 安装apk
     *
     * @param context
     * @param apkPath
     */
    public static void installApk(Context context, String apkPath) {
        try {
            /**
             * provider
             * 处理android 7.0 及以上系统安装异常问题
             */
            File file = new File(apkPath);
            Intent install = new Intent(Intent.ACTION_VIEW);
            install.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Uri apkUri = FileProvider.getUriForFile(context, context.getString(R.string.file_provider_authorities), file);//在AndroidManifest中的android:authorities值
                Log.d("======", "apkUri=" + apkUri);
                install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);//添加这一句表示对目标应用临时授权该Uri所代表的文件
                install.setDataAndType(apkUri, "application/vnd.android.package-archive");
            } else {
                install.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
            }
            context.startActivity(install);
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("======", e.getMessage());
        }
    }
}
