package liteweb;

import liteweb.http.Request;
import liteweb.http.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServerTest {

    @ParameterizedTest
    @CsvSource({", 8080", "1234, 1234", "8080, 8080"})
    void shouldReturnTrue_whenValid(String value, int port) {
        String[] args =  value == null ? new String[]{} : new String[]{value};
        assertEquals(port, Server.getValidPortParam(args));
    }

    @ParameterizedTest
    @CsvSource({"asda", "0", "65535"})
    void wrongParamThrowException(String value) {
        String[] args = {value};
        ;
        assertThrows(NumberFormatException.class, () -> {
            Server.getValidPortParam(args);
        });
    }

    @Test
    public void handleRequest(){
        // Create a mock input stream with request content
        String requestString = "GET / HTTP/1.1\nHost: localhost:8080\n\n";
        InputStream inputStream = new ByteArrayInputStream(requestString.getBytes());

        // Create a mock output stream to capture response content
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // Create a mock socket with the input and output streams
        Socket socket = new Socket() {
            @Override
            public InputStream getInputStream() {
                return inputStream;
            }

            @Override
            public OutputStream getOutputStream() {
                return outputStream;
            }
        };

        // Call the handle() method with the mocked socket
        Server server = new Server();
        server.handle(socket);

        // Verify that the response was written to the output stream
        String responseString = outputStream.toString();
        Assertions.assertNotNull(responseString);

        // Verify that the socket was closed
        assertEquals(true, socket.isClosed());
    }
}
