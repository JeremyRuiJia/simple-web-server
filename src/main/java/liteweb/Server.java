package liteweb;

import liteweb.http.Request;
import liteweb.http.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Server {

    private static final Logger log = LogManager.getLogger(Server.class);
    private static final int DEFAULT_PORT = 8080;
    private static final int THREAD_NUM = 20;
    private static final int BACKLOG_COUNT = 60;
    private static Map<String, Response> cache = new ConcurrentHashMap<>(3);

    public static void main(String[] args) throws IOException, InterruptedException {
        new Server().startListen(getValidPortParam(args));
    }

    public void startListen(int port) throws IOException, InterruptedException {
        Executor executor = Executors.newFixedThreadPool(THREAD_NUM);
        try (ServerSocket serverSocket = new ServerSocket(port, BACKLOG_COUNT)) {
            log.info("Web server listening on port %d (press CTRL-C to quit)", port);
            while (true) {
                TimeUnit.MILLISECONDS.sleep(1);
                Socket clientSocket = serverSocket.accept();
                executor.execute(() -> {
                    handle(clientSocket);
                });
            }
        }
    }

    public static void handle(Socket clientSocket) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            List<String> requestContent = new ArrayList<>();
            String temp = reader.readLine();
            while (temp != null && temp.length() > 0) {
                requestContent.add(temp);
                temp = reader.readLine();
            }
            Request req = new Request(requestContent);
            String uri = req.getUri();
            if (cache.containsKey(uri)) {
                // If response is in cache, write it directly to output stream
                Response cachedRes = cache.get(uri);
                cachedRes.write(clientSocket.getOutputStream());
            } else {
                // Generate a new response and add it to the cache if it's smaller than 1MB
                Response res = new Response(req);
                if (res.getContentLength() < 1048576) { // Only cache responses smaller than 1MB
                    cache.put(uri, res);
                    // If the cache has more than 3 entries after adding the new response,
                    // remove the least recently used entry
                    if (cache.size() > 3) {
                        Iterator<Map.Entry<String, Response>> iterator = cache.entrySet().iterator();
                        iterator.next(); // skip the first (oldest) entry
                        iterator.remove();
                    }
                }
                res.write(clientSocket.getOutputStream());
            }
        } catch (IOException e) {
            log.error("IO Error", e);
        } finally {
            try { clientSocket.close(); } catch (IOException e) {}
        }
    }

    /**
     * Parse command line arguments (string[] args) for valid port number
     *
     * @return int valid port number or default value (8080)
     */
    static int getValidPortParam(String[] args) throws NumberFormatException {
        if (args.length > 0) {
            int port = Integer.parseInt(args[0]);
            if (port > 0 && port < 65535) {
                return port;
            } else {
                throw new NumberFormatException("Invalid port! Port value is a number between 0 and 65535");
            }
        }
        return DEFAULT_PORT;
    }
}
