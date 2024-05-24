import java.util.*;

public class Game {


    // ANSI color codes for the console text
    // Grey  for settings, green for cards played, yellow for questions, white for game status
    public static final String ANSI_RESET = "\u001B[0m"; // Resetting to default color
    public static final String ANSI_RED = "\u001B[31m"; // Error messages
    public static final String ANSI_GREEN = "\u001B[32m"; // Game info messages
    public static final String ANSI_YELLOW = "\u001B[33m"; // Player questions and answers messages
    public static final String ANSI_BLUE = "\u001B[34m"; // Game settings questions and answers messages
    // All the variables used in the game
    public static int playerCount;
    public static int botCount;
    public static List<Player> players = new ArrayList<>();
    public static List<Card> cardsLeft = new ArrayList<>();
    public static int startingPlayerIndex;
    public static int roundsDone = 0;
    public static int turnsDone = 0;
    public static int numberOfAllCards;
    public static List<Card> cardsOnTheTable = new ArrayList<>();
    public static int currentPlayerIndex;
    public static int cardsPlayed = 0;
    static Scanner scanner = new Scanner(System.in);

    // Method to generate all possible cards based on the number of players
    public static List<Card> generateAllCards(int numOfPlayers) {
        List<Card> cards = new ArrayList<>();
        for (Suit suit : Suit.values()) {
            for (int value = 1; value <= 8; value++) {
                cards.add(new Card(suit, value));
            }
        }

        // Remove cards based on the number of players
        if (numOfPlayers == 3) {
            cards.remove(new Card(Suit.ACORNS, 2));
            cards.remove(new Card(Suit.HEARTS, 2));
        }

        return cards;
    }

    // Method to generate small amount of cards for testing+
    public static List<Card> generateTestCards() {
        List<Card> cards = new ArrayList<>();
        for (Suit suit : Suit.values()) {
            for (int value = 1; value <= 2; value++) {
                cards.add(new Card(suit, value));
            }
        }
        return cards;
    }

    // Method to deal cards to a player
    public static void dealCards(List<Card> cardsLeft) {
        Random random = new Random();
        if (cardsLeft.size() >= players.size()) {
            for (Player player : players) {
                // Remove null values from the player's hand
                player.hand = Arrays.stream(player.hand).filter(Objects::nonNull).toArray(Card[]::new);
                int cardsToDeal = 4 - player.hand.length; // Calculate how many cards need to be dealt
                for (int i = 0; i < cardsToDeal; i++) {
                    int randomIndex = random.nextInt(cardsLeft.size());
                    Card[] newHand = new Card[player.hand.length + 1];
                    System.arraycopy(player.hand, 0, newHand, 0, player.hand.length);
                    newHand[newHand.length - 1] = cardsLeft.get(randomIndex);
                    player.hand = newHand;
                    cardsLeft.remove(randomIndex);
                }
            }
        }

    }

    // Method to count grabbed cards for all players
    public static int countGrabbedCards(List<Player> players) {
        int grabbedCards = 0;
        for (Player player : players) {
            grabbedCards += player.grabbedCards.size();
        }
        return grabbedCards;
    }

    // Method to check if the starting player can fight for the cards on the table
    public static boolean canFight(List<Card> cardsOnTable, Player player) {
        boolean canFight = false;

        Card firstCard = cardsOnTable.getFirst();
        for (Card card : player.hand) {
            if (!canFight) canFight = card.fights(firstCard);
        }

        return canFight;
    }

    // Method to choose all the fighting cards
    public static List<Card> chooseFightingCards(Player player, List<Card> cardsOnTable) {
        List<Card> fightingCards = new ArrayList<>();
        for (Card card : player.hand) {
            if (card.fights(cardsOnTable.getFirst())) {
                fightingCards.add(card);
            }
        }
        return fightingCards;
    }

    // Method to check if someone fought for the cards on the table
    public static boolean someoneFoughtForCards(List<Card> cardsOnTable, List<Player> players) {
        Card firstCard = cardsOnTable.getFirst();
        for (Card card : cardsOnTable.subList(cardsOnTable.size()-players.size()+1, cardsOnTable.size())) {
            if (card.fights(firstCard)) return true;
        }

        return false;
    }

    // Method to check who fought for the cards on the table
    public static int fightingPlayerIndex(List<Card> cardsOnTable, List<Player> players, int startingPlayerIndex) {
        List <Player> rotatedPlayers = players;
        Collections.rotate(rotatedPlayers, startingPlayerIndex);
        int fightingPlayerIndex = startingPlayerIndex; // Default value is the starting player

        for (Player player : rotatedPlayers) {
            if (cardsOnTable.getFirst().value == cardsOnTable.get(rotatedPlayers.indexOf(player)).value || cardsOnTable.get(rotatedPlayers.indexOf(player)).value == 1) {
                fightingPlayerIndex = rotatedPlayers.indexOf(player);
                break;
            }
        }

        for (int i = startingPlayerIndex; i < cardsOnTable.size() + startingPlayerIndex; i++) {
            if (cardsOnTable.getFirst().value == cardsOnTable.get(i - startingPlayerIndex).value || cardsOnTable.get(i - startingPlayerIndex).value == 1) {
                fightingPlayerIndex = i;
            }
        }

        fightingPlayerIndex %= players.size();

        return players.indexOf(rotatedPlayers.get(fightingPlayerIndex));
    }

    // Initializing the game
    public static void initGame() {
        Player initPlayer = new Player("Init");
        // Ask for the number of players and bots
        playerCount = initPlayer.askQuestion(ANSI_BLUE + "Enter the number of players (1-4): " + ANSI_RESET, Integer.class);
        botCount = initPlayer.askQuestion(ANSI_BLUE + "Enter the number of bots (0-" + (4 - playerCount) + "): " + ANSI_RESET, Integer.class);

        // Generate all possible cards
        // cardsLeft = generateTestCards();
        cardsLeft = generateAllCards(playerCount + botCount);
        numberOfAllCards = cardsLeft.size();

        // Shuffle the cards
        Collections.shuffle(cardsLeft);

        // Create the players and ask for their names
        for (int i = 0; i < playerCount; i++) {
            players.add(new Player(initPlayer.askQuestion(ANSI_BLUE + "Enter the name of player " + (i + 1) + ": " + ANSI_RESET, String.class)));
        }
        for (int i = 0; i < botCount; i++) {
            players.add(new BotPlayer(i+1));
        }

        // Shuffle the players
        Collections.shuffle(players);

        // Deal cards to the players
        dealCards(cardsLeft);

        // Start the self-calling turn method
        turn();

        // Finish the game
        finishGame();
    }

    // Method for finishing the game
    public static void finishGame() {
        // Count the points for each player
        for (Player player : players) {
            player.points = player.countPoints();
        }

        // Sort the players by grabbed cards
        players.sort(Comparator.comparingInt(player -> -player.grabbedCards.size()));

        // Sort the players by points
        players.sort(Comparator.comparingInt(player -> -player.points));

        // Print the final results
        System.out.println(ANSI_GREEN + "Final results:" + ANSI_RESET);
        for (Player player : players) {
            System.out.println(player.name + ": " + player.points + " points, " + player.grabbedCards.size() + " grabbed cards");
        }

        System.out.println("Number of cards played: " + cardsPlayed);

    }

    // Self-calling method for the turns
    static void turn() {
        // Check if the game is over
        if (countGrabbedCards(players) == numberOfAllCards) {
            System.out.println(ANSI_GREEN + "Game over!" + ANSI_RESET);
        } else {

            // Print info about the current round
            System.out.println(ANSI_GREEN + "Round " + (roundsDone+1) + ", turn " + (turnsDone+1) + ANSI_RESET);

            if (cardsOnTheTable.isEmpty()){
                dealCards(cardsLeft);
                System.out.println(ANSI_GREEN + players.get(startingPlayerIndex).name + "'s turn" + ANSI_RESET);
                players.get(startingPlayerIndex).askForCard("Choose a card to play", cardsOnTheTable, false);
            } else {
                System.out.println(ANSI_GREEN + players.get(startingPlayerIndex).name + "'s turn" + ANSI_RESET);
                players.get(startingPlayerIndex).askForCard("Choose a card to play", cardsOnTheTable, true);
            }

            // Run the playerTurn() method for all other players
            playerTurn(startingPlayerIndex+1);
            turnsDone++;

            // Check if the starting player can fight for the cards on the table
            boolean startingPlayerCanFight = canFight(cardsOnTheTable, players.get(startingPlayerIndex));
            // Check if someone fought for the cards on the table
            boolean someoneFought = someoneFoughtForCards(cardsOnTheTable, players);


            if (startingPlayerCanFight){
                if (someoneFought){
                    // variable for storing player's decision to fight for the cards on the table
                    boolean wannaFight = players.get(startingPlayerIndex).askQuestion(ANSI_GREEN + "Someone fought for the cards on the table. \nDo you want to fight for the cards on the table? (y/n)", Boolean.class);
                    if (!wannaFight){
                        startingPlayerIndex = fightingPlayerIndex(cardsOnTheTable, players, startingPlayerIndex);
                        // Add the cards from the table to the winning player
                        players.get(startingPlayerIndex).grabbedCards.addAll(cardsOnTheTable);
                        cardsOnTheTable.clear();
                        roundsDone++;
                        turnsDone = 0;
                    }
                } else {
                    // variable for storing player's decision to fight for the cards on the table
                    boolean wannaFight = players.get(startingPlayerIndex).askQuestion(ANSI_GREEN + "No one fought for the cards on the table. \nDo you want to to continue by fighting for the cards on the table? (y/n)", Boolean.class);
                    if (!wannaFight){
                        startingPlayerIndex = fightingPlayerIndex(cardsOnTheTable, players, startingPlayerIndex);
                        // Add the cards from the table to the winning player
                        players.get(startingPlayerIndex).grabbedCards.addAll(cardsOnTheTable);
                        cardsOnTheTable.clear();
                        roundsDone++;
                        turnsDone = 0;
                    }
                }
            } else {
                System.out.print(ANSI_GREEN + "The starting player can't fight for the cards on the table but ");
                startingPlayerIndex = fightingPlayerIndex(cardsOnTheTable, players, startingPlayerIndex);
                if (someoneFought) System.out.print(players.get(startingPlayerIndex).name + " fought for the cards on the table and won them. \n" + ANSI_RESET);
                else System.out.print(" no one else fought so " + players.get(startingPlayerIndex).name + " won them. \n" + ANSI_RESET);
                // Add the cards from the table to the winning player
                players.get(startingPlayerIndex).grabbedCards.addAll(cardsOnTheTable);
                cardsOnTheTable.clear();
                roundsDone++;
                turnsDone = 0;
            }

            turn();
        }
    }

    // Method for turns for individual players
    public static void playerTurn(int playerIndex){
        // Check whether the playerIndex is out of bounds, if it is, set it to 0
        if (playerIndex >= players.size()){
            playerIndex = 0;
        }

        // Set the current player
        currentPlayerIndex = playerIndex;
        Player currentPlayer = players.get(currentPlayerIndex);

        // Play the turn
        System.out.println(ANSI_GREEN + currentPlayer.name + "'s turn" + ANSI_RESET);
        currentPlayer.askForCard("Choose a card to play", cardsOnTheTable, false);

        // Only continue playerTurn for the next player if the current player is not the last one from the round
        int endingPlayerIndex = startingPlayerIndex-1;
        if (endingPlayerIndex < 0){
            endingPlayerIndex = players.size()-1;
        }
        if (playerIndex != endingPlayerIndex){
            playerTurn(playerIndex+1);
        }
    }

    public static void main(String[] args) {
        initGame();
    }

    // Enum for the suits of the cards
    public enum Suit {
        ACORNS, LEAVES, HEARTS, BELLS
    }

    // Card class with suit and value
    public static class Card {
        private static final String[] VALUES = {"seven", "eight", "nine", "ten", "jack", "queen", "king", "ace"};
        private static final Map<String, Integer> VALUE_MAP = new HashMap<>();

        static {
            for (int i = 0; i < VALUES.length; i++) {
                VALUE_MAP.put(VALUES[i], i + 1);
            }
        }

        Suit suit;
        int value;

        public Card(Suit suit, int value) {
            this.suit = suit;
            this.value = value;
        }

        public static Card fromString(String str) {
            String[] parts = str.split(" of ");
            int value = VALUE_MAP.get(parts[0].toLowerCase());
            Suit suit = Suit.valueOf(parts[1].toUpperCase());
            return new Card(suit, value);
        }

        @Override
        public String toString() {
            return VALUES[value - 1] + " of " + suit.name().toLowerCase();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Card card = (Card) o;
            return value == card.value && suit == card.suit;
        }

        @Override
        public int hashCode() {
            return Objects.hash(suit, value);
        }

        public boolean fights(Card firstCard){
            return this.value == 1 || this.value == firstCard.value;
        }
    }

    // Player class with hand and grabbedCards
    public static class Player {
        Card[] hand = new Card[4];
        List<Card> grabbedCards = new ArrayList<>();
        String name;
        int points;

        // Constructor for player name
        Player(String name) {
            this.name = name;
        }

        // Method for asking questions with various outputs (int, String, boolean)
        public <T> T askQuestion(String question, Class<T> type) {
            while(true) {
                System.out.println(question);
                String response = scanner.nextLine();
                try {
                    if (type == Integer.class) {
                        return type.cast(Integer.parseInt(response));
                    } else if (type == String.class) {
                        return type.cast(response);
                    } else if (type == Boolean.class) {
                        return type.cast(response.equalsIgnoreCase("y") || response.equalsIgnoreCase("yes"));
                    } else if (type == Card.class) {
                        return type.cast(Card.fromString(response));
                    } else {
                        throw new IllegalArgumentException("Unsupported type " + type);

                    }
                } catch (Exception e) {
                    System.out.println(ANSI_RED + "Invalid input, try again" + ANSI_RESET);
                }
            }
        }

        // Method to count points for a player
        public int countPoints() {
            int points = 0;
            for (Card card : grabbedCards) {
                if (card.value == 8 || card.value == 4) { // 4 for ten, 8 for ace
                    points++;
                }
            }
            return points;
        }

        // Override toString method for player
        @Override
        public String toString() {
            return "Player{" +
                    "hand=" + Arrays.toString(hand) +
                    ", grabbedCards=" + grabbedCards +
                    '}';
        }

        // Method to ask the player for a card to play, plus show the player's hand, then adding the card to table and remove from hand
        public void askForCard(String prompt, List<Card> cardsOnTheTable, boolean fight) {
            Card card;
            List<Card> cardsToChooseFrom = new ArrayList<>();
            if (fight) {
                cardsToChooseFrom = chooseFightingCards(this, cardsOnTheTable);
            } else {
                cardsToChooseFrom = Arrays.asList(hand);
            }
            while (true) {
                try {
                    System.out.println(prompt);
                    // Print the player's hand with numbers representing the cards
                    for (int i = 0; i < cardsToChooseFrom.size(); i++) {
                        System.out.print((i + 1) + ": " + cardsToChooseFrom.get(i));
                        if (i < cardsToChooseFrom.size() - 1) System.out.print(", ");
                        else System.out.print("\n");
                    }
                    String input = new Scanner(System.in).nextLine();
                    if (input.matches("\\d+")) { // Check if the input is a number
                        int cardNumber = Integer.parseInt(input);
                        if (cardNumber >= 1 && cardNumber <= cardsToChooseFrom.size()) {
                            card = cardsToChooseFrom.get(cardNumber - 1); // Translate the number into a Card
                        } else {
                            throw new IllegalArgumentException(ANSI_RED + "Invalid card number" + ANSI_RESET);
                        }
                    } else {
                        card = Card.fromString(input);
                    }
                    // Check if the card is in the player's hand
                    if (cardsToChooseFrom.contains(card)) {
                        break;
                    } else {
                        System.out.println(ANSI_RED + "You don't have that card in your hand, try again" + ANSI_RESET);
                    }
                } catch (Exception e) {
                    System.out.println(ANSI_RED + "Invalid input, try again" + ANSI_RESET);
                }
            }
            // Print the played card
            System.out.println(ANSI_YELLOW + "You played: " + card + ANSI_RESET);

            // Remove the card from the player's hand and add it to the table
            Card finalCard = card;
            hand = Arrays.stream(hand).filter(c -> !c.equals(finalCard)).toArray(Card[]::new);
            cardsOnTheTable.add(card);

            cardsPlayed++;
        }

    }

    // Class for the bot
    public static class BotPlayer extends Player {
        private static final Random random = new Random();

        // Constructor for the bot
        public BotPlayer(int number) {
            super("Bot" + number);
        }

        // Method to ask questions with a different implementation for the bot
        @Override
        public <T> T askQuestion(String question, Class<T> type) {
            if (type == Boolean.class) {
                T ans = type.cast(random.nextBoolean());
                if (question.contains("fight")) {
                    if (ans.equals(true)){
                        System.out.println(ANSI_YELLOW + this.name + " fought for the cards on the table" + ANSI_RESET);
                    } else {
                        System.out.println(ANSI_YELLOW + this.name + " didn't fight for the cards on the table" + ANSI_RESET);
                    }
                } else {
                    System.out.println(ANSI_YELLOW + this.name + " answer:"  + ANSI_RESET);
                }
                return ans;
            } else {
                System.out.println(ANSI_RED + "Unsupported type for bot (" + type + ") + question:" + question + ANSI_RESET);
                return null;
            }

        }

        @Override
        public void askForCard(String prompt, List<Card> cardsOnTheTable, boolean fight) {
            List<Card> cardsToChooseFrom;
            if (fight) {
                cardsToChooseFrom = chooseFightingCards(this, cardsOnTheTable);
            } else {
                cardsToChooseFrom = Arrays.asList(hand);
            }
            Card card = chooseCard(cardsToChooseFrom);

            // Print the played card
            System.out.println(ANSI_YELLOW + this.name + " played: " + card + ANSI_RESET);

            // Remove the card from the bot's hand and add it to the table
            hand = Arrays.stream(hand).filter(c -> !c.equals(card)).toArray(Card[]::new);
            cardsOnTheTable.add(card);

            cardsPlayed++;
        }

        // Method for the bot to choose a card to play
        public Card chooseCard(List<Card> cardsToChooseFrom) {
            // For now, just choose a random card
            return cardsToChooseFrom.get(random.nextInt(cardsToChooseFrom.size()));
        }
}
}
