package com.example.vitality.ui.main;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.*;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import androidx.lifecycle.ViewModelProvider;
import android.widget.Toast;
import com.example.vitality.R;
import com.example.vitality.databinding.ActivityMainBinding;
import com.example.vitality.data.database.VitalityDatabase;
import com.example.vitality.data.repository.WellnessRepository;
import com.example.vitality.notification.NotificationDestination;
import com.example.vitality.viewmodel.VitalityViewModel;
public class MainActivity extends AppCompatActivity  {
    private NavController navigation;
    private int notificationOpen;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        ActivityMainBinding v=ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(v.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(v.main,(view,insets)-> {
            androidx.core.graphics.Insets i=insets.getInsets(WindowInsetsCompat.Type.systemBars()|WindowInsetsCompat.Type.ime());view.setPadding(i.left,i.top,i.right,i.bottom);return insets;
        }
        );
        NavHostFragment host=(NavHostFragment)getSupportFragmentManager().findFragmentById(R.id.nav_host);
        navigation=host.getNavController();
        NavigationUI.setupWithNavController(v.bottomNav,navigation);
        v.bottomNav.setOnItemSelectedListener(item-> {
            if(item.getItemId()!=R.id.home) return NavigationUI.onNavDestinationSelected(item,navigation);
            if(navigation.getCurrentDestination()!=null&&navigation.getCurrentDestination().getId()==R.id.home) return true;
            navigation.navigate(R.id.home,null,new NavOptions.Builder()
                    .setPopUpTo(R.id.home,false).setLaunchSingleTop(true).build());
            return true;
        });
        // Keep the selected tab tied to the actual screen, including body subpages
        // opened via dashboard shortcuts rather than a bottom-navigation tap.
        navigation.addOnDestinationChangedListener((controller,destination,args)-> {
            int id=destination.getId();
            int tab=id;
            if(id==R.id.measurementEditor||id==R.id.comparison)tab=R.id.body;
            else if(id==R.id.notificationSettings||id==R.id.healthDataExport)tab=R.id.profile;
            else if(id==R.id.goals||id==R.id.goalEditor||id==R.id.sleep||id==R.id.period||id==R.id.habit||id==R.id.diary
                    ||id==R.id.statistics||id==R.id.symptom||id==R.id.symptomEditor)tab=R.id.home;
            android.view.MenuItem item=v.bottomNav.getMenu().findItem(tab);
            if(item!=null)item.setChecked(true);
        });
        VitalityViewModel vm=new ViewModelProvider(this).get(VitalityViewModel.class);
        vm.message.observe(this,s-> {
            if(s!=null) {
                Toast.makeText(this,s,Toast.LENGTH_LONG).show();vm.message.setValue(null);
            }
        }
        );
        if(b==null)openNotification(getIntent());
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        openNotification(intent);
    }

    private void openNotification(Intent intent) {
        NotificationDestination target=NotificationDestination.take(intent);
        if(target==null)return;
        int opened=++notificationOpen;
        navigation.navigate(target.type.destination,null,new NavOptions.Builder().setLaunchSingleTop(true).build());
        if(!target.hasRecord())return;
        WellnessRepository.IO.execute(()-> {
            String detail=target.loadDetail(VitalityDatabase.get(getApplicationContext()));
            runOnUiThread(()-> {
                if(opened!=notificationOpen||isFinishing()||isDestroyed()||navigation.getCurrentDestination()==null
                        ||navigation.getCurrentDestination().getId()!=target.type.destination)return;
                if(detail==null) {
                    Toast.makeText(this,"Dữ liệu không còn khả dụng",Toast.LENGTH_LONG).show();
                    return;
                }
                new AlertDialog.Builder(this).setTitle(target.type.group.title)
                        .setMessage(detail).setPositiveButton("Đóng",null).show();
            });
        });
    }

    @Override protected void onResume() {
        super.onResume();
        new ViewModelProvider(this).get(VitalityViewModel.class).refresh();
    }
}
