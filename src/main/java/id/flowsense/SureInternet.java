package id.flowsense;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class SureInternet {

    public static boolean HasInternet() {
        try (Socket socket = new Socket()) {
            InetSocketAddress address = new InetSocketAddress("1.1.1.1", 53);
            socket.connect(address, 5000);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
