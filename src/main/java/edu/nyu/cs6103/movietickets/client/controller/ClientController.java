package edu.nyu.cs6103.movietickets.client.controller;

import edu.nyu.cs6103.movietickets.client.SceneManager;
import edu.nyu.cs6103.movietickets.client.Session;
import edu.nyu.cs6103.movietickets.client.SocketClient;

public interface ClientController {
    void configure(SocketClient client, Session session, SceneManager scenes, Object parameter);
}
