package app_update.sscctv.com.app_update;


import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;

import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;


//import com.sscctv.seeeyesupdate.models.Channel;
//import com.sscctv.seeeyesupdate.models.Package;
//import com.sscctv.seeeyesupdate.models.Release;
//import com.sscctv.seeeyesupdate.services.UpdateService;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "App Update[Main]";
    private final int ip_version = 1;
    private final int viewer_version = 2;
    private final int tdrc_version = 3;
    private final int tdru_version = 4;
    private final int launcher_version = 5;
    private final int manual_version = 6;
    private final int update_version = 7;

    private Button bt_launcher, bt_ip, bt_viewer, bt_tdrc, bt_tdru, bt_packet, bt_update;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);

        bt_ip = findViewById(R.id.bt_ip);
        bt_viewer = findViewById(R.id.bt_viewer);
        bt_tdrc = findViewById(R.id.bt_tdrc);
        bt_tdru = findViewById(R.id.bt_tdru);
        bt_launcher = findViewById(R.id.bt_launcher);
        bt_packet = findViewById(R.id.bt_packet);
        bt_update = findViewById(R.id.bt_update);

        bt_ip.setOnClickListener(this);
        bt_viewer.setOnClickListener(this);
        bt_tdrc.setOnClickListener(this);
        bt_tdru.setOnClickListener(this);
        bt_launcher.setOnClickListener(this);
        bt_packet.setOnClickListener(this);
        bt_update.setOnClickListener(this);

        callFragment(ip_version);

//        updateInstalledAppsInfo();

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_ip:
                callFragment(ip_version);
                break;
            case R.id.bt_viewer:
                callFragment(viewer_version);
                break;
            case R.id.bt_tdrc:
                callFragment(tdrc_version);
                break;
            case R.id.bt_tdru:
                callFragment(tdru_version);
                break;
            case R.id.bt_launcher:
                callFragment(launcher_version);
                break;

            case R.id.bt_packet:
                callFragment(manual_version);
                break;
            case R.id.bt_update:
                callFragment(update_version);
                break;
        }

    }

    private void callFragment(int fragment_no) {

         FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

         switch (fragment_no) {
             case 1:
                 Ip_activity ip_activity = new Ip_activity();
                 transaction.replace(R.id.fragment_container, ip_activity);
                 transaction.commit();
                 break;
             case 2:
                 Viewer_activity viewer_activity = new Viewer_activity();
                 transaction.replace(R.id.fragment_container, viewer_activity);
                 transaction.commit();
                 break;
             case 3:
                 TDRC_acitvity tdrc_acitvity = new TDRC_acitvity();
                 transaction.replace(R.id.fragment_container, tdrc_acitvity);
                 transaction.commit();
                 break;
             case 4:
                 TDRU_activity tdru_activity = new TDRU_activity();
                 transaction.replace(R.id.fragment_container, tdru_activity);
                 transaction.commit();
                 break;
             case 5:
                 Launcher_activity launcher_activity = new Launcher_activity();
                 transaction.replace(R.id.fragment_container, launcher_activity);
                 transaction.commit();
                 break;
             case 6:
                 Packet_activity packet_activity = new Packet_activity();
                 transaction.replace(R.id.fragment_container, packet_activity);
                 transaction.commit();
                 break;
             case 7:
                 Update_activity update_activity = new Update_activity();
                 transaction.replace(R.id.fragment_container, update_activity);
                 transaction.commit();
                 break;

         }

    }


}
