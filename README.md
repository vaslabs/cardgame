# cardgame
WIP: A generic purpose online card game


## Setting up

First, check if you have Java install on your laptop.

### Starting the server

1. Download the release from https://github.com/vaslabs/cardgame/releases/tag/1.0
2. `unzip service-1.0` and change directory to service-1.0 `cd service-1.0`
You should be able to see 3 directories (bin, lib and decks)
3. Start the service with ./bin/service

You should see a message like that:
```
11:33:00.568 [CardGame-akka.actor.default-dispatcher-3] WARN cardgame.Guardian$ - Admin token is 642293d4-fd03-4249-9619-ea3f110932ec
```

That is the admin token that you can use to create games.

### Creating a game
Now that you have the admin token you can create a game. That means that players will be able to join your game.

To create a game use the admin token in authorization. Example:

```bash
curl  'http://localhost:8080/games-admin' \
-H 'Accept: text/plain' \
-H 'Authorization: Bearer 642293d4-fd03-4249-9619-ea3f110932ec' \
--data-binary '{}' \
-X POST
```
And you'll get a game id. Example:
`b9da8c63-ef9b-4b2d-9269-b0ed2f4dbaee`

Now players can join your game.


### Joining game

It's very easy to join a game locally. The hard part comes when you want to play with players remotely.
What you'll need:
1. The game id
2. A username
3. The server location that runs your game.

For example, locally you can do:

```bash
curl 'http://localhost:8080/game/b9da8c63-ef9b-4b2d-9269-b0ed2f4dbaee/join?username=vaslabs' --data-binary '{}'
```

Now, how can someone join your game that you are running on your laptop?
The easiest thing to use is `ngrok` which will create a reverse proxy process on your laptop and
give you a public link to share with your friends.

When you install ngrok do
```bash
./ngrok http 8080
```
This will give you an http and https link.
Give your friends the https ngrok link to replace the localhost:8080 with.

The guide ends here. You need a few more things to play:
- A front end (easier than running some command line curl commands).
- Somewhere to host your images so you don't run out of ngrok connections.


I'm writing the guide for the front end soon, watch this space
