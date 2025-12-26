package llc.berserkr.logging;

public class ChannelResponse {

    private int port;

    private ChannelResponse() {}

    public ChannelResponse(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }
}
