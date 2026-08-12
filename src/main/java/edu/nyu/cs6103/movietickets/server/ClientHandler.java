package edu.nyu.cs6103.movietickets.server;

import edu.nyu.cs6103.movietickets.shared.JsonCodec;
import edu.nyu.cs6103.movietickets.shared.NetworkResponse;
import edu.nyu.cs6103.movietickets.shared.dto.ErrorResponse;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

/** Handles all newline-delimited requests for one connected client. */
public final class ClientHandler implements Runnable {

    public static final int MAX_MESSAGE_CHARACTERS = 1_048_576;
    private static final System.Logger LOGGER =
            System.getLogger(ClientHandler.class.getName());

    private final Socket socket;
    private final RequestRouter requestRouter;
    private final JsonCodec codec;

    public ClientHandler(Socket socket, RequestRouter requestRouter, JsonCodec codec) {
        this.socket = Objects.requireNonNull(socket, "socket must not be null");
        this.requestRouter = Objects.requireNonNull(requestRouter, "requestRouter must not be null");
        this.codec = Objects.requireNonNull(codec, "codec must not be null");
    }

    @Override
    public void run() {
        String client = String.valueOf(socket.getRemoteSocketAddress());
        try (socket;
             BufferedReader reader = new BufferedReader(new InputStreamReader(
                     socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                     socket.getOutputStream(), StandardCharsets.UTF_8))) {

            String message;
            while ((message = readLimitedLine(reader)) != null) {
                if (message.isBlank()) {
                    write(writer, codec.error(
                            "invalid-" + UUID.randomUUID(),
                            new ErrorResponse("INVALID_REQUEST", "Request line must not be blank")));
                    continue;
                }
                write(writer, requestRouter.handle(message));
            }
        } catch (SocketTimeoutException exception) {
            LOGGER.log(System.Logger.Level.DEBUG, "Client connection timed out: " + client);
        } catch (MessageTooLargeException exception) {
            LOGGER.log(System.Logger.Level.WARNING, "Oversized request rejected from " + client);
        } catch (IOException exception) {
            LOGGER.log(System.Logger.Level.DEBUG, "Client disconnected: " + client, exception);
        } catch (RuntimeException exception) {
            LOGGER.log(System.Logger.Level.ERROR, "Client handler failed: " + client, exception);
        }
    }

    private void write(BufferedWriter writer, NetworkResponse response) throws IOException {
        writer.write(codec.encode(response));
        writer.newLine();
        writer.flush();
    }

    private static String readLimitedLine(BufferedReader reader) throws IOException {
        StringBuilder line = new StringBuilder();
        int character;
        while ((character = reader.read()) != -1) {
            if (character == '\n') {
                break;
            }
            if (character == '\r') {
                continue;
            }
            if (line.length() >= MAX_MESSAGE_CHARACTERS) {
                throw new MessageTooLargeException();
            }
            line.append((char) character);
        }
        if (character == -1 && line.isEmpty()) {
            return null;
        }
        return line.toString();
    }

    private static final class MessageTooLargeException extends IOException {
    }
}
