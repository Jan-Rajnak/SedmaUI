import java.util.Scanner;

public class ConsoleTextFormatter {
    public static void main(String[] args) {
        Scanner scaner = new Scanner(System.in);
        String input = scaner.nextLine();
        String[] lines = input.split("\n");
        int count = 0;
        for (String line : lines) {
            if (line.contains("played:")) {
                count++;
            }
        }
        System.out.println("Number of cards played: " + count);

    }
}
