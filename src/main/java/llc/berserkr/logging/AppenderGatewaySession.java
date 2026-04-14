package llc.berserkr.logging;

import llc.berserkr.common.payload.auth.BaseAuthenticationProvider;
import llc.berserkr.common.payload.client.AuthenticatingPayloadGateway;
import llc.berserkr.common.payload.connection.SocketClientConnection;
import llc.berserkr.common.payload.exception.CommandException;
import llc.berserkr.common.payload.util.CleanupManager;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.io.PrintStream;
import java.util.UUID;
import java.util.function.Consumer;

public class AppenderGatewaySession extends CleanupManager.CleanupSession {

    private static final String LOG_PREFIX = "[berserkr] ";
    private static final PrintStream log = System.err;

    private static final char BROADCAST = 'B';

    private final String host;
    private final String password;
    private final Consumer<Void> flagback;
    private final String guid;
    private final LaunchAPI launchService;
    private volatile AuthenticatingPayloadGateway gateway;

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

        final OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
        if ("true".equalsIgnoreCase(System.getProperty("org.slf4j.berserkrLogger.insecureTls"))) {
            log.println(LOG_PREFIX + "WARNING: TLS hostname verification is disabled - vulnerable to MITM attacks");
            clientBuilder.hostnameVerifier((hostname, session) -> true);
        }
        final OkHttpClient client = clientBuilder.build();

        final Retrofit retrofit = new Retrofit.Builder()
            .baseUrl("https://" + host + "/chainsawchoker/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build();

        this.launchService = retrofit.create(LaunchAPI.class);


    }
    @Override
    public void start() {

        log.println(LOG_PREFIX + "starting appender gateway session");

        final Call<ChannelResponse> channelResponse = launchService.launchChannel(guid, password);

        try {

            final retrofit2.Response<ChannelResponse> executed = channelResponse.execute();

            log.println(LOG_PREFIX + "channel executed " + executed.isSuccessful() + " " + executed.body());

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

                        log.println(LOG_PREFIX + "channel disconnected");
                        flagback.accept(null);
                    }
                });

                try {

                    log.println(LOG_PREFIX + "channel connecting");
                    gatewayStarting.connect("berserkrLoggingAPIKey");
                    log.println(LOG_PREFIX + "channel connected successful");
                    gatewayStarting.authenticate(gatewayStarting.getProxyGUID(), password, (authenticated) -> {
                        if(authenticated) {
                            log.println(LOG_PREFIX + "channel authenticated successful");
                            this.gateway = gatewayStarting;
                        }
                        else {
                            flagback.accept(null);
                        }
                    });

                } catch (Throwable e) {

                    log.println(LOG_PREFIX + "channel connect failed");
                    e.printStackTrace(log);
                    throw new RuntimeException("channel connect failed", e);
                }

            }
            else {
                throw new RuntimeException("failed to launch channel " + executed.errorBody());
            }
        } catch (Exception e) {
            System.err.println("[berserkr] failed request to launch channel: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("failed request to launch channel", e);
        }

    }

    @Override
    public void destroy() {

        if(this.gateway != null) {
            this.gateway.disconnect();
            this.gateway = null;
        }
    }

    public void sendData(byte[] bytes) throws CommandException {

        final AuthenticatingPayloadGateway myGateway = this.gateway;

        if(myGateway != null) {

            final byte[] payloadData = new byte[bytes.length + 2];

            System.arraycopy(bytes, 0, payloadData, 2, bytes.length);

            final byte[] typeBytes = charToBytes(BROADCAST);

            payloadData[0] = typeBytes[0];
            payloadData[1] = typeBytes[1];

            myGateway.sendAuthenticatedCommand(myGateway.getProxyGUID(), payloadData);
        }

    }

    public static byte[] charToBytes(char ch) {

        // Extract the most significant byte (MSB)
        byte msb = (byte) ((ch >> 8) & 0xFF);

        // Extract the least significant byte (LSB)
        byte lsb = (byte) (ch & 0xFF);

        return new byte[] {msb, lsb};

    }

}
