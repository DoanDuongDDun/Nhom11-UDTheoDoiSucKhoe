package com.example.vitality;

import android.content.Intent;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class BottomNavigationTest {
    @Test public void bodyAndItsSubpagesSelectBodyTab() {
        android.app.Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
        MainActivity activity=(MainActivity)instrumentation.startActivitySync(
                new Intent(instrumentation.getTargetContext(),MainActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
        instrumentation.runOnMainSync(()-> {
            NavHostFragment host=(NavHostFragment)activity.getSupportFragmentManager().findFragmentById(R.id.nav_host);
            BottomNavigationView bottom=activity.findViewById(R.id.bottom_nav);
            bottom.setSelectedItemId(R.id.activity);
            assertEquals(R.id.activity,bottom.getSelectedItemId());
            bottom.setSelectedItemId(R.id.body);
            assertEquals(R.id.body,host.getNavController().getCurrentDestination().getId());
            assertEquals(R.id.body,bottom.getSelectedItemId());
            host.getNavController().navigate(R.id.measurementEditor);
            assertEquals(R.id.body,bottom.getSelectedItemId());
            host.getNavController().popBackStack();
            host.getNavController().navigate(R.id.comparison);
            assertEquals(R.id.body,bottom.getSelectedItemId());
        });
    }
}
