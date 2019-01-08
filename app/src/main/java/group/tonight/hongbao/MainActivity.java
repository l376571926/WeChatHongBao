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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
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
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.setting).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            }
        });
        ImageView imageView = (ImageView) findViewById(R.id.qr_code);
        Glide.with(imageView)
                .load("http://qr.liantu.com/api.php?text=" + "https://fir.im/zr2t")
                .into(imageView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.actioin_update) {
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


                                                    PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                                                    String versionName = packageInfo.versionName;
                                                    int versionCode = packageInfo.versionCode;
                                                    if (versionName.equals(version) && versionCode == build) {
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
                                                } catch (PackageManager.NameNotFoundException e) {
                                                    e.printStackTrace();
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
