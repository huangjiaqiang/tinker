package tinker.sample.android.app;

import android.content.Context;
import android.os.Debug;

import com.tencent.tinker.loader.app.TinkerApplication;
import com.tencent.tinker.loader.shareutil.ShareTinkerInternals;


public class SampleApplication extends TinkerApplication
{

    public SampleApplication() {
        super(15, "tinker.sample.android.app.SampleApplicationLike", "com.tencent.tinker.loader.TinkerLoader", false, false, false);
    }

    @Override
    protected void attachBaseContext(Context base)
    {
        if (ShareTinkerInternals.getProcessName(base).contains("patch")){
            Debug.waitForDebugger();
        }
        super.attachBaseContext(base);
    }
}