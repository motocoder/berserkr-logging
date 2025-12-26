package llc.berserkr.logging;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface LaunchAPI {
    @GET("channel")
    Call<ChannelResponse> launchChannel(@Query("channel") String channel, @Query("password") String password);
}
