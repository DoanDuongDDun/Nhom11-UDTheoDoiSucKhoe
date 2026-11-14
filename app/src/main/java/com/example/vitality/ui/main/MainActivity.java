package com.example.vitality.ui.main;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.*;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import androidx.lifecycle.ViewModelProvider;
import android.widget.Toast;
import com.example.vitality.R;
import com.example.vitality.databinding.ActivityMainBinding;
import com.example.vitality.viewmodel.VitalityViewModel;
public class MainActivity extends AppCompatActivity  {
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        ActivityMainBinding v=ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(v.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(v.main,(view,insets)-> {
            androidx.core.graphics.Insets i=insets.getInsets(WindowInsetsCompat.Type.systemBars()|WindowInsetsCompat.Type.ime());view.setPadding(i.left,i.top,i.right,i.bottom);return insets;
        }
        );
        NavHostFragment host=(NavHostFragment)getSupportFragmentManager().findFragmentById(R.id.nav_host);
        NavigationUI.setupWithNavController(v.bottomNav,host.getNavController());
        // Keep the selected tab tied to the actual screen, including body subpages
        // opened via dashboard shortcuts rather than a bottom-navigation tap.
        host.getNavController().addOnDestinationChangedListener((controller,destination,args)-> {
            int id=destination.getId();
            int tab=id;
            if(id==R.id.measurementEditor||id==R.id.comparison)tab=R.id.body;
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
    }
    @Override protected void onResume() {
        super.onResume();
        new ViewModelProvider(this).get(VitalityViewModel.class).refresh();
    }
}
