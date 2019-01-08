package group.tonight.hongbao;

import android.app.Application;

import com.lzy.okgo.OkGo;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        OkGo.getInstance().init(this);
    }
}
