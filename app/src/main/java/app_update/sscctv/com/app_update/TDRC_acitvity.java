package app_update.sscctv.com.app_update;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.github.zafarkhaja.semver.Version;
import com.jakewharton.rxbinding.view.RxView;
import com.laimiux.rxnetwork.RxNetwork;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import app_update.sscctv.com.app_update.models.Channel;
import app_update.sscctv.com.app_update.models.Package;
import app_update.sscctv.com.app_update.models.Release;
import app_update.sscctv.com.app_update.services.UpdateService;
import okhttp3.ResponseBody;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

public class TDRC_acitvity extends Fragment {
    private static final String UPDATE_CHANNEL = "stable";

    private static final String PACKAGE_LAUNCHER = "com.sscctv.tdr";
    private static final String TAG = "App Update[Viewer]";
    private TextView now_ver, new_ver, message;
    private Button checkButton, updateButton;

    private UpdateService mUpdateService;
    private Subscription mIsOnlineSubscription;
    private PublishSubject<Boolean> mIsOnline;

    private Package aPackage;

    public TDRC_acitvity() {

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        mUpdateService = new UpdateService();
        mIsOnline = PublishSubject.create();

        View view = inflater.inflate(R.layout.activity_tdrc, null);
        now_ver = view.findViewById(R.id.tdrc_version);
        new_ver = view.findViewById(R.id.tdrc_latest);

        checkButton = view.findViewById(R.id.tdrc_check_btn);
        updateButton = view.findViewById(R.id.tdrc_update_btn);
        message = view.findViewById(R.id.tdrc_message);

        Observable.combineLatest(
                mIsOnline
                        .distinct()    // 상태가 바뀌었을 때만 처리
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnNext(new Action1<Boolean>() {
                            @Override

                            public void call(Boolean isOnline) {
                                message.setTextColor(Color.WHITE);

                                // 네트워크 연결 상태가 바뀌면 UI를 업데이트함
                                if (!isOnline) {
                                    message.setText(getString(R.string.msg_no_connection));

                                    checkButton.setText(R.string.check_wifi);
                                    updateButton.setVisibility(Button.INVISIBLE);
                                } else {
                                    message.setText(R.string.msg_ok_connection);
                                    checkButton.setText(R.string.check_update);
                                    updateButton.setVisibility(Button.VISIBLE);
                                }

                                checkButton.setEnabled(true);
                                updateButton.setEnabled(false);
                            }
                        }),
                RxView.clicks(checkButton),
                new Func2<Boolean, Void, Boolean>() {
                    @Override
                    public Boolean call(Boolean isOnline, Void aVoid) {
                        return isOnline;
                    }
                })
                .flatMap(new Func1<Boolean, Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> call(final Boolean isOnline) {
                        if (!isOnline) {
                            return Observable.fromCallable(new Func0<Boolean>() {
                                @Override
                                public Boolean call() {
                                    startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                                    Log.d(TAG, "WIFI SETTING GOGO");
                                    return true;
                                }
                            });
                        } else {
                            // 버튼이 눌리면 업데이트 확인 시작
                            return updatePackageState();
                        }
                    }
                })
                .subscribe(new Observer<Boolean>() {
                    @Override
                    public void onCompleted() {
                        Log.d(TAG, "Update check done----------------------");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d(TAG, "Update check", e);
                    }

                    @Override
                    public void onNext(Boolean aBoolean) {
                    }
                });

        // 업데이트 버튼을 누르면 업데이트를 시작
        RxView
                .clicks(updateButton)
                .doOnNext(new Action1<Void>() {
                    @Override
                    public void call(Void aVoid) {
                        checkButton.setEnabled(false);
                        updateButton.setEnabled(false);

                        message.setTextColor(Color.WHITE);
                        message.setText(getString(R.string.updating));
                    }
                })
                .observeOn(Schedulers.io())
                .flatMap(new Func1<Void, Observable<Package>>() {
                    @Override
                    public Observable<Package> call(Void aVoid) {
                        return getPackages();
                    }
                })
                .flatMap(new Func1<Package, Observable<Release>>() {
                    @Override
                    public Observable<Release> call(final Package pkg) {
                        Version latest = Version.valueOf(pkg.latest);

                        switch (pkg.id) {
                            case "tdrc":
                                if (!latest.greaterThan(getPackageVersion(PACKAGE_LAUNCHER))) {
                                    return null;
                                }
                                break;

                            default:
                                return null;
                        }

                        Log.d(TAG, "Latest '" + pkg.name + "' version = " + pkg.latest);

                        // 릴리스 목록에서 최신판을 찾는다.
                        return mUpdateService.getApi()
                                .getReleases(pkg.id)
                                .flatMap(new Func1<Map<String, Release>, Observable<Release>>() {
                                    @Override
                                    public Observable<Release> call(Map<String, Release> stringReleaseMap) {
                                        if (!stringReleaseMap.containsKey(pkg.latest)) {
                                            throw new RuntimeException(
                                                    getString(R.string.release_not_found) + " (" + pkg.id + "-" + pkg.latest + ")");
                                        }
                                        return Observable.just(stringReleaseMap.get(pkg.latest));
                                    }
                                });
                    }
                })
                .flatMap(new Func1<Release, Observable<ResponseBody>>() {
                    @Override
                    public Observable<ResponseBody> call(Release release) {
                        Log.d(TAG, "Release " + release.url);

                        // 다운로드 한다
                        return mUpdateService.getApi()
                                .download(release.url);
                    }
                })
                // URL을 바로 지정하여 설치하려 하면 다음과 같은 오류 발생
                // android.content.ActivityNotFoundException: No Activity found to handle Intent { act=android.intent.action.VIEW dat=http://192.168.0.34:4802//update/releases/seeeyes-launcher-0.3.1.apk typ=application/vnd.android.package-archive }
                // 실제로 content와 file URL만 허용한다.
                .flatMap(new Func1<ResponseBody, Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> call(final ResponseBody body) {
                        return Observable.fromCallable(new Func0<Boolean>() {
                            @Override
                            public Boolean call() {
                                final String path = Environment.getExternalStorageDirectory() + "/update.apk";

                                try {
                                    File file = new File(path);
                                    boolean deleted = file.delete();

                                    InputStream inputStream = body.byteStream();
                                    OutputStream outputStream = new FileOutputStream(path);

                                    byte data[] = new byte[1024];
//                                    long total = 0;
                                    int count;
                                    while ((count = inputStream.read(data)) != -1) {
//                                        total += count;
//                                        publishProgress((int) (total * 100 / fileLength));
                                        outputStream.write(data, 0, count);
                                    }

                                    outputStream.flush();
                                    outputStream.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                                Intent intent = new Intent();
                                intent.setAction(Intent.ACTION_INSTALL_PACKAGE);
                                intent.setDataAndType(Uri.fromFile(new File(path)), "application/vnd.android.package-archive");
                                startActivity(intent);

                                return true;
                            }
                        });
                    }
                })
                .flatMap(new Func1<Boolean, Observable<?>>() {
                    @Override
                    public Observable<?> call(Boolean o) {
                        return updatePackageState();
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        message.setTextColor(Color.RED);
                        message.setText(throwable.getMessage());

                        checkButton.setEnabled(true);
                    }
                })
                .subscribe(new Observer<Object>() {
                    @Override
                    public void onCompleted() {
                        Log.d(TAG, "update.onCompleted");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d(TAG, "onError ", e);
                    }

                    @Override
                    public void onNext(Object o) {
                        Log.d(TAG, "onNext " + o);
                    }
                });


        PackageManager pm = getContext().getPackageManager();
        PackageInfo pi;
        Version v3 = Version.forIntegers(1, 2, 3);

        try {
            pi = pm.getPackageInfo(PACKAGE_LAUNCHER, 0);
//            v3 = Version.valueOf(pi.versionName);
            Log.d(TAG, "Viewer" + pi.versionName);
            Log.d(TAG, "semver" + v3.getMajorVersion() + " " + v3.getMinorVersion() + " " + v3.getPatchVersion());
            now_ver.setText(pi.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            now_ver.setText(R.string.not_installed);
        }


        return view;
    }

    private Observable<Boolean> updatePackageState() {

        return Observable.just(true)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean aBoolean) {
                        now_ver.setEnabled(false);
                        new_ver.setEnabled(false);

                        message.setTextColor(Color.WHITE);
                        message.setText(getString(R.string.checking_updates));

                    }
                })
                .observeOn(Schedulers.io())
                .flatMap(new Func1<Boolean, Observable<Package>>() {
                    @Override
                    public Observable<Package> call(Boolean aBoolean) {
                        return getPackages();
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .scan(false, new Func2<Boolean, Package, Boolean>() {
                    // 최신 버전을 표시한다.
                    @Override
                    public Boolean call(Boolean updateAvailable, Package pkg) {
                        Log.d(TAG, "Latest '" + pkg.name + "' version = " + pkg.latest);

                        Version latest = Version.valueOf(pkg.latest);

                        boolean updated;

                        switch (pkg.id) {
                            case "tdrc":
                                updated = latest.greaterThan(getPackageVersion(PACKAGE_LAUNCHER));
                                break;


                            default:
                                return updateAvailable;
                        }

                        new_ver.setText(pkg.latest);
                        new_ver.setTextColor(updated ? Color.YELLOW : Color.GREEN);

                        return updateAvailable || updated;
                    }
                })
                .last() // 마지막 상태만 취함
                .doOnNext(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean updateAvailable) {
                        Log.d(TAG, "updatePackageState.onNext " + updateAvailable);

                        if (!updateAvailable) {
                            message.setText(getString(R.string.no_updates));
                        } else {
                            message.setText(getString(R.string.updates_available));
                        }
                        updateButton.setEnabled(updateAvailable);
                    }
                })
                .doOnCompleted(new Action0() {
                    @Override
                    public void call() {
                        Log.d(TAG, "updatePackageState.onCompleted");

                        checkButton.setEnabled(true);
                    }
                })
                .doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.d(TAG, "updatePackageState.onError", throwable);

                        message.setText(getString(R.string.connection_error));

                        checkButton.setEnabled(true);
                    }
                })
                ;
    }

    private Observable<Package> getPackages() {
        return mUpdateService.getApi()
                .getChannels()
                .flatMap(new Func1<List<Channel>, Observable<Channel>>() {
                    // 채널 목록을 채널 스트림으로 바꿈
                    @Override
                    public Observable<Channel> call(List<Channel> channels) {
                        return Observable.from(channels);
                    }
                })
                .filter(new Func1<Channel, Boolean>() {
                    // 선택한 채널만 골라냄
                    @Override
                    public Boolean call(Channel channel) {
                        return channel.name.equals(UPDATE_CHANNEL);
                    }
                })
                .flatMap(new Func1<Channel, Observable<Channel>>() {
                    // 상세 정보를 얻는다.
                    @Override
                    public Observable<Channel> call(Channel channel) {
                        return mUpdateService.getApi()
                                .getChannel(channel.name);
                    }
                })
                .doOnNext(new Action1<Channel>() {
                    @Override
                    public void call(Channel channel) {
                        Log.d(TAG, channel.name + " channel selected");

                        for (Package pkg : channel.packages.values()) {
                            Log.d(TAG, " package " + pkg.id);
                        }
                    }
                })
                .flatMap(new Func1<Channel, Observable<Package>>() {
                    // 패키지 목록으로 변환
                    @Override
                    public Observable<Package> call(Channel channel) {
                        return Observable.from(channel.packages.values());
                    }
                });
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();

        int result = Settings.Secure.getInt(getContext().getContentResolver(), Settings.Secure.INSTALL_NON_MARKET_APPS, 0);
        if (result == 0) {
            AlertDialog.Builder buider = new AlertDialog.Builder(getContext());

            buider
                    .setTitle(R.string.install_non_market_apps)
                    .setMessage(R.string.install_non_market_apps_msg);
            buider.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // and may be show application settings dialog manually
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_SECURITY_SETTINGS);
                    startActivity(intent);
                }
            });
           /* Button btn = (Button) findViewById(R.id.delete_button);
            btn.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Intent.ACTION_DELETE);
                    intent.setData(Uri.parse("package:com.sscctv.seeeyesmonitor"));
                    startActivity(intent);
                }
            });*/
            AlertDialog dialog = buider.create();
            dialog.show();
        }

        mIsOnlineSubscription = RxNetwork.stream(getContext()).subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean isOnline) {
                mIsOnline.onNext(isOnline);
            }
        });

    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public boolean versionCheck(Boolean updateAvailable, Package pkg) {
        boolean updated;
        Log.d(TAG, "Latest '" + pkg.name + "' version = " + pkg.latest);

        Version latest = Version.valueOf(pkg.latest);
        updated = latest.greaterThan(getPackageVersion(PACKAGE_LAUNCHER));

        new_ver.setText(pkg.latest);
        return updateAvailable || updated;

    }

    private Version getPackageVersion(String packageName) {
        return Version.valueOf(getPackageVersionString(packageName));

    }

    private String getPackageVersionString(String packageName) {
        try {
            return getContext().getPackageManager().getPackageInfo(packageName, 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "0.0.00";
        }
    }

}
