package edu.nyu.cs6103.movietickets.client;

import edu.nyu.cs6103.movietickets.server.config.ServerConfig;
import edu.nyu.cs6103.movietickets.shared.*;
import edu.nyu.cs6103.movietickets.shared.dto.LoginRequest;
import edu.nyu.cs6103.movietickets.shared.dto.LoginResponse;
import edu.nyu.cs6103.movietickets.shared.dto.LogoutRequest;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** Persistent, thread-safe client for the newline-delimited JSON protocol. */
public final class SocketClient implements AutoCloseable {
    private static final int MAX_RESPONSE_CHARS = 1_048_576;

    private final String host;
    private final int port;
    private final int connectTimeoutMillis;
    private final int readTimeoutMillis;
    private final JsonCodec jsonCodec;
    private final Session session;
    private final Object transportLock = new Object();
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;

    public SocketClient(ServerConfig config, Session session) {
        this(config.host(), config.port(), config.socketConnectTimeoutMillis(),
                config.socketReadTimeoutMillis(), new JsonCodec(), session);
    }

    public SocketClient(String host, int port, int connectTimeoutMillis,
                        int readTimeoutMillis, JsonCodec jsonCodec, Session session) {
        this.host = requireNonBlank(host, "host");
        if (port < 1 || port > 65_535) throw new IllegalArgumentException("invalid port");
        if (connectTimeoutMillis <= 0 || readTimeoutMillis <= 0) {
            throw new IllegalArgumentException("socket timeouts must be positive");
        }
        this.port = port;
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
        this.jsonCodec = Objects.requireNonNull(jsonCodec, "jsonCodec must not be null");
        this.session = Objects.requireNonNull(session, "session must not be null");
    }

    public void connect() throws IOException {
        synchronized (transportLock) {
            if (isConnectedUnsafe()) return;
            closeTransportUnsafe();
            Socket newSocket = new Socket();
            try {
                newSocket.connect(new InetSocketAddress(host, port), connectTimeoutMillis);
                newSocket.setSoTimeout(readTimeoutMillis);
                newSocket.setTcpNoDelay(true);
                reader = new BufferedReader(new InputStreamReader(newSocket.getInputStream(), StandardCharsets.UTF_8));
                writer = new BufferedWriter(new OutputStreamWriter(newSocket.getOutputStream(), StandardCharsets.UTF_8));
                socket = newSocket;
            } catch (IOException exception) {
                try { newSocket.close(); } catch (IOException suppressed) { exception.addSuppressed(suppressed); }
                throw exception;
            }
        }
    }

    public boolean isConnected() {
        synchronized (transportLock) { return isConnectedUnsafe(); }
    }

    public Session session() { return session; }

    /** Sends one request and automatically supplies a token when required. */
    public NetworkResponse send(RequestType type, Object data) throws IOException {
        Objects.requireNonNull(type, "type must not be null");
        String token = requiresAuthentication(type) ? session.requireToken() : null;
        NetworkRequest request = jsonCodec.request(type, token, data);
        synchronized (transportLock) {
            ensureConnectedUnsafe();
            try {
                writer.write(jsonCodec.encode(request));
                writer.newLine();
                writer.flush();
                String responseLine = readLimitedLine();
                if (responseLine == null) throw new EOFException("Server closed without a response");
                NetworkResponse response = jsonCodec.decodeResponse(responseLine);
                if (!request.requestId().equals(response.requestId())) {
                    throw new IOException("Response requestId does not match the request");
                }
                return response;
            } catch (IOException | RuntimeException exception) {
                closeTransportUnsafe();
                throw exception;
            }
        }
    }

    public NetworkResponse login(String username, String password) throws IOException {
        NetworkResponse response = send(RequestType.LOGIN, new LoginRequest(username, password));
        if (response.successful()) session.authenticate(jsonCodec.responseDataAs(response, LoginResponse.class));
        return response;
    }

    public NetworkResponse logout() throws IOException {
        NetworkResponse response = send(RequestType.LOGOUT, new LogoutRequest());
        if (response.successful()) session.clear();
        return response;
    }

    public <T> T responseData(NetworkResponse response, Class<T> type) {
        return jsonCodec.responseDataAs(response, type);
    }

    @Override public void close() {
        synchronized (transportLock) { closeTransportUnsafe(); }
    }

    private void ensureConnectedUnsafe() throws IOException {
        if (!isConnectedUnsafe()) throw new IOException("Socket client is not connected");
    }

    private boolean isConnectedUnsafe() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    private String readLimitedLine() throws IOException {
        StringBuilder result = new StringBuilder();
        int character;
        while ((character = reader.read()) != -1) {
            if (character == '\n') break;
            if (character != '\r') {
                if (result.length() >= MAX_RESPONSE_CHARS) throw new IOException("Response is too large");
                result.append((char) character);
            }
        }
        return character == -1 && result.isEmpty() ? null : result.toString();
    }

    private void closeTransportUnsafe() {
        if (socket != null) {
            try { socket.close(); } catch (IOException ignored) { }
        }
        socket = null;
        reader = null;
        writer = null;
    }

    private static boolean requiresAuthentication(RequestType type) {
        return type != RequestType.REGISTER && type != RequestType.LOGIN;
    }

    private static String requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value.trim();
    }
}
