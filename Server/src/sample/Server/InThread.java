package sample.Server;

import java.io.IOException;
import java.net.ServerSocket;


class InThread extends Thread
{
    ServerSocket serverSocket;
    public InThread(ServerSocket serverSocket) {this.serverSocket=serverSocket;}



    @Override
    public void run() {

        while (!this.isInterrupted()) {
            InClientHandler inClientHandler = null;

            try {
                inClientHandler= new InClientHandler(serverSocket.accept());
                inClientHandler.start();
            } catch (IllegalStateException|IllegalArgumentException | IOException | SecurityException e) {
                if (this.isInterrupted()){  break;}
                System.out.println("Error accepting new connection: " + e.getMessage());
            }

        }
    }



}

