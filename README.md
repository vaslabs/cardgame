
# cardgame

![Test](https://github.com/vaslabs/cardgame/workflows/Scala%20CI/badge.svg)
 [![Codacy Badge](https://api.codacy.com/project/badge/Grade/ab2e989546574b1abff42ab2e2f4f5b6)](https://app.codacy.com/manual/vaslabs/cardgame?utm_source=github.com&utm_medium=referral&utm_content=vaslabs/cardgame&utm_campaign=Badge_Grade_Dashboard)

A singledeck multi-player cardgame simulator


## Setting up

First, check if you have Java installed on your computer.

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


## Using the front end

The UI is available [here](https://github.com/vaslabs/cardgame-js) .

Start the angular app with
```bash
ng serve
```
Visit from your browser [http://localhost:4200](http://localhost:4200) .

You can host this application with gh-pages or you can use my version [here](https://cardgame.vaslabs.io/board)

Note that this is just a front end, it doesn't serve any images , it just gives a game board view and allows you to do the in-game actions.

In order to join a game add the game id, a username and the server (if you are running the server locally, http://localhost:8080 will work for you, otherwise if you are using ngrok give to your friends the ngrok url).

### Autojoin
Once a game has been created you can use this link to help your friends join more easily:
`http://localhost:4200/setup?game-id=b9da8c63-ef9b-4b2d-9269-b0ed2f4dbaee&server=https://0000000.ngrok.io`
(The server is just an example, replace with your own publicly available hostname).

You will see the players list being populated as more of your friends join.

### Starting the game
Now that you are ready you can start a game. 

To start a game you need:
1. A *deck*
2. The game id
3. A location for serving the images from your deck
4. Your admin token

There's already a premade deck [provided](https://github.com/vaslabs/cardgame/tree/master/sample_decks/ee61823d-3128-4c30-b0e7-8f2d0074da8a).

So let's start with that.

To start the game with the sample deck provided you can do:
```bash
curl --location --request PATCH 'localhost:8080/games-admin?game=514b2598-ac38-4a0e-889b-2e3e2505fb55&deck=ee61823d-3128-4c30-b0e7-8f2d0074da8a&server=https://vaslabs.github.io/cardgame-cdn' \
--header 'Authorization: Bearer 642293d4-fd03-4249-9619-ea3f110932ec'
```

This will start a game with the sample deck, shuffle and give 7 cards to each player and select the first player to start.

The player in green highlight starts first. In order for other players to do any actions, you need to click the `select next` attached next to their username.

### Create your own deck
In order to create your own deck you need:
- Images in jpg format.
- A configuration

#### Configuration

The configuration is a json file that has this information:
1. How many copies of each card exist
2. How the game starts

For example, if you have card hero.jpg and you want to give to each player at least one hero you can do it in the [starting section](https://github.com/vaslabs/cardgame/blob/master/sample_decks/ee61823d-3128-4c30-b0e7-8f2d0074da8a/deck.json#L45)
```json
"startingRules": {
    "exactlyOne": ["hero"],
    "no": [],
    "hand": 5
}
```

Once you click start, the shuffling rules will apply. This game is not build for complex logic it only gives the primitives
for game creation. You can improvise to play more games and even combine with other online platforms such as online drawing tools and video calls to play more board games.


## Supported actions

- Game admin creating games
- Multiple players can join the game
- Game admin starts the game with a deck and starting rules (initial player hand, initial restrictions)
- Actions supported:
    * Draw cards (from top and bottom)
    * Borrow cards from deck (must be returned by end of turn)
    * Players switching turns
    * Stealing cards from each other
    * Throwing cards to discard pile
    * Retrieving cards from discard pile
    * Throwing dice
    * Shuffling hand
    * Returning a card back to deck at any position, face up or face down
    * Players can leave the game if they have an empty hand
    * Players can switch direction of play (just indicative)

- Actions are done through http
- Results are retrieve via a server sent event stream
- If clients get out of sync they can retrieve the whole game state  
