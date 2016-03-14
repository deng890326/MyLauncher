package com.example.wei.mylauncher;

import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;

public class LauncherActivity extends SingleFragmentActivity {

    @Override
    protected Fragment createFragment() {
        return LauncherFragment.newInstance();
    }

}
