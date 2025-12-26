package llc.berserkr.logging;

import llc.berserkr.common.payload.auth.BaseAuthenticationProvider;
import llc.berserkr.common.payload.client.AuthenticatingPayloadGateway;
import llc.berserkr.common.payload.connection.SocketClientConnection;
import llc.berserkr.common.payload.exception.CommandException;
import llc.berserkr.common.payload.exception.ProxyException;
import llc.berserkr.common.payload.util.CleanupManager;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.io.IOException;
import java.util.UUID;
import java.util.function.Consumer;

import static llc.ufwa.util.DataUtils.charToBytes;

public class AppenderGatewaySession extends CleanupManager.CleanupSession {

    //this feels wrong in a log4j project
    private static final Logger logger = LoggerFactory.getLogger(AppenderGatewaySession.class);

    private static final char BROADCAST = 'B';

    private final String host;
    private final String password;
    private final Consumer<Void> flagback;
    private final String guid;
    private final LaunchAPI launchService;
    private AuthenticatingPayloadGateway gateway;

    public AppenderGatewaySession(
        final String host,
        final String guid,
        final String password,
        final Consumer<Void> flagback
    ) {

        this.guid = guid;
        this.host = host;
        this.password = password;
        this.flagback = flagback;

        final OkHttpClient client = new OkHttpClient.Builder()
            .hostnameVerifier((hostname, session) -> true)
            .build();

        final Retrofit retrofit = new Retrofit.Builder()
            .baseUrl("https://" + host + "/chainsawchoker/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build();

        this.launchService = retrofit.create(LaunchAPI.class);


    }
    @Override
    public void start() {

        logger.info("starting appender gateway session");

        final Call<ChannelResponse> channelResponse = launchService.launchChannel(guid, password);

        try {

            final retrofit2.Response<ChannelResponse> executed = channelResponse.execute();

            logger.info("channel executed " + executed.isSuccessful() + " " + executed.body());

            if (executed.isSuccessful() && executed.body() != null) {

                final ChannelResponse channelRespone = executed.body();

                final AuthenticatingPayloadGateway gatewayStarting = new AuthenticatingPayloadGateway(
                    UUID.randomUUID().toString(),
                    new SocketClientConnection(host, channelRespone.getPort()),
                    new BaseAuthenticationProvider() {
                        @Override
                        public String getStoredPassword() {
                            return password;
                        }
                    }
                );
                gatewayStarting.addConnectionConsumer(connected -> {
                    if (!connected) {

                        logger.info("channel disconnected");
                        flagback.accept(null);
                    }
                });

                try {

                    logger.info("channel connecting");
                    gatewayStarting.connect();
                    logger.info("channel connected successful");
                    gatewayStarting.authenticate(gatewayStarting.getProxyGUID(), password, (authenticated) -> {
                        if(authenticated) {
                            logger.info("channel authenticated successful");
                            this.gateway = gatewayStarting;
                        }
                        else {
                            flagback.accept(null);
                        }
                    });

                } catch (ProxyException | CommandException e) {

                    logger.error("channel connect failed", e);
                    throw new RuntimeException("channel connect failed", e);
                }

            }
            else {
                throw new RuntimeException("failed to launch channel " + executed.errorBody());
            }
        } catch (IOException e) {
            logger.error("failed request to launch channel " + e.getMessage(), e);
            throw new RuntimeException("failed request to launch channel " + e.getMessage());
        }

    }

    @Override
    public void destroy() {

        if(this.gateway != null) {
            this.gateway.disconnect();
            this.gateway = null;
        }
    }

    public void sendData(byte[] bytes) {

        final AuthenticatingPayloadGateway myGateway = this.gateway;

        if(myGateway != null) {

            final byte[] payloadData = new byte[bytes.length + 2];

            System.arraycopy(bytes, 0, payloadData, 2, bytes.length);

            final byte[] typeBytes = charToBytes(BROADCAST);

            payloadData[0] = typeBytes[0];
            payloadData[1] = typeBytes[1];

            myGateway.sendCommand(myGateway.getProxyGUID(), payloadData);
        }

    }
}
