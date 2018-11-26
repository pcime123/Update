package app_update.sscctv.com.app_update.services;

import app_update.sscctv.com.app_update.models.Channel;
import app_update.sscctv.com.app_update.models.Release;

import java.util.List;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.GsonConverterFactory;
import retrofit2.Retrofit;
import retrofit2.RxJavaCallAdapterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Streaming;
import rx.Observable;

/**
 * Created by trlim on 2016. 2. 4..
 *
 * 업데이트 REST 서비스
 */
public class UpdateService {

//    private static final String UPDATE_SERVER_URL = "http://192.168.0.34:4802/";
    private static final String UPDATE_SERVER_URL = "http://sscctv.com";

    private UpdateApi mUpdateApi;

    public UpdateService() {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();

        Retrofit retrofit = new Retrofit.Builder()
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(UPDATE_SERVER_URL)
                .client(client)
                .build();

        mUpdateApi = retrofit.create(UpdateApi.class);
    }

    public UpdateApi getApi() {
        return mUpdateApi;
    }

    public interface UpdateApi {
        @GET("/update_new/channels.json")
        Observable<List<Channel>>
            getChannels();

        @GET("/update_new/channels/{name}.json")
        Observable<Channel>
            getChannel(@Path("name") String channelName);

        @GET("/update_new/releases/{package}.json")
        Observable<Map<String, Release>>
            getReleases(@Path("package") String packageId);

        @GET("/update_new/releases/{releaseUrl}")
        @Streaming
        Observable<ResponseBody>
            download(@Path(value = "releaseUrl") String releaseUrl);
    }
}
